package com.orbitdlc.client;

import com.google.gson.JsonObject;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public final class Optimization extends Module {
    static final Optimization INSTANCE = new Optimization();
    private static final int MINIMUM_TARGET = 200;
    private static final List<Integer> TARGETS = List.of(MINIMUM_TARGET, 240, 300, 360);
    private int target = MINIMUM_TARGET;
    private boolean adaptive;
    private int level;
    private int lowSamples;
    private int highSamples;
    private long nextSample;

    private Optimization() {
        super("optimization", Category.UTILITIES);
    }

    @Override
    public String description() {
        return "Reduce the cost of clouds, particles and shader effects.";
    }

    @Override
    public JsonObject saveSettings() {
        var settings = new JsonObject();
        settings.addProperty("targetFps", target);
        settings.addProperty("adaptive", adaptive);
        return settings;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        int savedTarget = settings.has("targetFps") ? settings.get("targetFps").getAsInt() : MINIMUM_TARGET;
        target = TARGETS.stream().filter(fps -> fps >= savedTarget).findFirst().orElse(TARGETS.getLast());
        adaptive = settings.has("adaptive") && settings.get("adaptive").getAsBoolean();
        reset();
    }

    @Override
    protected void onDisable() {
        reset();
    }

    private void reset() {
        level = lowSamples = highSamples = 0;
        nextSample = 0;
    }

    void tick(Minecraft client) {
        if (!isEnabled() || !adaptive) return;
        if (client.level == null) {
            reset();
            return;
        }
        if (client.gui.screen() != null || !client.isWindowActive()) return;
        long now = System.nanoTime();
        if (now < nextSample) return;
        nextSample = now + 1_000_000_000L;
        int fps = client.getFps();
        if (fps <= 0) return;
        int cap = client.options.framerateLimit().get();
        int goal = Math.min(target, cap > 0 ? cap : target);
        if (fps < goal) {
            highSamples = 0;
            if (++lowSamples >= 2) {
                level = Math.min(level + 1, 3);
                lowSamples = 0;
            }
        } else {
            lowSamples = 0;
            if (fps >= goal && ++highSamples >= 12) {
                level = Math.max(level - 1, 0);
                highSamples = 0;
            } else if (fps < goal) {
                highSamples = 0;
            }
        }
    }

    public static int level() {
        return INSTANCE.isEnabled() ? (INSTANCE.adaptive ? INSTANCE.level : 3) : 0;
    }

    static net.minecraft.client.renderer.culling.Frustum frustum(net.minecraft.client.Camera camera) {
        return INSTANCE.isEnabled() ? camera.getCullFrustum() : null;
    }

    public static net.minecraft.client.renderer.culling.Frustum worldFrustum(
            net.minecraft.client.renderer.state.level.CameraRenderState camera) {
        return INSTANCE.isEnabled() && camera != null && camera.initialized
                && !camera.isFrustumCaptured && !camera.isPanoramicMode ? camera.cullFrustum : null;
    }

    private String modeLabel() {
        return adaptive ? "Mode · Adaptive" : "Mode · Performance";
    }

    ModuleSettings settings(Screen parent) {
        return new ModuleSettings("Optimization", parent) {
            @Override
            protected void init() {
                chip(modeLabel(), 0, button -> {
                    adaptive = !adaptive;
                    reset();
                    button.setMessage(net.minecraft.network.chat.Component.literal(modeLabel()));
                });
                choicesAt(TARGETS.stream().map(fps -> fps + " FPS").toList(),
                        i -> target == TARGETS.get(i), i -> {
                            target = TARGETS.get(i);
                            reset();
                        }, 25);
            }

            @Override
            protected String summary() {
                if (!adaptive) return "Optimization always active";
                return "Target: " + target + " FPS · " + switch (level) {
                    case 1 -> "Light";
                    case 2 -> "Balanced";
                    case 3 -> "Maximum";
                    default -> "Full quality";
                };
            }
        };
    }
}
