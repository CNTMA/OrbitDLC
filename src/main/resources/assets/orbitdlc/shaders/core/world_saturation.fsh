#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 4) in vec2 TexCoord;
layout(location = 1) in vec4 FragColor;

uniform sampler2D Sampler0;

// Color isolation (keep only vivid pixels of a chosen hue, desaturate the rest)
const float IsolationStrength = 1.0;
const vec3 IsolationColor = vec3(0.18, 0.42, 1.0);
const float HueTolerance = 0.095;
const float MinSat = 0.32;
const float MinVal = 0.20;
const float BackgroundSat = 0.17;

layout(location = 0) out vec4 OutColor;

vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

void main() {
    vec4 col = texture(Sampler0, TexCoord);
    float l = dot(col.rgb, vec3(0.2126, 0.7152, 0.0722));

    float sat = 1.0;

    if (IsolationStrength > 0.001) {
        vec3 hsv = rgb2hsv(col.rgb);
        float targetHue = rgb2hsv(IsolationColor).x;

        // circular hue distance
        float hd = abs(hsv.x - targetHue);
        hd = min(hd, 1.0 - hd);

        // soft hue window + "vivid enough" gates so the whole world doesn't light up
        float hueMatch = 1.0 - smoothstep(HueTolerance, HueTolerance + 0.04, hd);
        float satMatch = smoothstep(MinSat, min(MinSat + 0.10, 1.0), hsv.y);
        float valMatch = smoothstep(MinVal, min(MinVal + 0.10, 1.0), hsv.z);

        float keep = hueMatch * satMatch * valMatch * IsolationStrength;

        // matched pixels keep their full color, everything else falls back to the background saturation
        sat = mix(BackgroundSat, 1.0, keep);
    }

    vec3 mixed = mix(vec3(l), col.rgb, sat);
    OutColor = vec4(mixed, col.a) * FragColor;
}
