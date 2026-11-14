package com.orbitdlc.client;

import brokke.cc.util.render.display.rounded.RoundedRectRenderState;
import com.orbitdlc.OrbitDLC;
import com.orbitdlc.client.module.ModuleManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import org.joml.Matrix4f;
import org.joml.Vector4f;


public final class ExtraVisuals {
    private ExtraVisuals() {}

    public static void buttonSurface(net.minecraft.client.gui.GuiGraphicsExtractor graphics,
                                     int x, int y, int w, int h, boolean active, boolean highlighted) {
        if (Minecraft.getInstance().level != null) GlassBackdrop.submit(graphics, x, y, w, h, 7);
        RoundedRectRenderState.submit(graphics, x, y, w, h, 7,
                !active ? 0xAA201820 : highlighted ? 0xCC73546C : 0xC0302531);
    }

    static void register(ModuleManager modules) {
        var hitboxes = new Hitboxes();
        modules.register(hitboxes);
        hitboxes.registerHud();
        var particles = WorldParticles.INSTANCE;
        modules.register(particles);
        ClientTickEvents.END_CLIENT_TICK.register(particles::tick);

        var target = new TargetEsp();
        modules.register(target);
        var marker = OrbitDLC.id("textures/effects/targetesp.png");
        var projection = new Matrix4f();
        var clip = new Vector4f();
        var markerState = new TargetMarkerState();
        HudElementRegistry.addLast(OrbitDLC.id("target_esp"), (graphics, delta) -> {
            var client = Minecraft.getInstance();
            if (!target.isEnabled() || client.gui.screen() != null || client.level == null) {
                markerState.entity = null;
                return;
            }
            long now = System.nanoTime();
            if (client.hitResult instanceof EntityHitResult hit
                    && hit.getEntity() instanceof LivingEntity living && living.isAlive()) {
                markerState.entity = living;
                markerState.lastSeen = now;
            }
            var entity = markerState.entity;
            if (entity == null || !entity.isAlive() || entity.isRemoved()
                    || entity.level() != client.level || now - markerState.lastSeen > 160_000_000L) return;
            var camera = client.gameRenderer.mainCamera();
            var offset = entity.getPosition(delta.getGameTimeDeltaPartialTick(false))
                    .add(0, entity.getBbHeight() * 0.5, 0).subtract(camera.position());
            camera.getViewRotationProjectionMatrix(projection)
                    .transform(clip.set((float) offset.x, (float) offset.y, (float) offset.z, 1f));
            if (clip.w <= 0.001f || Math.abs(clip.x) > clip.w || Math.abs(clip.y) > clip.w) return;
            float x = (0.5f + 0.5f * clip.x / clip.w) * graphics.guiWidth();
            float y = (0.5f - 0.5f * clip.y / clip.w) * graphics.guiHeight();
            float size = Math.clamp(145f / clip.w, 24f, 96f);
            var pose = graphics.pose();
            pose.pushMatrix();
            try {
                pose.translate(x, y);
                pose.rotate((float) ((System.nanoTime() % 3_000_000_000L) / 3e9 * Math.TAU));
                pose.scale(size / 64f);
                graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, marker,
                        -32, -32, 0f, 0f, 64, 64, 512, 512, 512, 512, target.tint());
            } finally {
                pose.popMatrix();
            }
        });
    }
    private static final class TargetMarkerState {
        LivingEntity entity;
        long lastSeen;
    }
}
