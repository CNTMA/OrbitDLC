/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.util.render.display.scissor;

import brokke.cc.util.render.display.batch.DisplayBatcher;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Scissor {
    private Scissor() {
    }

    public static void push(GuiGraphicsExtractor graphics, float x, float y, float width, float height) {
        if (width <= 0.0f || height <= 0.0f) {
            width = 0.0f;
            height = 0.0f;
        }
        DisplayBatcher.flush();
        int x0 = (int)Math.floor(x);
        int y0 = (int)Math.floor(y);
        int x1 = (int)Math.ceil(x + width);
        int y1 = (int)Math.ceil(y + height);
        if (x1 <= x0) {
            x1 = x0 + 1;
        }
        if (y1 <= y0) {
            y1 = y0 + 1;
        }
        graphics.enableScissor(x0, y0, x1, y1);
    }

    public static void pop(GuiGraphicsExtractor graphics) {
        DisplayBatcher.flush();
        graphics.disableScissor();
    }
}

