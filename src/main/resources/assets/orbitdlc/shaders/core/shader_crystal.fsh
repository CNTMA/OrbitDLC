#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location = 0) in vec2 TexCoord;
layout(location = 0) out vec4 OutColor;
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
layout(std140) uniform CrystalSettings {
    mat4 InverseViewProjection;
    vec4 Frame;
    vec4 Tint;
    vec4 Crystals[8];
};

vec3 unproject(float depth) {
    vec4 p = InverseViewProjection * vec4(TexCoord * 2.0 - 1.0,
        mix(depth * 2.0 - 1.0, depth, Frame.y), 1.0);
    return p.xyz / p.w;
}

mat3 rotation(vec4 crystal) {
    float c = cos(crystal.w), s = sin(crystal.w);
    return mat3(c, 0, -s, 0, 1, 0, s, 0, c)
         * mat3(0.82, 0.57, 0, -0.57, 0.82, 0, 0, 0, 1);
}

void main() {
    float depth = texture(Sampler1, TexCoord).r;
    vec3 ray = normalize(unproject(0.5));
    float nearPlane = length(unproject(1.0));
    float nearest = depth > 0.000001 ? length(unproject(depth)) : 128.0;
    vec3 localPoint = vec3(0);
    vec3 normal = vec3(0);
    float thickness = 0.0;
    bool hit = false;
    const float size = 0.61;
    for (int i = 0; i < int(Frame.x); ++i) {
        mat3 basis = rotation(Crystals[i]);
        vec3 origin = transpose(basis) * -Crystals[i].xyz;
        vec3 direction = transpose(basis) * ray;
        vec3 signs = mix(vec3(-1), vec3(1), greaterThanEqual(direction, vec3(0)));
        vec3 inv = signs / max(abs(direction), vec3(0.00001));
        vec3 a = (-size - origin) * inv;
        vec3 b = ( size - origin) * inv;
        vec3 enter = min(a, b);
        vec3 leave = max(a, b);
        float entry = max(max(enter.x, enter.y), enter.z);
        float exit = min(min(leave.x, leave.y), leave.z);
        // If the front face is clipped by the camera, use the far face from inside the glass.
        bool inside = entry <= nearPlane;
        float surface = inside ? exit : entry;
        if (exit <= entry || surface <= nearPlane || surface >= nearest) continue;
        nearest = surface;
        localPoint = origin + direction * surface;
        vec3 face = inside
            ? step(leave.xyz, leave.yzx) * step(leave.xyz, leave.zxy)
            : step(enter.yzx, enter.xyz) * step(enter.zxy, enter.xyz);
        normal = basis * (-signs * face);
        thickness = exit - max(entry, nearPlane);
        hit = true;
    }
    if (!hit) discard;

    vec3 facePoint = abs(localPoint);
    float side = min(max(facePoint.x, facePoint.y),
                     min(max(facePoint.x, facePoint.z), max(facePoint.y, facePoint.z)));
    float bevel = smoothstep(size - 0.075, size, side);
    float rim = pow(1.0 - clamp(dot(normal, -ray), 0.0, 1.0), 3.0);
    vec2 offset = (normal.xy * 0.016 + localPoint.xy * 0.009) * (0.5 + bevel);
    vec2 sampleUv = clamp(TexCoord + offset, Frame.zw, 1.0 - Frame.zw);
    vec3 glass = vec3(texture(Sampler0, sampleUv + offset * 0.11).r,
                      texture(Sampler0, sampleUv).g,
                      texture(Sampler0, sampleUv - offset * 0.11).b);
    glass *= mix(vec3(1.0), Tint.rgb, clamp(thickness * 0.32, 0.0, 0.46));
    float glint = pow(max(0.0, dot(reflect(ray, normal), normalize(vec3(-0.45, 0.85, 0.3)))), 18.0);
    glass = mix(glass, Tint.rgb, 0.14 + rim * 0.28);
    glass += mix(Tint.rgb, vec3(1), 0.55) * (bevel * 0.52 + glint * 0.48);
    OutColor = vec4(glass, 1.0);
}
