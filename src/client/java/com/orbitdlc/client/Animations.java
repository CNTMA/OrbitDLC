package com.orbitdlc.client;

import com.google.gson.JsonObject;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.minecraft.client.gui.screens.Screen;

import java.util.EnumSet;
import java.util.List;

public final class Animations extends Module {
    static final Animations INSTANCE = new Animations();

    public enum Part {
        SCREENS("Menus"), INVENTORY("Inventory"), TAB("Player list"),
        CHAT("New chat messages"), HOTBAR("Hotbar"), SLOT("Selected slot"),
        EFFECTS("Effects"), TARGET("Target HUD");

        final String label;

        Part(String label) { this.label = label; }
    }

    private final EnumSet<Part> selected = EnumSet.allOf(Part.class);

    private Animations() { super("animations", Category.VISUALS); }

    @Override public String description() { return "Configurable animations for screens and HUD."; }

    public static boolean plays(Part part) {
        return INSTANCE.isEnabled() && INSTANCE.selected.contains(part);
    }

    public static float remaining(long started, long millis) {
        if (started == 0) return 0;
        float elapsed = Math.clamp((System.nanoTime() - started) / (millis * 1_000_000f), 0f, 1f);
        float left = 1f - elapsed;
        return left * left * left;
    }

    @Override public JsonObject saveSettings() {
        var settings = new JsonObject();
        for (var part : Part.values()) settings.addProperty(part.name(), selected.contains(part));
        return settings;
    }

    @Override public void loadSettings(JsonObject settings) {
        selected.clear();
        for (var part : Part.values()) {
            if (!settings.has(part.name()) || settings.get(part.name()).getAsBoolean()) selected.add(part);
        }
    }

    ModuleSettings settings(Screen parent) {
        return new ModuleSettings("Animations", parent) {
            @Override protected void init() {
                var parts = Part.values();
                for (int i = 0; i < parts.length; i++) {
                    var part = parts[i];
                    toggleAt(part.label, i * 23, () -> selected.contains(part), () -> {
                        if (!selected.remove(part)) selected.add(part);
                    });
                }
            }

            @Override protected String summary() {
                return selected.size() + " / " + Part.values().length;
            }
        };
    }
}
