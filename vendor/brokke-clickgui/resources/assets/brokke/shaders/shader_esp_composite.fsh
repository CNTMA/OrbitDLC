#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 vTexCoord;
layout(location = 0) out vec4 fragColor;

layout(std140) uniform CompositeUniforms {
    vec4 uResolutionTime;
    vec4 uParams;
    vec4 uColor1;
    vec4 uColor2;
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

vec4 tintAt(vec2 uv, float time, float mode) {
    if (mode > 0.5) {
        float grad = clamp((1.0 - uv.y) + sin(time * 2.0 + uv.y * 3.0) * 0.15, 0.0, 1.0);
        return mix(uColor2, uColor1, grad);
    }
    return uColor1;
}

void main() {
    vec2 uv = vTexCoord;
    vec2 res = uResolutionTime.xy;
    float time = uResolutionTime.z;
    float mode = uResolutionTime.w;
    float outlineRadius = uParams.x;
    float outlineStrength = uParams.y;
    float glowIntensity = uParams.z;
    float fillOpacity = uParams.w;

    float centerA = texture(Sampler0, uv).a;

    vec2 gpx = res * 2.0;
    vec4 blur = texture(Sampler1, uv) * 0.4;
    blur += texture(Sampler1, uv + vec2( gpx.x,  gpx.y)) * 0.15;
    blur += texture(Sampler1, uv + vec2(-gpx.x,  gpx.y)) * 0.15;
    blur += texture(Sampler1, uv + vec2( gpx.x, -gpx.y)) * 0.15;
    blur += texture(Sampler1, uv + vec2(-gpx.x, -gpx.y)) * 0.15;
    float blurA = clamp(blur.a, 0.0, 1.0);

    vec4 tint = tintAt(uv, time, mode);
    vec3 glowColor = mix(tint.rgb, vec3(1.0), 0.15);

    if (centerA > 0.05) {
        float edge = clamp(1.0 - blurA, 0.0, 1.0);
        float inner = clamp(pow(edge, 1.3) * glowIntensity * 0.9, 0.0, 0.85);
        float bodyA = clamp(fillOpacity + inner + 0.06 * glowIntensity, 0.0, 1.0);
        vec3 bodyColor = mix(tint.rgb, mix(tint.rgb, vec3(1.0), 0.35), inner);
        fragColor = vec4(bodyColor, bodyA * tint.a);
        return;
    }

    vec2 px = res * outlineRadius;
    float maxA = 0.0;
    maxA = max(maxA, texture(Sampler0, uv + vec2( px.x, 0.0)).a);
    maxA = max(maxA, texture(Sampler0, uv + vec2(-px.x, 0.0)).a);
    maxA = max(maxA, texture(Sampler0, uv + vec2(0.0,  px.y)).a);
    maxA = max(maxA, texture(Sampler0, uv + vec2(0.0, -px.y)).a);
    maxA = max(maxA, texture(Sampler0, uv + vec2( px.x * 0.7071,  px.y * 0.7071)).a);
    maxA = max(maxA, texture(Sampler0, uv + vec2(-px.x * 0.7071,  px.y * 0.7071)).a);
    maxA = max(maxA, texture(Sampler0, uv + vec2( px.x * 0.7071, -px.y * 0.7071)).a);
    maxA = max(maxA, texture(Sampler0, uv + vec2(-px.x * 0.7071, -px.y * 0.7071)).a);

    float outline = clamp(maxA * outlineStrength, 0.0, 1.0);
    float glow = 1.0 - exp(-blurA * glowIntensity * 2.2);

    float totalAlpha = clamp(max(outline, glow), 0.0, 1.0);
    if (totalAlpha < 0.004) {
        discard;
    }

    vec3 outlineColor = mix(tint.rgb, vec3(1.0), 0.55);
    vec3 resultColor = mix(glowColor, outlineColor, outline);

    fragColor = vec4(resultColor, totalAlpha * tint.a);
}
