/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.util.render.display.rounded;

import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.batch.DisplayBatcher;
import brokke.cc.util.render.display.rounded.RoundedRectRenderState;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class RoundedRect {
    private float x;
    private float y;
    private float width;
    private float height;
    private float radiusTopLeft;
    private float radiusTopRight;
    private float radiusBottomRight;
    private float radiusBottomLeft;
    private int colorTopLeft = -1;
    private int colorTopRight = -1;
    private int colorBottomRight = -1;
    private int colorBottomLeft = -1;
    private float alpha = 1.0f;

    private RoundedRect() {
    }

    public static RoundedRect create() {
        return new RoundedRect();
    }

    public static RoundedRect of(float x, float y, float width, float height) {
        return new RoundedRect().bounds(x, y, width, height);
    }

    public RoundedRect position(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public RoundedRect size(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public RoundedRect bounds(float x, float y, float width, float height) {
        return this.position(x, y).size(width, height);
    }

    public RoundedRect radius(float radius) {
        return this.radius(radius, radius, radius, radius);
    }

    public RoundedRect radius(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        this.radiusTopLeft = topLeft;
        this.radiusTopRight = topRight;
        this.radiusBottomRight = bottomRight;
        this.radiusBottomLeft = bottomLeft;
        return this;
    }

    public RoundedRect radiusTop(float radius) {
        this.radiusTopLeft = radius;
        this.radiusTopRight = radius;
        return this;
    }

    public RoundedRect radiusBottom(float radius) {
        this.radiusBottomLeft = radius;
        this.radiusBottomRight = radius;
        return this;
    }

    public RoundedRect radiusLeft(float radius) {
        this.radiusTopLeft = radius;
        this.radiusBottomLeft = radius;
        return this;
    }

    public RoundedRect radiusRight(float radius) {
        this.radiusTopRight = radius;
        this.radiusBottomRight = radius;
        return this;
    }

    public RoundedRect color(int argb) {
        return this.corners(argb, argb, argb, argb);
    }

    public RoundedRect verticalGradient(int top, int bottom) {
        return this.corners(top, top, bottom, bottom);
    }

    public RoundedRect horizontalGradient(int left, int right) {
        return this.corners(left, right, right, left);
    }

    public RoundedRect corners(int topLeft, int topRight, int bottomRight, int bottomLeft) {
        this.colorTopLeft = topLeft;
        this.colorTopRight = topRight;
        this.colorBottomRight = bottomRight;
        this.colorBottomLeft = bottomLeft;
        return this;
    }

    public RoundedRect color(Color color) {
        return this.color(color.argb());
    }

    public RoundedRect verticalGradient(Color top, Color bottom) {
        return this.verticalGradient(top.argb(), bottom.argb());
    }

    public RoundedRect horizontalGradient(Color left, Color right) {
        return this.horizontalGradient(left.argb(), right.argb());
    }

    public RoundedRect corners(Color topLeft, Color topRight, Color bottomRight, Color bottomLeft) {
        return this.corners(topLeft.argb(), topRight.argb(), bottomRight.argb(), bottomLeft.argb());
    }

    public RoundedRect alpha(float alpha) {
        this.alpha = Math.clamp(alpha, 0.0f, 1.0f);
        return this;
    }

    public RoundedRectRenderState build(GuiGraphicsExtractor graphics) {
        float maxRadius = Math.min(this.width, this.height) * 0.5f;
        int topLeft = RoundedRect.packRadius(this.radiusTopLeft, maxRadius);
        int topRight = RoundedRect.packRadius(this.radiusTopRight, maxRadius);
        int bottomRight = RoundedRect.packRadius(this.radiusBottomRight, maxRadius);
        int bottomLeft = RoundedRect.packRadius(this.radiusBottomLeft, maxRadius);
        return new RoundedRectRenderState(DisplayBatcher.pose(graphics), this.x, this.y, this.width, this.height, topLeft | topRight << 8, bottomLeft | bottomRight << 8, this.applyAlpha(this.colorTopLeft), this.applyAlpha(this.colorTopRight), this.applyAlpha(this.colorBottomRight), this.applyAlpha(this.colorBottomLeft), graphics.scissorStack.peek());
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (this.width <= 0.0f || this.height <= 0.0f || this.alpha <= 0.0f) {
            return;
        }
        DisplayBatcher.submit(graphics, this.build(graphics));
    }

    private int applyAlpha(int color) {
        int a = Math.round((float)(color >>> 24) * this.alpha * DisplayBatcher.getAlphaScale());
        return a << 24 | color & 0xFFFFFF;
    }

    private static int packRadius(float radius, float max) {
        return Math.clamp((long)Math.round(Math.clamp(radius, 0.0f, max) * 2.0f), 0, 255);
    }
}

