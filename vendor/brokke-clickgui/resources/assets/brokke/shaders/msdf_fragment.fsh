#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform MsdfData {
    mat4 uProjection;
    vec4 uColor;
    vec4 uParams;
    vec4 uZ_Padding;
};

layout(location = 0) in vec2 vTexCoord;
layout(location = 1) in vec4 vColor;
layout(location = 2) in float vPxRange;
layout(location = 3) in vec2 vScreenPos;
layout(location = 4) in float vTime;
layout(location = 5) in float vShimmerMode;

layout(location = 0) out vec4 fragColor;

uniform sampler2D Sampler0;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

float screenPxRange() {
    vec2 unitRange = vec2(vPxRange) / vec2(textureSize(Sampler0, 0));
    vec2 screenTexSize = vec2(1.0) / max(fwidth(vTexCoord), vec2(1e-6));
    return max(0.5 * dot(unitRange, screenTexSize), 1.0);
}

void main() {
    vec4 mtsdf = texture(Sampler0, vTexCoord);
    float sd = median(mtsdf.r, mtsdf.g, mtsdf.b);
    float pxRange = screenPxRange();
    float screenPxDist = pxRange * (sd - 0.5);
    float alpha = clamp(screenPxDist + 0.5, 0.0, 1.0);
    
    if (alpha < 0.01) discard;
    
    if (vShimmerMode > 0.5) {
        float maxC = max(vColor.r, max(vColor.g, vColor.b));
        float minC = min(vColor.r, min(vColor.g, vColor.b));
        vec3 baseTheme = ((maxC - minC) > 0.08) ? vColor.rgb : uColor.rgb;
        vec3 darkTheme = baseTheme * 0.38;
        vec3 lightTheme = min(baseTheme * 1.18, vec3(1.0));
        
        if (vShimmerMode > 1.5) {
            float iconFlow = (vScreenPos.x * 0.045 - vScreenPos.y * 0.045) - vTime * 2.0;
            float w1 = sin(iconFlow);
            float w2 = sin(iconFlow * 2.0 + 1.2) * 0.35;
            float wave = clamp((w1 + w2 + 1.0) * 0.5, 0.0, 1.0);
            vec3 finalIconRgb = mix(darkTheme, lightTheme, wave);
            fragColor = vec4(finalIconRgb, vColor.a * alpha);
        } else {
            float textFlow = (vScreenPos.x * 0.14 + vScreenPos.y * 0.05) - vTime * 3.5;
            float textWave = smoothstep(0.05, 0.95, 0.5 + 0.5 * sin(textFlow));
            vec3 textRgb = mix(darkTheme, lightTheme, textWave);
            fragColor = vec4(textRgb, vColor.a * alpha);
        }
    } else {
        fragColor = vec4(vColor.rgb, vColor.a * alpha);
    }
}
