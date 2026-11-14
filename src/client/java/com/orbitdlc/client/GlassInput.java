package com.orbitdlc.client;

import brokke.cc.util.animation.Animation;
import brokke.cc.util.render.display.rounded.RoundedRectRenderState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

final class GlassInput extends EditBox {
    private final Animation focus = new Animation();
    private final int left, top, width, height;

    GlassInput(Font font, int x, int y, int width, int height, String label) {
        super(font, x + 7, y + (height - 9) / 2, width - 14, (height + 9) / 2,
                Component.literal(label));
        this.left = x;
        this.top = y;
        this.width = width;
        this.height = height;
        setBordered(false);
        setHint(Component.literal(label).setStyle(OrbitButton.TYPE));
        addFormatter((value, start) -> Component.literal(value).setStyle(OrbitButton.TYPE).getVisualOrderText());
    }

    @Override
    public boolean isMouseOver(double x, double y) {
        return visible && x >= left && x < left + width && y >= top && y < top + height;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        focus.animate(isFocused() ? 1f : isMouseOver(mouseX, mouseY) ? 0.4f : 0f, delta, 7f);
        GlassBackdrop.clickGuiPanel(graphics, left, top, width, height, 7);
        RoundedRectRenderState.submit(graphics, left, top, width, height, 7,
                OrbitButton.blend(0x2B18141F, OrbitTheme.tint(0x66), focus.value()));
        if (focus.value() > 0.01f) {
            RoundedRectRenderState.submit(graphics, left + 7, top + height - 2, width - 14, 1, 1,
                    OrbitTheme.tint(Math.round(178 * focus.value())));
        }
        setTextColor(OrbitTheme.foreground());
        super.extractWidgetRenderState(graphics, mouseX, mouseY, delta);
    }
}
