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
    float t = uParams.x * uParams.z * 0.08;
    vec3 axis = normalize(vec3(-0.45, 0.65, 0.6));
    vec3 right = normalize(cross(axis, vec3(0.0, 1.0, 0.0)));
    vec3 up = cross(right, axis);
    vec2 p = vec2(dot(d, right), dot(d, up));
    float front = smoothstep(0.3, 0.65, dot(d, axis));
    float radius = length(p);
    vec3 color = mix(vec3(0.08, 0.035, 0.04), vec3(0.012, 0.015, 0.038), smoothstep(-0.1, 0.8, d.y));
    vec3 gold = vec3(1.0, 0.65, 0.26);
    vec3 frost = mix(vec3(0.55, 0.78, 1.0), uThemeColor.rgb, 0.2);
    float disc = 1.0 - smoothstep(0.138, 0.143, radius);
    float surface = 0.35 + 0.12 * sin(p.x * 110.0 + sin(p.y * 45.0 + t) * 2.0);
    color += gold * disc * surface * front;
    color += gold * exp(-abs(radius - 0.144) * 95.0) * 0.6 * front;
    vec2 tilted = vec2(p.x * 0.91 + p.y * 0.41, -p.x * 0.41 + p.y * 0.91);
    float ringRadius = length(tilted * vec2(1.0, 2.6));
    for (int i = 0; i < 3; i++) {
        float f = float(i);
        float ring = exp(-abs(ringRadius - (0.28 + f * 0.045)) * 190.0);
        float sparkle = 0.55 + 0.45 * sin(atan(tilted.y, tilted.x) * 3.0 + t + f);
        color += mix(gold, frost, f * 0.35) * ring * sparkle * front * (1.0 - disc);
    }
    color += gold * exp(-radius * 8.0) * front * 0.055;
    color += stars(d, t * 4.0) * 0.8;
    finishSky(color);
}