#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform Uniforms {
    mat4 ProjMat;
    vec4 RectPosSize;
    vec4 Params;
    vec4 Color;
    vec4 Misc;
};

layout(location = 0) out vec2 v_TexCoord;
layout(location = 1) out vec2 v_RectSize;

const vec2 CORNERS[6] = vec2[6](
    vec2(0.0, 0.0), vec2(0.0, 1.0), vec2(1.0, 1.0),
    vec2(0.0, 0.0), vec2(1.0, 1.0), vec2(1.0, 0.0)
);

void main() {
    v_RectSize = RectPosSize.zw;
    float thickness = max(Params.y, 0.001);
    float glowSize = Misc.y > 0.0 ? Misc.y : max(thickness * 3.0, 8.0);

    vec2 rawPos = CORNERS[gl_VertexIndex];
    v_TexCoord = rawPos;

    vec2 extendedSize = RectPosSize.zw + vec2(glowSize * 2.0);
    vec2 finalPos = RectPosSize.xy - vec2(glowSize) + (rawPos * extendedSize);

    gl_Position = ProjMat * vec4(finalPos, Misc.x, 1.0);
}