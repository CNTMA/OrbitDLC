#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform Uniforms {
    mat4 uInvProjection;
    mat4 uProjection;
    mat4 uViewRot;
    vec4 uCameraPos;
    vec4 uScreen;
    vec4 uParams;
    vec4 uQualityParams;
    vec4 uFogParams;
    vec4 uFogColor;
    vec4 uExtra;
    vec4 uSplash[16];
    vec4 uFoot[32];
    vec4 uSnow;
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;

layout(location = 0) in vec2 vUV;
layout(location = 0) out vec4 fragColor;


float brokkeClipDepth(float depth) {
#ifdef BROKKE_DEPTH_ZERO_TO_ONE
    return depth;
#else
    return depth * 2.0 - 1.0;
#endif
}

float getDither(vec2 coord) {
    return fract(52.9829189 * fract(dot(coord, vec2(0.06711056, 0.00583715))));
}

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
        mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), f.x),
        f.y
    );
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 rot = mat2(0.8, 0.6, -0.6, 0.8);
    for (int i = 0; i < 5; i++) {
        v += noise(p) * a;
        p = rot * p * 2.02 + vec2(1.7, 9.2);
        a *= 0.5;
    }
    return v;
}

vec3 getViewPosFromRawDepth(vec2 uv, float d) {
    vec2 ndc = uv * 2.0 - 1.0;
    vec4 clip = vec4(ndc, brokkeClipDepth(d), 1.0);
    vec4 v = uInvProjection * clip;
    return v.xyz / max(abs(v.w), 0.00001);
}

vec3 getViewPosFromDepth(vec2 uv) {
    float d = texture(Sampler1, clamp(uv, vec2(0.0001), vec2(0.9999))).r;
    return getViewPosFromRawDepth(uv, d);
}

float getViewZFromDepth(vec2 uv, float d) {
    return getViewPosFromRawDepth(uv, d).z;
}

vec3 getNormalFromDepth(vec2 uv, vec3 viewPos) {
    vec2 texel = 1.0 / max(uScreen.xy, vec2(1.0));

    float dL = texture(Sampler1, clamp(uv - vec2(texel.x, 0.0), vec2(0.0001), vec2(0.9999))).r;
    float dR = texture(Sampler1, clamp(uv + vec2(texel.x, 0.0), vec2(0.0001), vec2(0.9999))).r;
    float dD = texture(Sampler1, clamp(uv - vec2(0.0, texel.y), vec2(0.0001), vec2(0.9999))).r;
    float dU = texture(Sampler1, clamp(uv + vec2(0.0, texel.y), vec2(0.0001), vec2(0.9999))).r;

    vec3 pL = getViewPosFromRawDepth(uv - vec2(texel.x, 0.0), dL);
    vec3 pR = getViewPosFromRawDepth(uv + vec2(texel.x, 0.0), dR);
    vec3 pD = getViewPosFromRawDepth(uv - vec2(0.0, texel.y), dD);
    vec3 pU = getViewPosFromRawDepth(uv + vec2(0.0, texel.y), dU);

    vec3 vL = viewPos - pL;
    vec3 vR = pR - viewPos;
    vec3 vD = viewPos - pD;
    vec3 vU = pU - viewPos;

    vec3 dX = (abs(vL.z) < abs(vR.z)) ? vL : vR;
    vec3 dY = (abs(vD.z) < abs(vU.z)) ? vD : vU;

    vec3 N = cross(dX, dY);
    float nLen = length(N);
    if (nLen > 1e-6) {
        N /= nLen;
    } else {
        float vpLen = length(viewPos);
        N = vpLen > 1e-6 ? -viewPos / vpLen : vec3(0.0, 1.0, 0.0);
    }

    if (dot(N, viewPos) > 0.0) {
        N = -N;
    }
    return N;
}

vec3 getNormalFromDepth(vec2 uv) {
    vec3 viewPos = getViewPosFromDepth(uv);
    return getNormalFromDepth(uv, viewPos);
}

bool gRtx = true;

vec2 hash22(vec2 p) {
    vec3 a = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    a += dot(a, a.yzx + 33.33);
    return fract((a.xx + a.yz) * a.zy);
}

vec2 rainRipples(vec2 p, float t) {
    vec2 grad = vec2(0.0);
    int layers = gRtx ? 3 : 1;
    for (int layer = 0; layer < 3; layer++) {
        if (layer >= layers) break;
        float fl = float(layer);
        vec2 q = p * (1.6 + fl * 0.7) + vec2(fl * 17.3, fl * 31.7);
        vec2 cell = floor(q);
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                vec2 id = cell + vec2(x, y);
                vec2 rnd = hash22(id + fl * 7.0);
                float phase = fract(t * (0.55 + rnd.y * 0.35) + rnd.x);
                vec2 center = id + 0.15 + rnd * 0.7;
                vec2 d = q - center;
                float dist = length(d);
                float x0 = dist - phase * 1.1;
                float wave = cos(x0 * 45.0) * exp(-x0 * x0 * 90.0);
                float fade = (1.0 - phase) * (1.0 - phase) * smoothstep(1.1, 0.2, dist);
                grad += (d / max(dist, 1e-3)) * wave * fade;
            }
        }
    }
    return grad * 0.11;
}

vec2 surfaceWaves(vec2 p, float t) {
    float e = 0.08;
    vec2 off = vec2(t * 0.12, t * 0.07);
    float h0 = fbm(p * 0.9 + off);
    float hx = fbm((p + vec2(e, 0.0)) * 0.9 + off);
    float hy = fbm((p + vec2(0.0, e)) * 0.9 + off);
    return vec2(hx - h0, hy - h0) / e * 0.035;
}

float puddleMask(vec2 p, float amount) {
    float n = fbm(p * 0.11) * 0.7 + fbm(p * 0.37 + 7.3) * 0.3;
    float threshold = mix(0.78, 0.30, clamp(amount, 0.0, 1.0));
    return smoothstep(threshold, threshold + 0.07, n);
}

vec2 projectToUV(vec3 viewP, out bool ok) {
    vec4 c = uProjection * vec4(viewP, 1.0);
    ok = c.w > 0.001;
    return (c.xy / max(c.w, 0.001)) * 0.5 + 0.5;
}

