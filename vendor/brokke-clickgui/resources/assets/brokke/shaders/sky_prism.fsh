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
float hash31(vec3 p) {
    p = fract(p * vec3(443.8975, 397.2973, 491.1871));
    p += dot(p, p.yxz + 19.19);
    return fract((p.x + p.y) * p.z);
}
vec3 stars(vec3 d, float t) {
    vec3 p = d * 150.0;
    float h = hash31(floor(p));
    float star = exp(-dot(fract(p) - 0.5, fract(p) - 0.5) * 700.0);
    star *= step(0.987, h) * (0.6 + 0.4 * sin(t + h * 100.0));
    return mix(vec3(0.6, 0.8, 1.0), vec3(1.0, 0.8, 0.65), h) * star;
}
void finishSky(vec3 color) {
    color = max(color, vec3(0.0));
    color = color / (1.0 + color * 0.45);
    OutColor = vec4(color * uParams.w, 1.0);
}
void main() {
    vec3 d = normalize(vWorldDir);
    float t = uParams.x * uParams.z * 0.12;
    vec3 color = mix(vec3(0.07, 0.025, 0.11), vec3(0.008, 0.012, 0.035), smoothstep(-0.1, 0.9, d.y));
    float heightMask = smoothstep(-0.18, 0.15, d.y);
    for (int i = 0; i < 4; i++) {
        float f = float(i);
        float flow = dot(d, normalize(vec3(0.3 + f * 0.2, 0.5, 0.8 - f * 0.3)));
        float warp = sin(d.x * 3.0 + d.z * 4.0 + t + f) * 0.1;
        float band = sin(flow * 8.0 + warp * 5.0 + t * 0.7 + f * 1.7);
        float ribbon = exp(-band * band * 85.0);
        float halo = exp(-band * band * 9.0);
        vec3 tint = 0.55 + 0.45 * cos(vec3(0.0, 2.1, 4.2) + flow * 5.0 + f + t * 0.2);
        color += tint * (ribbon * 0.23 + halo * 0.075) * heightMask;
    }
    color += stars(d, t * 4.0) * 0.65;
    finishSky(color);
}