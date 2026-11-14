package com.orbitdlc.client.mixin;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.orbitdlc.client.ShaderSky;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
abstract class ShaderSkyMixin {
    @Shadow @Final private RenderTarget renderTarget;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void orbitdlc$renderSky(GpuBufferSlice fog, SkyRenderState state, CallbackInfo callback) {
        if (ShaderSky.render(state, renderTarget)) callback.cancel();
    }
}

