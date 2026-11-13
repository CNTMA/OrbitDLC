#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 vTexCoord;

layout(location = 0) out vec4 fragColor;

layout(std140) uniform FogData {
    mat4 uInverseProjection;
    vec4 uResolution;
    vec4 uFogColor;
    vec4 uParams;
    vec4 uFlags;
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

const float GOLDEN_ANGLE = 2.39996323;
const int BLUR_TAPS = 12;


float brokkeClipDepth(float depth) {
#ifdef BROKKE_DEPTH_ZERO_TO_ONE
    return depth;
#else
    return depth * 2.0 - 1.0;
#endif
}

float viewDistance(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, brokkeClipDepth(depth), 1.0);
    vec4 view = uInverseProjection * clip;
    float w = abs(view.w) < 1.0e-6 ? 1.0e-6 : view.w;
    return length(view.xyz / w);
}

vec3 blurScene(vec2 uv, float radiusPixels, float centerDepth) {
    vec3 sum = texture(Sampler0, uv).rgb;
    if (radiusPixels < 0.75) return sum;

    vec2 texel = 1.0 / max(uResolution.xy, vec2(1.0));
    float total = 1.0;

    for (int i = 1; i <= BLUR_TAPS; i++) {
        float t = float(i) / float(BLUR_TAPS);
        float angle = float(i) * GOLDEN_ANGLE;
        vec2 offset = vec2(cos(angle), sin(angle)) * sqrt(t) * radiusPixels * texel;
        vec2 sampleUV = clamp(uv + offset, vec2(0.0005), vec2(0.9995));
        float sampleDepth = texture(Sampler1, sampleUV).r;
        float weight = exp(-t * 2.0) * exp(-abs(sampleDepth - centerDepth) / max(abs(centerDepth) * 0.12, 0.0001));
        sum += texture(Sampler0, sampleUV).rgb * weight;
        total += weight;
    }

    return sum / total;
}

void main() {
    vec2 uv = vTexCoord;
    vec3 scene = texture(Sampler0, uv).rgb;
    float depth = texture(Sampler1, uv).r;

    float strength = clamp(uParams.z, 0.0, 1.0);
    vec4 nearLimit = uInverseProjection * vec4(0.0, 0.0, brokkeClipDepth(0.0), 1.0);
    vec4 farLimit = uInverseProjection * vec4(0.0, 0.0, brokkeClipDepth(1.0), 1.0);
    bool reversed = abs(nearLimit.w) < abs(farLimit.w);
    bool sky = reversed ? depth <= 0.000001 : depth >= 0.999999;

    bool atmo = uFlags.z > 0.5;
    if (atmo) {
        float afog;
        float dist = sky ? 100000.0 : viewDistance(uv, depth);
        if (sky) {
            afog = strength * 0.78;
        } else {
            float t = max(dist - uParams.x, 0.0) / max(uParams.y, 0.01);
            afog = (1.0 - exp(-t * 1.35)) * strength;
            afog = max(afog, strength * 0.12 * clamp(dist / 12.0, 0.0, 1.0));
        }
        if (afog <= 0.002) {
            fragColor = vec4(scene, 1.0);
            return;
        }
        float lum = dot(scene, vec3(0.299, 0.587, 0.114));
        vec3 desat = mix(scene, vec3(lum), clamp(afog * 0.75, 0.0, 1.0));
        vec3 haze = mix(uFogColor.rgb, uFogColor.rgb * (0.85 + 0.3 * lum), 0.5);
        vec3 aresult = mix(desat, haze, afog);
        aresult = mix(aresult, aresult * 0.92 + vec3(0.04), afog * 0.5);
        float g = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
        aresult += (g - 0.5) * afog / 255.0;
        fragColor = vec4(aresult, 1.0);
        return;
    }

    float fog;
    if (sky) {
        fog = (uFlags.x > 0.5 && uFlags.y > 0.5) ? strength : 0.0;
    } else {
        float spread = max(uParams.y, 0.01);
        float dist = viewDistance(uv, depth);
        float t = max((dist - uParams.x) / spread, 0.0);
        fog = (1.0 - exp(-t * t * 2.0)) * strength;
    }

    if (fog <= 0.002) {
        fragColor = vec4(scene, 1.0);
        return;
    }

    float scattering = fog * (1.0 - fog);
    vec3 mist = uFogColor.rgb * (0.82 + 0.18 * fog);
    mist += vec3(0.11, 0.14, 0.17) * scattering;
    vec3 result = uFlags.x > 0.5
        ? mix(scene, mist, fog)
        : mix(scene, blurScene(uv, uParams.w * fog, depth), fog);
    float grain = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    result += (grain - 0.5) * fog / 255.0;

    fragColor = vec4(result, 1.0);
}
