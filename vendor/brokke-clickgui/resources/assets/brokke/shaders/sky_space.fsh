#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 vUV;
layout(location = 1) in vec3 vWorldDir;
layout(location = 0) out vec4 OutColor;

layout(std140) uniform SkyUniforms {
    mat4 uInverseViewProj;
    vec4 uParams;
    vec4 uExtra;
    vec4 uThemeColor;
};

#define Direction vWorldDir
#define Time (uParams.x * uParams.z)
#define Accent (uThemeColor.rgb)
#define ColorModulator vec4(vec3(uParams.w), 1.0)
#define vColor vec4(1.0)
#define FrameBlend 0.0

float bk_hash(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.1, 0.2, 0.3));
    p += dot(p, p.yzx + 19.19);
    return fract((p.x + p.y) * p.z);
}

float noise(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(bk_hash(p), bk_hash(p + vec3(1,0,0)), f.x), mix(bk_hash(p + vec3(0,1,0)), bk_hash(p + vec3(1,1,0)), f.x), f.y), mix(mix(bk_hash(p + vec3(0,0,1)), bk_hash(p + vec3(1,0,1)), f.x), mix(bk_hash(p + vec3(0,1,1)), bk_hash(p + vec3(1,1,1)), f.x), f.y), f.z);
}

float fbm(vec3 p) {
    float a = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 3; i++) {
        a += amp * noise(p);
        p *= 2.1;
        amp *= 0.5;
    }
    return a;
}

vec4 bakeSky(vec2 TexCoord) {
    float bk_longitude = (TexCoord.x - 0.5) * 6.28318530718;
    float bk_latitude = (0.5 - TexCoord.y) * 3.14159265359;
    vec3 bk_dir = vec3(cos(bk_latitude) * cos(bk_longitude), sin(bk_latitude), cos(bk_latitude) * sin(bk_longitude));

    vec3 bk_theme = max(Accent, vec3(0.03));
    float bk_themeMax = max(bk_theme.r, max(bk_theme.g, bk_theme.b));
    vec3 bk_themeNorm = bk_themeMax > 0.001 ? bk_theme / bk_themeMax : vec3(0.55, 0.52, 0.65);

    vec3 bk_col = mix(vec3(0.008, 0.01, 0.045), bk_theme * 0.22, 0.78);
    float zen = bk_dir.y * 0.5 + 0.5;
    bk_col += mix(vec3(0.02, 0.03, 0.08), bk_theme * 0.14, 0.72) * pow(1.0 - zen, 2.5);

    vec3 p = bk_dir * 2.8;
    float n = fbm(p + vec3(Time * 0.03, Time * 0.018, -Time * 0.02));
    float n2 = noise(p.yzx * 1.38 + vec3(1.9, 2.1, 0.7));
    float nebMask = smoothstep(0.24, 0.92, n * n2);
    vec3 nebula = mix(vec3(0.45, 0.12, 0.55), bk_themeNorm * vec3(0.55, 0.35, 0.65) + bk_theme * 0.35, 0.58);
    nebula = mix(nebula, mix(vec3(0.1, 0.35, 0.65), bk_theme * 0.42, 0.52), n);
    bk_col += nebula * 0.85 * pow(nebMask, 0.65);

    vec3 galAxis = normalize(vec3(0.1, 0.84, 0.16));
    float galW = abs(dot(bk_dir, galAxis));
    bk_col += mix(vec3(0.35, 0.32, 0.55), bk_theme * 0.55 + bk_themeNorm * 0.2, 0.55) * pow(1.0 - galW, 6.0) * 0.75;

    bk_col = bk_col / (bk_col + vec3(0.85));
    bk_col = pow(bk_col, vec3(0.92));

    return vec4(clamp(bk_col, 0.0, 1.0), 1.0);
}

float hash(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.1, 0.2, 0.3));
    p += dot(p, p.yzx + 19.19);
    return fract((p.x + p.y) * p.z);
}

void main() {
    vec3 dir = normalize(Direction);

    float longitude = atan(dir.z, dir.x);
    float latitude = asin(clamp(dir.y, -1.0, 1.0));
    vec3 stored = bakeSky(vec2(longitude / 6.28318530718 + 0.5, 0.5 - latitude / 3.14159265359)).rgb;

    vec3 col = pow(stored, vec3(1.0 / 0.92));
    col = 0.85 * col / max(1.0 - col, vec3(0.001));

    vec3 theme = max(Accent, vec3(0.03));
    float themeMax = max(theme.r, max(theme.g, theme.b));
    vec3 themeNorm = themeMax > 0.001 ? theme / themeMax : vec3(0.55, 0.52, 0.65);

    vec3 sd = dir * 520.0;
    vec3 cell = floor(sd);
    vec3 fr = fract(sd) - 0.5;
    float h = hash(cell);
    float star = smoothstep(0.986, 0.998, h) * smoothstep(0.42, 0.0, length(fr));
    col += mix(vec3(0.95, 0.97, 1.0), mix(vec3(1.0), themeNorm, 0.35), 0.4) * star * 3.2;

    col = col / (col + vec3(0.85));
    col = pow(col, vec3(0.92));
    OutColor = vec4(col, 1.0) * ColorModulator * vColor;
}
