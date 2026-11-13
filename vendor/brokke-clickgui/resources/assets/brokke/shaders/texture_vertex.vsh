#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform Uniforms {
    mat4 uProjection;
    vec4 uRect;
    vec4 uColor;
    vec4 uRadius_Padding;
    vec4 uZ_Padding;
    vec4 uUV;
};

layout(location = 0) out vec2 vUV;
layout(location = 1) out vec2 vPos;

void main() {
    vec2 vertices[4] = vec2[](
        vec2(0.0, 0.0),
        vec2(1.0, 0.0),
        vec2(1.0, 1.0),
        vec2(0.0, 1.0)
    );
    int indices[6] = int[](0, 1, 2, 2, 3, 0);
    vec2 vertex = vertices[indices[gl_VertexIndex % 6]];

    float ang = uRadius_Padding.y;
    vec2 local = vertex * uRect.zw - uRect.zw * 0.5;
    if (ang != 0.0) {
        float ca = cos(ang);
        float sa = sin(ang);
        local = vec2(local.x * ca - local.y * sa, local.x * sa + local.y * ca);
    }

    vPos = vertex * uRect.zw;
    vec2 pos = uRect.xy + uRect.zw * 0.5 + local;
    gl_Position = uProjection * vec4(pos, uZ_Padding.x, 1.0);

    vUV = uUV.xy + vertex * uUV.zw;
}
