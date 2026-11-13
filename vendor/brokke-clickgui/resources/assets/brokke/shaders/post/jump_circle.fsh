#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) out vec4 fragColor;

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
uniform sampler2D SealSampler;

struct CircleData {
    vec4 originAndProgress;
    vec4 colorAndRadius;
    vec4 fxParams;
};

layout(std140) uniform WorldJumpCircleUniforms {
    mat4 InvProjMat;
    mat4 InvViewMat;
    vec4 CameraPos;
    vec4 ScreenParams;
    CircleData Circles[16];
};

vec3 reconstructWorldPos(vec2 texCoord, float depth) {
    float z = depth;
    float clipZ = (CameraPos.w > 0.5) ? z : z * 2.0 - 1.0;
    vec4 clipSpace = vec4(texCoord * 2.0 - 1.0, clipZ, 1.0);
    vec4 viewSpace = InvProjMat * clipSpace;
    viewSpace /= viewSpace.w;
    vec4 worldSpace = InvViewMat * viewSpace;
    return worldSpace.xyz + CameraPos.xyz;
}

void main() {
    vec2 uv = gl_FragCoord.xy / vec2(textureSize(SceneSampler, 0));
    float depth = texture(DepthSampler, uv).r;

    bool hasSurface = depth > 0.0;
    vec3 worldPos = reconstructWorldPos(uv, hasSurface ? depth : 0.0001);
    vec3 viewRay = normalize(reconstructWorldPos(uv, 1.0) - CameraPos.xyz);

    // Compute surface normal via screen derivatives
    vec3 dWorldX = dFdx(worldPos);
    vec3 dWorldY = dFdy(worldPos);
    vec3 surfaceNormal = cross(dWorldX, dWorldY);
    float normalLen = length(surfaceNormal);

    // Filter glow on vertical walls (normalY ~ 0.0), ground has normalY ~ 1.0
    float slopeMask = (normalLen > 1e-4) ? smoothstep(0.3, 0.7, abs(surfaceNormal.y / normalLen)) : 1.0;

    vec2 totalDistortion = vec2(0.0);
    vec3 totalGlow = vec3(0.0);

    int count = int(ScreenParams.w);

    for (int i = 0; i < count; i++) {
        CircleData circle = Circles[i];

        vec3 origin = circle.originAndProgress.xyz;
        float progress = circle.originAndProgress.w;
        vec3 circleColor = circle.colorAndRadius.rgb;
        float maxRadius = circle.colorAndRadius.w;

        float distortionPower = circle.fxParams.x;
        float glowPower = circle.fxParams.y;
        float ringWidth = max(circle.fxParams.z, 0.05);
        float heightFalloff = circle.fxParams.w;

        vec3 deltaWorld = worldPos - origin;
        float distXZ = length(deltaWorld.xz);
        float distY = abs(deltaWorld.y);

        if (ScreenParams.z >= 0.0) {
            circleColor = 0.65 + 0.35 * cos(atan(deltaWorld.z, deltaWorld.x) + ScreenParams.z * 0.8 + vec3(0.0, 2.1, 4.2));
        }

        if (heightFalloff < 0.0) {
            if (abs(viewRay.y) < 0.00001) continue;
            float planeDistance = (origin.y + 0.02 - CameraPos.y) / viewRay.y;
            if (planeDistance <= 0.0) continue;
            if (hasSurface && planeDistance > length(worldPos - CameraPos.xyz) + 0.03) continue;
            vec3 sealPos = CameraPos.xyz + viewRay * planeDistance;
            vec2 sealDelta = sealPos.xz - origin.xz;
            float sealRadius = maxRadius * (0.3 + 0.7 * sin(progress * 1.5707963));
            float angle = progress * 0.65;
            mat2 rotation = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
            vec2 sealUV = rotation * sealDelta / max(sealRadius * 2.0, 0.001) + 0.5;
            if (all(greaterThanEqual(sealUV, vec2(0.0))) && all(lessThanEqual(sealUV, vec2(1.0)))) {
                float ink = texture(SealSampler, sealUV).a;
                vec2 texel = vec2(1.5 / 1024.0);
                float halo = texture(SealSampler, sealUV + vec2(texel.x, 0.0)).a;
                halo += texture(SealSampler, sealUV - vec2(texel.x, 0.0)).a;
                halo += texture(SealSampler, sealUV + vec2(0.0, texel.y)).a;
                halo += texture(SealSampler, sealUV - vec2(0.0, texel.y)).a;
                float life = smoothstep(0.0, 0.08, progress) * pow(1.0 - progress, 0.7);
                totalGlow += (circleColor * (ink * 1.1 + halo * 0.24) + vec3(ink * 0.25)) * glowPower * life;
            }
            continue;
        }

        if (!hasSurface) continue;

        float easedProgress = sin(progress * 1.5707963);
        float currentRadius = easedProgress * maxRadius;

        float d = distXZ - currentRadius;
        float absD = abs(d);

        float verticalMask = smoothstep(heightFalloff, 0.0, distY);
        float fadeIn = smoothstep(0.0, 0.06, progress);
        float fadeOut = pow(1.0 - progress, 1.2);

        // Glow is masked by slope so it doesn't paint vertical walls
        float lifeMask = fadeIn * fadeOut * verticalMask * slopeMask;

        if (lifeMask > 0.0005) {
            float coreLineWidth = ringWidth * 0.09;
            float coreLine = exp(-pow(d / coreLineWidth, 2.0));

            float auraNear = exp(-absD / (ringWidth * 0.35)) * 1.3;
            float auraMid  = exp(-absD / (ringWidth * 0.90)) * 0.65;
            float auraFar  = exp(-absD / (ringWidth * 2.20)) * 0.25;

            float fullAura = auraNear + auraMid + auraFar;

            vec3 coreColor = mix(circleColor, vec3(1.0), 0.7);
            vec3 neonLight = (circleColor * fullAura + coreColor * (coreLine * 2.8)) * glowPower;
            totalGlow += neonLight * lifeMask;
        }

        // Distortion spreads across all nearby blocks (fade * vertical)
        float distortMask = fadeIn * fadeOut * verticalMask;
        if (distortionPower > 0.01 && distortMask > 0.0005) {
            vec2 dirXZ = (distXZ > 0.0001) ? (deltaWorld.xz / distXZ) : vec2(0.0, 1.0);
            float waveFactor = (d / ringWidth) * exp(-pow(d / (ringWidth * 0.65), 2.0));
            vec2 screenDir = vec2(dirXZ.x, dirXZ.y * (ScreenParams.y / ScreenParams.x));
            vec2 distortOffset = screenDir * (-waveFactor) * (distortionPower * 0.045) * distortMask;
            totalDistortion += distortOffset;
        }
    }

    vec2 finalUV = uv;
    vec3 sceneColor;
    float distMag = length(totalDistortion);

    if (distMag > 0.0001) {
        // Clamp distortion UV so it never samples outside the screen buffer
        vec2 candidateUV = clamp(uv + totalDistortion, vec2(0.001), vec2(0.999));
        float sampleDepth = texture(DepthSampler, candidateUV).r;

        // If candidate pixel is the sky (depth >= 1.0) while current pixel is terrain, don't distort into the sky
        if (sampleDepth <= 0.0) {
            candidateUV = uv;
        }

        finalUV = candidateUV;
        float ca = distMag * 0.45;
        sceneColor.r = texture(SceneSampler, clamp(finalUV + totalDistortion * ca, vec2(0.001), vec2(0.999))).r;
        sceneColor.g = texture(SceneSampler, finalUV).g;
        sceneColor.b = texture(SceneSampler, clamp(finalUV - totalDistortion * ca, vec2(0.001), vec2(0.999))).b;
    } else {
        sceneColor = texture(SceneSampler, uv).rgb;
    }

    fragColor = vec4(sceneColor + totalGlow, 1.0);
}
