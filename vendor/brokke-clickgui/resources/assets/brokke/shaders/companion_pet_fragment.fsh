#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform CompanionUniforms { mat4 ProjView; vec4 Lighting; };
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
layout(location = 0) in vec2 uv;
layout(location = 1) in vec4 tint;
layout(location = 2) in vec3 normal;
layout(location = 3) in vec3 eye;
layout(location = 0) out vec4 fragColor;
void main() {
    vec3 n = normalize(normal);
    vec3 light = normalize(vec3(-0.45, 0.8, 0.55));
    vec4 skin = uv.x < 0.0 ? texture(Sampler1, vec2(-uv.x - 1.0, uv.y)) : texture(Sampler0, uv);
    if (skin.a < 0.5) discard;
    vec3 base = tint.rgb * skin.rgb;
    float diffuse = max(dot(n, light), 0.0);
    float fill = max(dot(n, normalize(vec3(0.65, 0.3, -0.55))), 0.0);
    float ambient = 0.58 + n.y * 0.035;
    vec3 color = base * (ambient + diffuse * 0.33 + fill * 0.09) * Lighting.x;
    fragColor = vec4(color, 1.0);
}
