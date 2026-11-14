package com.orbitdlc.client.mixin;

import com.orbitdlc.client.Animations;
import com.orbitdlc.client.ClickGui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
abstract class ScreenAnimationsMixin {
    @Unique private long orbitdlc$openedAt;

    @Inject(method = "init(II)V", at = @At("HEAD"))
    private void orbitdlc$opening(CallbackInfo callback) { orbitdlc$openedAt = System.nanoTime(); }

    @Redirect(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void orbitdlc$animate(Screen screen, GuiGraphicsExtractor graphics, int x, int y, float delta) {
        var part = screen instanceof AbstractContainerScreen<?> ? Animations.Part.INVENTORY : Animations.Part.SCREENS;
        float offset = screen instanceof ClickGui || screen instanceof ChatScreen || !Animations.plays(part)
                ? 0 : 9 * Animations.remaining(orbitdlc$openedAt, 290);
        if (offset == 0) {
            screen.extractRenderState(graphics, x, y, delta);
            return;
        }
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(0, offset);
            screen.extractRenderState(graphics, x, Math.round(y - offset), delta);
        } finally {
            graphics.pose().popMatrix();
        }
    }
}
