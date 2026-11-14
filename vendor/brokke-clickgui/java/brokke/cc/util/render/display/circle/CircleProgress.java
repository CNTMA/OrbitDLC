/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.util.render.display.circle;

import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.batch.DisplayBatcher;
import brokke.cc.util.render.display.circle.CircleProgressRenderState;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class CircleProgress {
    private static final float THICKNESS_SCALE = 16.0f;
    private float x;
    private float y;
    private float size;
    private float thickness = 1.0f;
    private float progress = 1.0f;
    private int color = -1;
    private float alpha = 1.0f;

    private CircleProgress() {
    }

    public static CircleProgress of(float x, float y, float size) {
        CircleProgress cp = new CircleProgress();
        cp.x = x;
        cp.y = y;
        cp.size = size;
        return cp;
    }

    public static CircleProgress of(float x, float y, float width, float height) {
        return CircleProgress.of(x, y, Math.min(width, height));
    }

    public CircleProgress thickness(float thickness) {
        this.thickness = thickness;
        return this;
    }

    public CircleProgress progress(float progress) {
        this.progress = Math.clamp(progress, 0.0f, 1.0f);
        return this;
    }

    public CircleProgress color(int color) {
        this.color = color;
        return this;
    }

    public CircleProgress color(Color color) {
        return this.color(color.argb());
    }

    public CircleProgress alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (this.size <= 0.0f || this.alpha <= 0.0f || this.progress <= 0.001f) {
            return;
        }
        int finalColor = this.applyAlpha(this.color);
        if (finalColor >>> 24 == 0) {
            return;
        }
        int pScaled = Math.clamp((long)Math.round(this.progress * 1000.0f), 0, 1000);
        int tScaled = Math.clamp((long)Math.round(this.thickness * 16.0f), 0, 255);
        CircleProgressRenderState state = new CircleProgressRenderState(DisplayBatcher.pose(graphics), this.x, this.y, this.size, this.size, pScaled, tScaled, finalColor, graphics.scissorStack.peek());
        DisplayBatcher.submit(graphics, state);
    }

    private int applyAlpha(int col) {
        int a = Math.round((float)(col >>> 24) * this.alpha);
        return a << 24 | col & 0xFFFFFF;
    }
}

