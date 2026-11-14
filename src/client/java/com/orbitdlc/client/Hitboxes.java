package com.orbitdlc.client;

import com.google.gson.JsonObject;
import com.orbitdlc.OrbitDLC;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Vector4f;

final class Hitboxes extends Module {
    private final Matrix4f projection = new Matrix4f();
    private final Vector4f clip = new Vector4f();
    private final float[] x = new float[8];
    private final float[] y = new float[8];
    private int bloom = 50;

    Hitboxes() { super("hitboxes", Category.VISUALS); }

    void registerHud() {
        HudElementRegistry.addLast(OrbitDLC.id("hitboxes"), (graphics, delta) -> {
            var client = Minecraft.getInstance();
            var player = client.player;
            if (!isEnabled() || player == null || client.level == null || client.gui.screen() != null) return;
            float tick = delta.getGameTimeDeltaPartialTick(false);
            var camera = client.gameRenderer.mainCamera();
            camera.getViewRotationProjectionMatrix(projection);
            var frustum = Optimization.frustum(camera);
            for (var target : client.level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(32),
                    entity -> entity != player && entity.isAlive() && !entity.isSpectator()
                            && !entity.isInvisibleTo(player) && entity.distanceToSqr(player) <= 1024)) {
                if (frustum != null && !frustum.isVisible(target.getBoundingBox().inflate(0.5))) continue;
                if (player.hasLineOfSight(target)) draw(graphics, camera.position(), target, tick);
            }
        });
    }

    private void draw(GuiGraphicsExtractor graphics, net.minecraft.world.phys.Vec3 cameraPosition, LivingEntity target, float tick) {
        var box = target.getBoundingBox().move(target.getPosition(tick).subtract(target.position())
                .subtract(cameraPosition));
        for (int i = 0; i < 8; i++) {
            projection.transform(clip.set((float) ((i & 1) == 0 ? box.minX : box.maxX),
                    (float) ((i & 2) == 0 ? box.minY : box.maxY),
                    (float) ((i & 4) == 0 ? box.minZ : box.maxZ), 1f));
            if (clip.w <= 0.05f) return;
            x[i] = (0.5f + 0.5f * clip.x / clip.w) * graphics.guiWidth();
            y[i] = (0.5f - 0.5f * clip.y / clip.w) * graphics.guiHeight();
        }

        int color = OrbitTheme.accent();
        if (bloom > 0) {
            for (int spread = 5; spread >= 1; spread--) {
                int alpha = Math.round(bloom * (6 - spread) * 0.065f);
                drawEdges(graphics, spread, (alpha << 24) | (color & 0xFFFFFF));
            }
        }
        drawEdges(graphics, 0, color);
    }

    private void drawEdges(GuiGraphicsExtractor graphics, int spread, int color) {
        for (int axis = 1; axis <= 4; axis *= 2) {
            for (int i = 0; i < 8; i++) {
                if ((i & axis) != 0) continue;
                int end = i | axis;
                float dx = x[end] - x[i], dy = y[end] - y[i];
                int length = Math.round((float) Math.hypot(dx, dy));
                if (length == 0) continue;
                var pose = graphics.pose();
                pose.pushMatrix();
                try {
                    pose.translate(x[i], y[i]);
                    pose.rotate((float) Math.atan2(dy, dx));
                    graphics.fill(-spread, -spread, length + spread, spread + 1, color);
                } finally {
                    pose.popMatrix();
                }
            }
        }
    }

    @Override
    public String description() { return "Draws nearby visible entities' hitboxes with an adjustable glow."; }

    @Override
    public JsonObject saveSettings() {
        var settings = super.saveSettings();
        settings.addProperty("bloom", bloom);
        return settings;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        bloom = settings.has("bloom") ? Math.clamp(settings.get("bloom").getAsInt(), 0, 100) : 50;
    }

    ModuleSettings settings(ClickGui parent) {
        return new ModuleSettings("Hitboxes", parent) {
            @Override
            protected void init() {
                slider("Bloom", "%", 0, 0, 100, () -> bloom, value -> bloom = value);
            }
        };
    }
}
