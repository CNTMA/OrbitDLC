#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D DiffuseSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

float karisWeight(vec3 c) {
    float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));
    return 1.0 / (1.0 + luma);
}

vec3 groupColor(vec3 a, vec3 b, vec3 c, vec3 d) {
    vec3 avg = (a + b + c + d) * 0.25;
    return avg * karisWeight(avg);
}

float groupWeight(vec3 a, vec3 b, vec3 c, vec3 d) {
    return karisWeight((a + b + c + d) * 0.25);
}

void main() {
    vec2 t = 1.0 / InSize;

    vec3 a = texture(DiffuseSampler, texCoord + t * vec2(-2.0,  2.0)).rgb;
    vec3 b = texture(DiffuseSampler, texCoord + t * vec2( 0.0,  2.0)).rgb;
    vec3 c = texture(DiffuseSampler, texCoord + t * vec2( 2.0,  2.0)).rgb;
    vec3 d = texture(DiffuseSampler, texCoord + t * vec2(-2.0,  0.0)).rgb;
    vec3 e = texture(DiffuseSampler, texCoord).rgb;
    vec3 f = texture(DiffuseSampler, texCoord + t * vec2( 2.0,  0.0)).rgb;
    vec3 g = texture(DiffuseSampler, texCoord + t * vec2(-2.0, -2.0)).rgb;
    vec3 h = texture(DiffuseSampler, texCoord + t * vec2( 0.0, -2.0)).rgb;
    vec3 i = texture(DiffuseSampler, texCoord + t * vec2( 2.0, -2.0)).rgb;
    vec3 j = texture(DiffuseSampler, texCoord + t * vec2(-1.0,  1.0)).rgb;
    vec3 k = texture(DiffuseSampler, texCoord + t * vec2( 1.0,  1.0)).rgb;
    vec3 l = texture(DiffuseSampler, texCoord + t * vec2(-1.0, -1.0)).rgb;
    vec3 m = texture(DiffuseSampler, texCoord + t * vec2( 1.0, -1.0)).rgb;

    vec3 sum = groupColor(j, k, l, m) * 0.5
             + groupColor(a, b, d, e) * 0.125
             + groupColor(b, c, e, f) * 0.125
             + groupColor(d, e, g, h) * 0.125
             + groupColor(e, f, h, i) * 0.125;

    float weight = groupWeight(j, k, l, m) * 0.5
                 + groupWeight(a, b, d, e) * 0.125
                 + groupWeight(b, c, e, f) * 0.125
                 + groupWeight(d, e, g, h) * 0.125
                 + groupWeight(e, f, h, i) * 0.125;

    fragColor = vec4(sum / max(weight, 1.0e-4), 1.0);
}
