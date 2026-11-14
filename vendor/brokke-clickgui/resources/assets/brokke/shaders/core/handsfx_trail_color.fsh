#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D BloomSampler;
uniform sampler2D PrevSampler;

layout(std140) uniform HandsFxData {
    vec4 color1;
    vec4 color2;
    vec4 fillColor;
    vec4 outlineColor;
    vec4 params0;
    vec4 params1;
    vec4 params2;
    vec4 params3;
    vec4 screen;
    vec4 misc;
};

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

float wob(float y, float t) {
    return sin(y * 9.0 + t * 4.3) * 0.6
         + sin(y * 17.0 - t * 2.1) * 0.3
         + sin(y * 4.0 + t * 1.3) * 0.4
         + sin(y * 28.0 + t * 5.7) * 0.15;
}

vec4 blurSample(vec2 uv) {
    vec2 px = screen.zw;
    vec4 c = texture(PrevSampler, uv) * 0.36;
    c += texture(PrevSampler, uv + vec2(px.x, 0.0)) * 0.16;
    c += texture(PrevSampler, uv - vec2(px.x, 0.0)) * 0.16;
    c += texture(PrevSampler, uv + vec2(0.0, px.y)) * 0.16;
    c += texture(PrevSampler, uv - vec2(0.0, px.y)) * 0.16;
    return c;
}

vec3 bloomColor(vec4 bloom) {
    vec3 base = bloom.rgb / max(bloom.a, 0.001);
    float peak = max(base.r, max(base.g, base.b));
    if (peak > 0.001) {
        base /= peak;
    }
    float lum = dot(base, vec3(0.299, 0.587, 0.114));
    return clamp(mix(vec3(lum), base, color2.a), 0.0, 1.0);
}

vec4 softBloom(vec2 uv) {
    vec2 px = screen.zw;
    vec4 acc = texture(BloomSampler, uv) * 0.30;
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.785398 + 0.39;
        vec2 d = vec2(cos(a), sin(a));
        acc += texture(BloomSampler, uv + d * px * 5.0) * 0.055;
        acc += texture(BloomSampler, uv + d * px * 12.0) * 0.0325;
    }
    return acc;
}

void main() {
    vec2 uv = texCoord;
    float turb = params2.z;
    float dxTurb = 0.0;
    if (turb != 0.0) dxTurb = (wob(uv.y, params2.x) - wob(uv.y, params2.x - params2.y)) * turb;
    vec2 displacement = vec2(params3.x + dxTurb, params3.y * (0.7 + uv.y * 0.8));
    vec4 previous = blurSample(uv - displacement);
    float flick = 1.0;
    if (params2.w != 0.0) flick = 1.0 - params2.w + params2.w * (0.5 + 0.5 * sin(params2.x * 14.0) + 0.25 * sin(params2.x * 9.3));
    float fadedAlpha = max(0.0, previous.a * (0.985 * flick) - params3.z);
    vec4 faded = vec4(previous.rgb * (previous.a > 0.001 ? fadedAlpha / previous.a : 0.0), fadedAlpha);
    int style = int(floor((misc.w + 0.5) / 8.0));
    vec4 bloom = style == 0 ? texture(BloomSampler, uv) : softBloom(uv);
    float mask = bloom.a;
    if (mask < 0.001) {
        fragColor = faded;
        return;
    }
    bool autoColor = params0.x > 0.5;
    float exposure = misc.y;
    float time = params2.x;

    vec3 c1 = autoColor ? bloomColor(bloom) : color1.rgb;
    vec3 c2 = autoColor ? c1 : color2.rgb;
    vec3 col;
    float alpha;

    if (style == 1) {
        float intensity = mask * exposure;
        float body = smoothstep(0.05, 0.45, mask);
        float core = smoothstep(0.60, 1.10, intensity);
        vec3 hot = mix(mix(c1, c2, 0.22), vec3(1.0), 0.35);
        col = mix(c2, c1, body);
        col = mix(col, hot, core);
        col *= 1.0 + core * 0.55;
        alpha = 1.0 - exp(-intensity * 2.2);
    } else if (style == 2) {
        float halo = smoothstep(0.002, 0.24, mask);
        float body = smoothstep(0.10, 0.58, mask);
        float core = smoothstep(0.38, 0.92, mask * exposure);
        float gradient = smoothstep(0.18, 0.92, uv.y);
        vec3 baseColor = mix(c1, c2, gradient);
        vec3 outerColor = mix(c2, baseColor, 0.35);
        float filamentA = 0.5 + 0.5 * sin(uv.y * 92.0 + sin(uv.x * 31.0 - time * 1.7) * 2.2 - time * 8.0);
        float filamentB = 0.5 + 0.5 * sin(uv.x * 67.0 - uv.y * 29.0 + time * 5.3);
        float filament = pow(max(filamentA, filamentB * 0.82), 7.0) * halo;
        float pulse = 1.0 + 0.12 * sin(time * 5.5 + uv.y * 18.0);
        col = mix(outerColor * 0.68, baseColor * 1.18, body);
        col = mix(col, min(baseColor * 1.22, vec3(1.0)), core * 0.72);
        col += mix(baseColor, c1, 0.55) * filament * 0.42;
        col *= 1.20 + core * 0.48 + filament * 0.20;
        alpha = clamp((halo * 0.22 + body * 0.38 + core * 0.34 + filament * 0.12) * exposure * pulse, 0.0, 0.94);
    } else if (style == 3) {
        if (mask > 0.98) {
            fragColor = faded;
        return;
        }
        float aspect = screen.x / max(screen.y, 1.0);
        vec2 position = uv * vec2(aspect, 1.0);
        float bend = sin(position.y * 19.0 - time * 2.3 + sin(position.x * 8.0 + time) * 1.2) * 1.2;
        float contour = mask * 21.0 + bend * 0.65;
        float ribbon = pow(0.5 + 0.5 * cos(contour), 5.0);
        float halo = smoothstep(0.003, 0.045, mask) * (1.0 - smoothstep(0.6, 0.98, mask));
        float flow = 0.82 + 0.18 * sin(position.y * 22.0 - time * 4.0 + contour * 0.4);
        float gradient = 0.5 + 0.5 * sin(position.y * 5.0 - time * 0.8 + mask * 3.0);
        col = mix(c1, c2, gradient);
        alpha = clamp(halo * (0.045 + ribbon * 0.58) * flow * exposure, 0.0, 0.85);
    } else {
        col = autoColor ? c1 : mix(c1, c2, uv.y);
        alpha = clamp(mask * exposure, 0.0, 1.0);
    }

    fragColor = vec4(clamp(col, 0.0, 1.0) * alpha + faded.rgb * (1.0 - alpha), alpha + faded.a * (1.0 - alpha));
}
