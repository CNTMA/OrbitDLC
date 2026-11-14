#version 330
#extension GL_ARB_separate_shader_objects : require

struct OutlineData {
    vec4 rect;
    vec4 radii;
    vec4 params;
    vec4 c0;
    vec4 c1;
    vec4 c2;
    vec4 c3;
};

layout(std140) uniform Uniforms {
    mat4 uProjection;
    vec4 uHeader;
    OutlineData uOutlines[64];
};

layout(location = 0) out vec2 vUV;
layout(location = 1) out vec2 vSize;
layout(location = 2) flat out vec4 vRadii;
layout(location = 3) flat out float vThickness;
layout(location = 4) flat out vec4 vC0;
layout(location = 5) flat out vec4 vC1;
layout(location = 6) flat out vec4 vC2;
layout(location = 7) flat out vec4 vC3;

void main() {
    int outlineIdx = gl_VertexIndex / 6;
    int cornerIdx = gl_VertexIndex % 6;

    vec2 vertices[4] = vec2[](
        vec2(0.0, 0.0),
        vec2(1.0, 0.0),
        vec2(1.0, 1.0),
        vec2(0.0, 1.0)
    );
    int indices[6] = int[](0, 1, 2, 2, 3, 0);
    vec2 vertex = vertices[indices[cornerIdx]];

    OutlineData data = uOutlines[outlineIdx];
    float thickness = data.params.x;
    float pad = max(thickness, 1.0) + 1.5;

    vec2 pos = data.rect.xy - vec2(pad) + vertex * (data.rect.zw + vec2(pad * 2.0));
    vUV = (pos - data.rect.xy) / max(data.rect.zw, vec2(1.0));
    vSize = data.rect.zw;
    vRadii = data.radii;
    vThickness = thickness;
    vC0 = data.c0;
    vC1 = data.c1;
    vC2 = data.c2;
    vC3 = data.c3;

    gl_Position = uProjection * vec4(pos, uHeader.x, 1.0);
}
