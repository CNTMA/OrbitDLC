#version 450
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;
uniform sampler2D Sampler4;
uniform sampler2D Sampler5;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform CustomUniforms {
    float Time;
    vec4 Mouse;
    float ScreenWantsBlur;
    vec3 RIM_LIGHT_VEC;
    vec4 RIM_LIGHT_COLOR;
    float EPS_PIX;
    float DebugStep;
    float Pixelated;
    float PixelGridSize;
    float HoverScalePx;
    float FocusScalePx;
    float FocusBorderWidthPx;
    float FocusBorderIntensity;
    float FocusBorderSpeed;
};

#define MAX_WIDGETS 64
#define MAX_CUTOUTS 16
#define MAX_LAYERS 16
layout(std140) uniform WidgetInfo {
    float Count;
    vec4 LayerRanges[MAX_LAYERS];
    vec4 LayerBounds[MAX_LAYERS];
    vec4 LayerShadowBounds[MAX_LAYERS];
    vec4 Rects[MAX_WIDGETS];
    vec4 Rads[MAX_WIDGETS];
    vec4 Tints[MAX_WIDGETS];
    vec4 Optics0[MAX_WIDGETS];
    vec4 Optics1[MAX_WIDGETS];
    vec4 Smoothings[MAX_WIDGETS];
    vec4 ScissorRects[MAX_WIDGETS];
    vec4 ShadowRects[MAX_WIDGETS];
    vec4 Shadow0[MAX_WIDGETS];
    vec4 ShadowColor[MAX_WIDGETS];
    vec4 Extra0[MAX_WIDGETS];
    vec4 Pull[MAX_WIDGETS];
    vec4 Cutouts[MAX_CUTOUTS];
    vec4 CutoutRads[MAX_CUTOUTS];
    vec4 CutoutInfo;
};

layout(std140) uniform BgConfig {
    float ShadowExpand;
    float ShadowFactor;
    vec2 ShadowOffset;
};

layout(location = 0) out vec4 fragColor;

struct SDFResult { float dist; vec2 normal; int index; };

bool outside(vec2 p, vec4 r) {
    return p.x < r.x || p.y < r.y || p.x > r.z || p.y > r.w;
}

float sdBox(in vec2 p, in vec2 b, vec4 ra) {
    ra.xy = (p.x > 0.0) ? ra.xy : ra.zw;
    float r = (p.y > 0.0) ? ra.x : ra.y;
    vec2 w = abs(p) - (b - r);
    float g = max(w.x, w.y);
    return ((g > 0.0) ? length(max(w, 0.0)) : g) - r;
}

vec3 sdgBox(in vec2 p, in vec2 b, vec4 ra) {
    ra.xy = (p.x > 0.0) ? ra.xy : ra.zw;
    float r = (p.y > 0.0) ? ra.x : ra.y;
    vec2 w = abs(p) - (b - r);
    vec2 s = vec2(p.x < 0.0 ? -1.0 : 1.0, p.y < 0.0 ? -1.0 : 1.0);
    float g = max(w.x, w.y);
    vec2 q = max(w, 0.0);
    float l = length(q);
    float dist = (g > 0.0) ? l - r : g - r;
    vec2 n = (g > 0.0) ? (q / max(l, 1e-6)) : ((w.x > w.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0));
    return vec3(dist, s * n);
}

SDFResult opSmoothUnion(in SDFResult a, in SDFResult b, in float k) {
    if (k <= 0.0 || (a.dist >= 0.0 && b.dist >= 0.0)) return (a.dist < b.dist) ? a : b;
    float h = clamp(0.5 + 0.5 * (a.dist - b.dist) / k, 0.0, 1.0);
    float d = mix(a.dist, b.dist, h) - k * h * (1.0 - h);
    vec2 nMixed = mix(a.normal, b.normal, h);
    float nmL = length(nMixed);
    vec2 n = (nmL > 1e-6) ? (nMixed / nmL) : vec2(0.0, 1.0);
    int index = (a.dist < b.dist) ? a.index : b.index;
    return SDFResult(d, n, index);
}

SDFResult opHardUnion(SDFResult a, SDFResult b) { return (a.dist < b.dist) ? a : b; }

