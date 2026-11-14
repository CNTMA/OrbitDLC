/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.longs.Long2FloatMap
 *  it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap
 *  net.minecraft.client.Minecraft
 *  net.minecraft.resources.Identifier
 */
package brokke.cc.util.render.display.text.font;

import brokke.cc.util.render.display.text.font.FontAtlas;
import brokke.cc.util.render.display.text.font.FontData;
import brokke.cc.util.render.display.text.font.FontMetrics;
import brokke.cc.util.render.display.text.font.Glyph;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

final class FontLoader {
    private FontLoader() {
    }

    static FontData load(Identifier id) {
        FontData fontData;
        try (BufferedReader reader = Minecraft.getInstance().getResourceManager().getResourceOrThrow(id).openAsReader();){
            fontData = FontLoader.parse(JsonParser.parseReader((Reader)reader).getAsJsonObject());
        }
        catch (IOException exception) {
            throw new UncheckedIOException("Failed to load font " + String.valueOf(id), exception);
        }
        return fontData;
    }

    static long pair(int left, int right) {
        return (long)left << 32 | (long)right & 0xFFFFFFFFL;
    }

    private static FontData parse(JsonObject root) {
        float em;
        float distanceRange;
        boolean topOrigin;
        JsonObject atlasJson = root.getAsJsonObject("atlas");
        String type = atlasJson.has("type") ? atlasJson.get("type").getAsString() : "msdf";
        boolean bl = topOrigin = atlasJson.has("yOrigin") && atlasJson.get("yOrigin").getAsString().equalsIgnoreCase("top");
        distanceRange = atlasJson.has("distanceRange") ? FontLoader.number(atlasJson, "distanceRange") : atlasJson.has("pxRange") ? FontLoader.number(atlasJson, "pxRange") : 6.0f;
        float size = atlasJson.has("size") ? FontLoader.number(atlasJson, "size") : (atlasJson.has("glyphSize") ? FontLoader.number(atlasJson, "glyphSize") : 74.0f);
        FontAtlas atlas = new FontAtlas(type, distanceRange, size, atlasJson.get("width").getAsInt(), atlasJson.get("height").getAsInt());
        JsonObject metricsJson = root.getAsJsonObject("metrics");
        float glyphEm = em = metricsJson.has("emSize") ? FontLoader.number(metricsJson, "emSize") : 1.0f;
        if (em > 10.0f && root.has("glyphs") && !root.getAsJsonArray("glyphs").isEmpty()) {
            float sampleAdvance;
            JsonObject sample = root.getAsJsonArray("glyphs").get(0).getAsJsonObject();
            if (sample.has("planeBounds")) {
                JsonObject pb = sample.getAsJsonObject("planeBounds");
                float sampleRight = FontLoader.number(pb, "right");
                if (sampleRight > 0.0f && sampleRight <= 2.0f) {
                    glyphEm = 1.0f;
                }
            } else if (sample.has("advance") && (sampleAdvance = FontLoader.number(sample, "advance")) > 0.0f && sampleAdvance <= 2.0f) {
                glyphEm = 1.0f;
            }
        }
        float metricsEm = metricsJson.has("lineHeight") && FontLoader.number(metricsJson, "lineHeight") <= 2.0f ? 1.0f : em;
        FontMetrics metrics = new FontMetrics(FontLoader.number(metricsJson, "lineHeight") / metricsEm, FontLoader.number(metricsJson, "ascender") / metricsEm, FontLoader.number(metricsJson, "descender") / metricsEm, metricsJson.has("underlineY") ? FontLoader.number(metricsJson, "underlineY") / metricsEm : 0.0f, metricsJson.has("underlineThickness") ? FontLoader.number(metricsJson, "underlineThickness") / metricsEm : 0.0f);
        Int2ObjectOpenHashMap glyphs = new Int2ObjectOpenHashMap();
        for (JsonElement element : root.getAsJsonArray("glyphs")) {
            JsonObject json = element.getAsJsonObject();
            if (!json.has("unicode")) continue;
            int unicode = json.get("unicode").getAsInt();
            float advance = FontLoader.number(json, "advance") / glyphEm;
            glyphs.put(unicode, (Object)(json.has("planeBounds") && json.has("atlasBounds") ? FontLoader.visible(unicode, advance, glyphEm, atlas, topOrigin, json.getAsJsonObject("planeBounds"), json.getAsJsonObject("atlasBounds")) : new Glyph(unicode, advance, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)));
        }
        Long2FloatOpenHashMap kerning = new Long2FloatOpenHashMap();
        if (root.has("kerning") && root.get("kerning").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("kerning")) {
                JsonObject json = element.getAsJsonObject();
                kerning.put(FontLoader.pair(json.get("unicode1").getAsInt(), json.get("unicode2").getAsInt()), FontLoader.number(json, "advance") / glyphEm);
            }
        }
        return new FontData(atlas, metrics, (Int2ObjectMap<Glyph>)glyphs, (Long2FloatMap)kerning, (Glyph)glyphs.get(63));
    }

    private static Glyph visible(int unicode, float advance, float em, FontAtlas atlas, boolean topOrigin, JsonObject plane, JsonObject bounds) {
        float al = FontLoader.number(bounds, "left");
        float ar = FontLoader.number(bounds, "right");
        float low = Math.min(FontLoader.number(bounds, "bottom"), FontLoader.number(bounds, "top"));
        float high = Math.max(FontLoader.number(bounds, "bottom"), FontLoader.number(bounds, "top"));
        float v0 = topOrigin ? low / (float)atlas.height() : 1.0f - high / (float)atlas.height();
        float v1 = topOrigin ? high / (float)atlas.height() : 1.0f - low / (float)atlas.height();
        return new Glyph(unicode, advance, FontLoader.number(plane, "left") / em, FontLoader.number(plane, "bottom") / em, FontLoader.number(plane, "right") / em, FontLoader.number(plane, "top") / em, al / (float)atlas.width(), v0, ar / (float)atlas.width(), v1);
    }

    private static float number(JsonObject json, String key) {
        return json.get(key).getAsFloat();
    }
}

