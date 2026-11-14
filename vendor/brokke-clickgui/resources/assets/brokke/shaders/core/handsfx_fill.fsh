#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D HandSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform HandsFxData {
    vec4 color1;
    vec4 color2;
    vec4 fillColor;
    vec4 outlineColor;
    vec4 params0;
    vec4 params1;
    vec4 params2;
    vec4 params3;
    vec4 screen;
    vec4 misc;
};

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

const float TAU = 6.28318530718;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float hash31(vec3 n) {
    return fract(sin(dot(n, vec3(12.9898, 4.1414, 5.28934))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
               mix(hash21(i + vec2(0.0, 1.0)), hash21(i + 1.0), f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    mat2 rotation = mat2(1.6, 1.2, -1.2, 1.6);
    for (int i = 0; i < 4; i++) {
        value += noise(p) * amplitude;
        p = rotation * p;
        amplitude *= 0.5;
    }
    return value * 1.033333333;
}

float timeValue() {
    return params2.x * params1.z;
}

float scaleValue() {
    return params1.w;
}

float brightValue() {
    return misc.x;
}

bool starsOn() {
    return params0.y > 0.5;
}

vec3 stars(vec3 p) {
    if (!starsOn()) return vec3(0.0);
    vec3 q = fract(p * 400.0) - 0.5;
    vec3 id = floor(p * 400.0);
    float random = hash31(id);
    if (random > 0.003) return vec3(0.0);
    float point = 1.0 - smoothstep(0.0, 0.6, length(q));
    vec3 tint = mix(vec3(1.0, 0.49, 0.1), vec3(0.75, 0.9, 1.0), hash31(id + 100.0));
    vec3 color = point * (tint * 0.1 + 0.9);
    return color * color * 0.8;
}

vec3 nebula(vec3 ray, vec2 domain) {
    float time = timeValue() * 0.15;
    vec2 q = domain * scaleValue() * 1.5;
    float first = fbm(q + vec2(time, -time * 0.5));
    float second = fbm(q * 2.0 + vec2(-time * 0.7, time) + first * 2.0);
    float density = pow(clamp(first * second * 1.8, 0.0, 1.0), 1.6);
    vec3 color = mix(color1.rgb, color2.rgb, second);
    color *= density * (1.0 + brightValue());
    color += color2.rgb * (first * first * first) * 0.4 * brightValue();
    color += mix(color1.rgb, color2.rgb, first) * 0.12;
    color += stars(ray) * 1.2;
    return color;
}

vec3 waves(vec3 ray, vec2 domain) {
    vec2 p = domain * scaleValue() * TAU - 250.0;
    vec2 iterated = p;
    float accumulated = 1.0;
    float intensity = 0.005;
    const int iterations = 5;
    for (int n = 0; n < iterations; n++) {
        float iterationTime = timeValue() * (1.0 - 3.5 / float(n + 1));
        iterated = p + vec2(cos(iterationTime - iterated.x) + sin(iterationTime + iterated.y),
                            sin(iterationTime - iterated.y) + cos(iterationTime + iterated.x));
        float waveX = sin(iterated.x + iterationTime);
        float waveY = cos(iterated.y + iterationTime);
        vec2 denominator = p * vec2(waveY, waveX) * intensity;
        accumulated += abs(waveX * waveY) * inversesqrt(max(dot(denominator, denominator), 1e-20));
    }
    accumulated /= float(iterations);
    accumulated = 1.17 - pow(accumulated, 1.4);
    float pattern = min(abs(accumulated), 1.0);
    pattern *= pattern;
    pattern *= pattern;
    pattern *= pattern;
    vec3 base = mix(color1.rgb, color2.rgb, pattern);
    vec3 color = base * (1.0 + pattern * brightValue() * 1.5);
    color += stars(ray) * (1.0 - pattern);
    return color;
}

float ridged(vec2 p) {
    float value = 0.0;
    float amplitude = 0.55;
    for (int i = 0; i < 4; i++) {
        float ridge = 1.0 - abs(noise(p) * 2.0 - 1.0);
        value += ridge * amplitude;
        p = p * 2.18 + vec2(3.1, 9.2);
        amplitude *= 0.52;
    }
    return value;
}

vec3 energy(vec2 uv, vec3 source, float strength) {
    float time = timeValue();
    vec2 flow = uv * 2.5 * max(scaleValue(), 0.2);
    vec2 drift = vec2(time * 0.20, -time * 0.15);
    vec2 warp = vec2(
            fbm(flow * 0.90 + drift * 0.75 + vec2(0.0, 4.1)),
            fbm(flow * 0.78 - drift * 0.48 + vec2(3.7, 1.8))
    );
    vec2 q = flow + (warp - 0.5) * 1.8;
    float mist = fbm(q * 0.72 - drift * 0.24 + vec2(4.2, 8.1));
    float veins = pow(clamp(ridged(q * 1.85 + vec2(mist * 2.5, mist * 1.6) - drift * 0.55), 0.0, 1.0), 2.4);
    float stripeA = pow(clamp(1.0 - abs(sin((q.x * 1.08 + q.y * 0.42) * 1.7 + time * 0.85 + mist * 4.3)), 0.0, 1.0), 4.8);
    float stripeB = pow(clamp(1.0 - abs(sin((q.x * -0.58 + q.y * 1.12) * 1.45 - time * 0.65 - mist * 2.9)), 0.0, 1.0), 5.4);
    float e = clamp(mist * 0.22 + veins * 0.88 + stripeA * 0.55 + stripeB * 0.32, 0.0, 1.0);
    float core = smoothstep(0.18, 0.98, e);
    float accent = pow(clamp(max(veins, stripeA), 0.0, 1.0), 1.25);
    vec3 tint = mix(color1.rgb, color2.rgb, clamp(accent, 0.0, 1.0));
    vec3 color = mix(tint, mix(tint, vec3(1.0), 0.4), clamp(core * 0.75 + stripeB * 0.25, 0.0, 1.0));
    float fill = (0.26 + core * 0.82 + accent * 0.28) * (1.0 + brightValue() * 0.25);
    return mix(source, color, clamp(strength * fill * 0.92, 0.0, 1.0));
}

mat2 rot2(float a) {
    float c = cos(a);
    float s = sin(a);
    return mat2(c, s, -s, c);
}

float tri(float x) {
    return clamp(abs(fract(x) - 0.5), 0.01, 0.49);
}

vec2 tri2(vec2 p) {
    return vec2(tri(p.x) + tri(p.y), tri(p.y + tri(p.x)));
}

float triNoise(vec2 p, float spd) {
    float z = 1.8;
    float z2 = 2.5;
    float rz = 0.0;
    p *= rot2(p.x * 0.06);
    vec2 bp = p;
    for (int i = 0; i < 5; i++) {
        vec2 dg = tri2(bp * 1.85) * 0.75;
        dg *= rot2(timeValue() * spd);
        p -= dg / z2;
        bp *= 1.3;
        z2 *= 0.45;
        z *= 0.42;
        p *= 1.21 + (rz - 1.0) * 0.02;
        rz += tri(p.x + tri(p.y)) * z;
        p *= -mat2(0.95534, 0.29552, -0.29552, 0.95534);
    }
    return clamp(1.0 / pow(rz * 29.0, 1.3), 0.0, 0.55);
}

vec3 aurora(vec2 uv) {
    float aspect = screen.x / max(screen.y, 1.0);
    vec2 p = vec2(uv.x * aspect, uv.y) * scaleValue() * 2.2;
    vec3 acc = vec3(0.0);
    float total = 0.0;
    for (int i = 0; i < 18; i++) {
        float fi = float(i);
        float of = 0.006 * hash21(gl_FragCoord.xy) * smoothstep(0.0, 15.0, fi);
        vec2 bpos = vec2(p.x * 0.9, p.y * 0.35 + fi * 0.045 + of) + vec2(timeValue() * 0.05, 0.0);
        float rzt = triNoise(bpos * 1.6, 0.06);
        float k = fi / 17.0;
        vec3 tint = mix(color1.rgb, color2.rgb, k);
        acc += tint * rzt * exp2(-fi * 0.065);
        total += exp2(-fi * 0.065);
    }
    acc = acc / max(total, 0.001) * 10.0 * (0.7 + brightValue());
    float shimmer = 0.85 + 0.15 * sin(timeValue() * 2.0 + p.x * 6.0);
    vec3 base = mix(color1.rgb, color2.rgb, uv.y) * 0.10;
    return base + acc * shimmer + stars(vec3(uv * 2.0 - 1.0, 1.0)) * 1.5;
}

void main() {
    vec2 uv = texCoord;
    vec2 px = screen.zw;
    vec4 hand = texture(HandSampler, uv);
    float mask = hand.a;
    vec3 source = hand.rgb / max(hand.a, 0.001);
    if (mask < 0.5) {
        vec4 l = texture(HandSampler, uv - vec2(px.x, 0.0));
        vec4 r = texture(HandSampler, uv + vec2(px.x, 0.0));
        vec4 d = texture(HandSampler, uv - vec2(0.0, px.y));
        vec4 u = texture(HandSampler, uv + vec2(0.0, px.y));
        vec4 dl = texture(HandSampler, uv - px);
        vec4 ur = texture(HandSampler, uv + px);
        vec4 ul = texture(HandSampler, uv + vec2(-px.x, px.y));
        vec4 dr = texture(HandSampler, uv + vec2(px.x, -px.y));
        float bridge = max(max(min(l.a, r.a), min(d.a, u.a)), max(min(dl.a, ur.a), min(ul.a, dr.a)));
        float count = l.a + r.a + d.a + u.a + dl.a + ur.a + ul.a + dr.a;
        if (bridge > 0.3 || count > 2.5) {
            vec3 sum = l.rgb + r.rgb + d.rgb + u.rgb + dl.rgb + ur.rgb + ul.rgb + dr.rgb;
            source = sum / max(count, 0.001);
            mask = 1.0;
        }
    }
    if (mask < 0.05) {
        discard;
    }
    float strength = clamp(fillColor.a, 0.0, 1.0);
    int style = int(mod(misc.w + 0.5, 8.0));

    vec3 result;
    if (style == 4) {
        result = energy(uv, source, strength);
    } else {
        vec3 color;
        if (style == 6) {
            color = aurora(uv);
        } else if (style == 5) {
            color = mix(color2.rgb, color1.rgb, smoothstep(0.0, 1.0, uv.y));
        } else {
            float aspect = screen.x / max(screen.y, 1.0);
            vec2 ndc = uv * 2.0 - 1.0;
            vec3 ray = normalize(vec3(ndc.x * aspect, ndc.y, 1.0));
            vec2 domain = ray.xy / (abs(ray.z) + 0.5) * 3.0;
            if (style == 1) {
                color = nebula(ray, domain);
            } else {
                color = waves(ray, domain);
            }
        }
        if (params0.z > 0.5) {
            color *= mix(1.0, dot(source, vec3(0.299, 0.587, 0.114)), params0.w);
        }
        result = mix(source, color, strength);
    }

    fragColor = vec4(max(result, vec3(0.0)), clamp(mask * misc.z, 0.0, 1.0));
}
