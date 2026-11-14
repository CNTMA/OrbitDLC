package com.orbitdlc.client;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public final class SwingAnimations extends Module {
    static final SwingAnimations INSTANCE = new SwingAnimations();
    private int mode = 1;
    private int angle = 75;
    private int tip = -20;
    private int intensity = 5;

    private SwingAnimations() { super("swing_animations", Category.VISUALS); }

    @Override public String description() { return "Five first-person item swing styles from Source3."; }

    public static boolean transform(PoseStack pose, float progress, int side) {
        if (!INSTANCE.isEnabled()) return false;
        INSTANCE.apply(pose, progress, side);
        return true;
    }

    private void apply(PoseStack pose, float progress, int side) {
        float swing = (float) Math.sin(progress * Math.PI);
        float power = intensity * 10f;
        boolean fifth = mode == 5;
        pose.translate(side * (fifth ? 0.5f : 0.72f), -0.5f, fifth ? -0.72f : -1f);
        if (!fifth) pose.rotateDegrees(Axis.XP, -tip);

        switch (mode) {
            case 1 -> {
                pose.rotateDegrees(Axis.YP, side * 90f);
                pose.rotateDegrees(Axis.ZP, side * -70f);
                pose.rotateDegrees(Axis.XP, -angle - power * swing);
            }
            case 2 -> {
                pose.rotateDegrees(Axis.YP, side * 90f);
                pose.rotateDegrees(Axis.ZP, side * -65f);
                pose.rotateDegrees(Axis.XP, -65f + power * swing);
            }
            case 3 -> {
                pose.rotateDegrees(Axis.YP, side * -90f);
                pose.rotateDegrees(Axis.ZP, side * 60f);
                pose.rotateDegrees(Axis.XP, 30f);
                pose.rotateDegrees(Axis.ZP, side * power * swing);
            }
            case 4 -> {
                pose.rotateDegrees(Axis.YP, side * 90f);
                pose.rotateDegrees(Axis.ZP, side * -75f);
                pose.rotateDegrees(Axis.XP, -45f - power * swing);
                pose.rotateDegrees(Axis.YP, side * power * swing * 0.5f);
            }
            case 5 -> {
                float strength = power / 80f;
                float squaredSwing = swing * swing;
                float twist = (float) Math.sin(progress * progress * Math.PI);
                pose.rotateDegrees(Axis.YP, side * (45f - twist * 20f * strength));
                pose.rotateDegrees(Axis.ZP, side * squaredSwing * -22f * strength);
                pose.rotateDegrees(Axis.XP, squaredSwing * -85f * strength);
                pose.rotateDegrees(Axis.YP, side * -45f);
            }
        }
    }

    @Override public JsonObject saveSettings() {
        var settings = new JsonObject();
        settings.addProperty("mode", mode);
        settings.addProperty("angle", angle);
        settings.addProperty("tip", tip);
        settings.addProperty("intensity", intensity);
        return settings;
    }

    @Override public void loadSettings(JsonObject settings) {
        mode = read(settings, "mode", 1, 1, 5);
        angle = read(settings, "angle", 75, 0, 360);
        tip = read(settings, "tip", -20, -90, 90);
        intensity = read(settings, "intensity", 5, 1, 10);
    }

    private static int read(JsonObject settings, String key, int fallback, int min, int max) {
        return settings.has(key) ? Math.clamp(settings.get(key).getAsInt(), min, max) : fallback;
    }

    ModuleSettings settings(ClickGui parent) {
        return new ModuleSettings("SwingAnimations", parent) {
            @Override protected void init() {
                int row = choicesAt(List.of("Mode 1", "Mode 2", "Mode 3", "Mode 4", "Mode 5"),
                        index -> mode == index + 1, index -> {
                            mode = index + 1;
                            parent.refreshTab();
                        }, 0) + 4;
                slider("Intensity", "", row, 1, 10, () -> intensity, value -> intensity = value);
                if (mode == 1) {
                    row += 24;
                    slider("Rotation", "°", row, 0, 360, () -> angle, value -> angle = value);
                }
                if (mode != 5) slider("Tip tilt", "°", row + 24, -90, 90, () -> tip, value -> tip = value);
            }
        };
    }
}
