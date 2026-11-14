package com.orbitdlc.client;

import com.orbitdlc.OrbitDLC;
import com.orbitdlc.OrbitPresence;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Set;
import java.util.UUID;

final class OrbitBadges {
    private static Set<UUID> confirmed = Set.of();

    private OrbitBadges() {}

    static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OrbitPresence.TYPE,
                (payload, context) -> confirmed = Set.copyOf(payload.players()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> confirmed = Set.of());

        var projection = new Matrix4f();
        var clip = new Vector4f();
        var icon = OrbitDLC.id("black_hole.png");
        HudElementRegistry.addLast(OrbitDLC.id("player_badges"), (graphics, delta) -> {
            var client = Minecraft.getInstance();
            if (confirmed.isEmpty() || client.player == null || client.level == null) return;
            var camera = client.gameRenderer.mainCamera();
            camera.getViewRotationProjectionMatrix(projection);
            float tick = delta.getGameTimeDeltaPartialTick(false);
            for (var player : client.level.players()) {
                if (player == client.player || !confirmed.contains(player.getUUID())
                        || player.isInvisibleTo(client.player) || player.distanceToSqr(client.player) > 1024
                        || !client.player.hasLineOfSight(player)) continue;
                var offset = player.getPosition(tick).add(0, player.getBbHeight() + 0.55, 0)
                        .subtract(camera.position());
                projection.transform(clip.set((float) offset.x, (float) offset.y, (float) offset.z, 1));
                if (clip.w <= 0.001f || Math.abs(clip.x) > clip.w || Math.abs(clip.y) > clip.w) continue;
                int x = Math.round((0.5f + 0.5f * clip.x / clip.w) * graphics.guiWidth());
                int y = Math.round((0.5f - 0.5f * clip.y / clip.w) * graphics.guiHeight());
                graphics.blit(icon, x - 8, y - 8, x + 8, y + 8, 0f, 1f, 0f, 1f);
            }
        });
    }
}
