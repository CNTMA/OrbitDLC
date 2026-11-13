package com.orbitdlc.client;

import brokke.cc.util.animation.Animation;
import brokke.cc.util.render.display.rounded.RoundedRectRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

final class OrbitButton extends Button {
    static final Style TYPE = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(com.orbitdlc.OrbitDLC.id("onest_regular")));

    private final Animation hover = new Animation();

    OrbitButton(Component text, int x, int y, int width, int height, OnPress action) {
        super(x, y, width, height, text, action, DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        hover.animate(active && isHoveredOrFocused() ? 1f : 0f, delta, 12f);
        int surface = !active ? 0x9020222D : blend(0xD5262839, 0xEC404266, hover.value());
        RoundedRectRenderState.submit(graphics, getX(), getY(), getWidth(), getHeight(), 6, surface);
        graphics.centeredText(Minecraft.getInstance().font, Component.literal(getMessage().getString()).setStyle(TYPE),
                getX() + getWidth() / 2, getY() + (getHeight() - 9) / 2, active ? 0xFFF4EDFF : 0xFF9B95AB);
    }
    static int blend(int start, int end, float amount) {
        float progress = Math.clamp(amount, 0f, 1f);
        int result = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int from = (start >>> shift) & 255;
            int to = (end >>> shift) & 255;
            result |= Math.round(from + (to - from) * progress) << shift;
        }
        return result;
    }
}
