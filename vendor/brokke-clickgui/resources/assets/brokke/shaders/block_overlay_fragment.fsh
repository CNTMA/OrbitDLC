#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 vTexCoord;
layout(location = 0) out vec4 fragColor;

layout(std140) uniform BlockOverlayUniforms {
    vec4 uResolutionTime;
    vec4 uParams;
    vec4 uOutlineColor;
    vec4 uFirstColor;
    vec4 uSecondColor;
};

uniform sampler2D Sampler0;

const float TAU = 6.28318530718;
const int DIRECTIONS = 16;
const int OCTAVES = 5;

float random(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 cell = floor(p);
    vec2 local = fract(p);
    float a = random(cell);
    float b = random(cell + vec2(1.0, 0.0));
    float c = random(cell + vec2(0.0, 1.0));
    float d = random(cell + vec2(1.0, 1.0));
    vec2 smoothLocal = local * local * (3.0 - 2.0 * local);
    return mix(a, b, smoothLocal.x) + (c - a) * smoothLocal.y * (1.0 - smoothLocal.x) + (d - b) * smoothLocal.x * smoothLocal.y;
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
    for (int i = 0; i < OCTAVES; i++) {
        value += amplitude * noise(p);
        p = rot * p * 2.0 + vec2(100.0);
        amplitude *= 0.5;
    }
    return value;
}

