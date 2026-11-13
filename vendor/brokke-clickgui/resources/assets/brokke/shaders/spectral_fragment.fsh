#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform SpectralUniforms { mat4 ProjView; vec4 Effect; };
layout(location = 0) in vec2 uv;
layout(location = 1) in vec4 tint;
layout(location = 0) out vec4 fragColor;
float ellipse(vec2 p, vec2 center, vec2 radius) { return length((p - center) / radius); }
void main() {
    float t = Effect.x;
    float mode = Effect.y;
    vec2 p = uv;
    vec3 color = tint.rgb;
    float alpha = 0.0;
    if (mode < 0.5) {
        float wave = sin(p.y * 10.0 - t * 3.0) * 0.025 * smoothstep(0.3, 0.9, p.y);
        p.x += wave;
        float head = ellipse(p, vec2(0.5, 0.35), vec2(0.255, 0.255));
        float hem = 0.76 + sin(p.x * 38.0 + t * 3.0) * 0.045;
        float body = (1.0 - smoothstep(0.22, 0.26, abs(p.x - 0.5)))
                * smoothstep(0.22, 0.35, p.y) * (1.0 - smoothstep(hem - 0.04, hem + 0.02, p.y));
        float silhouette = max(1.0 - smoothstep(0.92, 1.04, head), body);
        float aura = exp(-ellipse(p, vec2(0.5, 0.43), vec2(0.36, 0.44)) * 3.4) * 0.48;
        float eyes = exp(-pow(ellipse(p, vec2(0.411, 0.355), vec2(0.026, 0.047)), 2.0) * 2.0)
                   + exp(-pow(ellipse(p, vec2(0.589, 0.355), vec2(0.026, 0.047)), 2.0) * 2.0);
        float rim = pow(clamp(head, 0.0, 1.0), 3.0);
        color = mix(color * 0.65, vec3(0.88, 0.97, 1.0), 0.35 + 0.5 * rim);
        color = mix(color, vec3(0.035, 0.075, 0.14), clamp(eyes, 0.0, 1.0));
        alpha = silhouette * (0.85 - 0.32 * smoothstep(0.5, 0.85, p.y)) + aura;
    } else if (mode < 1.5) {
        p.x += sin(p.y * 15.0 - t * 2.5) * 0.018 * p.y;
        float hood = ellipse(p, vec2(0.5, 0.24), vec2(0.19, 0.19));
        float hem = 0.87 + 0.07 * sin(p.x * 48.0 + sin(t * 1.4)) + 0.03 * sin(p.x * 97.0 - t);
        float cloakWidth = mix(0.115, 0.37, smoothstep(0.24, 0.92, p.y));
        float cloak = (1.0 - smoothstep(cloakWidth - 0.025, cloakWidth, abs(p.x - 0.5)))
                   * smoothstep(0.2, 0.35, p.y) * (1.0 - smoothstep(hem - 0.09, hem, p.y));
        float shape = max(1.0 - smoothstep(0.9, 1.06, hood), cloak);
        float face = 1.0 - smoothstep(0.65, 1.0, ellipse(p, vec2(0.5, 0.255), vec2(0.105, 0.125)));
        float folds = pow(0.5 + 0.5 * sin(atan(p.x - 0.5, p.y - 0.22) * 37.0 + sin(t + p.y * 8.0)), 5.0);
        float edge = smoothstep(cloakWidth - 0.065, cloakWidth, abs(p.x - 0.5));
        color = mix(vec3(0.025, 0.035, 0.07), tint.rgb * 0.42, folds * 0.45 + edge * 0.55);
        color = mix(color, vec3(0.003, 0.005, 0.012), face);
        float mouth = exp(-pow(ellipse(p, vec2(0.5, 0.315), vec2(0.023, 0.036)), 2.0) * 2.0);
        color += tint.rgb * mouth * (0.8 + 0.2 * sin(t * 5.0));
        alpha = shape * 0.97;
    } else if (mode < 2.5) {
        float across = 1.0 - abs(p.y * 2.0 - 1.0);
        alpha = pow(across, 1.8);
        color = mix(tint.rgb, vec3(0.9, 1.0, 1.0), pow(across, 9.0) * 0.7);
    } else if (mode < 3.5) {
        float scan = 0.6 + 0.4 * sin(gl_FragCoord.y * 1.2 - t * 8.0);
        float edge = pow(1.0 - min(min(p.x, 1.0 - p.x), min(p.y, 1.0 - p.y)) * 2.0, 5.0);
        alpha = 0.3 + edge * 0.45 + scan * 0.12;
        color = mix(tint.rgb, vec3(0.87, 1.0, 1.0), edge * 0.65);
    } else if (mode > 9.5) {
        vec2 q = p * 2.0 - 1.0;
        float radius = length(q);
        float star = exp(-abs(q.x) * 35.0) * exp(-abs(q.y) * 3.5)
                   + exp(-abs(q.y) * 35.0) * exp(-abs(q.x) * 3.5);
        alpha = (star + exp(-radius * radius * 18.0)) * (1.0 - smoothstep(0.65, 1.0, radius));
        color = mix(tint.rgb, vec3(0.96, 0.98, 1.0), exp(-radius * radius * 30.0));
    } else if (mode > 8.5) {
        vec2 q = p * 2.0 - 1.0;
        float diamond = abs(q.x) + abs(q.y);
        alpha = 1.0 - smoothstep(0.92, 1.0, diamond);
        float side = q.x > 0.0 ? 0.8 : 0.45;
        float facet = q.y > 0.0 ? 0.25 : 0.0;
        float edge = smoothstep(0.78, 0.94, diamond);
        color = mix(tint.rgb * (side + facet), vec3(0.92, 0.97, 1.0), edge * 0.75);
    } else if (mode > 7.5) {
        vec2 q = abs(p * 2.0 - 1.0);
        alpha = 1.0 - smoothstep(0.7, 0.98, max(q.x, q.y));
        color *= 0.65 + (1.0 - p.y) * 0.35;
    } else if (mode > 6.5) {
        vec2 q = vec2(abs(p.x - 0.5), p.y);
        float upper = ellipse(q, vec2(0.25, 0.33), vec2(0.23, 0.29));
        float lower = ellipse(q, vec2(0.19, 0.69), vec2(0.18, 0.24));
        float wing = min(upper, lower);
        float body = (1.0 - smoothstep(0.016, 0.033, q.x)) * (1.0 - smoothstep(0.28, 0.4, abs(p.y - 0.5)));
        alpha = max(1.0 - smoothstep(0.92, 1.0, wing), body);
        float border = smoothstep(0.77, 0.96, wing);
        float veins = pow(0.5 + 0.5 * sin(atan(p.y - 0.48, q.x) * 16.0 + q.x * 12.0), 20.0);
        float pearl = pow(max(0.0, 1.0 - wing), 0.8);
        color = mix(tint.rgb * (0.6 + pearl * 0.6), vec3(0.045, 0.055, 0.09), border * 0.85 + veins * 0.18);
        color = mix(color, vec3(0.035, 0.04, 0.06), body);
        float spots = pow(0.5 + 0.5 * cos(q.x * 55.0 + p.y * 38.0), 28.0) * smoothstep(0.7, 0.92, wing);
        color += vec3(0.75, 0.9, 1.0) * spots * 0.65;
    } else if (mode > 5.5) {
        float across = abs(p.y * 2.0 - 1.0);
        alpha = exp(-across * across * 5.5) * (1.0 - smoothstep(0.75, 1.0, across));
        color = tint.rgb;
    } else {
        vec2 q = p * 2.0 - 1.0;
        float d = dot(q, q);
        alpha = exp(-d * 5.5) * (1.0 - smoothstep(0.55, 1.0, d));
        color = mix(tint.rgb, vec3(0.95, 1.0, 1.0), exp(-d * 40.0) * 0.9);
    }
    alpha *= tint.a;
    if (alpha < 0.003) discard;
    fragColor = vec4(color * Effect.z, min(alpha, 1.0));
}
