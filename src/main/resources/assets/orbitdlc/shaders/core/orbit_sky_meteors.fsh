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
    float luma = dot(OutColor.rgb, vec3(0.2126, 0.7152, 0.0722));
    OutColor.rgb = mix(OutColor.rgb, luma * uThemeColor.rgb, 0.65 * uThemeColor.a);
}
void main() {
    vec3 d = normalize(vWorldDir);
    float t = uParams.x * uParams.z;
    vec2 uv = vec2(atan(d.z, d.x) / 6.2831853, asin(clamp(d.y, -1.0, 1.0)) / 3.14159265);
    vec3 color = mix(vec3(0.025, 0.055, 0.09), vec3(0.006, 0.009, 0.03), smoothstep(-0.1, 0.6, d.y));
    color += stars(d, t * 0.4) * 1.1;
    for (int i = 0; i < 14; i++) {
        float seed = hash31(vec3(float(i), 7.1, 3.9));
        float phase = fract(t * 0.09 + seed * 11.0);
        vec2 head = vec2(fract(seed * 15.7 + phase * 0.1), 0.46 - phase * 0.65);
        vec2 delta = uv - head;
        delta.x = fract(delta.x + 0.5) - 0.5;
        vec2 direction = normalize(vec2(0.15, -1.0));
        float along = dot(delta, direction);
        float side = dot(delta, vec2(-direction.y, direction.x));
        float tail = exp(-abs(side) * 1500.0) * exp(along * 32.0) * step(along, 0.0) * step(-0.12, along);
        float core = exp(-dot(delta, delta) * 200000.0);
        float visibility = smoothstep(0.0, 0.08, phase) * (1.0 - smoothstep(0.7, 1.0, phase));
        vec3 tint = mix(vec3(0.35, 0.8, 1.0), vec3(1.0, 0.68, 0.42), seed);
        color += (tint * tail * 0.75 + vec3(1.0) * core * 1.2) * visibility;
    }
    float dust = pow(max(0.0, sin(d.x * 9.0 + d.z * 6.0 + d.y * 4.0)), 8.0);
    color += vec3(0.05, 0.045, 0.1) * dust * smoothstep(0.0, 0.5, d.y);
    finishSky(color);
}