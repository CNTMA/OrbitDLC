#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform Uniforms { mat4 ProjView; vec4 Animation; };
layout(location = 0) in vec3 surface;
layout(location = 1) in vec4 tint;
layout(location = 0) out vec4 fragColor;
void main() {
    vec3 n = normalize(cross(dFdx(surface), dFdy(surface)));
    float t = Animation.x;
    float edge = pow(1.0 - abs(dot(n, normalize(-surface))), 2.0);
    float glint = pow(max(0.0, dot(n, normalize(vec3(sin(t * 0.7), 0.7, cos(t * 0.5))))), 26.0);
    vec3 iridescence = 0.5 + 0.5 * cos(vec3(0, 2.1, 4.2) + n.x * 3.0 + n.y * 2.0 + t * 0.5);
    vec3 color = mix(tint.rgb, iridescence, 0.18 + edge * 0.22);
    color += vec3(0.85, 0.96, 1.0) * (glint * 0.85 + edge * 0.3);
    fragColor = vec4(color, tint.a * (0.7 + edge * 0.3));
}
