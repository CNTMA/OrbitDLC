package com.orbitdlc.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class LiquidGlass {
    private LiquidGlass() {}

    public static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        rounded(graphics, x - 4, y + 4, width + 8, height + 8, 18, 0x28000000);
        rounded(graphics, x, y, width, height, 14, 0x90D9CDEF);
        rounded(graphics, x + 1, y + 1, width - 2, height - 2, 13, 0xCC262332);
        rounded(graphics, x + 7, y + 2, width - 14, 2, 1, 0x70FFFFFF);
    }

    private static void rounded(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) return;
        brokke.cc.util.render.display.rounded.RoundedRectRenderState.submit(graphics, x, y, width, height, radius, color);
    }
}
