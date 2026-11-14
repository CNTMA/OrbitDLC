#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location = 0) in vec2 TexCoord;
layout(location = 0) out vec4 OutColor;
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
layout(std140) uniform CustomFogSettings {
    mat4 InverseViewProjection;
    vec4 CameraAndDepth;
    vec4 TintAndDensity;
    vec4 DriftPhase;
};

vec4 unproject(float depth) {
    float clipZ = mix(depth * 2.0 - 1.0, depth, CameraAndDepth.w);
    return InverseViewProjection * vec4(TexCoord * 2.0 - 1.0, clipZ, 1.0);
}

void main() {
    vec4 scene = texture(Sampler0, TexCoord);
    float depth = texture(Sampler1, TexCoord).r;
    vec4 point = unproject(max(depth, 0.000001));
    float distance = depth > 0.000001
        ? min(length(point.xyz) / max(abs(point.w), 0.000001), 224.0)
        : 224.0;
    vec3 ray = normalize(point.xyz);
    float start = 5.0;
    float stepSize = max(0.0, distance - start) / 10.0;
    float opticalDepth = 0.0;
    for (int i = 0; i < 10; ++i) {
        float along = start + (float(i) + 0.5) * stepSize;
        vec3 samplePosition = CameraAndDepth.xyz + ray * along;
        float layer = exp(-max(samplePosition.y - 63.0, 0.0) * 0.022);
        float billow = sin(samplePosition.z * 0.045 + DriftPhase.y);
        float clouds = 0.82 + 0.18 * sin(samplePosition.x * 0.069 - DriftPhase.x + billow)
                             * cos(samplePosition.z * 0.051 + samplePosition.y * 0.075 + DriftPhase.z);
        opticalDepth += (0.16 + 0.84 * layer) * clouds * smoothstep(start, 24.0, along);
    }
    float fog = 1.0 - exp(-opticalDepth * stepSize * TintAndDensity.w * 0.017);
    float skyFade = depth > 0.000001 ? 1.0 : 1.0 - smoothstep(0.03, 0.38, ray.y);
    OutColor = vec4(mix(scene.rgb, TintAndDensity.rgb, fog * skyFade * 0.9), scene.a);
}
