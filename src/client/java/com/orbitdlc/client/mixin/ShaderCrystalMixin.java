package com.orbitdlc.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.orbitdlc.client.ShaderCrystal;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EndCrystalRenderer.class)
abstract class ShaderCrystalMixin {
    @Redirect(method = "submit(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/Identifier;III)V"))
    private <S> void orbitdlc$submitCrystal(SubmitNodeCollector collector, Model<S> model, S state,
                                         PoseStack pose, Identifier texture, int light, int overlay, int outline) {
        if (!(state instanceof EndCrystalRenderState crystal) || !ShaderCrystal.replace(crystal)) {
            collector.submitModel(model, state, pose, texture, light, overlay, outline);
        }
    }
}
