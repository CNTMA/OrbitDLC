package com.orbitdlc.client.mixin;

import brokke.cc.util.render.display.rounded.RoundedRectRenderState;
import com.orbitdlc.client.GlassBackdrop;
import com.orbitdlc.client.MenuStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSliderButton.class)
abstract class MenuSliderMixin {
    @Shadow protected double value;
    @Shadow protected abstract void handleCursor(GuiGraphicsExtractor graphics);

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
    private void orbitdlc$slider(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo callback) {
        if (!MenuStyle.active()) return;
        var slider = (AbstractSliderButton) (Object) this;
        int x = slider.getX(), y = slider.getY(), w = slider.getWidth(), h = slider.getHeight();
        GlassBackdrop.menuSurface(graphics, x, y, w, h, slider.active, slider.isHoveredOrFocused());
        int handle = x + 5 + (int) ((w - 10) * value);
        RoundedRectRenderState.submit(graphics, x + 5, y + h - 5, w - 10, 2, 1, 0x6AFFFFFF);
        RoundedRectRenderState.submit(graphics, handle - 2, y + h - 7, 5, 6, 2, 0xFFE0ECF4);
        graphics.centeredText(Minecraft.getInstance().font, MenuStyle.text(slider.getMessage()),
                x + w / 2, y + 3, slider.active ? 0xFFF7F7FA : 0xFFABB0BE);
        handleCursor(graphics);
        callback.cancel();
    }
}
