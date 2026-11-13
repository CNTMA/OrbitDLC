#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform SkyUniforms {
    mat4 uInverseViewProj;
    vec4 uParams;
    vec4 uExtra;
    vec4 uThemeColor;
};

layout(location = 0) out vec2 vUV;
layout(location = 1) out vec3 vWorldDir;

void main() {
    vec2 pos = vec2((gl_VertexIndex << 1) & 2, gl_VertexIndex & 2) * 2.0 - 1.0;
    gl_Position = vec4(pos, 1.0, 1.0);
    vUV = pos * 0.5 + 0.5;

    vec4 worldRay = uInverseViewProj * vec4(pos, 1.0, 1.0);
    float rw = abs(worldRay.w) < 1.0e-6 ? 1.0e-6 : worldRay.w;
    vWorldDir = worldRay.xyz / rw;
}
