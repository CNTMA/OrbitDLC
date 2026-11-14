package com.orbitdlc.client.mixin;

import com.orbitdlc.client.ExtraVisuals;
import com.orbitdlc.client.GlassBackdrop;
import com.orbitdlc.client.MenuStyle;
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
        if (MenuStyle.active()) {
            GlassBackdrop.menuSurface(graphics, button.getX(), button.getY(), button.getWidth(),
                    button.getHeight(), button.active, button.isHoveredOrFocused());
        } else ExtraVisuals.buttonSurface(graphics, button.getX(), button.getY(), button.getWidth(),
                button.getHeight(), button.active, button.isHoveredOrFocused());
        callback.cancel();
    }

    @org.spongepowered.asm.mixin.injection.ModifyArg(method = "extractDefaultLabel", index = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;extractScrollingStringOverContents(Lnet/minecraft/client/gui/ActiveTextCollector;Lnet/minecraft/network/chat/Component;I)V"))
    private net.minecraft.network.chat.Component orbitdlc$menuLabel(net.minecraft.network.chat.Component label) {
        return MenuStyle.active() ? MenuStyle.text(label) : label;
    }
}
