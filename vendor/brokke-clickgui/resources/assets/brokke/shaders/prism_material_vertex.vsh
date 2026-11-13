#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 5) in vec3 Normal;
layout(location = 0) out vec3 surface;
layout(location = 1) out vec3 normal;
layout(location = 2) out vec2 uv;
layout(location = 3) out vec4 tint;
layout(location = 4) out vec2 faceUV;
layout(location = 5) out vec3 viewPosition;
void main() {
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    viewPosition = view.xyz;
    surface = Position;
    normal = normalize(mat3(ModelViewMat) * Normal);
    uv = UV0;
    tint = Color * ColorModulator;
    int corner = gl_VertexIndex & 3;
    faceUV = vec2(corner >= 2 ? 1.0 : 0.0, corner == 1 || corner == 2 ? 1.0 : 0.0);
    gl_Position = ProjMat * view;
}
