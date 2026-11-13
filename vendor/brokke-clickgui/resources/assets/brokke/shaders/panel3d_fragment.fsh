#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 in_uv;
layout(location = 0) out vec4 out_color;

uniform sampler2D texture_in;

layout(std140) uniform Panel3DData {
    mat4 inv_homography;
    vec4 rect;
    vec4 params;
    vec4 exclude_a;
    vec4 exclude_b;
    vec4 tooltip_rect;
};

bool inside(vec4 r, vec2 p) {
    return p.x >= r.x && p.y >= r.y && p.x <= r.z && p.y <= r.w;
}

void main() {
    vec2 size = vec2(textureSize(texture_in, 0));
    vec2 pixel = vec2(in_uv.x * size.x, (1.0 - in_uv.y) * size.y);
    vec3 q = mat3(inv_homography) * vec3(pixel, 1.0);
    if (q.z <= 0.0) discard;
    vec2 tex = q.xy / q.z;
    bool tip = inside(tooltip_rect, tex);
    if (!inside(rect, tex)) discard;
    if (params.y > 0.5) {
        if ((inside(exclude_a, tex) || inside(exclude_b, tex)) && !tip) discard;
    } else if (tip) discard;
    if (tex.x < 0.0 || tex.y < 0.0 || tex.x > size.x || tex.y > size.y) discard;
    out_color = texture(texture_in, vec2(tex.x / size.x, 1.0 - tex.y / size.y)) * params.x;
}
