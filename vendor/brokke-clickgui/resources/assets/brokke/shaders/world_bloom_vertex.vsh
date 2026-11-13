#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;

layout(std140) uniform Uniforms {
    mat4 uProjView;
};

layout(location = 0) out vec2 vUV;
layout(location = 1) out vec4 vColor;

void main() {
    vUV = UV0;
    vColor = Color;
    gl_Position = uProjView * vec4(Position, 1.0);
}
