/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.Identifier
 */
package brokke.cc.util.render.display.text.font;

import brokke.cc.util.render.display.text.TextLayout;
import brokke.cc.util.render.display.text.font.MsdfFont;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class Fonts {
    private static final String NAMESPACE = "brokke";
    private static final String DIRECTORY = "fonts/";
    private static final Map<String, MsdfFont> REGISTRY = new LinkedHashMap<String, MsdfFont>();
    public static final MsdfFont MEDIUM = Fonts.register("medium");
    public static final MsdfFont SEMIBOLD = Fonts.register("semibold", "fonts/medium");
    public static final MsdfFont ICONS = Fonts.register("lucide");
    private static MsdfFont defaultFont = MEDIUM;

    private Fonts() {
    }

    public static void init() {
    }

    public static MsdfFont register(String name) {
        return Fonts.register(name, DIRECTORY + name);
    }

    public static MsdfFont register(String name, String path) {
        return REGISTRY.computeIfAbsent(name, key -> new MsdfFont((String)key, Fonts.id(path + ".json"), Fonts.id(path + ".png")));
    }

    public static MsdfFont get(String name) {
        MsdfFont font = REGISTRY.get(name);
        if (font == null) {
            throw new IllegalArgumentException("Unknown font: " + name);
        }
        return font;
    }

    public static boolean has(String name) {
        return REGISTRY.containsKey(name);
    }

    public static Collection<MsdfFont> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static MsdfFont defaultFont() {
        return defaultFont;
    }

    public static void defaultFont(MsdfFont font) {
        defaultFont = font;
    }

    public static void reload() {
        REGISTRY.values().forEach(MsdfFont::reload);
        TextLayout.clear();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath((String)NAMESPACE, (String)path);
    }
}

