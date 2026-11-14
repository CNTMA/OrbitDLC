#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 localPos;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) flat in vec2 halfSize;
layout(location = 3) flat in ivec2 params;

layout(location = 0) out vec4 fragColor;

void main() {
    float r = min(halfSize.x, halfSize.y);
    float t = float(params.y) * 0.0625;
    float dist = length(localPos);
    float ring = abs(dist - (r - t * 0.5)) - t * 0.5;
    float ringAA = clamp(0.5 - ring / max(fwidth(ring), 1e-4), 0.0, 1.0);
    if (ringAA <= 0.0) {
        discard;
    }

    float angle = atan(localPos.x, -localPos.y);
    if (angle < 0.0) angle += 6.28318530718;
    float normAngle = angle / 6.28318530718;
    float progress = clamp(float(params.x) * 0.001, 0.0, 1.0);

    float progressMask = 1.0;
    if (progress < 0.999) {
        float angleAA = 1.0 / max(dist * 6.28318530718, 1.0);
        progressMask = clamp((progress - normAngle) / angleAA + 0.5, 0.0, 1.0);
    }
    float a = vertexColor.a * ringAA * progressMask;
    if (a <= 0.0) {
        discard;
    }
    fragColor = vec4(vertexColor.rgb, a);
}
