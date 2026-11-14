#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location = 0) in vec2 TexCoord;
layout(location = 0) out vec4 OutColor;
uniform sampler2D SkyImage;
layout(std140) uniform SkyPngSettings {
    mat4 InverseViewProjection;
};

void main() {
    const float pi = 3.14159265;
    vec4 world = InverseViewProjection * vec4(TexCoord * 2.0 - 1.0, 1.0, 1.0);
    vec3 ray = normalize(world.xyz);
    vec2 uv = vec2(fract(atan(ray.z, ray.x) / (2.0 * pi) + 0.5),
                   0.5 - asin(clamp(ray.y, -1.0, 1.0)) / pi);
    vec4 image = texture(SkyImage, uv);
    OutColor = vec4(mix(vec3(0.025, 0.036, 0.065), image.rgb, image.a), 1.0);
}
