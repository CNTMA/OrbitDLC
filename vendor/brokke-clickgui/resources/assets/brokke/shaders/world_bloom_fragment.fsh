#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 vUV;
layout(location = 1) in vec4 vColor;

layout(location = 0) out vec4 fragColor;

uniform sampler2D Sampler0;

void main() {
    vec2 d = (vUV - vec2(0.5)) * 2.0;
    float distSq = dot(d, d);
    if (distSq >= 1.0) discard;
    float dist = sqrt(distSq);
    float falloff = (1.0 - dist) * (1.0 - dist * 0.5);
    vec4 tex = texture(Sampler0, vUV);
    vec4 col = tex * vColor;
    col.a *= falloff;
    if (col.a <= 0.001) discard;
    fragColor = col;
}
