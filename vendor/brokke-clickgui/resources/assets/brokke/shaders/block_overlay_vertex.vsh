#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) out vec2 vTexCoord;

void main() {
    vec2 positions[6] = vec2[](
        vec2(0.0, 0.0),
        vec2(1.0, 0.0),
        vec2(1.0, 1.0),
        vec2(0.0, 0.0),
        vec2(1.0, 1.0),
        vec2(0.0, 1.0)
    );

    vec2 pos = positions[gl_VertexIndex];
    gl_Position = vec4(pos * 2.0 - 1.0, 0.0, 1.0);
    vTexCoord = pos;
}