vec3 hsvToRgb(vec3 c) {
    vec3 rgb = clamp(abs(mod(c.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
    return c.z * mix(vec3(1.0), rgb, c.y);
}

float sampleMask(vec2 uv) {
    if (uv.x < 0.0 || uv.y < 0.0 || uv.x > 1.0 || uv.y > 1.0) {
        return 0.0;
    }
    return texture(Sampler0, uv).a;
}

vec3 getEffectColor(vec2 uv, vec4 baseColor, vec4 accentColor, float time, int mode, out float energy) {
    vec2 res = uResolutionTime.xy;
    vec2 aspectUv = (uv - 0.5) * vec2(res.x / max(res.y, 1.0), 1.0);

    if (mode == 1) {
        float distortion = fbm(uv * 8.0 + vec2(time * 0.2, -time * 0.15));
        float wave = 0.5 + 0.5 * sin(uv.x * 24.0 + uv.y * 14.0 - time * 3.5 + distortion * 6.0);
        energy = smoothstep(0.1, 0.95, wave);
        return mix(baseColor.rgb * 0.6, accentColor.rgb * 1.25, energy);
    }

    if (mode == 2) {
        float hue = fract(uv.x * 0.8 + uv.y * 0.4 - time * 0.12);
        float shimmer = 0.8 + 0.2 * sin((uv.x - uv.y) * 28.0 + time * 4.5);
        energy = shimmer;
        return hsvToRgb(vec3(hue, 0.8, shimmer));
    }

    if (mode == 3) {
        float scan = 0.5 + 0.5 * sin(gl_FragCoord.y * 0.45 - time * 12.0);
        float bars = smoothstep(0.55, 1.0, scan);
        float glitch = step(0.93, random(vec2(floor(time * 15.0), floor(gl_FragCoord.y / 6.0))));
        energy = clamp(0.35 + bars * 0.5 + glitch * 0.4, 0.0, 1.0);
        return mix(baseColor.rgb * 0.35, accentColor.rgb * (1.0 + glitch * 0.35), energy);
    }

    if (mode == 4) {
        float curtainA = sin(uv.x * 14.0 + time * 1.6 + sin(uv.y * 8.0 - time));
        float curtainB = sin(uv.x * 8.0 - time * 1.2 + fbm(uv * 5.0) * 4.0);
        float aurora = smoothstep(-0.2, 1.0, curtainA * 0.55 + curtainB * 0.45);
        float vertical = 0.6 + 0.4 * sin(uv.y * 10.0 + time * 0.8);
        energy = clamp(aurora * vertical, 0.0, 1.0);
        return mix(baseColor.rgb * 0.3, accentColor.rgb * 1.2, energy);
    }

    if (mode == 5) {
        float plasma = sin(aspectUv.x * 16.0 + time * 2.0)
                     + sin(aspectUv.y * 18.0 - time * 1.8)
                     + sin((aspectUv.x + aspectUv.y) * 14.0 + time * 1.4);
        energy = 0.5 + 0.1666667 * plasma;
        vec3 pCol = mix(baseColor.rgb, accentColor.rgb, energy);
        return clamp(pCol * (0.85 + energy * 0.35), 0.0, 1.0);
    }

    if (mode == 6) {
        vec2 cosmosCoord = aspectUv * 6.0;
        float neb = fbm(cosmosCoord + vec2(time * 0.08, -time * 0.05));
        float nebDetail = fbm(cosmosCoord * 2.0 - vec2(time * 0.12));
        float starNoise = random(floor(cosmosCoord * 12.0) + vec2(12.3, 45.6));
        float star = step(0.965, starNoise) * (0.5 + 0.5 * sin(time * 6.0 + starNoise * 40.0));
        energy = clamp(neb * 0.7 + nebDetail * 0.3 + star * 0.8, 0.0, 1.0);
        vec3 col = mix(baseColor.rgb * 0.4, accentColor.rgb * 1.2, neb);
        col += vec3(star);
        return clamp(col, 0.0, 1.0);
    }

    if (mode == 7) {
        vec2 grid = abs(fract(aspectUv * 20.0 - 0.5) - 0.5) / fwidth(aspectUv * 20.0);
        float line = 1.0 - min(min(grid.x, grid.y), 1.0);
        float scanline = 0.5 + 0.5 * sin(gl_FragCoord.y * 0.8 + time * 8.0);
        float chromatic = step(0.96, random(vec2(floor(time * 20.0), floor(aspectUv.y * 15.0))));
        energy = clamp(line * 0.6 + scanline * 0.3 + chromatic * 0.5, 0.0, 1.0);
        vec3 neon = mix(baseColor.rgb, accentColor.rgb, 0.5 + 0.5 * sin(time * 3.0 + aspectUv.x * 5.0));
        return clamp(neon * (0.7 + energy * 0.6), 0.0, 1.0);
    }

    if (mode == 8) {
        float r1 = sin(aspectUv.x * 22.0 + time * 2.2);
        float r2 = cos(aspectUv.y * 20.0 - time * 1.8);
        float caustic = pow(0.5 + 0.5 * (r1 + r2) * 0.5, 3.0) * 1.8;
        float sheen = smoothstep(0.4, 0.85, fbm(aspectUv * 10.0 + vec2(time * 0.1)));
        energy = clamp(caustic * 0.65 + sheen * 0.5, 0.0, 1.0);
        vec3 glass = mix(baseColor.rgb * 0.5, accentColor.rgb * 1.3, energy);
        glass += vec3(caustic * 0.4);
        return clamp(glass, 0.0, 1.0);
    }

    if (mode == 9) {
        float dist = length(aspectUv);
        float angle = atan(aspectUv.y, aspectUv.x);
        float spiral = sin(angle * 4.0 + dist * 25.0 - time * 4.5);
        float pulse = 0.5 + 0.5 * cos(dist * 18.0 - time * 3.0);
        energy = clamp(0.5 + 0.5 * spiral + pulse * 0.3, 0.0, 1.0);
        vec3 portal = mix(baseColor.rgb, accentColor.rgb, energy);
        portal *= 0.8 + 0.4 * pulse;
        return clamp(portal, 0.0, 1.0);
    }

    if (mode == 10) {
        vec2 p = aspectUv * 8.0;
        float n1 = fbm(p + vec2(time * 1.5, -time * 2.0));
        float n2 = fbm(p * 1.8 + vec2(-time * 2.2, time * 1.8));
        float arc = abs(sin(p.x * 6.0 + n1 * 8.0) * cos(p.y * 6.0 + n2 * 8.0));
        float bolt = 1.0 - smoothstep(0.0, 0.12, arc);
        float glow = 1.0 - smoothstep(0.0, 0.55, arc);
        energy = clamp(bolt * 1.2 + glow * 0.4, 0.0, 1.0);
        vec3 elect = mix(baseColor.rgb * 0.3, accentColor.rgb * 1.4, energy);
        elect += vec3(bolt * 0.7);
        return clamp(elect, 0.0, 1.0);
    }

    vec2 flamePoint = gl_FragCoord.xy * 8.0 / res.xx;
    float baseNoise = fbm(flamePoint - time * 0.10);
    vec2 curl = vec2(
        fbm(flamePoint + baseNoise + vec2(time * 0.42) - flamePoint.x - flamePoint.y),
        fbm(flamePoint + baseNoise - vec2(time * 0.31, time * 0.24))
    );
    float body = fbm(flamePoint + curl);
    float detail = fbm(flamePoint * 1.75 + curl.yx * 1.35 + vec2(-time * 0.18, time * 0.22));
    float heightFade = 1.0 - clamp(gl_FragCoord.y / res.y, 0.0, 1.0);
    float pulse = 0.5 + 0.5 * cos(time * 1.15 + uv.y * 5.8 + curl.x * 3.4);
    float flameVal = body * 0.50 + curl.y * 0.30 + detail * 0.20;
    float flame = clamp(smoothstep(0.34, 1.12, flameVal * (0.42 + heightFade * 0.72) + pulse * 0.14), 0.0, 1.0);
    float flicker = 0.82 + 0.18 * sin(time * 3.6 + flame * 5.0 + uv.x * 7.0);
    energy = clamp(flame * flicker, 0.0, 1.0);
    float hotCore = smoothstep(0.62, 1.0, energy);
    vec3 flameRgb = clamp(accentColor.rgb * (0.92 + hotCore * 0.28), 0.0, 1.0);
    return mix(baseColor.rgb, flameRgb, clamp(energy * 0.92, 0.0, 1.0));
}

void main() {
    vec2 res = uResolutionTime.xy;
    vec2 oneTexel = 1.0 / max(res, vec2(1.0));
    float center = sampleMask(vTexCoord);

    float outlineRadius = max(uParams.y, 1.0);
    float outlineMask = 0.0;
    for (int dir = 0; dir < DIRECTIONS; dir++) {
        float angle = TAU * (float(dir) / float(DIRECTIONS));
        vec2 offset = vec2(cos(angle), sin(angle)) * oneTexel * outlineRadius;
        outlineMask = max(outlineMask, max(sampleMask(vTexCoord + offset) - center, 0.0));
    }

    if (center <= 0.001 && outlineMask <= 0.001) {
        discard;
    }

    float time = uResolutionTime.z;
    float effectAlpha = uResolutionTime.w;
    int mode = int(uParams.x + 0.5);

    float colorSwap = smoothstep(0.12, 0.88, 0.5 + 0.5 * sin(time * 0.65));
    vec4 baseColor = mix(uFirstColor, uSecondColor, colorSwap);
    vec4 accentColor = mix(uSecondColor, uFirstColor, colorSwap);

    float energy;
    vec3 fillRgb = getEffectColor(vTexCoord, baseColor, accentColor, time, mode, energy);

    float outsideMask = 1.0 - smoothstep(0.001, 0.20, center);
    float edgePulse = 0.85 + 0.15 * sin(time * 3.0 + energy * 4.0);
    float fillAlpha = center * baseColor.a;
    float energyAlpha = center * energy * accentColor.a * 0.75;
    float insideAlpha = clamp(fillAlpha + energyAlpha * (1.0 - fillAlpha * 0.4), 0.0, 1.0);
    float edgeAlpha = outlineMask * outsideMask * uOutlineColor.a * edgePulse;
    float alpha = clamp(insideAlpha + edgeAlpha, 0.0, 1.0) * effectAlpha;

    vec3 blendedColor = fillRgb * insideAlpha + uOutlineColor.rgb * edgeAlpha;
    float div = max(insideAlpha + edgeAlpha, 0.001);
    fragColor = vec4(clamp(blendedColor / div, 0.0, 1.0), alpha);
}
