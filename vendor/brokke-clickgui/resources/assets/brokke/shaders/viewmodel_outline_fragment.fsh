#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 vTexCoord;
layout(location = 0) out vec4 fragColor;

layout(std140) uniform Uniforms {
    vec4 uResolutionTexel;
    vec4 uParams;
    vec4 uColor;
};

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

int isHand(ivec2 p) {
    if (p.x < 0 || p.y < 0 || p.x >= int(uResolutionTexel.x) || p.y >= int(uResolutionTexel.y)) {
        return 0;
    }

    float now = texelFetch(Sampler1, p, 0).r;
    float was = texelFetch(Sampler2, p, 0).r;
    if ((was - now) <= 0.0001) {
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
    float splitX = uParams.z;
    float holdAnim = (gl_FragCoord.x >= splitX) ? rightHoldAnim : leftHoldAnim;
    float finalAlpha = alpha * holdAnim * uColor.a;

    if (finalAlpha <= 0.005) {
        discard;
    }

    fragColor = vec4(uColor.rgb, clamp(finalAlpha, 0.0, 1.0));
}
