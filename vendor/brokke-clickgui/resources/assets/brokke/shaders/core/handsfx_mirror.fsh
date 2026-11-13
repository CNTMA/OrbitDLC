#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D BlurredSampler;
uniform sampler2D HandSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform HandsFxData {
    vec4 color1;
    vec4 color2;
    vec4 fillColor;
    vec4 outlineColor;
    vec4 params0;
    vec4 params1;
    vec4 params2;
    vec4 params3;
    vec4 screen;
    vec4 misc;
};

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec2 uv = texCoord;
    float mask = texture(MaskSampler, uv).a;
    if (mask < 0.05) {
        discard;
    }

    vec3 handColor = texture(HandSampler, uv).rgb;
    vec3 blurred = texture(BlurredSampler, uv).rgb;
    vec3 tintedGlass = blurred * fillColor.rgb;
    vec3 finalRgb = mix(tintedGlass, handColor, misc.x);
    fragColor = vec4(finalRgb, clamp(mask * misc.z, 0.0, 1.0));
}
