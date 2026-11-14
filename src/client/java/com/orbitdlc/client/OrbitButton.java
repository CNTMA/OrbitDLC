package com.orbitdlc.client;

import brokke.cc.util.animation.Animation;
import brokke.cc.util.render.display.rounded.RoundedRectRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

class OrbitButton extends Button {
    static final Style TYPE = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(com.orbitdlc.OrbitDLC.id("onest_regular")));
    static final Style HEADING = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(com.orbitdlc.OrbitDLC.id("sf_medium")));

    private final java.util.function.BooleanSupplier selected;
    private final Animation hover = new Animation();
    private final Animation selection = new Animation();

    OrbitButton(Component text, int x, int y, int width, int height, OnPress action) {
        this(text, x, y, width, height, action, () -> false);
    }

    OrbitButton(Component text, int x, int y, int width, int height, OnPress action,
                java.util.function.BooleanSupplier selected) {
        super(x, y, width, height, text, action, DEFAULT_NARRATION);
        this.selected = selected;
        selection.set(selected.getAsBoolean() ? 1f : 0f);
    }

    @Override
    public void playDownSound(net.minecraft.client.sounds.SoundManager sounds) {}

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        hover.animate(active && isHoveredOrFocused() ? 1f : 0f, delta, 12f);
        selection.animate(selected.getAsBoolean() ? 1f : 0f, delta, 9f);
        int base = blend(0x40251C25, OrbitTheme.tint(0x88), selection.value());
        int surface = active ? blend(base, OrbitTheme.accent(), 0.35f * hover.value()) : 0x58282027;
        if (Minecraft.getInstance().gui.screen() instanceof ClickGui) {
            int radius = Math.min(7, getHeight() / 2);
            GlassBackdrop.clickGuiPanel(graphics, getX(), getY(), getWidth(), getHeight(), radius);
            RoundedRectRenderState.submit(graphics, getX(), getY(), getWidth(), getHeight(), radius,
                    blend(0x24181220, OrbitTheme.tint(0x70), Math.max(hover.value(), selection.value())));
        } else {
            background(graphics, surface);
        }
        graphics.centeredText(Minecraft.getInstance().font, Component.literal(getMessage().getString()).setStyle(TYPE),
                getX() + getWidth() / 2, getY() + (getHeight() - 9) / 2, active ? 0xFFF4EDFF : 0xFF9B95AB);
    }
    protected void background(GuiGraphicsExtractor graphics, int color) {
        GlassBackdrop.submit(graphics, getX(), getY(), getWidth(), getHeight(), 6);
        RoundedRectRenderState.submit(graphics, getX(), getY(), getWidth(), getHeight(), 6, color);
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
