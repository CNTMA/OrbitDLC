package com.orbitdlc.client;

import com.google.gson.JsonObject;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class AspectRatio extends Module {
    static final AspectRatio INSTANCE = new AspectRatio();

    private enum Mode { FOUR_THREE, SIXTEEN_NINE, CUSTOM }

    private Mode mode = Mode.FOUR_THREE;
    private int width = 16;
    private int height = 10;

    private AspectRatio() {
        super("aspectratio", Category.VISUALS);
    }

    @Override
    public String description() {
        return "Change the world camera aspect ratio.";
    }

    public static float resolve(float vanilla) {
        if (!INSTANCE.isEnabled()) return vanilla;
        return switch (INSTANCE.mode) {
            case FOUR_THREE -> 4f / 3;
            case SIXTEEN_NINE -> 16f / 9;
            case CUSTOM -> (float) INSTANCE.width / INSTANCE.height;
        };
    }

    public static float projectionWidth(float width, float height) {
        if (height <= 0) return width;
        float vanilla = width / height;
        float ratio = resolve(vanilla);
        return ratio == vanilla ? width : height * ratio;
    }

    @Override
    public JsonObject saveSettings() {
        var settings = new JsonObject();
        settings.addProperty("mode", mode.name());
        settings.addProperty("width", width);
        settings.addProperty("height", height);
        return settings;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        mode = settings.has("mode") ? Mode.valueOf(settings.get("mode").getAsString()) : Mode.FOUR_THREE;
        width = settings.has("width") ? Math.clamp(settings.get("width").getAsInt(), 1, 64) : 16;
        height = settings.has("height") ? Math.clamp(settings.get("height").getAsInt(), 1, 64) : 10;
    }

    private void apply(Mode selected) {
        mode = selected;
        setEnabled(true);
    }

    ModuleSettings settings(Screen parent) {
        return new ModuleSettings("AspectRatio", parent) {
            @Override
            protected void init() {
                choices(List.of("4:3", "16:9", "Custom"), i -> mode.ordinal() == i,
                        i -> apply(Mode.values()[i]));
                slider("Width", "", 36, 1, 64, () -> width, value -> { width = value; apply(Mode.CUSTOM); });
                slider("Height", "", 60, 1, 64, () -> height, value -> { height = value; apply(Mode.CUSTOM); });
            }

            @Override
            protected String summary() {
                return switch (mode) {
                    case FOUR_THREE -> "4:3";
                    case SIXTEEN_NINE -> "16:9";
                    case CUSTOM -> "Custom · " + width + ":" + height;
                };
            }
        };
    }
}
