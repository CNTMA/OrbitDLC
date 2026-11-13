#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 vUV;
layout(location = 1) in vec3 vWorldDir;
layout(location = 0) out vec4 OutColor;

layout(std140) uniform SkyUniforms {
    mat4 uInverseViewProj;
    vec4 uParams;
    vec4 uExtra;
    vec4 uThemeColor;
};

#define Direction vWorldDir
#define Time (uParams.x * uParams.z)
#define Accent (uThemeColor.rgb)
#define ColorModulator vec4(vec3(uParams.w), 1.0)
#define vColor vec4(1.0)
#define FrameBlend 0.0

vec3 bk_hash33(vec3 p3) {
    p3 = fract(p3 * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yxz + 33.33);
    return fract((p3.xxy + p3.yxx) * p3.zyx);
}

float valueNoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = bk_hash33(i).x;
    float n100 = bk_hash33(i + vec3(1.0, 0.0, 0.0)).x;
    float n010 = bk_hash33(i + vec3(0.0, 1.0, 0.0)).x;
    float n110 = bk_hash33(i + vec3(1.0, 1.0, 0.0)).x;
    float n001 = bk_hash33(i + vec3(0.0, 0.0, 1.0)).x;
    float n101 = bk_hash33(i + vec3(1.0, 0.0, 1.0)).x;
    float n011 = bk_hash33(i + vec3(0.0, 1.0, 1.0)).x;
    float n111 = bk_hash33(i + vec3(1.0)).x;
    float nx00 = mix(n000, n100, f.x);
    float nx10 = mix(n010, n110, f.x);
    float nx01 = mix(n001, n101, f.x);
    float nx11 = mix(n011, n111, f.x);
    return mix(mix(nx00, nx10, f.y), mix(nx01, nx11, f.y), f.z);
}

float fbm(vec3 p) {
    float sum = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;
    for (int i = 0; i < 5; i++) {
        sum += amplitude * valueNoise(p * frequency);
        frequency *= 2.02;
        amplitude *= 0.5;
    }
    return sum;
}

vec4 bakeSky(vec2 TexCoord) {
    float bk_longitude = (TexCoord.x - 0.5) * 6.28318530718;
    float bk_latitude = (0.5 - TexCoord.y) * 3.14159265359;
    vec3 bk_direction = vec3(cos(bk_latitude) * cos(bk_longitude), sin(bk_latitude), cos(bk_latitude) * sin(bk_longitude));
    float bk_time = Time * 0.35;

    vec3 primary = max(Accent, vec3(0.05));
    vec3 secondary = mix(primary, vec3(0.16, 0.52, 1.0), 0.55);
    secondary = mix(secondary, vec3(1.0, 0.54, 0.22), 0.28);

    vec3 position = bk_direction * 2.2 + vec3(bk_time * 0.030, bk_time * 0.018, -bk_time * 0.022);
    vec3 warp = vec3(
        fbm(position * 0.7 + vec3(bk_time * 0.08, 0.0, 0.0)),
        fbm(position * 0.7 + vec3(5.2, bk_time * 0.07, 1.3)),
        fbm(position * 0.7 + vec3(1.7, 9.2, -bk_time * 0.06))
    );
    position += (warp - 0.5) * 2.0;
    float firstNoise = fbm(position * 1.1);
    float secondNoise = fbm(position * 2.4 + 4.0);
    float density = pow(
        smoothstep(0.32, 0.95, firstNoise * 0.7 + secondNoise * 0.3),
        1.4
    );
    float hue = fbm(position * 0.6 + 9.0);
    vec3 bk_nebula = mix(primary, secondary, smoothstep(0.18, 0.85, hue));
    bk_nebula = mix(bk_nebula, bk_nebula * 1.7 + 0.25, density);

    vec3 bk_color = bk_nebula * density * 1.35;
    bk_color += mix(primary, secondary, 0.5) * firstNoise * firstNoise * 0.12;

    bk_color = bk_color / (1.0 + bk_color);
    bk_color = pow(bk_color, vec3(0.85));

    return vec4(clamp(bk_color, 0.0, 1.0), clamp(firstNoise, 0.0, 1.0));
}

float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

float hash21(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec3 hash33(vec3 p3) {
    p3 = fract(p3 * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yxz + 33.33);
    return fract((p3.xxy + p3.yxx) * p3.zyx);
}

vec3 rotateY(vec3 p, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec3(p.x * c - p.z * s, p.y, p.x * s + p.z * c);
}

vec3 rotateX(vec3 p, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec3(p.x, p.y * c - p.z * s, p.y * s + p.z * c);
}

float starGlow(vec3 direction, float scale, float density, float time) {
    vec3 p = direction * scale;
    vec3 cell = floor(p);
    vec3 h = hash33(cell);
    if (h.x > density) return 0.0;
    float distanceToStar = length(fract(p) - (0.2 + 0.6 * h));
    float brightness = hash11(h.y + 1.7);
    brightness *= brightness;
    brightness *= brightness;
    float twinkle = 0.6 + 0.4 * sin(time * 2.0 + h.z * 50.0);
    float core = 1.0 - smoothstep(0.0, 0.06, distanceToStar);
    float halo = exp(-distanceToStar * 12.0) * 0.5;
    return (core + halo) * (0.4 + brightness * 2.6) * twinkle;
}

void main() {
    vec3 direction = normalize(Direction);
    float time = Time * 0.35;

    vec3 flow = rotateY(direction, Time * 0.010);
    flow = rotateX(flow, sin(Time * 0.008) * 0.10);

    float longitude = atan(flow.z, flow.x);
    float latitude = asin(clamp(flow.y, -1.0, 1.0));
    vec2 uv = vec2(longitude / 6.28318530718 + 0.5, 0.5 - latitude / 3.14159265359);
    // The third, unfinished map is never sampled by the sky draw.
    vec4 baked = bakeSky(uv);

    vec3 nebula = pow(baked.rgb, vec3(1.0 / 0.85));
    nebula = nebula / max(1.0 - nebula, vec3(0.001));
    nebula *= 0.90 + 0.10 * sin(Time * 0.20 + baked.a * 4.0);

    vec3 color = vec3(0.010, 0.013, 0.024) + nebula;

    vec3 starDir = rotateY(direction, Time * 0.004);
    color += vec3(0.85, 0.90, 1.0) * starGlow(starDir, 240.0, 0.05, time);
    color += vec3(1.0, 0.96, 0.90)
            * starGlow(starDir * 1.7 + 31.0, 130.0, 0.025, time * 1.3)
            * 1.4;

    color = color / (1.0 + color);
    color = pow(color, vec3(0.85));
    color += vec3((hash21(gl_FragCoord.xy) - 0.5) / 255.0);

    OutColor = vec4(clamp(color, 0.0, 1.0), 1.0) * ColorModulator * vColor;
}
