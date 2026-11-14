#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location = 0) in vec2 TexCoord;
layout(location = 0) out vec4 OutColor;
uniform sampler2D Scene;
uniform sampler2D Depth;
layout(std140) uniform TrailSettings {
    mat4 Inverse;
    vec4 Settings;
    vec4 Tint;
    vec4 Bounds;
    vec4 Points[32];
};

vec3 unproject(float depth) {
    vec4 p = Inverse * vec4(TexCoord * 2.0 - 1.0, mix(depth * 2.0 - 1.0, depth, Settings.w), 1.0);
    return p.xyz / p.w;
}

void main() {
    if (any(lessThan(TexCoord, Bounds.xy)) || any(greaterThan(TexCoord, Bounds.zw))) discard;
    vec3 ray = normalize(unproject(0.5));
    float depth = texture(Depth, TexCoord).r;
    float farLimit = depth > 0.000001 ? length(unproject(depth)) + 0.024 : 512.0;
    float glow = 0.0, heat = 0.0, sparkle = 0.0;
    int count = int(Settings.x);
    for (int i = 0; i < count - 1; i++) {
        vec4 a = Points[i], b = Points[i + 1];
        vec3 segment = b.xyz - a.xyz;
        vec3 lateral = segment - ray * dot(segment, ray);
        vec3 base = a.xyz - ray * dot(a.xyz, ray);
        float along = clamp(-dot(base, lateral) / max(dot(lateral, lateral), 0.000001), 0.0, 1.0);
        vec3 p = a.xyz + segment * along;
        float t = dot(p, ray);
        if (t <= 0.05 || t >= farLimit) continue;
        vec3 offset = ray * t - p;
        float life = 1.0 - mix(a.w, b.w, along);
        float flicker = 0.78 + 0.22 * sin(Settings.z * 19.0 + p.x * 10.0 + p.z * 7.0);
        float width = (0.065 + 0.07 * life) * flicker;
        float flame = sin(Settings.z * 15.0 + p.x * 8.0 - p.z * 8.0);
        float height = offset.y > 0.0 ? 2.2 + 0.8 * flame : 0.68;
        float distance = length(vec3(offset.x, offset.y / height, offset.z)) / width;
        glow = max(glow, exp(-distance * distance * 0.62) * life);
        heat = max(heat, exp(-distance * distance * 3.6) * life * flicker);
    }
    if (Settings.y > 0.0) {
        for (int i = 0; i < int(Settings.y); i++) {
            float seed = float(i) * 2.3999632;
            float along = (float(i) + 0.5) * float(count - 1) / Settings.y;
            int j = int(along);
            vec4 point = mix(Points[j], Points[j + 1], fract(along));
            vec3 star = point.xyz + vec3(cos(seed) * 0.14, 0.08 + fract(seed) * 0.22, sin(seed) * 0.14);
            float t = dot(star, ray);
            if (t <= 0.05 || t >= farLimit) continue;
            vec3 right = cross(ray, vec3(0, 1, 0));
            right = length(right) < 0.01 ? vec3(1, 0, 0) : normalize(right);
            vec3 up = cross(right, ray);
            vec3 offset = ray * t - star;
            vec2 p = abs(vec2(dot(offset, right), dot(offset, up)));
            float starShape = (p.x + p.y + 3.0 * min(p.x, p.y)) / 0.075;
            float flicker = 0.45 + 0.55 * sin(seed + Settings.z * 6.0) * sin(seed + Settings.z * 6.0);
            sparkle = max(sparkle, (1.0 - smoothstep(0.12, 1.0, starShape)) * flicker * (1.0 - point.w));
        }
    }
    if (glow + heat + sparkle < 0.002) discard;
    vec4 scene = texture(Scene, TexCoord);
    vec3 core = mix(Tint.rgb, vec3(1.0, 0.96, 0.82), 0.76);
    vec3 fire = Tint.rgb * glow * 0.72 + core * heat * 0.92;
    OutColor = vec4(scene.rgb + fire + mix(Tint.rgb, vec3(1.0), 0.8) * sparkle, scene.a);
}
