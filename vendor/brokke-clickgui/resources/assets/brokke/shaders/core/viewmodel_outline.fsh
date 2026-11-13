#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

layout(std140) uniform Uniforms {
    vec4 uResolutionTexel;
    vec4 uParams;
    vec4 uColor;
    vec4 uRightBounds;
    vec4 uLeftBounds;
};

uniform sampler2D Sampler1;

int isHand(ivec2 p) {
    if (p.x < 0 || p.y < 0 || p.x >= int(uResolutionTexel.x) || p.y >= int(uResolutionTexel.y)) {
        return 0;
    }

    float now = texelFetch(Sampler1, p, 0).r;
    if (now <= 0.0) {
        return 0;
    }

    return 1;
}

void main() {
    ivec2 p = ivec2(gl_FragCoord.xy);
    int center = isHand(p);

    float thickness = max(uParams.w, 1.5);
    int radius = int(ceil(thickness + 1.0));

    float alpha = 0.0;

    if (center == 0) {
        float minDist = 999.0;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx == 0 && dy == 0) continue;
                if (isHand(p + ivec2(dx, dy)) == 1) {
                    float d = length(vec2(dx, dy));
                    if (d < minDist) {
                        minDist = d;
                    }
                }
            }
        }

        if (minDist <= thickness) {
            float edge = clamp(1.0 - (minDist - (thickness - 1.0)), 0.0, 1.0);
            alpha = mix(0.35, 1.0, edge);
        } else if (minDist <= thickness + 2.0) {
            float glow = clamp(1.0 - (minDist - thickness) / 2.0, 0.0, 1.0);
            alpha = glow * 0.35;
        }
    } else {
        float innerMinDist = 999.0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                if (isHand(p + ivec2(dx, dy)) == 0) {
                    float d = length(vec2(dx, dy));
                    if (d < innerMinDist) {
                        innerMinDist = d;
                    }
                }
            }
        }

        if (innerMinDist <= 1.5) {
            alpha = clamp(1.0 - innerMinDist / 1.5, 0.0, 1.0) * 0.5;
        }
    }

    float rightHoldAnim = uParams.x;
    float leftHoldAnim = uParams.y;
    vec2 pixel = gl_FragCoord.xy;
    vec4 padding = vec4(-4.0, -4.0, 4.0, 4.0);
    vec4 rb = uRightBounds + padding;
    vec4 lb = uLeftBounds + padding;
    bool right = all(greaterThanEqual(pixel, rb.xy)) && all(lessThanEqual(pixel, rb.zw));
    bool left = all(greaterThanEqual(pixel, lb.xy)) && all(lessThanEqual(pixel, lb.zw));
    float holdAnim = max(right ? rightHoldAnim : 0.0, left ? leftHoldAnim : 0.0);
    float finalAlpha = alpha * holdAnim * uColor.a;

    if (finalAlpha <= 0.005) {
        discard;
    }

    fragColor = vec4(uColor.rgb, clamp(finalAlpha, 0.0, 1.0));
}
