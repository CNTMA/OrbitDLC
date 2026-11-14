#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location = 0) in vec2 TexCoord;
layout(location = 1) in vec2 PixelSize;
layout(location = 0) out vec4 OutColor;
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
layout(std140) uniform BeautifulSettings {
    mat4 InverseViewProjection;
    vec4 Params;
};

float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

vec3 highlight(vec2 uv) {
    vec3 color = texture(Sampler0, uv).rgb;
    return color * smoothstep(0.56, 0.94, luminance(color));
}

void main() {
    vec2 uv = TexCoord;
    vec4 source = texture(Sampler0, uv);
    float strength = Params.z;
    vec2 pixel = PixelSize;
    float luma = clamp(luminance(source.rgb), 0.0, 1.0);
    vec3 graded = mix(vec3(luma), source.rgb, 1.12);
    graded += vec3(-0.025, 0.008, 0.045) * (1.0 - luma) * luma;
    graded += vec3(0.085, 0.036, -0.025) * smoothstep(0.35, 1.0, luma);
    graded += (graded - 0.5) * luma * (1.0 - luma) * 0.32;

    vec3 bloom = highlight(uv) * 0.16;
    float scale = max(1.0, 0.0014 / pixel.y);
    for (int i = 0; i < 8; ++i) {
        float angle = float(i) * 0.785398;
        vec2 ray = vec2(cos(angle), sin(angle)) * pixel * scale;
        bloom += highlight(uv + ray * 6.0) * 0.055;
        bloom += highlight(uv + ray * 18.0) * 0.035;
        bloom += highlight(uv + ray * 36.0) * 0.015;
    }
    graded += bloom * vec3(0.27, 0.23, 0.19);

    float depth = texture(Sampler1, uv).r;
    if (depth > 0.000001) {
        float clipZ = mix(depth * 2.0 - 1.0, depth, Params.w);
        vec4 position = InverseViewProjection * vec4(uv * 2.0 - 1.0, clipZ, 1.0);
        float distance = length(position.xyz) / max(abs(position.w), 0.00001);
        float haze = smoothstep(25.0, 175.0, distance) * 0.22;
        vec3 air = mix(vec3(0.14, 0.19, 0.26), vec3(0.69, 0.70, 0.65), smoothstep(0.1, 0.77, luma));
        graded = mix(graded, air, haze);
    }

    float edge = smoothstep(0.35, 0.72, length(uv - 0.5));
    graded *= 1.0 - 0.085 * edge;
    OutColor = vec4(clamp(mix(source.rgb, graded, strength), 0.0, 1.0), source.a);
}
