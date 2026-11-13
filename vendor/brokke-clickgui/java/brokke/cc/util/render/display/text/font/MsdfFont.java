/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.renderpearl.api.textures.GpuTextureView
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.texture.AbstractTexture
 *  net.minecraft.client.renderer.texture.ReloadableTexture
 *  net.minecraft.client.renderer.texture.SimpleTexture
 *  net.minecraft.client.renderer.texture.TextureManager
 *  net.minecraft.resources.Identifier
 *  org.jspecify.annotations.Nullable
 */
package brokke.cc.util.render.display.text.font;

import brokke.cc.util.Log;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.font.FontAtlas;
import brokke.cc.util.render.display.text.font.FontData;
import brokke.cc.util.render.display.text.font.FontLoader;
import brokke.cc.util.render.display.text.font.FontMetrics;
import brokke.cc.util.render.display.text.font.Glyph;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class MsdfFont {
    private static final int TAB_SPACES = 4;
    private final String name;
    private final Identifier descriptor;
    private final Identifier texture;
    private @Nullable FontData data;
    private final Map<String, Float> widths = Collections.synchronizedMap(new LinkedHashMap<String, Float>(256, 0.75f, true){

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Float> entry) {
            return this.size() > 2048;
        }
    });

    MsdfFont(String name, Identifier descriptor, Identifier texture) {
        this.name = name;
        this.descriptor = descriptor;
        this.texture = texture;
    }

    public String name() {
        return this.name;
    }

    public Identifier texture() {
        return this.texture;
    }

    public FontAtlas atlas() {
        return this.data().atlas();
    }

    public FontMetrics metrics() {
        return this.data().metrics();
    }

    public @Nullable GpuTextureView textureView() {
        TextureManager tm = Minecraft.getInstance().getTextureManager();
        if (tm == null) {
            return null;
        }
        AbstractTexture tex = tm.getTexture(this.texture);
        if (tex == null || tex.getTextureView() == null) {
            try {
                tm.registerAndLoad(this.texture, (ReloadableTexture)new SimpleTexture(this.texture));
                tex = tm.getTexture(this.texture);
            }
            catch (Throwable t) {
                Log.error(t);
            }
        }
        return tex != null ? tex.getTextureView() : null;
    }

    public @Nullable Glyph glyph(int codePoint) {
        if (codePoint < 32) {
            return null;
        }
        FontData font = this.data();
        Glyph glyph = (Glyph)font.glyphs().get(codePoint);
        return glyph != null ? glyph : font.fallback();
    }

    public boolean has(int codePoint) {
        return this.data().glyphs().containsKey(codePoint);
    }

    public float advance(int codePoint) {
        if (codePoint == 9) {
            return this.advance(32) * 4.0f;
        }
        Glyph glyph = this.glyph(codePoint);
        return glyph != null ? glyph.advance() : 0.0f;
    }

    public float kerning(int left, int right) {
        return this.data().kerning().get(FontLoader.pair(left, right));
    }

    public float width(CharSequence text, float size) {
        return this.width(text, size, 0.0f);
    }

    public float width(CharSequence text, float size, float letterSpacing) {
        String value;
        if (letterSpacing == 0.0f && text instanceof String && (value = (String)text).length() <= 512) {
            Float cached = this.widths.get(value);
            if (cached == null) {
                cached = Float.valueOf(this.measure(value, 1.0f, 0.0f));
                this.widths.put(value, cached);
            }
            return cached.floatValue() * size;
        }
        return this.measure(text, size, letterSpacing);
    }

    private float measure(CharSequence text, float size, float letterSpacing) {
        float spacing = size > 0.0f ? letterSpacing / size : 0.0f;
        float pen = 0.0f;
        int count = 0;
        int prev = -1;
        int i = 0;
        while (i < text.length()) {
            int codePoint = Character.codePointAt(text, i);
            i += Character.charCount(codePoint);
            if (codePoint < 32 && codePoint != 9) continue;
            if (prev >= 0) {
                pen += this.kerning(prev, codePoint);
            }
            pen += this.advance(codePoint) + spacing;
            prev = codePoint;
            ++count;
        }
        return count > 0 ? (pen - spacing) * size : 0.0f;
    }

    public float lineHeight(float size) {
        return this.metrics().lineHeight() * size;
    }

    public float ascent(float size) {
        return this.metrics().ascender() * size;
    }

    public float descent(float size) {
        return -this.metrics().descender() * size;
    }

    public String trim(String text, float size, float maxWidth) {
        if (this.width(text, size) <= maxWidth) {
            return text;
        }
        int end = 0;
        float pen = 0.0f;
        int prev = -1;
        int i = 0;
        while (i < text.length()) {
            int codePoint = text.codePointAt(i);
            int next = i + Character.charCount(codePoint);
            if (codePoint >= 32 || codePoint == 9) {
                if (prev >= 0) {
                    pen += this.kerning(prev, codePoint);
                }
                pen += this.advance(codePoint);
                prev = codePoint;
            }
            if (pen * size > maxWidth) break;
            end = next;
            i = next;
        }
        return text.substring(0, end);
    }

    public Text text(String text) {
        return Text.of(this, text);
    }

    void reload() {
        this.widths.clear();
        this.data = null;
    }

    private FontData data() {
        FontData loaded = this.data;
        if (loaded == null) {
            this.data = loaded = FontLoader.load(this.descriptor);
        }
        return loaded;
    }
}

