package com.orbitdlc.client.mixin;

import com.orbitdlc.client.NoRender;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
abstract class NoRenderHudMixin {
    @Inject(method = "extractBossOverlay", at = @At("HEAD"), cancellable = true)
    private void orbitdlc$extractBossOverlay(CallbackInfo callback) {
        if (NoRender.hides(NoRender.Effect.BOSS_BAR)) callback.cancel();
    }

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void orbitdlc$extractScoreboardSidebar(CallbackInfo callback) {
        if (NoRender.hides(NoRender.Effect.SCOREBOARD)) callback.cancel();
    }

    @Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
    private void orbitdlc$extractVignette(CallbackInfo callback) {
        if (NoRender.hides(NoRender.Effect.VIGNETTE)) callback.cancel();
    }

    @Inject(method = "extractPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void orbitdlc$extractPortalOverlay(CallbackInfo callback) {
        if (NoRender.hides(NoRender.Effect.PORTAL)) callback.cancel();
    }

    @Inject(method = "extractTitle", at = @At("HEAD"), cancellable = true)
    private void orbitdlc$extractTitle(CallbackInfo callback) {
        if (NoRender.hides(NoRender.Effect.TITLES)) callback.cancel();
    }

}
