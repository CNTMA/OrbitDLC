#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D BloomSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform ShaderEspData {
    vec4 color1;
    vec4 color2;
    vec4 fillColor;
    vec4 outlineColor;
    vec4 params;   // x = colorMode, y = exposure, z = outlineWidth, w = mode
    vec4 screen;   // xy = size in px, zw = texel size
    vec4 misc;     // x = time, y = rainbowSpeed, z = rainbowScale, w = fill flag
    vec4 params2;  // x = glowSize, y = falloff, z = wide halo weight, w = inner rim
};

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

const float TAU = 6.28318530718;

vec3 hue2rgb(float h) {
    vec3 p = abs(fract(vec3(h) + vec3(0.0, 2.0 / 3.0, 1.0 / 3.0)) * 6.0 - 3.0);
    return clamp(p - 1.0, 0.0, 1.0);
}

// Smooth 13 tap gaussian-ish blur of the silhouette coverage.
// Averaged, never max'd - a max over discrete directions is what makes an
// outline look faceted instead of round.
float blurCoverage(vec2 uv, float radiusPx, vec2 texel) {
    float sum = texture(MaskSampler, uv).a;
    float wsum = 1.0;
    for (int i = 0; i < 6; i++) {
        float ang = (float(i) / 6.0) * TAU;
        vec2 d = vec2(cos(ang), sin(ang)) * radiusPx * texel;
        sum += texture(MaskSampler, uv + d * 0.55).a * 0.75;
        sum += texture(MaskSampler, uv + d).a * 0.42;
        wsum += 1.17;
    }
    return sum / wsum;
}

// Standard "over" with the incoming layer on top, premultiplied accumulator.
void over(inout vec3 pre, inout float acc, vec3 c, float a) {
    a = clamp(a, 0.0, 1.0);
    pre = pre * (1.0 - a) + c * a;
    acc = acc * (1.0 - a) + a;
}

void main() {
    vec2 uv = texCoord;
    vec2 texel = screen.zw;
    vec4 mask = texture(MaskSampler, uv);
    vec4 bloom = texture(BloomSampler, uv);

    float m = mask.a;

    // Most of the screen is nowhere near an entity - bail out before the taps.
    if (m < 0.004 && bloom.a < 0.0015) {
        discard;
    }

    int mode = int(params.w + 0.5);          // 0 = Both, 1 = Glow only, 2 = Outline only
    int colorMode = int(params.x + 0.5);     // 0 = Custom/Auto, 1 = Gradient, 2 = Rainbow
    float exposure = params.y;

    // Keep the look identical across resolutions instead of thinning out at 4K.
    float dpi = clamp(screen.y / 1080.0, 0.7, 2.5);
    float outlinePx = max(0.35, params.z * dpi);
    float tail = params2.x / 13.0;
    float falloff = max(0.5, params2.y);
    float wideWeight = params2.z;
    float innerAmt = params2.w;

    bool wantGlow = (mode == 0 || mode == 1);
    bool wantOutline = (mode == 0 || mode == 2);

    // Fades everything out across the model's own antialiased border.
    float insideFade = smoothstep(0.15, 0.85, m);

    // Glow colour
    vec3 glowCol;
    if (colorMode == 1) {
        glowCol = mix(color1.rgb, color2.rgb, uv.y);
    } else if (colorMode == 2) {
        float t = misc.x * misc.y;
        glowCol = hue2rgb(fract(uv.y * misc.z + t));
    } else if (bloom.a > 0.02) {
        vec3 base = bloom.rgb / max(bloom.a, 0.001);
        glowCol = mix(color1.rgb, base, 0.5);
    } else {
        glowCol = color1.rgb;
    }

    // Custom mode keeps the user's contour colour, generated modes lift their own.
    vec3 outCol = (colorMode == 0) ? outlineColor.rgb : mix(glowCol, vec3(1.0), 0.55);

    // --- contour: a smooth blurred coverage field, thresholded ---------------
    // Thickness follows the blur radius, and smoothstep gives free antialiasing,
    // so the contour stays round on wings and fingers.
    float outlineA = 0.0;
    float innerA = 0.0;
    if (wantOutline || innerAmt > 0.001) {
        float oc = blurCoverage(uv, outlinePx * 2.2, texel);
        if (wantOutline) {
            outlineA = smoothstep(0.055, 0.40, oc) * (1.0 - insideFade) * outlineColor.a;
        }
        innerA = innerAmt * m * smoothstep(0.95, 0.55, oc);
    }

    // --- near field: tight bright rim hugging the silhouette ------------------
    // Capped: 13 taps stay smooth up to roughly 6px, past that the width is the
    // mip chain's job, not this loop's.
    float near = wantGlow ? blurCoverage(uv, 4.5 * dpi * clamp(tail, 0.6, 1.4), texel) : 0.0;

    // --- wide halo from the blurred mip chain --------------------------------
    // The chain itself is flat by nature; the exponent is what turns it into a
    // falloff, bright against the body and thinning out with distance.
    float wide = 0.0;
    if (wantGlow) {
        wide = pow(clamp(bloom.a, 0.0, 1.0), falloff * 0.5) * clamp(tail, 0.35, 2.0);
    }

    // Soft saturation instead of a hard clamp, so the glow never flattens
    // into a solid band right next to the body.
    float glow = 0.0;
    if (wantGlow) {
        glow = 1.0 - exp(-(near + wide * wideWeight * 1.6) * exposure * 2.0);
        glow *= (1.0 - insideFade) * 0.95;
    }

    float fillA = (misc.w > 0.5) ? m * fillColor.a : 0.0;

    // --- composite, bottom to top --------------------------------------------
    vec3 pre = vec3(0.0);
    float acc = 0.0;
    over(pre, acc, fillColor.rgb, fillA);
    over(pre, acc, glowCol, glow);
    over(pre, acc, outCol, innerA);
    over(pre, acc, outCol, outlineA);

    if (acc < 0.004) {
        discard;
    }

    vec3 rgb = pre / max(acc, 0.001);
    // Neon lift on the contour so it reads bright against the halo behind it.
    rgb = clamp(rgb * (1.0 + 0.35 * max(outlineA, innerA)), 0.0, 1.0);

    fragColor = vec4(rgb, clamp(acc, 0.0, 1.0));
}
