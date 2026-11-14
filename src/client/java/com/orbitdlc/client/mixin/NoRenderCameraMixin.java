package com.orbitdlc.client.mixin;

import com.orbitdlc.client.NoRender;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class NoRenderCameraMixin {
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void orbitdlc$bobHurt(CallbackInfo callback) {
        if (NoRender.hides(NoRender.Effect.SHAKE)) callback.cancel();
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void orbitdlc$bobView(CallbackInfo callback) {
        if (NoRender.hides(NoRender.Effect.BOBBING)) callback.cancel();
    }

}
