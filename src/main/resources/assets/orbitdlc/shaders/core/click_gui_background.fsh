#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D Sampler0;
layout(location = 3) flat in ivec2 radii;
layout(location = 4) in vec2 screenUv;
layout(location = 0) out vec4 OutColor;

void main() {
    vec2 uv = screenUv;
    vec2 pixel = 1.0 / vec2(textureSize(Sampler0, 0));
    float time = float(radii.y) * 0.00067111659;
    vec2 p = uv - 0.5;
    p.x *= pixel.y / pixel.x;
    float ribbon = p.y + 0.19 * sin(p.x * 2.5 + time)
                       + 0.10 * sin(p.x * 5.0 - time);
    float light = exp(-9.0 * abs(ribbon - 0.12));
    float halo = exp(-3.0 * length(p - vec2(0.23 * sin(time), -0.16)));
    vec2 spread = pixel * (3.0 + light * 2.0);
    vec2 warped = clamp(uv + vec2(light * sin(time + p.y * 8.0), light * 0.5) * spread, 0.0, 1.0);
    vec3 scene = texture(Sampler0, warped).rgb * 0.36;
    scene += texture(Sampler0, warped + vec2(spread.x, 0)).rgb * 0.16;
    scene += texture(Sampler0, warped - vec2(spread.x, 0)).rgb * 0.16;
    scene += texture(Sampler0, warped + vec2(0, spread.y)).rgb * 0.16;
    scene += texture(Sampler0, warped - vec2(0, spread.y)).rgb * 0.16;
    vec3 glow = mix(vec3(0.18, 0.09, 0.35), vec3(0.37, 0.10, 0.32), uv.x);
    OutColor = vec4(scene * (0.69 - 0.16 * dot(p, p) / (1.0 + dot(p, p)))
                    + glow * (0.26 * light + 0.25 * halo), 1.0);
}
