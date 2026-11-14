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
import net.minecraft.world.entity.Pose;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.awt.Color;
import java.io.BufferedInputStream;

public final class BadTrip extends Module {
    static final BadTrip INSTANCE = new BadTrip();
    private static final double BEAT_SECONDS = 0.52;
    private Clip music;
    private long started;
    private long playbackRequest;

    private BadTrip() { super("bad_trip", Category.VISUALS); }

    @Override public String description() { return "Squash and dance effect with shifting rainbows and looping music."; }

    static void registerHud() {
        HudElementRegistry.addLast(OrbitDLC.id("bad_trip"), (graphics, delta) -> INSTANCE.drawRainbow(graphics));
    }

    void tick(Minecraft client) {
        if (!isEnabled() || client.level == null) {
            stop();
            return;
        }
        if (started != 0) return;
        started = System.nanoTime();
        long request = nextPlaybackRequest();
        Thread.ofVirtual().start(() -> openMusic(request));
    }

    private synchronized long nextPlaybackRequest() { return ++playbackRequest; }

    private void openMusic(long request) {
        Clip clip = null;
        try (var resource = BadTrip.class.getResourceAsStream("/assets/orbitdlc/sounds/bad_trip/loop.wav")) {
            if (resource == null) throw new java.io.FileNotFoundException("BadTrip loop.wav");
            try (var audio = AudioSystem.getAudioInputStream(new BufferedInputStream(resource))) {
                clip = AudioSystem.getClip();
                clip.open(audio);
            }
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                var gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gain.setValue(Math.clamp(-6f, gain.getMinimum(), gain.getMaximum()));
            }
            startMusic(request, clip);
            clip = null;
        } catch (Exception error) {
            OrbitDLC.LOGGER.warn("BadTrip audio could not play", error);
        } finally {
            if (clip != null) clip.close();
        }
    }

    private synchronized void startMusic(long request, Clip clip) {
        if (request != playbackRequest) {
            clip.close();
            return;
        }
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        music = clip;
    }

    @Override protected void onDisable() { stop(); }

    synchronized void stop() {
        if (started == 0) return;
        playbackRequest++;
        if (music != null) music.close();
        music = null;
        started = 0;
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
