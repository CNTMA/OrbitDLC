#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

layout(location = 0) out vec2 TexCoord;
layout(location = 1) out vec4 FragColor;

void main() {
    vec4 pos = ProjMat * ModelViewMat * vec4(Position, 1.0);

    gl_Position = pos;
    TexCoord = pos.xy * 0.5 + 0.5;
    FragColor = Color;
}
