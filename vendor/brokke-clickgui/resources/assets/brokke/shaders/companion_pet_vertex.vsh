#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;
layout(location = 3) in vec4 Normal;
layout(std140) uniform CompanionUniforms { mat4 ProjView; vec4 Lighting; };
layout(location = 0) out vec2 uv;
layout(location = 1) out vec4 tint;
layout(location = 2) out vec3 normal;
layout(location = 3) out vec3 eye;
void main() {
    uv = UV0;
    tint = Color;
    normal = Normal.xyz;
    eye = -Position;
    gl_Position = ProjView * vec4(Position, 1.0);
}
