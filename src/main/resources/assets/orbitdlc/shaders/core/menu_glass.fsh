#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D Sampler0;
layout(location = 0) in vec2 localPos;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) flat in vec2 halfExtent;
layout(location = 3) flat in ivec2 radii;
layout(location = 4) in vec2 screenUv;
layout(location = 0) out vec4 OutColor;
const float CornerSmoothness = 2.0;
const float BaseAlpha = 0.94;
const float DistortStrength = 0.055;
const float DistortRadius = 18.0;
const float Aberration = 0.4;
const float Saturation = 1.1;
float roundedBoxSDF(vec2 p, vec2 b, vec4 r, float smoothness) {
    r = min(r, vec4(min(b.x, b.y) * smoothness * 0.5));
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x  = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    vec2 q_clamped = max(q, 0.0);
    float len = pow(pow(q_clamped.x, smoothness) + pow(q_clamped.y, smoothness), 1.0 / smoothness);
    return min(max(q.x, q.y), 0.0) + len - r.x;
}

// Мягкое покрытие фигуры (0.5 ровно на кромке, 1 вглубь на ширину band, 0 снаружи) —
// аналитический аналог mask + gaussianBlur из drawLiquidGlass. -p для совпадения скруглений
// с покрытием панели (sdCover).
float glassCoverage(vec2 p, vec2 b, vec4 r, float smoothness, float band) {
    return smoothstep(-band, band, -roundedBoxSDF(-p, b, r, smoothness));
}

vec3 forestScene(vec2 uv, float phase) {
    float water = smoothstep(0.67, 0.79, uv.y)
                * (1.0 - smoothstep(0.43 + uv.y * 0.25, 0.57 + uv.y * 0.25, uv.x));
    float wave = sin(uv.y * 230.0 + uv.x * 16.0 - phase * 863.0);
    float swell = sin(uv.y * 105.0 - uv.x * 24.0 + phase * 587.0);
    vec2 ripple = vec2(wave * 0.0028 + swell * 0.002, swell * 0.0014) * water;
    vec3 color = texture(Sampler0, clamp(uv + ripple, 0.0, 1.0)).rgb;
    float sunMask = (1.0 - smoothstep(0.08, 0.52, uv.x)) * (1.0 - smoothstep(0.48, 0.83, uv.y));
    float beam = uv.x - uv.y * 0.34;
    float rays = 0.5 + 0.5 * sin(beam * 44.0 + sin(phase * 47.0) * 0.7);
    color += vec3(0.15, 0.20, 0.19) * rays * rays * sunMask * 0.55;
    float glint = pow(max(0.0, wave * swell), 5.0);
    color += vec3(0.22, 0.32, 0.36) * glint * water * 0.42;
    return clamp(color, 0.0, 1.0);
}

