package com.orbitdlc.client.mixin;

import com.orbitdlc.OrbitDLC;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
abstract class OrbitLoadingOverlayMixin {
    @Unique private static final Identifier orbitdlc$icon = OrbitDLC.id("black_hole.png");
    @Shadow @Final private net.minecraft.client.Minecraft minecraft;
    @Shadow @Final private ReloadInstance reload;
    @Shadow private float currentProgress;
    @Shadow private long fadeOutStart;
    @Shadow private long fadeInStart;

    @Inject(method = "registerTextures", at = @At("TAIL"))
    private static void orbitdlc$loadIcon(TextureManager textures, CallbackInfo callback) {
        textures.registerAndLoad(orbitdlc$icon, new SimpleTexture(orbitdlc$icon));
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void orbitdlc$draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo callback) {
        long now = Util.getMillis();
        if (fadeOutStart >= 0) {
            if (minecraft.gui.screen() != null) {
                minecraft.gui.screen().extractRenderStateWithTooltipAndSubtitles(graphics, mouseX, mouseY, delta);
            }
            graphics.nextStratum();
            if (now - fadeOutStart >= 700) {
                minecraft.gui.setOverlay(null);
                callback.cancel();
                return;
            }
        }
        if (fadeInStart < 0) fadeInStart = now;
        float progress = Math.clamp(reload.getActualProgress(), 0f, 1f);
        currentProgress = reload.isDone() ? 1f : Math.max(currentProgress, currentProgress * 0.90f + progress * 0.10f);
        float visibility = fadeOutStart < 0 ? 1f : 1f - Math.clamp((now - fadeOutStart) / 700f, 0f, 1f);
        if (visibility > 0f) {
            int width = graphics.guiWidth();
            int height = graphics.guiHeight();
            int center = width / 2;
            int middle = height / 2;
            int alpha = Math.round(visibility * 255) << 24;
            graphics.fillGradient(0, 0, width, height, alpha | 0x17132D, alpha | 0x08091B);
            int size = Math.clamp(height / 3, 48, 112);
            graphics.blit(RenderPipelines.GUI_TEXTURED, orbitdlc$icon, center - size / 2, middle - size / 2 - 12,
                    0f, 0f, size, size, 1280, 1280, 1280, 1280, alpha | 0xFFFFFF);
            int half = Math.min(78, width / 3);
            int y = middle + size / 2 + 10;
            graphics.fill(center - half, y, center + half, y + 3, alpha | 0x332D4C);
            graphics.fill(center - half, y, center - half + Math.round(2 * half * currentProgress), y + 3,
                    alpha | 0xD595FA);
        }
        callback.cancel();
    }
}
