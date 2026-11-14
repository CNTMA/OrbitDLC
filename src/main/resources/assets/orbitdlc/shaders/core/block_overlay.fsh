#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location = 0) in vec2 TexCoord;
layout(location = 0) out vec4 OutColor;

uniform sampler2D Scene;
uniform sampler2D Depth;
layout(std140) uniform OverlaySettings {
    mat4 InverseViewProjection;
    vec4 BlockAndMode;
    vec4 ColorAndDepth;
    vec4 Frame;
};

void main() {
    float depth = texture(Depth, TexCoord).r;
    if (depth <= 0.000001) discard;
    vec4 point = InverseViewProjection * vec4(TexCoord * 2.0 - 1.0,
        mix(depth * 2.0 - 1.0, depth, ColorAndDepth.w), 1.0);
    vec3 p = point.xyz / point.w - BlockAndMode.xyz;
    if (any(lessThan(p, vec3(-0.015))) || any(greaterThan(p, vec3(1.015)))) discard;
    vec3 edgeDistances = abs(clamp(p, 0.0, 1.0) - 0.5);
    float edge = min(max(edgeDistances.x, edgeDistances.y),
        min(max(edgeDistances.x, edgeDistances.z), max(edgeDistances.y, edgeDistances.z)));
    float rim = smoothstep(0.40, 0.498, edge);
    vec3 tint = ColorAndDepth.rgb;
    vec3 scene = texture(Scene, TexCoord).rgb;

    if (BlockAndMode.w < 0.5) {
        vec2 ripple = sin(p.xy * 12.0 + p.z * 7.0 + Frame.z * 6.2831853) * Frame.xy * (2.5 + rim * 5.0);
        vec2 offset = clamp(TexCoord + ripple, Frame.xy, 1.0 - Frame.xy);
        scene = vec3(texture(Scene, offset + ripple * 0.25).r, texture(Scene, offset).g,
            texture(Scene, offset - ripple * 0.25).b);
        scene = mix(scene, tint, 0.14 + 0.55 * rim);
        scene += vec3(0.18) * rim;
    } else if (BlockAndMode.w < 1.5) {
        scene = mix(scene, tint, 0.10 + 0.75 * rim) + tint * rim * 0.24;
    } else {
        scene = mix(scene, tint, 0.36 + 0.40 * rim);
    }
    OutColor = vec4(scene, 1.0);
}
