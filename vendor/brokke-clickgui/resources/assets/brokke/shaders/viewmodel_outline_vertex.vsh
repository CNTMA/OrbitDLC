#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) out vec2 vTexCoord;

const vec2 coords[6] = vec2[](
    vec2(-1.0, -1.0),
    vec2( 1.0, -1.0),
    vec2( 1.0,  1.0),
    vec2(-1.0, -1.0),
    vec2( 1.0,  1.0),
    vec2(-1.0,  1.0)
);

void main() {
    vec2 pos = coords[gl_VertexIndex];
    gl_Position = vec4(pos, 0.0, 1.0);
    vTexCoord = pos * 0.5 + 0.5;
}
