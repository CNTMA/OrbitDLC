package com.orbitdlc.client;

import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class TargetEsp extends Module {
    private VisualColor color = VisualColor.ORIGINAL;

    TargetEsp() { super("target_esp", Category.VISUALS); }

    int tint() { return 0xFF000000 | color.renderColor(); }

    @Override
    public String description() { return "Rotating marker for the entity under the crosshair."; }

    @Override
    public com.google.gson.JsonObject saveSettings() {
        var settings = super.saveSettings();
        settings.addProperty("color", color.name());
        return settings;
    }

    @Override
    public void loadSettings(com.google.gson.JsonObject settings) {
        color = VisualColor.load(settings);
    }

    ModuleSettings settings(Screen parent) {
        return new ModuleSettings("TargetESP", parent) {
            @Override
            protected void init() {
                chip("Color · " + color.label, 0, button -> {
                    color = color.next();
                    button.setMessage(Component.literal("Color · " + color.label));
                });
            }
        };
    }
}
