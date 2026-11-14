#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location = 0) in vec2 TexCoord;
layout(location = 0) out vec4 OutColor;
uniform sampler2D Scene;
uniform sampler2D Highlights;
layout(std140) uniform BloomSettings { vec4 Settings; };
void main() {
    vec2 px = 1.0 / vec2(textureSize(Highlights, 0));
    vec3 glow = texture(Highlights, TexCoord).rgb * 0.10;
    for (int i = 0; i < 8; i++) {
        float angle = float(i) * 0.78539816;
        vec2 direction = vec2(cos(angle), sin(angle)) * px;
        glow += texture(Highlights, TexCoord + direction * 2.5).rgb * 0.0625;
        glow += texture(Highlights, TexCoord + direction * 6.0).rgb * 0.05;
    }
    vec4 scene = texture(Scene, TexCoord);
    OutColor = vec4(scene.rgb + glow * Settings.x * 0.34, scene.a);
}
