package com.orbitdlc.client.mixin;

import com.orbitdlc.client.UtilityHud;
import com.orbitdlc.client.OrbitBadges;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
abstract class NametagsMixin {
    @Inject(method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V",
            at = @At("TAIL"))
    private void orbitdlc$hideVanillaPlayerTag(Entity entity, EntityRenderState state, float partialTick,
            double nameDistance, double scoreDistance, CallbackInfo callback) {
        if (!(entity instanceof Player player)) return;
        if (UtilityHud.nametagsEnabled()) {
            state.nameTag = null;
            state.scoreText = null;
            state.nameTagAttachment = null;
        } else if (state.nameTag != null) {
            state.nameTag = OrbitBadges.decorate(player.getUUID(), state.nameTag);
        }
    }
}
