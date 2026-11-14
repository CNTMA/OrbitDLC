package com.orbitdlc.client.mixin;

import com.orbitdlc.client.GlassBackdrop;
import com.orbitdlc.client.MenuStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSelectionList.class)
abstract class MenuWidgetsMixin {
    @Inject(method = {"extractListBackground", "extractListSeparators"}, at = @At("HEAD"), cancellable = true)
    private void orbitdlc$clearListBackground(GuiGraphicsExtractor graphics, CallbackInfo callback) {
        if (MenuStyle.active()) callback.cancel();
    }

    @Inject(method = "extractItem", at = @At("HEAD"))
    private void orbitdlc$row(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta,
            @Coerce Object row, CallbackInfo callback) {
        Object list = this;
        if (list instanceof WorldSelectionList || list instanceof ServerSelectionList) {
            var entry = (LayoutElement) row;
            boolean hover = mouseX >= entry.getX() && mouseX < entry.getX() + entry.getWidth()
                    && mouseY >= entry.getY() && mouseY < entry.getY() + entry.getHeight();
            drawRow(graphics, (AbstractSelectionList<?>) list, entry, hover);
        }
    }

    @Inject(method = "extractSelection", at = @At("HEAD"), cancellable = true)
    private void orbitdlc$selection(GuiGraphicsExtractor graphics, @Coerce Object row,
            int color, CallbackInfo callback) {
        Object list = this;
        if (list instanceof WorldSelectionList || list instanceof ServerSelectionList) {
            drawRow(graphics, (AbstractSelectionList<?>) list, (LayoutElement) row, true);
            callback.cancel();
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private static void drawRow(GuiGraphicsExtractor graphics, AbstractSelectionList<?> list, LayoutElement entry, boolean hover) {
        int top = Math.max(entry.getY(), list.getY());
        int bottom = Math.min(entry.getY() + entry.getHeight() - 2, list.getY() + list.getHeight());
        if (bottom > top) GlassBackdrop.menuSurface(graphics, entry.getX(), top, entry.getWidth(), bottom - top, true, hover);
    }
}