bool traceSSR(vec3 origin, vec3 dir, float dither, out vec2 hitUV, out float hitT) {
    hitUV = vec2(0.0);
    hitT = 0.0;
    float t = 0.05 + dither * 0.12;
    float stepLen = 0.08;
    float prevT = 0.0;
    int maxSteps = gRtx ? 88 : 40;
    int refine = gRtx ? 10 : 4;
    float growth = gRtx ? 1.075 : 1.12;
    float maxT = gRtx ? 96.0 : 48.0;
    for (int i = 0; i < 88; i++) {
        if (i >= maxSteps) return false;
        vec3 P = origin + dir * t;
        bool ok;
        vec2 uv = projectToUV(P, ok);
        if (!ok || uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return false;
        float d = textureLod(Sampler1, uv, 0.0).r;
        if (d < 0.99999) {
            float delta = getViewZFromDepth(uv, d) - P.z;
            float thickness = max(stepLen * 2.0, 0.08 + t * 0.02);
            if (delta >= 0.0 && delta < thickness) {
                float a = prevT;
                float b = t;
                vec2 best = uv;
                for (int r = 0; r < 10; r++) {
                    if (r >= refine) break;
                    float m = (a + b) * 0.5;
                    vec3 Pm = origin + dir * m;
                    bool okm;
                    vec2 uvm = clamp(projectToUV(Pm, okm), 0.0, 1.0);
                    float dm = textureLod(Sampler1, uvm, 0.0).r;
                    if (dm < 0.99999 && getViewZFromDepth(uvm, dm) - Pm.z >= 0.0) {
                        b = m;
                        best = uvm;
                    } else {
                        a = m;
                    }
                }
                if (b < 0.3 && length((best - vUV) * uScreen.xy) < 3.0) return false;
                hitUV = best;
                hitT = b;
                return true;
            }
        }
        prevT = t;
        stepLen = min(stepLen * growth, 1.6);
        t += stepLen;
        if (t > maxT) return false;
    }
    return false;
}

vec3 sampleBlurred(vec2 uv, float radius) {
    if (radius < 0.5) return textureLod(Sampler0, uv, 0.0).rgb;
    vec2 texel = radius / max(uScreen.xy, vec2(1.0));
    vec3 acc = textureLod(Sampler0, uv, 0.0).rgb * 0.2;
    for (int i = 0; i < 8; i++) {
        float fi = float(i) + 1.0;
        vec2 o = vec2(cos(fi * 2.39996), sin(fi * 2.39996)) * sqrt(fi / 8.0);
        acc += textureLod(Sampler0, clamp(uv + o * texel, 0.0, 1.0), 0.0).rgb * 0.1;
    }
    return acc;
}

vec3 skyReflection(vec3 R, float rain, float time) {
    float up = clamp(R.y, -1.0, 1.0);
    vec3 horizon = mix(vec3(0.62, 0.72, 0.84), vec3(0.42, 0.47, 0.54), rain);
    vec3 zenith = mix(vec3(0.20, 0.38, 0.72), vec3(0.24, 0.28, 0.34), rain);
    vec3 sky = mix(horizon, zenith, pow(max(up, 0.0), 0.6));
    vec2 cuv = R.xz / max(R.y + 0.15, 0.08);
    float cl = smoothstep(0.45, 0.8, fbm(cuv * 1.2 + time * 0.01));
    sky = mix(sky, mix(vec3(0.92), vec3(0.55, 0.58, 0.63), rain), cl * 0.6 * smoothstep(0.0, 0.35, up));
    if (up < 0.0) sky = mix(horizon, horizon * 0.35, smoothstep(0.0, -0.4, up));
    return sky;
}

float computeAO(vec2 uv, vec3 viewPos, vec3 Nv, float dither) {
    float radius = 0.6;
    float occ = 0.0;
    vec3 tangent = normalize(cross(Nv, abs(Nv.y) < 0.9 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0)));
    vec3 bitangent = cross(Nv, tangent);
    int aoTaps = gRtx ? 12 : 6;
    for (int i = 0; i < 12; i++) {
        if (i >= aoTaps) break;
        float fi = float(i) + dither;
        float r = sqrt((fi + 0.5) / 12.0) * radius;
        float a = fi * 2.39996;
        vec3 S = viewPos + (tangent * cos(a) + bitangent * sin(a)) * r + Nv * (0.1 + r * 0.5);
        bool ok;
        vec2 suv = projectToUV(S, ok);
        if (!ok || suv.x < 0.0 || suv.x > 1.0 || suv.y < 0.0 || suv.y > 1.0) continue;
        float sd = textureLod(Sampler1, suv, 0.0).r;
        if (sd >= 0.99999) continue;
        float diff = getViewZFromDepth(suv, sd) - S.z;
        if (diff > 0.02 && diff < 1.5) occ += smoothstep(0.0, 1.0, radius / diff);
    }
    return clamp(1.0 - occ / 12.0, 0.0, 1.0);
}

vec2 oceanWaves(vec2 p, float t, float dist, out float height) {
    vec2 grad = vec2(0.0);
    height = 0.0;
    float ampSum = 0.0;
    float wind = 0.6;
    float wavelength = 9.0;
    float amp = 0.35;
    for (int i = 0; i < 16; i++) {
        float fi = float(i);
        float ang = wind + (hash21(vec2(fi, 3.7)) - 0.5) * 1.6;
        vec2 dir = vec2(cos(ang), sin(ang));
        float k = 6.28318 / wavelength;
        float w = sqrt(9.81 * k);
        float phase = dot(dir, p) * k - w * t * 0.55 + hash21(vec2(fi, 9.1)) * 6.28318;
        float steep = amp * k;
        float fade = 1.0 - smoothstep(wavelength * 6.0, wavelength * 14.0, dist);
        grad += dir * steep * cos(phase) * fade;
        height += amp * sin(phase) * fade;
        ampSum += amp * fade;
        wavelength *= 0.78;
        amp *= 0.8;
    }
    vec2 cap = vec2(fbm(p * 3.0 + vec2(t * 0.9, -t * 0.6)), fbm(p * 3.0 + vec2(5.3 - t * 0.7, 1.7 + t * 0.8))) - 0.5;
    grad += cap * 0.12 * (1.0 - smoothstep(6.0, 20.0, dist));
    height = height / max(ampSum, 1e-3) * 0.5 + 0.5;
    return grad * 0.07;
}

