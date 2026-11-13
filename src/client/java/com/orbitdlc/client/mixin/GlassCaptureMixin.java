package com.orbitdlc.client.mixin;

import com.orbitdlc.client.GlassBackdrop;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GlassCaptureMixin {
    @Inject(method = "draw", at = @At("HEAD"))
    private void orbitdlc$captureBackground(CallbackInfo callback) {
        GlassBackdrop.capture();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void orbitdlc$releaseBackground(CallbackInfo callback) {
        GlassBackdrop.close();
    }
}
