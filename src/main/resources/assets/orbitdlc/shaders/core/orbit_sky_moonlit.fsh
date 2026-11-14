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
vec3 hash(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.xxy + p.yxx) * p.zyx);
}
float noise(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(i).x, hash(i + vec3(1,0,0)).x, f.x),
                   mix(hash(i + vec3(0,1,0)).x, hash(i + vec3(1,1,0)).x, f.x), f.y),
               mix(mix(hash(i + vec3(0,0,1)).x, hash(i + vec3(1,0,1)).x, f.x),
                   mix(hash(i + vec3(0,1,1)).x, hash(i + vec3(1,1,1)).x, f.x), f.y), f.z);
}
float clouds(vec3 p) {
    float sum = 0.0, amplitude = 0.53;
    for (int i = 0; i < 4; i++) {
        sum += noise(p) * amplitude;
        p = p * 2.07 + vec3(3.7, 9.2, 1.8);
        amplitude *= 0.48;
    }
    return sum;
}
float stars(vec3 d, float scale, float time) {
    vec3 p = d * scale, h = hash(floor(p));
    vec3 q = fract(p) - (0.15 + h * 0.7);
    float radius = length(q);
    float width = max(length(fwidth(p)) * 0.32, 0.025);
    float core = 1.0 - smoothstep(width * 0.2, width, radius);
    float halo = exp(-radius * 16.0) * 0.3;
    return (core + halo) * step(0.975, h.x) * (0.65 + 0.35 * sin(time + h.z * 60.0));
}
void main() {
    vec3 d = normalize(vWorldDir);
    float time = uParams.x * uParams.z;
    float angle = time * 0.002;
    d.xz = mat2(cos(angle), -sin(angle), sin(angle), cos(angle)) * d.xz;
    float band = exp(-pow(dot(d, normalize(vec3(0.38, 0.62, -0.67))) * 3.2, 2.0));
    vec3 p = d * 3.6 + vec3(time * 0.005, 0.0, 0.0);
    float cloud = clouds(p);
    float detail = clouds(p * 2.7 + cloud * 1.9);
    float dust = smoothstep(0.24, 0.79, cloud * 0.65 + detail * 0.35);
    float filaments = pow(1.0 - abs(detail * 2.0 - 1.0), 7.0);
    float light = band * dust;
    vec3 color = vec3(0.003, 0.018, 0.032);
    color += vec3(0.015, 0.18, 0.26) * cloud * 0.65;
    color += mix(vec3(0.015, 0.26, 0.40), vec3(0.48, 0.89, 1.0), dust) * light * 2.4;
    color += vec3(0.30, 0.68, 0.82) * filaments * band * dust * 0.65;
    color *= 1.0 - smoothstep(0.52, 0.75, detail) * band * 0.62;
    color += vec3(0.73, 0.91, 1.0) * stars(d, 155.0, time * 0.7) * 2.1;
    color += vec3(0.93, 0.98, 1.0) * stars(d + 7.3, 310.0, time * 0.5);
    float moon = max(dot(d, normalize(vec3(-0.45, 0.7, 0.35))), 0.0);
    color += vec3(0.40, 0.73, 0.88) * pow(moon, 50.0) * 0.30;
    color += vec3(0.82, 0.95, 1.0) * smoothstep(0.99925, 0.99945, moon) * 1.4;
    color *= smoothstep(-0.26, 0.20, d.y) * 0.86 + 0.14;
    color = vec3(1.0) - exp(-color * uParams.w);
    OutColor = vec4(pow(color, vec3(0.88)), 1.0);
    float luma = dot(OutColor.rgb, vec3(0.2126, 0.7152, 0.0722));
    OutColor.rgb = mix(OutColor.rgb, luma * uThemeColor.rgb, 0.65 * uThemeColor.a);
}
