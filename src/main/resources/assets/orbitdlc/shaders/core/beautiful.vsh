#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location = 0) out vec2 TexCoord;
layout(location = 1) out vec2 PixelSize;
layout(std140) uniform BeautifulSettings {
    mat4 InverseViewProjection;
    vec4 Params;
};

void main() {
    TexCoord = vec2((gl_VertexIndex << 1) & 2, gl_VertexIndex & 2);
    gl_Position = vec4(TexCoord * 2.0 - 1.0, 0.0, 1.0);
    PixelSize = Params.xy;
}