void main() {
    vec2 sourceSize = vec2(textureSize(Sampler0, 0));
    float sourceAspect = sourceSize.x / sourceSize.y;
    float screenAspect = abs(dFdy(screenUv.y) / max(abs(dFdx(screenUv.x)), 1e-8));
    vec2 crop = min(vec2(1.0), vec2(screenAspect / sourceAspect, sourceAspect / screenAspect));
    float phase = float(radii.y) * (6.28318530718 / 65536.0);
    vec2 drift = vec2(sin(phase * 34.0), cos(phase * 26.0)) * vec2(0.009, 0.006);
    vec2 TexCoord = (vec2(screenUv.x, 1.0 - screenUv.y) - 0.5) * crop * 0.96 + 0.5 + drift;
    if (radii.x == 0) {
        OutColor = vec4(forestScene(TexCoord, phase), 1.0);
        return;
    }
    vec2 Size = (halfExtent + 1.0) * 2.0;
    vec2 FragCoord = localPos / Size + 0.5;
    vec4 Radius = vec4(float(radii.x & 255) * 0.5);
    vec2 center = Size * 0.5;
    vec2 halfSize = center - 1.0;
    vec2 pos = (FragCoord * Size) - center;

    // Покрытие фигуры (как в оригинале: -pos для совпадения скруглений с остальным UI).
    float sdCover = roundedBoxSDF(-pos, halfSize, Radius, CornerSmoothness);
    float alpha = clamp(0.5 - sdCover / max(fwidth(sdCover), 1e-4), 0.0, 1.0);
    if (alpha < 0.001) discard;

    float minSide = min(halfSize.x, halfSize.y);
    float thickness = clamp(DistortRadius, 2.0, minSide);  // ширина перехода (роль blurRadius)

    // Мягкое покрытие и его градиент БОЛЬШИМ шагом — 1:1 структура из drawLiquidGlass:
    // smoothAlpha = blurred mask, нормаль = normalize(grad(smoothAlpha)). Большой шаг
    // (thickness * 0.6, как stepSize = blurRadius * 0.6) сглаживает углы — отсюда «масляное»
    // стекло без резких изломов нормали (главное отличие от аналитической нормали).
    float bigStep = thickness * 0.6;
    float c  = glassCoverage(pos,                       halfSize, Radius, CornerSmoothness, thickness);
    float cl = glassCoverage(pos + vec2(-bigStep, 0.0), halfSize, Radius, CornerSmoothness, thickness);
    float cr = glassCoverage(pos + vec2( bigStep, 0.0), halfSize, Radius, CornerSmoothness, thickness);
    float cd = glassCoverage(pos + vec2(0.0, -bigStep), halfSize, Radius, CornerSmoothness, thickness);
    float cu = glassCoverage(pos + vec2(0.0,  bigStep), halfSize, Radius, CornerSmoothness, thickness);

    // Градиент покрытия указывает ВНУТРЬ панели (к большему покрытию) — это направление
    // преломления. Те же равные пиксельные шаги по обеим осям, что и в референсе.
    vec2 gradient = vec2(cr - cl, cu - cd);
    vec2 normal = length(gradient) > 1e-4 ? normalize(gradient) : vec2(0.0);

    float distToEdge = 1.0 - c;                        // 1 снаружи -> 0.5 у кромки -> 0 в центре

    // Кривая отклика края — 1:1 из drawLiquidGlass: плавный подъём по полосе + резкий пик
    // у самой кромки.
    float responseCurve = smoothstep(0.0, 1.0, distToEdge * 1.2);
    float edgePeak = smoothstep(0.6, 1.0, distToEdge);
    float strength = mix(responseCurve * 0.6, 1.0, edgePeak);

    // Магнитуда — 1:1 из drawLiquidGlass: refractStrength = strength * distortion * 2.5,
    // смещение по нормали в ДОЛЯХ ректа. DistortStrength играет роль distortion из референса.
    float refractStrength = strength * DistortStrength * 2.5;

    // Доля ректа -> texCoord. FragCoord — нормализованные координаты ректа (0..1), TexCoord —
    // его срез в полноэкранном буфере. Якобиан fwidth(TexCoord)/fwidth(FragCoord) = «весь рект
    // в texCoord» (texExtent) и НЕ зависит от gui-scale (старое Size*fwidth недосчитывало в
    // guiScale раз — отсюда слабое искажение).
    vec2 fragToTex = fwidth(TexCoord) / max(fwidth(FragCoord), vec2(1e-6));
    vec2 refractDir = normal * refractStrength * fragToTex;

    // Хроматическая аберрация (порт): красный смещается сильнее, синий слабее — цветная
    // кайма у кромки. ab — половина расщепления из drawLiquidGlass (aberration * 0.4).
    float ab = Aberration * 0.4;
    vec3 col;
    col.r = forestScene(TexCoord + refractDir * (1.0 + ab), phase).r;
    col.g = forestScene(TexCoord + refractDir, phase).g;
    col.b = forestScene(TexCoord + refractDir * (1.0 - ab), phase).b;

    // Насыщение (порт) + вибранси тёмного стекла: чуть поднимаем насыщенность, чтобы
    // затемнённый фон «играл» сквозь дымку, как у iOS.
    float luma = dot(col, vec3(0.2126, 0.7152, 0.0722));
    col = mix(vec3(luma), col, Saturation * 1.15);

    // Тёмное стекло (как у iOS в тёмной теме): затемняем множителем, а не заливкой к
    // чёрному — цвета и контраст фона сохраняются (преломление остаётся читаемым),
    // просто всё темнее. Белый tint панели убран — от него была серая дымка.
    vec2 pixel = 1.0 / vec2(textureSize(Sampler0, 0));
    vec3 diffuse = texture(Sampler0, clamp(TexCoord + vec2(pixel.x, 0.0) * 2.0, 0.0, 1.0)).rgb
                 + texture(Sampler0, clamp(TexCoord - vec2(pixel.x, 0.0) * 2.0, 0.0, 1.0)).rgb
                 + texture(Sampler0, clamp(TexCoord + vec2(0.0, pixel.y) * 2.0, 0.0, 1.0)).rgb
                 + texture(Sampler0, clamp(TexCoord - vec2(0.0, pixel.y) * 2.0, 0.0, 1.0)).rgb;
    col = mix(col, diffuse * 0.25, 0.36 * (1.0 - strength));
    col = col * 0.56 + vec3(0.020, 0.029, 0.046);

    // Тонкий световой ободок у кромки (порт fresnel, сильно приглушён — яркий белый
    // на тёмном теле читается как свечение).
    float fresnel = smoothstep(0.0, 1.0, strength) * 0.1;
    float rim = exp(-abs(sdCover) * 1.25);
    col += vec3(fresnel) + vec3(0.18, 0.22, 0.29) * rim;

    float a = alpha * BaseAlpha * vertexColor.a;
    a = clamp(a, 0.0, 1.0);
    if (a < 0.001) discard;

    OutColor = vec4(col, a);
}