SDFResult opHardSubtract(SDFResult a, SDFResult b) {
    float d = max(a.dist, -b.dist);
    if (d == a.dist) return a;
    return SDFResult(d, -b.normal, a.index);
}

#define GLASS_FETCH(S) { \
    vec4 t = texture(S, uv); \
    plain = t.rgb; \
    if (disp) { t.r = texture(S, uvR).r; t.b = texture(S, uvB).b; } \
    if (hov) hover = texture(S, uvH).rgb; \
    c = t; \
}

vec4 sampleGlass(int idx, vec2 uv, vec2 uvR, vec2 uvB, vec2 uvH, bool disp, bool hov,
                 out vec3 plain, out vec3 hover) {
    vec4 c;
    plain = vec3(0.0);
    hover = vec3(0.0);
    uv = clamp(uv, 0.001, 0.999);
    if (disp) {
        uvR = clamp(uvR, 0.001, 0.999);
        uvB = clamp(uvB, 0.001, 0.999);
    }
    if (hov) {
        uvH = clamp(uvH, 0.001, 0.999);
    }
    if (idx <= 0) GLASS_FETCH(Sampler1)
    else if (idx == 1) GLASS_FETCH(Sampler2)
    else if (idx == 2) GLASS_FETCH(Sampler3)
    else if (idx == 3) GLASS_FETCH(Sampler4)
    else GLASS_FETCH(Sampler5)
    return c;
}

float cutoutField(vec2 coord, float invH) {
    int cn = int(CutoutInfo.x + 0.5);
    float d = 1e6;
    for (int i = 0; i < MAX_CUTOUTS; i++) {
        if (i >= cn) break;
        vec4 rc = Cutouts[i];
        if (rc.z <= 0.0 || rc.w <= 0.0) continue;
        vec2 p = coord - vec2(rc.x + 0.5 * rc.z, rc.y + 0.5 * rc.w);
        d = min(d, (sdBox(p, 0.5 * vec2(rc.z, rc.w), CutoutRads[i]) + 1.0) * invH);
    }
    return d;
}

SDFResult widgetField(int wi, vec2 coord, float invH) {
    vec4 rc = Rects[wi];
    if (rc.z <= 1.0 || rc.w <= 1.0) return SDFResult(1e6, vec2(0.0), -1);
    vec4 pl = Pull[wi];
    vec2 p = (coord - (vec2(rc.x + 0.5 * rc.z, rc.y + 0.5 * rc.w) + pl.xy)) * invH;
    vec2 b = 0.5 * vec2(rc.z * pl.z, rc.w * pl.w) * invH;
    vec3 g = sdgBox(p, b, Rads[wi] * invH);
    vec4 extra = Extra0[wi];
    float scaleOff = (HoverScalePx * extra.y + FocusScalePx * extra.z) * invH;
    return SDFResult(g.x - scaleOff, g.yz, wi);
}

SDFResult fieldWidgets(vec2 coord, float invH, int layerIndex) {
    vec4 range = LayerRanges[layerIndex];
    int start = int(range.x + 0.5);
    int posCount = int(range.y + 0.5);
    int subCount = int(range.z + 0.5);

    SDFResult f = SDFResult(1e6, vec2(0.0), -1);
    bool hasPos = false;

    for (int i = 0; i < MAX_WIDGETS; i++) {
        if (i >= posCount) break;
        int wi = start + i;
        if (outside(coord, ScissorRects[wi])) continue;
        SDFResult s = widgetField(wi, coord, invH);
        if (!hasPos) { f = s; hasPos = true; }
        else f = opSmoothUnion(f, s, Smoothings[wi].x);
    }

    int subStart = start + posCount;
    for (int i = 0; i < MAX_WIDGETS; i++) {
        if (i >= subCount) break;
        int wi = subStart + i;
        if (outside(coord, ScissorRects[wi])) continue;
        SDFResult s = widgetField(wi, coord, invH);
        SDFResult se = SDFResult(s.dist + Smoothings[wi].x, s.normal, s.index);
        f = opHardSubtract(f, se);
        f = opHardUnion(f, s);
    }

    return f;
}

