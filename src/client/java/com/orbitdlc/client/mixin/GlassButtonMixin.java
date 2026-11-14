package com.orbitdlc.client.mixin;

import com.orbitdlc.client.ExtraVisuals;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
abstract class GlassButtonMixin {
    @Inject(method = "extractDefaultSprite", at = @At("HEAD"), cancellable = true)
    private void orbitdlc$glass(GuiGraphicsExtractor graphics, CallbackInfo callback) {
        var button = (AbstractWidget) (Object) this;
        ExtraVisuals.buttonSurface(graphics, button.getX(), button.getY(), button.getWidth(),
                button.getHeight(), button.active, button.isHoveredOrFocused());
        callback.cancel();
    }
}
