#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(std140) uniform Uniforms { mat4 ProjView; vec4 Animation; };
layout(location = 0) out vec3 surface;
layout(location = 1) out vec4 tint;
void main() {
    surface = Position;
    tint = Color;
    gl_Position = ProjView * vec4(Position, 1.0);
}
