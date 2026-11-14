package com.orbitdlc.client;

import brokke.cc.util.render.display.rounded.RoundedRectRenderState;
import com.orbitdlc.OrbitDLC;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import com.orbitdlc.client.module.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Locale;

public final class UtilityHud {
    private static Module nametags;
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private UtilityHud() {}

    static void register(ModuleManager modules) {
        var helper = MaceHelper.INSTANCE;
        nametags = register(modules, "nametags", Category.VISUALS, "Shows visible players' names, armor and distance.");
        var durability = register(modules, "held_durability", Category.UTILITIES, "Shows remaining durability of the held item.");
        var projection = new Matrix4f();
        var clip = new Vector4f();
        HudElementRegistry.addLast(OrbitDLC.id("utility_hud"), (graphics, delta) -> {
            var client = Minecraft.getInstance();
            var player = client.player;
            if (player == null || client.level == null) return;

            var held = player.getMainHandItem();
            if (helper.isEnabled() && held.is(Items.MACE) && !player.onGround()) {
                boolean ready = MaceItem.canSmashAttack(player);
                String fall = String.format(Locale.ROOT, "%.1f m", player.fallDistance);
                badge(graphics, client, (ready ? "Smash ready" : "Falling") + " · " + fall,
                        graphics.guiWidth() / 2, graphics.guiHeight() / 2 + 18,
                        ready ? 0xFFBCF6CD : 0xFFE8E3DE);
            }
            if (durability.isEnabled() && held.isDamageableItem()) {
                badge(graphics, client, (held.getMaxDamage() - held.getDamageValue()) + " / " + held.getMaxDamage(),
                        graphics.guiWidth() / 2, graphics.guiHeight() - 72, 0xFFF4F5FA);
            }
            if (!nametagsEnabled()) return;
            var camera = client.gameRenderer.mainCamera();
            camera.getViewRotationProjectionMatrix(projection);
            float tick = delta.getGameTimeDeltaPartialTick(false);
            for (var other : client.level.players()) {
                double distance = player.distanceToSqr(other);
                if (other == player || !other.isAlive() || other.isSpectator() || other.isInvisibleTo(player)
                        || other.isCrouching() || distance > 1024 || !player.hasLineOfSight(other)) continue;
                var offset = other.getPosition(tick).add(0, other.getBbHeight() + 0.78, 0).subtract(camera.position());
                projection.transform(clip.set((float) offset.x, (float) offset.y, (float) offset.z, 1f));
                if (clip.w <= 0.001f || Math.abs(clip.x) >= clip.w || Math.abs(clip.y) >= clip.w) continue;
                int x = Math.round((0.5f + 0.5f * clip.x / clip.w) * graphics.guiWidth());
                int y = Math.round((0.5f - 0.5f * clip.y / clip.w) * graphics.guiHeight());
                var name = other.getDisplayName().copy()
                        .append(Component.literal(" · " + Math.round(Math.sqrt(distance)) + " m"));
                badge(graphics, client, OrbitBadges.decorate(other.getUUID(), name), x, y, 0xFFF4F5FA);
                int armorCount = 0;
                for (var slot : ARMOR) if (!other.getItemBySlot(slot).isEmpty()) armorCount++;
                int itemX = x - armorCount * 9;
                for (var slot : ARMOR) {
                    var stack = other.getItemBySlot(slot);
                    if (stack.isEmpty()) continue;
                    graphics.item(stack, itemX, y - 18);
                    graphics.itemDecorations(client.font, stack, itemX, y - 18);
                    itemX += 18;
                }
            }
        });
    }

    public static boolean nametagsEnabled() {
        return nametags != null && nametags.isEnabled();
    }

    private static Module register(ModuleManager modules, String id, Category category, String description) {
        var module = new Module(id, category) {
            @Override
            public String description() { return description; }
        };
        modules.register(module);
        return module;
    }

    private static void badge(GuiGraphicsExtractor graphics, Minecraft client, String label, int x, int y, int color) {
        badge(graphics, client, Component.literal(label).setStyle(OrbitButton.TYPE), x, y, color);
    }

    private static void badge(GuiGraphicsExtractor graphics, Minecraft client, Component text, int x, int y, int color) {
        int width = client.font.width(text) + 12;
        RoundedRectRenderState.submit(graphics, x - width / 2, y, width, 15, 5, 0xCC19171D);
        graphics.centeredText(client.font, text, x, y + 3, color);
    }
}
