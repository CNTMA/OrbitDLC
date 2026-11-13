#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D Sampler0;
layout(location = 4) in vec2 uv;
layout(location = 0) out vec4 OutColor;

void main() {
    vec3 scene = texture(Sampler0, uv).rgb;
    float luminance = dot(scene, vec3(0.2126, 0.7152, 0.0722));
    vec3 shaded = scene * vec3(0.72, 0.81, 0.90);
    vec3 highlights = smoothstep(0.27, 0.95, scene) * vec3(0.23, 0.25, 0.27);
    OutColor = vec4(mix(shaded + highlights, scene, smoothstep(0.70, 1.0, luminance)), 1.0);
}
