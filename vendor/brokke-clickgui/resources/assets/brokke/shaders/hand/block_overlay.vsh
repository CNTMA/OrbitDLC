#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec3 Position;

layout(location = 0) out vec2 handUv;

void main() {
    gl_Position = vec4(Position, 1.0);
    handUv = (Position.xy + 1.0) * 0.5;
}
