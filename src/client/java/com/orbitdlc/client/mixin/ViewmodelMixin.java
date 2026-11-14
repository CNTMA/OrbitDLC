package com.orbitdlc.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.orbitdlc.client.Viewmodel;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(net.minecraft.client.renderer.GameRenderer.class)
abstract class ViewmodelMixin {
    @Redirect(method = "renderItemInHand", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;)V"))
    private void orbitdlc$submitViewmodel(FirstPersonHandsAndItemsRenderer renderer, float partialTick, PoseStack pose,
            SubmitNodeCollector collector, PlayerRenderState player, FirstPersonHandsAndItemsRenderState hands) {
        pose.pushPose();
        try {
            Viewmodel.transform(pose);
            renderer.submitHandsWithItems(partialTick, pose, collector, player, hands);
        } finally {
            pose.popPose();
        }
    }
}