vec3 splashWaves(vec2 p, float t) {
    vec2 grad = vec2(0.0);
    float foam = 0.0;
    for (int i = 0; i < 16; i++) {
        vec4 s = uSplash[i];
        if (s.w <= 0.0) continue;
        float age = t - s.z;
        if (age < 0.0 || age > 5.0) continue;
        vec2 d = p - s.xy;
        float r = length(d);
        float front = age * 2.6 + 0.2;
        float x = r - front;
        float env = exp(-x * x * 1.6) * exp(-age * 0.9) * s.w / (1.0 + r * 0.6);
        float wave = sin(x * 7.0) * env + sin((r - front * 0.6) * 11.0) * exp(-pow(r - front * 0.6, 2.0) * 3.0) * exp(-age * 1.3) * s.w * 0.5;
        grad += (d / max(r, 1e-3)) * wave * 1.6;
        foam += exp(-x * x * 6.0) * exp(-age * 1.8) * s.w;
        foam += (1.0 - smoothstep(0.0, 0.9 + age * 0.6, r)) * exp(-age * 2.2) * s.w;
    }
    return vec3(grad, foam);
}

vec3 worldToView(vec3 w) {
    return (transpose(uViewRot) * vec4(w - uCameraPos.xyz, 0.0)).xyz;
}

float skyVisibility(vec3 worldPos, vec3 Nw) {
    vec3 start = worldPos + Nw * 0.03;
    int steps = gRtx ? 14 : 8;
    for (int i = 1; i <= 14; i++) {
        if (i > steps) break;
        float fi = float(i) / float(steps);
        float h = 0.12 + fi * fi * 12.0;
        vec3 P = worldToView(start + vec3(0.0, h, 0.0));
        bool ok;
        vec2 uv = projectToUV(P, ok);
        if (!ok || uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return 1.0;
        float d = textureLod(Sampler1, uv, 0.0).r;
        if (d >= 0.99999) continue;
        float delta = getViewZFromDepth(uv, d) - P.z;
        float thickness = 0.6 + h * 0.35;
        if (delta > 0.04 && delta < thickness) return 0.0;
    }
    return 1.0;
}

float wallContinuesAbove(vec3 worldPos, vec3 Nw, float fromTop, float dist) {
    vec3 P = worldToView(worldPos + vec3(0.0, fromTop + 0.12, 0.0) - Nw * 0.01);
    bool ok;
    vec2 uv = projectToUV(P, ok);
    if (!ok || uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return 0.0;
    float d = textureLod(Sampler1, uv, 0.0).r;
    if (d >= 0.99999) return 0.0;
    float delta = getViewZFromDepth(uv, d) - P.z;
    return delta > -(0.05 + dist * 0.004) ? 1.0 : 0.0;
}

float iceCracks(vec2 p) {
    vec2 c = floor(p);
    float f1 = 8.0;
    float f2 = 8.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 id = c + vec2(x, y);
            vec2 o = id + hash22(id);
            float d = length(p - o);
            if (d < f1) {
                f2 = f1;
                f1 = d;
            } else if (d < f2) {
                f2 = d;
            }
        }
    }
    return 1.0 - smoothstep(0.0, 0.06, f2 - f1);
}

float footprintDepth(vec2 p, out float rim) {
    float d = 0.0;
    rim = 0.0;
    for (int i = 0; i < 32; i++) {
        vec4 f = uFoot[i];
        float age = uParams.z - f.z;
        if (age < 0.0 || age > 120.0) continue;
        vec2 r = p - f.xy;
        if (dot(r, r) > 0.25) continue;
        float fade = 1.0 - smoothstep(60.0, 120.0, age);
        vec2 l = vec2(dot(r, vec2(cos(f.w), sin(f.w))), dot(r, vec2(-sin(f.w), cos(f.w))));
        float w = mix(0.065, 0.085, smoothstep(-0.12, 0.08, l.x));
        float e = length(vec2(l.x / 0.15, l.y / w));
        float cup = 1.0 - smoothstep(0.55, 1.0, e);
        float tread = 0.82 + 0.18 * step(0.5, fract(l.x * 22.0));
        d = max(d, cup * tread * fade);
        rim = max(rim, smoothstep(0.85, 1.05, e) * (1.0 - smoothstep(1.05, 1.5, e)) * fade);
    }
    return d;
}

float snowHeightAt(vec2 p, bool feet, out float print) {
    float h = fbm(p * 0.45) * 0.4 + fbm(p * 1.3) * 0.38 + fbm(p * 5.7 + 3.1) * 0.22;
    print = 0.0;
    if (feet) {
        float rim;
        print = footprintDepth(p, rim);
        h += rim * 0.12 - print * 0.55;
    }
    return h;
}

float gSceneTopN = -1.0e9;

