package com.orbitdlc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.orbitdlc.OrbitDLC;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Pose;

import java.awt.Color;

public final class BadTrip extends Module {
    static final BadTrip INSTANCE = new BadTrip();
    private static final double BEAT_SECONDS = 0.52;
    private SimpleSoundInstance music;
    private long started;
    private long retryAt;

    private BadTrip() { super("bad_trip", Category.VISUALS); }

    @Override public String description() { return "Squash and dance effect with shifting rainbows and looping music."; }

    static void registerHud() {
        HudElementRegistry.addLast(OrbitDLC.id("bad_trip"), (graphics, delta) -> INSTANCE.drawRainbow(graphics));
    }

    void tick(Minecraft client) {
        if (!isEnabled() || client.level == null) {
            stop(client);
            return;
        }
        long now = System.nanoTime();
        if (started == 0) started = now;
        var sounds = client.getSoundManager();
        if (now < retryAt || (music != null && sounds.isActive(music))) return;
        music = new SimpleSoundInstance(OrbitDLC.id("bad_trip.loop"), SoundSource.MUSIC,
                0.8f, 1f, RandomSource.create(), true, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
        sounds.play(music);
        retryAt = now + 1_000_000_000L;
    }

    @Override protected void onDisable() { stop(Minecraft.getInstance()); }

    void stop(Minecraft client) {
        if (music != null) client.getSoundManager().stop(music);
        music = null;
        started = 0;
        retryAt = 0;
    }

    public static void dance(LivingEntityRenderState state, PoseStack pose) {
        if (INSTANCE.started == 0 || state.deathTime > 0 || state.hasPose(Pose.SLEEPING)) return;
        double beat = INSTANCE.seconds() / BEAT_SECONDS * Math.TAU;
        double phase = beat + (state.x + state.z) * 0.15;
        float sway = (float) Math.sin(phase * 0.5);
        float pulse = (float) (0.5 + 0.5 * Math.cos(phase));
        pose.translate(sway * 0.12f, (1f - pulse) * 0.2f, 0f);
        pose.rotateDegrees(Axis.YP, sway * 32f);
        pose.rotateDegrees(Axis.ZP, sway * 16f);
        pose.scale(1.13f + pulse * 0.30f, 0.78f - pulse * 0.38f, 1.13f + pulse * 0.22f);
    }

    private void drawRainbow(GuiGraphicsExtractor graphics) {
        if (started == 0) return;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float hue = (float) ((seconds() * 0.15) % 1.0);
        for (int i = 0; i < 16; i++) {
            int top = height * i / 16;
            int bottom = height * (i + 1) / 16;
            if (bottom > top) graphics.fillGradient(0, top, width, bottom,
                    rainbow(hue + i / 16f), rainbow(hue + (i + 1) / 16f));
        }
    }

    private static int rainbow(float hue) {
        return 0x58000000 | (Color.HSBtoRGB(hue, 0.85f, 1f) & 0xFFFFFF);
    }

    private double seconds() { return (System.nanoTime() - started) / 1_000_000_000.0; }
}
