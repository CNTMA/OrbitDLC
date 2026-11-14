#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location = 0) in vec2 TexCoord;
layout(location = 0) out vec4 OutColor;
uniform sampler2D Scene;
vec3 bright(vec2 uv) {
    vec3 color = texture(Scene, uv).rgb;
    float peak = max(color.r, max(color.g, color.b));
    return color * smoothstep(0.66, 1.0, peak);
}
void main() {
    vec2 d = 1.0 / vec2(textureSize(Scene, 0));
    vec3 color = bright(TexCoord + d) + bright(TexCoord - d)
               + bright(TexCoord + vec2(d.x, -d.y)) + bright(TexCoord + vec2(-d.x, d.y));
    OutColor = vec4(color * 0.25, 1.0);
}
