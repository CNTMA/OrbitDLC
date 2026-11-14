/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2FloatMap
 *  org.jspecify.annotations.Nullable
 */
package brokke.cc.util.render.display.text.font;

import brokke.cc.util.render.display.text.font.FontAtlas;
import brokke.cc.util.render.display.text.font.FontMetrics;
import brokke.cc.util.render.display.text.font.Glyph;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import org.jspecify.annotations.Nullable;

record FontData(FontAtlas atlas, FontMetrics metrics, Int2ObjectMap<Glyph> glyphs, Long2FloatMap kerning, @Nullable Glyph fallback) {
}

