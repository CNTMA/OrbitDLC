package com.orbitdlc.client.mixin;

import com.orbitdlc.client.NoRender;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
abstract class NoRenderOverlayMixin {
    @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
    private static void orbitdlc$submitFire(CallbackInfo callback) {
        if (NoRender.hides(NoRender.Effect.FIRE)) callback.cancel();
    }

    @Inject(method = "submitWater", at = @At("HEAD"), cancellable = true)
    private static void orbitdlc$submitWater(CallbackInfo callback) {
        if (NoRender.hides(NoRender.Effect.WATER)) callback.cancel();
    }

}
