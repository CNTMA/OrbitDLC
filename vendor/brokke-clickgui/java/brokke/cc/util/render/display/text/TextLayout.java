/*
 * Decompiled with CFR 0.152.
 */
package brokke.cc.util.render.display.text;

import brokke.cc.util.render.display.text.font.Glyph;
import brokke.cc.util.render.display.text.font.MsdfFont;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TextLayout {
    public static final int STRIDE = 8;
    private static final int CACHE_SIZE = 1024;
    private static final Map<Key, TextLayout> CACHE = new LinkedHashMap<Key, TextLayout>(256, 0.75f, true){

        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, TextLayout> eldest) {
            return this.size() > 1024;
        }
    };
    private final MsdfFont font;
    private final float[] quads;
    private final int[] indices;
    private final int count;
    private final float width;
    private final float height;
    private final float ascender;

    private TextLayout(MsdfFont font, float[] quads, int[] indices, int count, float width, float height, float ascender) {
        this.font = font;
        this.quads = quads;
        this.indices = indices;
        this.count = count;
        this.width = width;
        this.height = height;
        this.ascender = ascender;
    }

    public static TextLayout of(MsdfFont font, String text, float letterSpacing) {
        return CACHE.computeIfAbsent(new Key(font, text, letterSpacing), TextLayout::compute);
    }

    public static void clear() {
        CACHE.clear();
    }

    public MsdfFont font() {
        return this.font;
    }

    public float[] quads() {
        return this.quads;
    }

    public int[] indices() {
        return this.indices;
    }

    public int glyphCount() {
        return this.count;
    }

    public float width() {
        return this.width;
    }

    public float height() {
        return this.height;
    }

    public float ascender() {
        return this.ascender;
    }

    private static TextLayout compute(Key key) {
        MsdfFont font = key.font();
        String text = key.text();
        int capacity = text.codePointCount(0, text.length());
        float spacing = key.letterSpacing();
        float ascender = font.metrics().ascender();
        float[] data = new float[capacity * 8];
        int[] indices = new int[capacity];
        int count = 0;
        int visible = 0;
        float pen = 0.0f;
        int prev = -1;
        int glyphIndex = 0;
        int offset = 0;
        while (offset < text.length()) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (codePoint >= 32 || codePoint == 9) {
                Glyph glyph;
                if (prev >= 0) {
                    pen += font.kerning(prev, codePoint);
                }
                if ((glyph = font.glyph(codePoint)) != null && glyph.visible()) {
                    int o = count * 8;
                    data[o] = pen + glyph.left();
                    data[o + 1] = ascender - glyph.top();
                    data[o + 2] = pen + glyph.right();
                    data[o + 3] = ascender - glyph.bottom();
                    data[o + 4] = glyph.u0();
                    data[o + 5] = glyph.v0();
                    data[o + 6] = glyph.u1();
                    data[o + 7] = glyph.v1();
                    indices[count++] = glyphIndex;
                }
                pen += font.advance(codePoint) + spacing;
                prev = codePoint;
                ++visible;
            }
            ++glyphIndex;
        }
        float width = visible > 0 ? pen - spacing : 0.0f;
        return new TextLayout(font, count == capacity ? data : Arrays.copyOf(data, count * 8), count == capacity ? indices : Arrays.copyOf(indices, count), count, width, font.metrics().lineHeight(), ascender);
    }

    private record Key(MsdfFont font, String text, float letterSpacing) {
    }
}

