#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:globals.glsl>
uniform sampler2D Sampler0;
layout(location = 0) in vec3 surface;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 uv;
layout(location = 3) in vec4 tint;
layout(location = 4) in vec2 faceUV;
layout(location = 5) in vec3 viewPosition;
layout(location = 0) out vec4 fragColor;
void main() {
    vec4 textureColor = texture(Sampler0, uv);
#if PRISM_MATERIAL == 4
    vec2 edgeDistance = min(faceUV, 1.0 - faceUV) / max(fwidth(faceUV), vec2(0.00001));
    float distance = min(edgeDistance.x, edgeDistance.y);
    float coverage = 1.0 - smoothstep(PRISM_OUTLINE_WIDTH - 0.5, PRISM_OUTLINE_WIDTH + 0.5, distance);
    if (coverage < 0.003) discard;
    fragColor = vec4(tint.rgb, tint.a * coverage);
    return;
#elif PRISM_MATERIAL == 3
    vec3 n = normalize(normal);
    vec3 eye = normalize(-viewPosition);
    float fresnel = pow(1.0 - abs(dot(n, eye)), 3.5);
    float keyLight = max(0.0, dot(n, normalize(vec3(-0.45, 0.75, 0.55))));
    float fillLight = max(0.0, dot(n, normalize(vec3(0.75, 0.15, 0.45))));
    vec3 halfVector = normalize(eye + normalize(vec3(-0.45, 0.75, 0.55)));
    float gloss = pow(max(0.0, dot(n, halfVector)), 34.0);
    float softbox = pow(max(0.0, dot(reflect(-eye, n), normalize(vec3(0.4, 0.75, 0.55)))), 10.0);
    float shade = 0.24 + keyLight * 0.38 + fillLight * 0.14;
    vec3 glass = vec3(shade) + vec3(0.92, 0.97, 1.0) * (gloss * 0.48 + softbox * 0.18 + fresnel * 0.12);
    float alpha = min(0.85, 0.16 + tint.a * 0.60 + fresnel * 0.11 + gloss * 0.14 + softbox * 0.06);
    fragColor = vec4(glass, alpha);
    return;
#else
    float mask = 1.0;
    float t = GameTime * 1200.0;
    vec3 n = normalize(normal);
    float fresnel = pow(1.0 - abs(n.z), 2.3);
    float flow = sin(dot(surface, vec3(3.0, 4.0, 2.0)) - t * 1.5);
    vec3 rainbow = 0.5 + 0.5 * cos(vec3(0.0, 2.1, 4.2) + n.y * 3.0 + n.x * 2.0 + flow * 0.5 + t * 0.3);
    vec3 base = tint.rgb;
    float alpha = tint.a * mask;
#if PRISM_MATERIAL == 1
    float scan = pow(0.5 + 0.5 * sin(surface.y * 95.0 - t * 6.0), 8.0);
    float band = pow(max(0.0, sin(surface.y * 5.0 - t * 2.0)), 18.0);
    base = base * (0.35 + fresnel * 0.9 + scan * 0.3) + vec3(0.65, 0.9, 1.0) * band * 0.45;
    alpha *= 0.4 + fresnel * 0.5 + scan * 0.1;
#elif PRISM_MATERIAL == 2
    vec3 facet = normalize(cross(dFdx(surface), dFdy(surface)));
    float caustic = pow(max(0.0, sin(dot(surface, vec3(13.0, 17.0, 11.0)) + t * 1.4)
            * sin(dot(surface, vec3(-9.0, 12.0, 15.0)) - t)), 10.0);
    float shine = pow(max(0.0, dot(n, normalize(vec3(sin(t * 0.4), 0.65, 1.0)))), 35.0);
    base = mix(base * (0.55 + 0.25 * abs(facet.y)), rainbow, 0.22 + fresnel * 0.3);
    base += vec3(0.8, 0.95, 1.0) * (shine * 0.8 + caustic * 0.65 + fresnel * 0.35);
    alpha *= 0.62 + fresnel * 0.38;
#else
    float shine = pow(max(0.0, dot(n, normalize(vec3(-0.4, 0.65, 1.0)))), 25.0);
    base = mix(base * (0.65 + n.y * 0.2), rainbow, 0.16 + fresnel * 0.35);
    base += vec3(0.85, 0.95, 1.0) * (shine * 0.65 + fresnel * 0.4);
#endif
    fragColor = vec4(base, alpha);
#endif
}
