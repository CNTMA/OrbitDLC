#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location = 0) in vec2 TexCoord;
layout(location = 0) out vec4 OutColor;
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
layout(std140) uniform HandFire {
    vec4 Params;
    vec4 FlameColorAndBloom;
    vec4 SmokeMotion;
};

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(cell), hash(cell + vec2(1, 0)), f.x),
               mix(hash(cell + vec2(0, 1)), hash(cell + 1.0), f.x), f.y);
}

float hand(vec2 uv) {
    return step(0.000001, texture(Sampler1, uv).r);
}

void main() {
    vec2 uv = TexCoord;
    vec4 scene = texture(Sampler0, uv);
    vec2 pixel = Params.xy;
    float time = Params.z;
    float unit = max(1.0, 0.0015 / pixel.y);
    float core = hand(uv);
    float halo = 0.0;
    float rim = 0.0;
    float bloomStrength = FlameColorAndBloom.a;
    float bloom = 0.0;
    float bloomRadius = 22.0 + 8.0 * bloomStrength;
    for (int i = 0; i < 12; ++i) {
        float angle = float(i) * 0.523599;
        vec2 direction = vec2(cos(angle), sin(angle)) * pixel * unit;
        float near = hand(uv + direction * (1.4 + float(i % 3) * 0.7));
        rim += abs(core - near) / 12.0;
        halo += (near * 0.65 + hand(uv + direction * (5.0 + float(i % 3))) * 0.35) / 12.0;
        if (core == 0.0 && bloomStrength > 0.0) {
            // Stagger the sample radii to avoid visible bands around the silhouette.
            float stagger = float(i % 4) * 0.05;
            bloom += hand(uv + direction * bloomRadius * (0.25 + stagger)) * 0.45
                   + hand(uv + direction * bloomRadius * (0.48 + stagger)) * 0.34
                   + hand(uv + direction * bloomRadius * (0.76 + stagger)) * 0.16
                   + hand(uv + direction * bloomRadius * (1.02 + stagger)) * 0.05;
        }
    }

    vec2 p = uv / (pixel * unit);
    float turbulence = noise(vec2(p.x * 0.085, p.y * 0.042 - time * 4.0))
                     + noise(vec2(p.x * 0.16 + time, p.y * 0.12 - time * 8.0)) * 0.4;
    vec2 lift = vec2((turbulence - 0.65) * 13.0, -(10.0 + turbulence * 21.0) * Params.w) * pixel * unit;
    float flame = 0.0;
    for (int i = 1; i <= 4; ++i) {
        float height = float(i) * 0.25;
        flame = max(flame, hand(uv + lift * height) * (1.0 - height * 0.81));
    }
    flame *= smoothstep(0.32, 0.92, turbulence) * (1.0 - core);

    vec2 emberGrid = vec2(p.x, p.y - time * 33.0) / vec2(14.0, 18.0);
    vec2 cell = floor(emberGrid);
    vec2 offset = vec2(hash(cell), hash(cell + vec2(3.7, 9.2))) * 0.7 + 0.15;
    float spark = 1.0 - smoothstep(0.035, 0.13, length(fract(emberGrid) - offset));
    spark *= smoothstep(0.08, 0.6, flame + halo * 0.2) * (1.0 - core);
    float glint = pow(max(0.0, sin(p.x * 0.20 + p.y * 0.13 + time * 9.0)), 14.0);

    vec3 fireColor = FlameColorAndBloom.rgb;
    vec3 hotColor = mix(fireColor, vec3(1.0), 0.46);
    vec3 light = fireColor * (halo * (1.0 - core) * 0.16 + flame * 0.70);
    light += hotColor * (rim * (0.11 + turbulence * 0.16) + core * glint * 0.11);
    light += mix(hotColor, vec3(1.0), 0.42) * spark;
    float softBloom = smoothstep(0.0, 0.55, bloom / 12.0);
    light += fireColor * (softBloom * 0.22 + flame * 0.05 + spark * 0.07) * bloomStrength;
    if (core == 0.0 && SmokeMotion.z > 0.0) {
        vec2 flow = p * vec2(0.034, 0.025) - vec2(time * 0.65, time * 2.4);
        vec2 curls = vec2(noise(flow), noise(flow * 1.65 + 17.0));
        vec2 rise = vec2((curls.x - 0.5) * 34.0, -(100.0 + 40.0 * curls.y) * sqrt(Params.w));
        float turn = smoothstep(6.0, 120.0, length(SmokeMotion.xy));
        float density = 0.0;
        for (int i = 1; i <= 14; ++i) {
            float age = float(i) / 14.0;
            vec2 trail = rise * age + SmokeMotion.xy * (age * (0.45 + 0.55 * age));
            vec2 source = uv + trail * pixel * unit;
            if (any(lessThan(source, vec2(0.0))) || any(greaterThan(source, vec2(1.0)))) continue;
            float spread = 5.0 + (16.0 + 8.0 * turn) * age;
            vec2 across = vec2(spread, 0.0) * (pixel * unit);
            float footprint = (hand(source - across) + 2.0 * hand(source) + hand(source + across)) * 0.25;
            density += footprint * (1.0 - age) * smoothstep(0.0, 0.14, age);
        }
        float wisps = mix(0.25, 1.0, smoothstep(0.19, 0.75, noise(flow + (curls - 0.5) * 2.5)));
        float opacity = 1.0 - exp(-density * wisps * mix(0.12, 0.37, turn));
        vec3 smokeColor = mix(vec3(0.31, 0.33, 0.36), fireColor, 0.43);
        scene.rgb = mix(scene.rgb, smokeColor, opacity);
        light += fireColor * opacity * turn * 0.18;
    }
    OutColor = vec4(1.0 - (1.0 - scene.rgb) * exp(-light * 2.2), scene.a);
}
