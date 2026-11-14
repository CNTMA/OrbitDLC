package com.orbitdlc.client.mixin;

import com.orbitdlc.client.WetReflections;
import com.orbitdlc.client.Beautiful;
import com.orbitdlc.client.BlockOverlay;
import com.orbitdlc.client.Bloom;
import com.orbitdlc.client.MotionBlur;
import com.orbitdlc.client.Trail;
import com.orbitdlc.client.ShaderCrystal;
import com.orbitdlc.client.CustomFog;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class WetWorldMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void orbitdlc$beginCrystals(CallbackInfo callback) {
        ShaderCrystal.beginFrame();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/renderer/LevelRenderer;render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;Lorg/joml/Vector4f;ZZ)V"))
    private void orbitdlc$shadeWetSurfaces(CallbackInfo callback) {
        var renderer = (GameRenderer) (Object) this;
        WetReflections.apply(renderer.mainRenderTarget(), renderer.mainCamera());
        BlockOverlay.render(renderer.mainRenderTarget(), renderer.mainCamera());
        ShaderCrystal.render(renderer.mainRenderTarget(), renderer.mainCamera());
        Beautiful.render(renderer.mainRenderTarget(), renderer.mainCamera());
        CustomFog.render(renderer.mainRenderTarget(), renderer.mainCamera());
        Trail.render(renderer.mainRenderTarget(), renderer.mainCamera());
        Bloom.render(renderer.mainRenderTarget());
        MotionBlur.render(renderer.mainRenderTarget(), renderer.mainCamera());
    }
}
