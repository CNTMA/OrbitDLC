#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;

layout(std140) uniform Uniforms {
    mat4 uProjView;
    vec4 uOffset;
    vec4 Origin4;
    vec4 SunDir4;
    vec4 SunColor4;
    vec4 SkyColor4;
    vec4 uGrain;
    vec4 uSurface;
};

layout(location = 0) out vec3 WorldPos;
layout(location = 1) out vec3 ViewPos;
layout(location = 2) out vec4 FragColor;

void main() {
    vec3 relative = Position + uOffset.xyz;
    gl_Position = uProjView * vec4(relative, 1.0);
    WorldPos = relative + Origin4.xyz;
    ViewPos = relative;
    FragColor = Color;
}
