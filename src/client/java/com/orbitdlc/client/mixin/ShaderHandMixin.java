package com.orbitdlc.client.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.orbitdlc.client.ShaderHand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class ShaderHandMixin {
    @Shadow @Final private RenderTarget mainRenderTarget;
    @Shadow @Final private RenderTarget hud3DTarget;

    @Inject(method = "render3dHud", at = @At("TAIL"))
    private void orbitdlc$renderHandFire(CameraRenderState camera, PlayerRenderState player,
            OptionsRenderState options, boolean separateDepth, CallbackInfo callback) {
        var client = Minecraft.getInstance();
        if (!client.options.getCameraType().isFirstPerson()) return;
        var depthTarget = separateDepth ? hud3DTarget : mainRenderTarget;
        ShaderHand.render(mainRenderTarget, depthTarget.getDepthTextureView(), camera.yRot, camera.xRot);
    }
}
