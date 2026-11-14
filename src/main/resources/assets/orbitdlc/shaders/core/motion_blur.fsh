#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location = 0) in vec2 TexCoord;
layout(location = 0) out vec4 OutColor;
uniform sampler2D Scene;
uniform sampler2D Depth;
layout(std140) uniform MotionSettings {
    mat4 InverseCurrent;
    mat4 Previous;
    vec4 CameraMotionAndExposure;
};

void main() {
    vec4 center = texture(Scene, TexCoord);
    OutColor = center;
    float exposure = CameraMotionAndExposure.w;
    float depth = texture(Depth, TexCoord).r;
    float clipDepth = exposure > 0.0 ? depth : depth * 2.0 - 1.0;
    vec4 point = InverseCurrent * vec4(TexCoord * 2.0 - 1.0, clipDepth, 1.0);
    if (depth < 0.999999) point.xyz += CameraMotionAndExposure.xyz * point.w;
    vec4 lastClip = Previous * point;
    if (lastClip.w <= 0.0) return;

    vec2 motion = (lastClip.xy / lastClip.w * 0.5 + 0.5 - TexCoord) * abs(exposure);
    vec2 resolution = vec2(textureSize(Scene, 0));
    float extent = length(motion * resolution);
    if (extent < 0.65) return;
    motion *= min(1.0, 0.045 * resolution.y / extent);

    vec3 color = center.rgb;
    float total = 1.0;
    for (int tap = 1; tap <= 8; ++tap) {
        float phase = float(tap) / 8.0;
        vec2 uv = TexCoord + motion * phase;
        if (any(lessThan(uv, vec2(0.0))) || any(greaterThan(uv, vec2(1.0)))) break;
        float sampleDepth = texture(Depth, uv).r;
        float edge = abs(sampleDepth - depth) / max(0.00002, 1.0 - min(depth, sampleDepth));
        if (edge > 0.22) break;
        float weight = (1.0 - phase * 0.85) * (1.0 - smoothstep(0.06, 0.22, edge));
        color += texture(Scene, uv).rgb * weight;
        total += weight;
    }
    OutColor = vec4(color / total, center.a);
}
