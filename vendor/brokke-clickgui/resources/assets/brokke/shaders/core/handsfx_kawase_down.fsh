#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InputSampler;

layout(std140) uniform HandsFxKawase {
    vec4 kawase;
};

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec2 uv = texCoord;
    vec2 h = kawase.xy * kawase.z;
    vec4 sum = texture(InputSampler, uv) * 4.0;
    sum += texture(InputSampler, uv - h);
    sum += texture(InputSampler, uv + h);
    sum += texture(InputSampler, uv + vec2(h.x, -h.y));
    sum += texture(InputSampler, uv - vec2(h.x, -h.y));
    fragColor = sum / 8.0;
}
