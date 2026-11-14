/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.util.render.display.shadow;

import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.batch.DisplayBatcher;
import brokke.cc.util.render.display.shadow.ShadowRenderState;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Shadow {
    private static final float MAX_RADIUS = 31.0f;
    private static final float MAX_STRENGTH = 4.0f;
    private static final float STRENGTH_STEP = 0.2f;
    private float x;
    private float y;
    private float width;
    private float height;
    private float radiusTopLeft;
    private float radiusTopRight;
    private float radiusBottomRight;
    private float radiusBottomLeft;
    private float blur = 8.0f;
    private float spread;
    private float offsetX;
    private float offsetY;
    private float strength = 1.0f;
    private int colorTopLeft = -16777216;
    private int colorTopRight = -16777216;
    private int colorBottomRight = -16777216;
    private int colorBottomLeft = -16777216;
    private float alpha = 1.0f;

    private Shadow() {
    }

    public static Shadow create() {
        return new Shadow();
    }

    public static Shadow of(float x, float y, float width, float height) {
        return new Shadow().bounds(x, y, width, height);
    }

    public Shadow position(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Shadow size(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public Shadow size(float size) {
        return this.size(size, size);
    }

    public Shadow bounds(float x, float y, float width, float height) {
        return this.position(x, y).size(width, height);
    }

    public Shadow radius(float radius) {
        return this.radius(radius, radius, radius, radius);
    }

    public Shadow radius(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        this.radiusTopLeft = topLeft;
        this.radiusTopRight = topRight;
        this.radiusBottomRight = bottomRight;
        this.radiusBottomLeft = bottomLeft;
        return this;
    }

    public Shadow radiusTop(float radius) {
        this.radiusTopLeft = radius;
        this.radiusTopRight = radius;
        return this;
    }

    public Shadow radiusBottom(float radius) {
        this.radiusBottomLeft = radius;
        this.radiusBottomRight = radius;
        return this;
    }

    public Shadow radiusLeft(float radius) {
        this.radiusTopLeft = radius;
        this.radiusBottomLeft = radius;
        return this;
    }

    public Shadow radiusRight(float radius) {
        this.radiusTopRight = radius;
        this.radiusBottomRight = radius;
        return this;
    }

    public Shadow blur(float blur) {
        this.blur = Math.clamp(blur, 0.0f, 63.5f);
        return this;
    }

    public Shadow spread(float spread) {
        this.spread = spread;
        return this;
    }

    public Shadow offset(float x, float y) {
        this.offsetX = x;
        this.offsetY = y;
        return this;
    }

    public Shadow strength(float strength) {
        this.strength = Math.clamp(strength, 0.0f, 4.0f);
        return this;
    }

    public Shadow color(int argb) {
        return this.corners(argb, argb, argb, argb);
    }

    public Shadow verticalGradient(int top, int bottom) {
        return this.corners(top, top, bottom, bottom);
    }

    public Shadow horizontalGradient(int left, int right) {
        return this.corners(left, right, right, left);
    }

    public Shadow corners(int topLeft, int topRight, int bottomRight, int bottomLeft) {
        this.colorTopLeft = topLeft;
        this.colorTopRight = topRight;
        this.colorBottomRight = bottomRight;
        this.colorBottomLeft = bottomLeft;
        return this;
    }

    public Shadow color(Color color) {
        return this.color(color.argb());
    }

    public Shadow verticalGradient(Color top, Color bottom) {
        return this.verticalGradient(top.argb(), bottom.argb());
    }

    public Shadow horizontalGradient(Color left, Color right) {
        return this.horizontalGradient(left.argb(), right.argb());
    }

    public Shadow corners(Color topLeft, Color topRight, Color bottomRight, Color bottomLeft) {
        return this.corners(topLeft.argb(), topRight.argb(), bottomRight.argb(), bottomLeft.argb());
    }

    public Shadow alpha(float alpha) {
        this.alpha = Math.clamp(alpha, 0.0f, 1.0f);
        return this;
    }

    public ShadowRenderState build(GuiGraphicsExtractor graphics) {
        int blurBits = Math.clamp((long)Math.round(this.blur * 2.0f), 0, 127);
        int strengthBits = Math.clamp((long)Math.round((Math.max(this.strength, 1.0f) - 1.0f) / 0.2f), 0, 15);
        float shapeWidth = this.width + this.spread * 2.0f;
        float shapeHeight = this.height + this.spread * 2.0f;
        float maxRadius = Math.min(Math.min(shapeWidth, shapeHeight) * 0.5f, 31.0f);
        int topLeft = Shadow.packRadius(this.radiusTopLeft + this.spread, maxRadius);
        int topRight = Shadow.packRadius(this.radiusTopRight + this.spread, maxRadius);
        int bottomRight = Shadow.packRadius(this.radiusBottomRight + this.spread, maxRadius);
        int bottomLeft = Shadow.packRadius(this.radiusBottomLeft + this.spread, maxRadius);
        float factor = this.alpha * Math.min(this.strength, 1.0f);
        return new ShadowRenderState(DisplayBatcher.pose(graphics), this.x + this.offsetX - this.spread, this.y + this.offsetY - this.spread, shapeWidth, shapeHeight, (float)blurBits * 0.5f, topLeft | topRight << 5 | bottomRight << 10, bottomLeft | blurBits << 5 | strengthBits << 12, Shadow.applyAlpha(this.colorTopLeft, factor), Shadow.applyAlpha(this.colorTopRight, factor), Shadow.applyAlpha(this.colorBottomRight, factor), Shadow.applyAlpha(this.colorBottomLeft, factor), graphics.scissorStack.peek());
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (this.width + this.spread * 2.0f <= 0.0f || this.height + this.spread * 2.0f <= 0.0f || this.alpha <= 0.0f || this.strength <= 0.0f) {
            return;
        }
        DisplayBatcher.submit(graphics, this.build(graphics));
    }

    private static int applyAlpha(int color, float factor) {
        int a = Math.round((float)(color >>> 24) * factor * DisplayBatcher.getAlphaScale());
        return a << 24 | color & 0xFFFFFF;
    }

    private static int packRadius(float radius, float max) {
        return Math.clamp((long)Math.round(Math.clamp(radius, 0.0f, max)), 0, 31);
    }
}