bool solidAt(vec3 w) {
    bool ok;
    vec3 v = worldToView(w);
    vec2 uv = projectToUV(v, ok);
    if (!ok || uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return false;
    float d = textureLod(Sampler1, uv, 0.0).r;
    if (d >= 0.99999) return false;
    float delta = getViewZFromDepth(uv, d) - v.z;
    return delta > 0.02 && delta < 1.2;
}

bool snowTopAt(vec2 xz, float n, float t, out vec2 topUV) {
    vec3 q = vec3(xz.x, n, xz.y);
    bool ok;
    topUV = projectToUV(worldToView(q), ok);
    if (!ok || topUV.x < 0.0 || topUV.x > 1.0 || topUV.y < 0.0 || topUV.y > 1.0) {
        topUV = clamp(topUV, vec2(0.001), vec2(0.999));
        return abs(n - gSceneTopN) < 0.01;
    }
    float d = textureLod(Sampler1, topUV, 0.0).r;
    if (d >= 0.99999) return false;
    vec3 w = uCameraPos.xyz + (uViewRot * vec4(getViewPosFromRawDepth(topUV, d), 0.0)).xyz;
    if (abs(w.y - n) > 0.03 + t * 0.003 || length(w.xz - xz) > 0.15 + t * 0.01) return false;
    vec3 below = worldToView(q - vec3(0.0, 0.12, 0.0));
    vec2 buv = projectToUV(below, ok);
    if (!ok || buv.x < 0.0 || buv.x > 1.0 || buv.y < 0.0 || buv.y > 1.0) return true;
    float bd = textureLod(Sampler1, buv, 0.0).r;
    return !(bd >= 0.99999 || getViewZFromDepth(buv, bd) - below.z < 0.04);
}

float driftAt(vec2 xz, vec4 walls) {
    vec2 f = fract(xz);
    float dr = 0.0;
    dr = max(dr, walls.x * smoothstep(0.75, 0.0, 1.0 - f.x));
    dr = max(dr, walls.y * smoothstep(0.75, 0.0, f.x));
    dr = max(dr, walls.z * smoothstep(0.75, 0.0, 1.0 - f.y));
    dr = max(dr, walls.w * smoothstep(0.75, 0.0, f.y));
    return dr * dr;
}

float layerHeight(vec2 xz, float H, vec4 walls, float t) {
    float h = H * (0.6 + 0.4 * noise(xz * 1.3)) + H * 0.9 * driftAt(xz, walls);
    if (t < 10.0) {
        float rim;
        h -= footprintDepth(xz, rim) * H * 0.85;
        h += rim * H * 0.15;
    }
    return h;
}

vec3 snowfall(vec3 col, vec3 D, float sceneDist) {
    float amount = uSnow.y;
    if (amount <= 0.001) return col;
    vec3 C = uCameraPos.xyz;
    float t = uParams.z;
    float acc = 0.0;
    for (int i = 0; i < 4; i++) {
        float d = 1.6 + float(i) * float(i + 1) * 1.4;
        if (d > sceneDist) break;
        vec3 P = C + D * d;
        float cellSize = 0.35 + float(i) * 0.25;
        vec3 q = P + vec3(sin(t * 0.7 + P.y * 0.4) * 0.3 + t * uSnow.z, t * (1.1 + float(i) * 0.15), t * uSnow.z * 0.5);
        q /= cellSize;
        vec3 cell = floor(q);
        vec3 fr = fract(q) - 0.5;
        float h = hash21(cell.xz + cell.y * 17.31);
        if (h > amount * 0.55) continue;
        vec3 off = vec3(hash21(cell.xy + 3.1), hash21(cell.yz + 7.7), hash21(cell.zx + 1.9)) - 0.5;
        vec3 dv = fr - off * 0.6;
        vec3 perp = dv - D * dot(dv, D);
        float r = length(perp) * cellSize;
        float size = 0.012 + 0.01 * float(i);
        acc += (1.0 - smoothstep(size * 0.4, size, r)) * (1.0 - float(i) * 0.15);
    }
    return mix(col, vec3(0.95, 0.97, 1.0) * max(uSnow.x, 0.8), clamp(acc, 0.0, 0.9));
}

vec3 applySnowLayer(vec3 col, vec2 uv, float sceneDist) {
    vec3 vp = getViewPosFromRawDepth(uv, 0.5);
    vec3 D = normalize((uViewRot * vec4(normalize(vp), 0.0)).xyz);
    float H = uExtra.w;
    if (uScreen.z < 0.5) return col;
    if (H < 0.01 || D.y >= -1.0e-4) return snowfall(col, D, sceneDist);
    vec3 C = uCameraPos.xyz;
    float maxT = min(sceneDist, 40.0);
    float Hmax = H * 1.05;
    float yEnd = C.y + D.y * maxT;
    float lo = floor(min(C.y, yEnd) - Hmax);
    float hi = min(floor(C.y), ceil(C.y) - 1.0);
    float bestT = 1.0e9;
    bool bestSide = false;
    float bestN = 0.0;
    vec2 bestUV = uv;
    vec4 bestWalls = vec4(0.0);
    float bestH = H;
    for (int k = 0; k < 6; k++) {
        float n = hi - float(k);
        if (n < lo) break;
        float ta = (n + Hmax - C.y) / D.y;
        float tb = (n - C.y) / D.y;
        float t0 = max(ta, 0.05);
        float t1 = min(tb, maxT);
        if (t0 >= t1 || t0 >= bestT) continue;
        vec3 P0 = C + D * mix(t0, t1, 0.5);
        float Hl = H;
        vec4 walls = vec4(0.0);
        int steps = int(clamp((t1 - t0) * 4.0, 3.0, 12.0));
        float prevT = t0;
        bool prevOutside = false;
        for (int i = 0; i < 12; i++) {
            if (i >= steps) break;
            float t = mix(t0, t1, float(i) / float(steps - 1));
            if (t >= bestT) break;
            vec3 P = C + D * t;
            if (P.y - n > layerHeight(P.xz, Hl, walls, t)) { prevT = t; prevOutside = false; continue; }
            vec2 tuv;
            if (!snowTopAt(P.xz, n, t, tuv)) { prevT = t; prevOutside = true; continue; }
            float ta2 = prevT;
            float tb2 = t;
            for (int j = 0; j < 5; j++) {
                float tm = (ta2 + tb2) * 0.5;
                vec3 Pm = C + D * tm;
                vec2 tuvm;
                if (Pm.y - n > layerHeight(Pm.xz, Hl, walls, tm)) { ta2 = tm; prevOutside = false; }
                else if (!snowTopAt(Pm.xz, n, tm, tuvm)) { ta2 = tm; prevOutside = true; }
                else { tb2 = tm; tuv = tuvm; }
            }
            bestT = tb2;
            bestSide = prevOutside && i > 0;
            bestN = n;
            bestUV = tuv;
            bestWalls = walls;
            bestH = Hl;
            break;
        }
    }
    if (bestT > 1.0e8) return snowfall(col, D, sceneDist);
    vec3 P = C + D * bestT;
    float e = 0.05;
    float h0 = layerHeight(P.xz, bestH, bestWalls, bestT);
    float hx = layerHeight(P.xz + vec2(e, 0.0), bestH, bestWalls, bestT);
    float hz = layerHeight(P.xz + vec2(0.0, e), bestH, bestWalls, bestT);
    vec3 N = normalize(vec3(-(hx - h0) / e, 1.0, -(hz - h0) / e));
    if (bestSide) {
        vec2 f = fract(P.xz) - 0.5;
        N = abs(f.x) > abs(f.y) ? vec3(sign(f.x), 0.0, 0.0) : vec3(0.0, 0.0, sign(f.y));
    }
    vec3 sun = normalize(vec3(0.35, 0.8, 0.25));
    float sunTerm = clamp(dot(N, sun), 0.0, 1.0);
    vec3 snowCol = mix(vec3(0.66, 0.75, 0.92), vec3(1.0, 0.99, 0.96), 0.3 + 0.7 * sunTerm);
    if (bestSide) {
        float rel = clamp((P.y - bestN) / max(h0, 0.01), 0.0, 1.0);
        snowCol *= mix(0.72, 1.0, rel);
    }
    vec2 gp = bestSide ? vec2(P.x + P.z, P.y * 3.0) : P.xz;
    float grain = noise(gp * 9.0) * 0.6 + noise(gp * 31.0) * 0.4;
    snowCol *= 0.9 + 0.1 * grain;
    float rimF;
    float print = bestT < 10.0 ? footprintDepth(P.xz, rimF) : 0.0;
    snowCol = mix(snowCol, snowCol * vec3(0.78, 0.86, 1.0), print * 0.7);
    float sparkleSeed = hash21(floor(P.xz * 48.0));
    snowCol += vec3(0.9, 0.95, 1.0) * step(0.996, sparkleSeed) * (0.5 + 0.5 * sin(uParams.z * 3.0 + sparkleSeed * 400.0)) * (bestSide ? 0.0 : 0.5) * (1.0 - smoothstep(4.0, 12.0, bestT));
    snowCol *= uSnow.x;
    float fogF = clamp((bestT - uFogParams.x) / max(uFogParams.y - uFogParams.x, 0.001), 0.0, 1.0);
    float a = (1.0 - smoothstep(30.0, 40.0, bestT)) * (1.0 - fogF);
    if (uFogParams.z > 0.5 && uFogColor.a > 0.001) snowCol = mix(snowCol, uFogColor.rgb, fogF * uFogColor.a);
    return snowfall(mix(col, snowCol, a), D, bestT);
}

void main() {
    gRtx = uQualityParams.x > 0.5;
    vec4 originalColor = texture(Sampler0, vUV);
    float depthVal = texture(Sampler1, vUV).r;
    if (depthVal >= 0.99999) {
        fragColor = vec4(applySnowLayer(originalColor.rgb, vUV, 40.0), 1.0);
        return;
    }

    bool entityPixel = false;
    if (uExtra.z > 0.5) {
        float terrainDepth = textureLod(Sampler2, vUV, 0.0).r;
        entityPixel = depthVal < terrainDepth - 1.0e-7;
        if (entityPixel && depthVal >= textureLod(Sampler3, vUV, 0.0).r - 1.0e-7) {
            fragColor = vec4(uScreen.z > 0.5 ? snowfall(originalColor.rgb, normalize((uViewRot * vec4(normalize(getViewPosFromRawDepth(vUV, 0.5)), 0.0)).xyz), 40.0) : originalColor.rgb, 1.0);
            return;
        }
    }

    vec3 viewPos = getViewPosFromDepth(vUV);
    float dist = length(viewPos);
    if (!(dist < 4000.0) || dist < 1e-4 || dist > uFogParams.y * 1.02 || depthVal >= 0.9999) {
        fragColor = vec4(applySnowLayer(originalColor.rgb, vUV, dist < 4000.0 ? dist : 40.0), 1.0);
        return;
    }
    vec3 V = dist > 1e-6 ? viewPos / dist : vec3(0.0, 0.0, -1.0);
    vec3 worldPos = uCameraPos.xyz + (uViewRot * vec4(viewPos, 0.0)).xyz;

    vec3 Nv = getNormalFromDepth(vUV, viewPos);
    vec3 Nw = normalize((uViewRot * vec4(Nv, 0.0)).xyz);
    vec3 an = abs(Nw);
    float am = max(an.x, max(an.y, an.z));
    if (am > 0.75) {
        if (an.y >= am) Nw = vec3(0.0, sign(Nw.y), 0.0);
        else if (an.x >= am) Nw = vec3(sign(Nw.x), 0.0, 0.0);
        else Nw = vec3(0.0, 0.0, sign(Nw.z));
    }

    float strength = uParams.x;
    float darkening = uParams.y;
    float time = uParams.z;
    float puddleAmount = uParams.w;
    float ripplesOn = uQualityParams.y;
    float rippleSpeed = uQualityParams.z;
    float rain = clamp(uQualityParams.w, 0.0, 1.0);

    float winter = uScreen.z;
    float waterStrength = uScreen.w;
    float aoStrength = uExtra.x;
    float snowAmount = uExtra.y;

    float ground = smoothstep(0.55, 0.92, Nw.y);
    float wall = 1.0 - ground;
    vec3 origRgb = originalColor.rgb;
    float yFrac = fract(worldPos.y + 0.001);
    float flatTop = step(0.97, Nw.y);
    if (flatTop > 0.5 && abs(worldPos.y - floor(worldPos.y + 0.5)) < 0.03 + dist * 0.002) gSceneTopN = floor(worldPos.y + 0.5);
    float liquidHeight = 1.0 - smoothstep(0.05 + dist * 0.002, 0.09 + dist * 0.004, abs(yFrac - 0.8889));
    vec3 detRgb = gRtx ? sampleBlurred(vUV, 7.0) : originalColor.rgb;
    float waterTint = smoothstep(0.0, 0.06, max(detRgb.b, detRgb.g * 0.9) - detRgb.r) * smoothstep(detRgb.g * 0.45, detRgb.g * 0.6, detRgb.b);
    float lavaTint = smoothstep(0.5, 0.7, origRgb.r) * smoothstep(1.2, 1.6, origRgb.r / max(origRgb.g, 0.01)) * step(origRgb.b, origRgb.r * 0.4);
    float liquidOn = flatTop * liquidHeight * step(0.01, waterStrength);
    float blueness = detRgb.b / max(max(detRgb.r, detRgb.g), 0.02);
    float waterBody = smoothstep(1.1, 1.35, blueness) * (1.0 - smoothstep(0.4, 0.55, detRgb.r)) * step(0.01, waterStrength) * flatTop * liquidHeight;
    float lava = liquidOn * lavaTint;
    if (entityPixel && max(max(liquidOn * waterTint, waterBody), lava) < 0.01) {
        fragColor = vec4(uScreen.z > 0.5 ? snowfall(originalColor.rgb, normalize((uViewRot * vec4(V, 0.0)).xyz), dist) : originalColor.rgb, 1.0);
        return;
    }
    float water = max(max(liquidOn * waterTint, waterBody), lava);
    Nw = normalize(mix(Nw, vec3(0.0, 1.0, 0.0), clamp(water * 2.0, 0.0, 1.0)));
    ground = max(ground, water);
    wall = 1.0 - ground;
    float underWater = max(liquidOn * waterTint, waterBody);
    float ice = winter * max(water, underWater) * max(flatTop, water) * (1.0 - lava);
    float rtx = step(0.5, uQualityParams.x);
    lava *= rtx;
    water *= rtx;

    vec2 sp = worldPos.xz;
    float snowH = 0.5;
    vec3 snowN = vec3(0.0, 1.0, 0.0);
    float footPrint = 0.0;
    float snowSelfShadow = 0.0;
    if (winter > 0.5) {
        bool feet = dist < 48.0 && Nw.y > 0.5;
        snowH = snowHeightAt(sp, feet, footPrint);
        float dummy;
        float eps = 0.035;
        float hx = snowHeightAt(sp + vec2(eps, 0.0), feet, dummy);
        float hz = snowHeightAt(sp + vec2(0.0, eps), feet, dummy);
        float bump = mix(4.5, 6.5, step(0.5, uQualityParams.x)) * (1.0 - smoothstep(30.0, 90.0, dist) * 0.6);
        snowN = normalize(vec3(-(hx - snowH) / eps * 0.09 * bump, 1.0, -(hz - snowH) / eps * 0.09 * bump));
        vec2 sunXZ = normalize(vec2(0.35, 0.25));
        float hs1 = snowHeightAt(sp + sunXZ * 0.12, feet, dummy);
        float hs2 = snowHeightAt(sp + sunXZ * 0.35, false, dummy);
        snowSelfShadow = clamp(smoothstep(0.015, 0.09, hs1 - snowH) * 0.7 + smoothstep(0.03, 0.14, hs2 - snowH) * 0.5, 0.0, 1.0);
    }
    float snowTop = 0.0;
    float snowEdge = 0.0;
    float snowShadow = 0.0;
    float lipDepth = 0.12;
    float fromTop = 1.0 - yFrac;
    float snowLip = 0.0;
    float lipShadow = 0.0;
    if (winter > 0.5) {
        float snowCoverage = fbm(sp * 0.35) * 0.55 + snowH * 0.45;
        float snowThreshold = uExtra.w > 0.01 ? -1.0 : mix(0.75, 0.05, clamp(snowAmount, 0.0, 1.0));
        float snowField = snowCoverage - snowThreshold;
        snowTop = 0.0;
        if (uSnow.w > 0.5) snowTop *= smoothstep(uSnow.w - 2.0, uSnow.w + 1.0, length(worldPos.xz - uCameraPos.xz));
        snowEdge = 0.0;
        snowShadow = 0.0;
        if (wall > 0.001) {
            float along = dot(worldPos.xz, abs(Nw.x) > 0.5 ? vec2(0.0, 1.0) : vec2(1.0, 0.0));
            lipDepth = 0.12 + 0.2 * fbm(vec2(along * 3.0, floor(worldPos.y) * 7.1));
            float lipOn = step(0.5, snowAmount + fbm(vec2(along * 0.4, worldPos.y)) * 0.6);
            if (lipOn > 0.5 && fromTop < lipDepth + 0.16) {
                lipOn *= 1.0 - wallContinuesAbove(worldPos, Nw, fromTop, dist);
                if (lipOn > 0.5) lipOn *= skyVisibility(worldPos + vec3(0.0, fromTop + 0.02, 0.0), Nw);
            } else {
                lipOn = 0.0;
            }
            snowLip = 0.0;
            lipShadow = 0.0;
        }
    }
    float snow = winter * clamp(max(snowTop, snowLip), 0.0, 1.0) * (1.0 - max(water, underWater));
    float frost = 0.0;
    if (winter > 0.5 && dist < 64.0 && !entityPixel) {
        vec2 px = 1.0 / uScreen.xy;
        float dl = abs(getViewZFromDepth(vUV, texture(Sampler1, vUV - vec2(px.x, 0.0)).r) - viewPos.z);
        float dr = abs(getViewZFromDepth(vUV, texture(Sampler1, vUV + vec2(px.x, 0.0)).r) - viewPos.z);
        float du = abs(getViewZFromDepth(vUV, texture(Sampler1, vUV + vec2(0.0, px.y)).r) - viewPos.z);
        float edge = smoothstep(0.15, 0.6, max(max(dl, dr), du));
        float mx = max(origRgb.r, max(origRgb.g, origRgb.b));
        float sat = (mx - min(origRgb.r, min(origRgb.g, origRgb.b))) / max(mx, 0.01);
        float plant = edge * smoothstep(0.25, 0.45, sat) * step(origRgb.b, max(origRgb.r, origRgb.g));
        float topish = clamp(Nw.y * 0.5 + 0.6, 0.0, 1.0);
        frost = plant * topish * smoothstep(0.35, 0.6, noise(worldPos.xz * 3.0 + worldPos.y * 2.0) + Nw.y * 0.3) * (1.0 - smoothstep(40.0, 64.0, dist));
        if (frost > 0.01) frost *= skyVisibility(worldPos, vec3(0.0, 1.0, 0.0));
    }
    snowShadow *= winter * (1.0 - max(water, underWater));
    lipShadow *= winter * (1.0 - snowLip);

    float puddle = (ground > 0.001 && snow < 0.999) ? puddleMask(worldPos.xz, puddleAmount) * ground * (1.0 - snow) : 0.0;
    puddle *= 1.0 - winter;
    puddle = max(puddle, water);
    float streaks = wall > 0.001 ? smoothstep(0.35, 0.75, fbm(vec2(worldPos.x + worldPos.z, worldPos.y * 0.25 + time * 0.6 * rain) * 2.0)) * wall : 0.0;
    float wet = clamp(0.35 + ground * 0.4 + puddle * 0.6 + streaks * 0.25, 0.0, 1.0);
    float roughness = mix(0.42, 0.02, puddle);
    roughness = mix(roughness, 0.6, wall * (1.0 - streaks));
    roughness = mix(roughness, 0.3, snow);
    roughness = mix(roughness, 0.0, water);
    roughness = mix(roughness, 0.12, lava);
    roughness = mix(roughness, 0.06, ice);
    wet = mix(wet, 0.55, snow);
    wet = mix(wet, 1.0, water);

    vec2 grad = vec2(0.0);
    float foamAmount = 0.0;
    if (ground > 0.01) {
        if (gRtx && puddle < 0.999) grad += surfaceWaves(worldPos.xz, time) * (1.0 - puddle * 0.6);
        vec2 rip = ripplesOn > 0.5 ? rainRipples(worldPos.xz, time * rippleSpeed) : vec2(0.0);
        grad += rip * mix(0.35, 1.0, puddle) * max(rain, 0.35);
        grad *= ground;
        if (water > 0.01) {
            float waveH;
            vec2 wg = oceanWaves(worldPos.xz, time, dist, waveH);
            vec3 sw = splashWaves(worldPos.xz, time);
            wg += sw.xy;
            float foamNoise = fbm(worldPos.xz * 2.2 + vec2(time * 0.25, -time * 0.18));
            float crest = smoothstep(0.62, 0.85, waveH + length(wg) * 0.6);
            foamAmount = clamp(crest * smoothstep(0.35, 0.65, foamNoise) + sw.z * smoothstep(0.25, 0.6, foamNoise + 0.2), 0.0, 1.0) * water * (1.0 - smoothstep(25.0, 60.0, dist));
            wg += rip * rain;
            float wl = length(wg);
            if (wl > 0.18) wg *= 0.18 / wl;
            grad = mix(grad, wg, water);
        }
        grad *= 1.0 - snow;
        grad *= 1.0 - ice;
        grad *= 1.0 - lava * 0.8;
    }
    vec3 pNw = normalize(Nw + vec3(-grad.x, 0.0, -grad.y));
    pNw = normalize(mix(pNw, snowN, snowTop * winter * (1.0 - max(water, underWater))));
    vec3 pNv = normalize((transpose(uViewRot) * vec4(pNw, 0.0)).xyz);
    if (dot(pNv, V) > 0.0) pNv = -pNv;

    vec3 R = reflect(V, pNv);
    vec3 Rw = normalize((uViewRot * vec4(R, 0.0)).xyz);
    if (water > 0.01 && Rw.y < 0.03) {
        Rw = normalize(vec3(Rw.x, mix(Rw.y, 0.03, water), Rw.z));
        R = normalize((transpose(uViewRot) * vec4(Rw, 0.0)).xyz);
    }

    float NdotV = clamp(dot(-V, pNv), 0.0, 1.0);

    float F0 = 0.02 + 0.02 * puddle;
    float fresnel = F0 + (1.0 - F0) * pow(1.0 - NdotV, 5.0);
    fresnel = mix(fresnel, fresnel * 0.5 + 0.08, roughness);
    fresnel = clamp(fresnel * mix(1.0, 1.35, puddle), 0.0, 0.98);
    fresnel = mix(fresnel, clamp(0.3 + 0.7 * pow(1.0 - NdotV, 3.0), 0.0, 0.98), water);
    fresnel = mix(fresnel, fresnel * 0.6 + 0.04, snow);
    fresnel = mix(fresnel, clamp(0.06 + 0.7 * pow(1.0 - NdotV, 4.0), 0.0, 0.9), ice);

    float fogF = clamp((dist - uFogParams.x) / max(uFogParams.y - uFogParams.x, 0.001), 0.0, 1.0);
    fogF = fogF * fogF * (3.0 - 2.0 * fogF);
    float nearFade = smoothstep(0.3, 0.8, abs(viewPos.z));
    nearFade *= 1.0 - smoothstep(90.0, 160.0, dist);

    float amount = clamp(strength * wet * nearFade * (1.0 - fogF), 0.0, 1.5);
    amount = mix(amount, clamp(waterStrength * nearFade * (1.0 - fogF), 0.0, 1.5), water);
    amount *= 1.0 - snow;
    amount *= 1.0 - winter * (1.0 - max(water, ice));
    amount *= mix(1.0, 0.45, lava);
    amount *= mix(0.5, 1.0, rtx);
    amount = mix(amount, clamp(strength * nearFade * (1.0 - fogF), 0.0, 1.5) * mix(0.6, 1.0, rtx), ice);
    float reflWeight = clamp(fresnel * amount, 0.0, 1.0);

    float dither = getDither(gl_FragCoord.xy);
    vec3 refl = reflWeight > 0.004 ? skyReflection(Rw, rain, time) : vec3(0.0);
    float hitA = 0.0;
    if (R.z < 0.1 && reflWeight > 0.004) {
        vec2 hitUV;
        float hitT;
        if (traceSSR(viewPos + pNv * 0.04, R, dither, hitUV, hitT)) {
            float blur = roughness * clamp(hitT * 1.6, 0.0, 22.0);
            vec3 hc = gRtx ? sampleBlurred(hitUV, blur) : texture(Sampler0, hitUV).rgb;
            vec2 edge = smoothstep(0.0, 0.08, hitUV) * (1.0 - smoothstep(0.92, 1.0, hitUV));
            float fade = edge.x * edge.y * (1.0 - smoothstep(60.0, 96.0, hitT)) * (1.0 - smoothstep(-0.1, 0.1, R.z));
            refl = mix(refl, hc, fade);
            hitA = fade;
        }
    }

    vec3 sunDir = normalize(vec3(0.35, 0.8, 0.25));
    float spec = pow(max(dot(Rw, sunDir), 0.0), mix(24.0, 900.0, 1.0 - roughness));
    refl += vec3(1.0, 0.96, 0.88) * spec * mix(0.4, 2.2, 1.0 - roughness) * (1.0 - hitA) * (1.0 - rain * 0.6);
    refl += vec3(1.0, 0.92, 0.8) * pow(max(dot(Rw, sunDir), 0.0), 2200.0) * 6.0 * water * (1.0 - lava) * (1.0 - rain * 0.7);
    float sparkleSeed = hash21(floor(worldPos.xz * 24.0) + floor(worldPos.y) * 13.7);
    float sparkle = step(0.997, sparkleSeed) * (0.5 + 0.5 * sin(time * 3.0 + sparkleSeed * 400.0)) * (1.0 - smoothstep(3.0, 10.0, dist)) * step(uExtra.w, 0.01);
    refl += vec3(1.0) * sparkle * snow * (1.0 - footPrint) * (1.0 - snowSelfShadow) * 0.15;

    vec3 base = originalColor.rgb;
    float lum = dot(base, vec3(0.299, 0.587, 0.114));
    vec3 wetBase = max(mix(vec3(lum), base, 1.18), 0.0) * (1.0 - darkening * wet * (1.0 - winter));
    wetBase = mix(wetBase, wetBase * vec3(0.92, 0.96, 1.03), puddle * 0.5);
    float skyLight = clamp(lum * 1.4 + 0.3, 0.3, 1.0);
    float sunTerm = clamp(dot(pNw, normalize(vec3(0.35, 0.8, 0.25))), 0.0, 1.0);
    float snowAO = mix(0.6, 1.0, smoothstep(0.2, 0.75, snowH + footPrint * 0.4));
    snowAO *= 1.0 - footPrint * 0.25;
    snowAO *= 1.0 - snowSelfShadow * 0.3;
    vec3 snowShade = mix(vec3(0.62, 0.72, 0.90), vec3(1.0, 0.99, 0.96), 0.35 + 0.65 * sunTerm);
    vec3 snowCol = snowShade * snowAO * max(uSnow.x, skyLight);
    snowCol *= mix(1.0, 0.82, snowEdge * snowTop);
    snowCol = mix(snowCol, snowCol * vec3(0.74, 0.84, 1.0), clamp(footPrint * 0.8 + snowSelfShadow * 0.5, 0.0, 1.0));
    snowCol += vec3(0.05, 0.06, 0.08) * pow(1.0 - NdotV, 3.0) * skyLight;
    snowCol = mix(snowCol, snowCol * vec3(0.85, 0.9, 1.0), snowLip * (1.0 - fromTop / max(lipDepth, 0.01)) * 0.5);
    snowCol = vec3(0.92, 0.94, 0.98) * clamp(skyLight * 1.1, 0.85, 1.0);
    wetBase = mix(wetBase, snowCol, snow);
    frost = 0.0;
    wetBase *= 1.0 - snowShadow * 0.35;
    wetBase *= 1.0 - lipShadow * 0.45;
    base = mix(wetBase, base, fogF);

    float pureWater = water * (1.0 - lava) * (1.0 - ice);
    if (pureWater > 0.01) {
        vec3 avg = sampleBlurred(vUV, 14.0) * 0.5 + sampleBlurred(vUV, 28.0) * 0.5;
        float avgLum = dot(avg, vec3(0.299, 0.587, 0.114));
        float facing = clamp(dot(-V, pNv), 0.0, 1.0);
        vec3 deep = vec3(0.012, 0.055, 0.085) * (0.6 + avgLum);
        vec3 shallow = mix(avg, vec3(0.05, 0.22, 0.28) * (0.5 + avgLum), 0.6);
        vec3 waterBase = mix(deep, shallow, facing * 0.7);
        base = mix(base, waterBase, smoothstep(0.15, 0.6, pureWater));
        foamAmount *= pureWater;
    }

    if (ice > 0.01) {
        vec3 avgI = sampleBlurred(vUV, 16.0);
        float aLum = dot(avgI, vec3(0.299, 0.587, 0.114));
        vec3 iceCol = mix(avgI * 0.5, vec3(0.62, 0.80, 0.92) * (0.55 + aLum * 0.6), 0.7);
        float depthVar = fbm(worldPos.xz * 0.45);
        iceCol = mix(iceCol * vec3(0.75, 0.88, 1.0), iceCol, depthVar);
        float cracks = iceCracks(worldPos.xz * 0.9) * 0.7 + iceCracks(worldPos.xz * 2.7 + 5.0) * 0.35;
        iceCol = mix(iceCol, vec3(0.93, 0.97, 1.0), clamp(cracks, 0.0, 1.0) * 0.55);
        float frost = smoothstep(0.55, 0.75, fbm(worldPos.xz * 1.6 + 11.0));
        iceCol = mix(iceCol, vec3(0.9, 0.94, 1.0) * (0.6 + aLum * 0.5), frost * 0.6);
        float bubbles = step(0.992, hash21(floor(worldPos.xz * 22.0))) * (1.0 - frost);
        iceCol += vec3(0.25, 0.3, 0.35) * bubbles;
        base = mix(base, iceCol, ice);
    }

    if (aoStrength > 0.001 && dist < 48.0) {
        float ao = computeAO(vUV, viewPos, Nv, dither);
        base *= mix(1.0, ao, aoStrength * (1.0 - smoothstep(24.0, 48.0, dist)));
    }

    vec3 col = mix(base, refl, reflWeight);
    float foamLum = clamp(dot(originalColor.rgb, vec3(0.299, 0.587, 0.114)) * 1.6 + 0.35, 0.35, 1.0);
    col = mix(col, vec3(0.9, 0.95, 0.97) * foamLum, foamAmount * 0.85 * (1.0 - ice));
    col = mix(col, max(col, base), lava);

    if (uFogParams.z > 0.5 && uFogColor.a > 0.001) {
        col = mix(col, uFogColor.rgb, fogF * uFogColor.a);
    }
    col = applySnowLayer(col, vUV, dist);
    if (any(isnan(col)) || any(isinf(col))) col = originalColor.rgb;
    fragColor = vec4(col, 1.0);
}
