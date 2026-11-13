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
    float t = uParams.x * uParams.z * 0.045;
    vec2 uv = d.xz / (abs(d.y) + 0.65) * 4.0;
    uv += vec2(sin(d.z * 3.0 + t), cos(d.x * 3.0 - t)) * 0.12;
    vec2 cell = floor(uv);
    vec2 local = fract(uv);
    float first = 100.0;
    float second = 100.0;
    float selected = 0.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 offset = vec2(float(x), float(y));
            float seed = hash31(vec3(cell + offset, 5.1));
            vec2 point = offset + 0.5 + 0.3 * vec2(sin(seed * 37.0 + t), cos(seed * 61.0 - t));
            float distanceToPoint = length(point - local);
            if (distanceToPoint < first) {
                second = first;
                first = distanceToPoint;
                selected = seed;
            } else {
                second = min(second, distanceToPoint);
            }
        }
    }
    float boundary = second - first;
    float edge = exp(-boundary * 110.0);
    vec3 tint = 0.5 + 0.5 * cos(vec3(0.0, 2.1, 4.2) + selected * 6.2831853 + t * 0.5);
    tint = mix(tint, uThemeColor.rgb, 0.12);
    vec3 color = vec3(0.012, 0.018, 0.04) + tint * (0.045 + 0.13 * exp(-first * 2.5));
    color += mix(tint, vec3(0.8, 0.9, 1.0), 0.55) * edge * 0.3;
    color += tint * exp(-boundary * 14.0) * 0.045;
    color += stars(d, t * 8.0) * 0.5;
    finishSky(color);
}