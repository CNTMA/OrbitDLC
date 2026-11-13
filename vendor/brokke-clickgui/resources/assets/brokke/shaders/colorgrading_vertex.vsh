#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) out vec2 vUV;

void main() {
    vec2 vertex = vec2((gl_VertexIndex << 1) & 2, gl_VertexIndex & 2) * 2.0 - 1.0;

    vUV = vertex * 0.5 + 0.5;
    gl_Position = vec4(vertex, 0.0, 1.0);
}
