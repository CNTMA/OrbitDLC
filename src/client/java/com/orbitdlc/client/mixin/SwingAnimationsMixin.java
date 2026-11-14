package com.orbitdlc.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.orbitdlc.client.SwingAnimations;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
abstract class SwingAnimationsMixin {
    @WrapOperation(method = "submitArmWithItem", at = @At(value = "INVOKE", ordinal = 2,
            target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V"))
    private void orbitdlc$swing(FirstPersonHandsAndItemsRenderer renderer, float progress, PoseStack pose,
            int side, HumanoidArm arm, Operation<Void> original, @Local(argsOnly = true) InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !SwingAnimations.transform(pose, progress, side)) {
            original.call(renderer, progress, pose, side, arm);
        }
    }
}
