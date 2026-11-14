#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D DiffuseSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

#define MAX_TAPS 65
layout(std140) uniform Config {
    vec4 Params;
    vec4 Taps[MAX_TAPS];
};

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    int taps = int(Params.z + 0.5);
    vec2 delta = Params.xy / OutSize;

    vec4 sum = texture(DiffuseSampler, texCoord) * Taps[0].y;

    for (int i = 1; i < MAX_TAPS; ++i) {
        if (i >= taps) break;
        vec2 offs = delta * Taps[i].x;
        sum += (texture(DiffuseSampler, texCoord + offs) + texture(DiffuseSampler, texCoord - offs)) * Taps[i].y;
    }

    fragColor = sum;
}