void main() {
    vec2 inSize = InSize;
    if (inSize.x <= 0.0 || inSize.y <= 0.0) inSize = vec2(textureSize(Sampler0, 0));

    vec2 coord = gl_FragCoord.xy;

    int n = int(Count + 0.5);
    if (n == 0) {
        discard;
    }

    float invH = 1.0 / max(inSize.y, 1.0);
    int layerLimit = min(MAX_LAYERS, int(CutoutInfo.y + 0.5));
    float shadowAlpha = 0.0;
    vec3 shadowCol = vec3(0.0);
    float glassAlpha = 0.0;
    vec3 glassCol = vec3(0.0);

    float cutDist = cutoutField(coord, invH);

    for (int l = 0; l < MAX_LAYERS; l++) {
        if (l >= layerLimit) break;
        vec4 range = LayerRanges[l];
        int total = int(range.w + 0.5);
        if (total <= 0) continue;

        if (ShadowFactor > 0.001 && !outside(coord, LayerShadowBounds[l])) {
            int start = int(range.x + 0.5);
            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i >= total) break;
                int wi = start + i;
                if (outside(coord, ShadowRects[wi])) continue;

                vec4 rc = Rects[wi];
                if (rc.z <= 1.0 || rc.w <= 1.0) continue;
                vec4 sh0 = Shadow0[wi];
                vec2 p = coord + sh0.zw - vec2(rc.x + 0.5 * rc.z, rc.y + 0.5 * rc.w);
                float distPx = sdBox(p, 0.5 * vec2(rc.z, rc.w), Rads[wi]);
                float maxSpan = 35.0;
                if (distPx < maxSpan) {
                    float t = clamp(max(0.0, distPx) / maxSpan, 0.0, 1.0);
                    float fade = 1.0 - t * t * (3.0 - 2.0 * t);
                    float sh = exp(-abs(distPx) / max(sh0.x, 1e-4)) * 0.6 * sh0.y * fade;
                    vec4 scol = ShadowColor[wi];
                    float sA = clamp(scol.a * sh, 0.0, 1.0);
                    if (sA > shadowAlpha) {
                        shadowAlpha = sA;
                        shadowCol = scol.rgb;
                    }
                }
            }
        }

        if (outside(coord, LayerBounds[l])) continue;

        SDFResult f = fieldWidgets(coord, invH, l);
        float merged = max(f.dist, -cutDist);
        float mergedPx = merged * inSize.y;
        if (mergedPx >= 0.5) continue;

        int idx = f.index;
        if (idx < 0) continue;

        vec2 coordPix = coord;
        if (Pixelated > 0.5) {
            coordPix = floor(coord / PixelGridSize) * PixelGridSize + 0.5 * PixelGridSize;
        }
        vec2 uvPix = coordPix / max(inSize, vec2(1.0));

        vec4 o0 = Optics0[idx];
        vec4 o1 = Optics1[idx];
        vec4 tint = Tints[idx];
        vec4 extra = Extra0[idx];

        float refThickness = o0.x;
        float refFactor = o0.y;
        float refDisp = o0.z;
        float refFresRange = o0.w;

        float refFresHard = o1.x / 100.0;
        float refFresFac = o1.y / 100.0;
        float glareRange = o1.z;
        float glareHard = o1.w / 100.0;

        float hoverK = extra.y;
        float focusK = extra.z;
        int blurIndex = int(extra.x + 0.5);

        vec4 paneRect = Rects[idx];
        float paneSize = min(paneRect.z, paneRect.w);
        float thickness = min(refThickness, 0.25 * paneSize);

        vec2 normal = f.normal;
        float nlen = length(normal);
        if (nlen > 1e-6) normal /= nlen;

        float nmerged = -mergedPx;

        vec2 refrDir = vec2(0.0);
        float edgeProx = 0.0;
        bool disp = false;
        bool hov = hoverK > 0.0001;

        if (nmerged < thickness) {
            float xR = clamp(1.0 - nmerged / max(thickness, 1e-6), 0.0, 1.0);
            float thetaI = asin(clamp(xR * xR, -1.0, 1.0));
            float thetaT = asin(clamp((1.0 / max(refFactor, 1e-6)) * sin(thetaI), -1.0, 1.0));
            float edgeFactor = -tan(thetaT - thetaI);
            refrDir = -normal * edgeFactor * min(0.08 * inSize.y, 0.5 * paneSize) / max(inSize, vec2(1.0));
            edgeProx = exp(-abs(mergedPx) / 9.0);
            disp = refDisp > 0.01;
        }

        const float NR = 0.985;
        const float NB = 1.015;
        vec2 uvR = uvPix + refrDir * (1.0 - (NR - 1.0) * refDisp);
        vec2 uvB = uvPix + refrDir * (1.0 - (NB - 1.0) * refDisp);
        vec2 uvH = uvPix + refrDir * (1.0 + 0.35 * hoverK * edgeProx);

        vec3 plain;
        vec3 hoverSample;
        vec4 outColor = sampleGlass(blurIndex, uvPix + refrDir, uvR, uvB, uvH, disp, hov, plain, hoverSample);
        outColor = mix(outColor, vec4(tint.rgb, 1.0), tint.a);

        if (nmerged < thickness) {
            float fresScale = 500.0 / max(refFresRange, 1e-6);
            float fresBase = max(1.0 + mergedPx / 1500.0 * fresScale * fresScale + refFresHard, 0.0);
            float fresnelFactor = clamp(pow(fresBase, 5.0), 0.0, 1.0);
            vec3 fresTint = mix(vec3(1.0), tint.rgb, tint.a * 0.5);
            outColor = mix(outColor, vec4(fresTint, 1.0), fresnelFactor * refFresFac * 0.7 * nlen);

            float glareScale = 500.0 / max(glareRange, 1e-6);
            float glareBase = max(1.0 + mergedPx / 1500.0 * glareScale * glareScale + glareHard, 0.0);
            float glareGeo = clamp(pow(glareBase, 5.0), 0.0, 1.0);
            vec3 glareMix = mix(plain, tint.rgb, tint.a * 0.5);
            outColor.rgb = mix(outColor.rgb, glareMix, 0.25 * glareGeo * nlen);
        }

        if (hov) {
            vec3 add = mix(hoverSample, vec3(1.0), 0.08) * (0.15 + 0.35 * edgeProx) * hoverK;
            outColor.rgb = clamp(outColor.rgb + add, 0.0, 1.0);
        }

        if (focusK > 0.0001) {
            vec2 centerPx = vec2(paneRect.x + 0.5 * paneRect.z, paneRect.y + 0.5 * paneRect.w);
            vec2 v = (coord - centerPx) * invH;

            float phi = atan(v.y, v.x);
            float phi0 = Time * FocusBorderSpeed;
            float c = 0.5 + 0.5 * cos(phi - phi0);
            float sweep = smoothstep(0.75, 1.0, c);

            float w = max(FocusBorderWidthPx, 1e-3);
            float outsideEdge = step(0.0, mergedPx);
            float e = max(0.0, mergedPx) / w;
            float band = exp(-e * e) * outsideEdge;

            float intensity = FocusBorderIntensity * focusK;
            vec3 ringTint = mix(vec3(1.0), tint.rgb, 0.2);
            vec3 ring = ringTint * (band * sweep) * intensity;
            outColor.rgb = clamp(outColor.rgb + ring, 0.0, 1.0);
        }

        float widgetAlpha = Smoothings[idx].y > 0.001 ? clamp(Smoothings[idx].y, 0.0, 1.0) : 1.0;
        float aa = clamp(0.5 - mergedPx, 0.0, 1.0) * widgetAlpha;
        if (aa > glassAlpha) {
            glassAlpha = aa;
            glassCol = outColor.rgb;
        }
    }

    if (glassAlpha <= 0.001 && shadowAlpha <= 0.001) {
        discard;
    }

    float finalAlpha = clamp(glassAlpha + shadowAlpha * (1.0 - glassAlpha), 0.0, 1.0);
    vec3 finalRgb = (glassCol * glassAlpha + shadowCol * shadowAlpha * (1.0 - glassAlpha)) / max(finalAlpha, 1e-4);
    fragColor = vec4(finalRgb, finalAlpha);
}
