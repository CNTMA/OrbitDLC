#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform MsdfData {
    mat4 uProjection;
    vec4 uTextColor;
    vec4 uGlowColor;
    vec4 uParams;
    vec4 uZ_Padding;
};

struct Glyph {
    vec4 rect;
    vec4 texCoords;
};

layout(std140) uniform GlyphData {
    Glyph glyphs[128];
};

layout(location = 0) out vec2 vTexCoord;
layout(location = 1) out vec4 vTextColor;
layout(location = 2) out vec4 vGlowColor;
layout(location = 3) out float vPxRange;
layout(location = 4) out float vGlowIntensity;

void main() {
    int glyphIndex = gl_VertexIndex / 6;
    int vertexIndex = gl_VertexIndex % 6;
    
    Glyph glyph = glyphs[glyphIndex];
    
    vec2 positions[6];
    positions[0] = vec2(0.0, 0.0);
    positions[1] = vec2(1.0, 0.0);
    positions[2] = vec2(1.0, 1.0);
    positions[3] = vec2(0.0, 0.0);
    positions[4] = vec2(1.0, 1.0);
    positions[5] = vec2(0.0, 1.0);
    
    vec2 pos = positions[vertexIndex];
    
    float glowIntensity = uParams.y;
    float glowExpand = glowIntensity * 4.0;
    vec2 expandedSize = glyph.rect.zw + glowExpand * 2.0;
    vec2 expandedPos = glyph.rect.xy - glowExpand + pos * expandedSize;
    
    vTexCoord = glyph.texCoords.xy + pos * glyph.texCoords.zw;
    
    vTextColor = uTextColor;
    vGlowColor = uGlowColor;
    vPxRange = uParams.x;
    vGlowIntensity = glowIntensity;
    
    gl_Position = uProjection * vec4(expandedPos, uZ_Padding.x, 1.0);
}
