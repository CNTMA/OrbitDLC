package com.orbitdlc.client.mixin;

import com.orbitdlc.client.GlassBackdrop;
import com.orbitdlc.client.MenuStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
abstract class MenuScreenMixin {
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void orbitdlc$background(GuiGraphicsExtractor graphics, int x, int y, float delta, CallbackInfo callback) {
        var screen = (Screen) (Object) this;
        if (!MenuStyle.applies(screen)) return;
        GlassBackdrop.menuBackground(graphics, screen.width, screen.height);
        for (var child : screen.children()) {
            if (screen instanceof net.minecraft.client.gui.screens.worldselection.SelectWorldScreen
                    && child instanceof EditBox field) {
                GlassBackdrop.menuSurface(graphics, field.getX() - 4, field.getY() - 5,
                        field.getWidth() + 8, field.getHeight() + 5, true, field.isFocused());
            }
        }
        if (screen instanceof net.minecraft.client.gui.screens.options.OptionsScreen) {
            int cardWidth = Math.min(338, screen.width - 12);
            GlassBackdrop.menuSurface(graphics, (screen.width - cardWidth) / 2, 5, cardWidth,
                    Math.min(218, screen.height - 35), true, false);
        }
        callback.cancel();
    }

    @Inject(method = "init(II)V", at = @At("TAIL"))
    private void orbitdlc$styleControls(CallbackInfo callback) {
        var screen = (Screen) (Object) this;
        if (!MenuStyle.applies(screen)) return;
        for (var child : screen.children()) {
            if (child instanceof StringWidget title) title.setMessage(MenuStyle.text(title.getMessage()));
            if (screen instanceof net.minecraft.client.gui.screens.worldselection.SelectWorldScreen
                    && child instanceof EditBox field && field.isBordered()) {
                field.setBordered(false);
                field.setY(field.getY() + 5);
                field.setHeight(field.getHeight() - 5);
                field.setHint(MenuStyle.text(Component.translatable("selectWorld.search")));
                field.addFormatter((value, offset) -> MenuStyle.text(Component.literal(value)).getVisualOrderText());
            }
        }
    }
}
