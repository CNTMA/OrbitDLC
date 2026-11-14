/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.util.render.display.outline;

import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.batch.DisplayBatcher;
import brokke.cc.util.render.display.outline.OutlineRenderState;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Outline {
    private static final float MAX_RADIUS = 31.5f;
    private static final float THICKNESS_SCALE = 16.0f;
    private float x;
    private float y;
    private float width;
    private float height;
    private float thickness = 1.0f;
    private float radiusTopLeft;
    private float radiusTopRight;
    private float radiusBottomRight;
    private float radiusBottomLeft;
    private int colorTopLeft = -1;
    private int colorTopRight = -1;
    private int colorBottomRight = -1;
    private int colorBottomLeft = -1;
    private float alpha = 1.0f;

    private Outline() {
    }

    public static Outline create() {
        return new Outline();
    }

    public static Outline of(float x, float y, float width, float height) {
        return new Outline().bounds(x, y, width, height);
    }

    public Outline position(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Outline size(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public Outline bounds(float x, float y, float width, float height) {
        return this.position(x, y).size(width, height);
    }

    public Outline thickness(float thickness) {
        this.thickness = Math.clamp(thickness, 0.0f, 15.9375f);
        return this;
    }

    public Outline radius(float radius) {
        return this.radius(radius, radius, radius, radius);
    }

    public Outline radius(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        this.radiusTopLeft = topLeft;
        this.radiusTopRight = topRight;
        this.radiusBottomRight = bottomRight;
        this.radiusBottomLeft = bottomLeft;
        return this;
    }

    public Outline radiusTop(float radius) {
        this.radiusTopLeft = radius;
        this.radiusTopRight = radius;
        return this;
    }

    public Outline radiusBottom(float radius) {
        this.radiusBottomLeft = radius;
        this.radiusBottomRight = radius;
        return this;
    }

    public Outline radiusLeft(float radius) {
        this.radiusTopLeft = radius;
        this.radiusBottomLeft = radius;
        return this;
    }

    public Outline radiusRight(float radius) {
        this.radiusTopRight = radius;
        this.radiusBottomRight = radius;
        return this;
    }

    public Outline color(int argb) {
        return this.corners(argb, argb, argb, argb);
    }

    public Outline color(Color color) {
        return this.color(color.argb());
    }

    public Outline verticalGradient(int top, int bottom) {
        return this.corners(top, top, bottom, bottom);
    }

    public Outline verticalGradient(Color top, Color bottom) {
        return this.verticalGradient(top.argb(), bottom.argb());
    }

    public Outline horizontalGradient(int left, int right) {
        return this.corners(left, right, right, left);
    }

    public Outline horizontalGradient(Color left, Color right) {
        return this.horizontalGradient(left.argb(), right.argb());
    }

    public Outline corners(int topLeft, int topRight, int bottomRight, int bottomLeft) {
        this.colorTopLeft = topLeft;
        this.colorTopRight = topRight;
        this.colorBottomRight = bottomRight;
        this.colorBottomLeft = bottomLeft;
        return this;
    }

    public Outline corners(Color topLeft, Color topRight, Color bottomRight, Color bottomLeft) {
        return this.corners(topLeft.argb(), topRight.argb(), bottomRight.argb(), bottomLeft.argb());
    }

    public Outline alpha(float alpha) {
        this.alpha = Math.clamp(alpha, 0.0f, 1.0f);
        return this;
    }

    public OutlineRenderState build(GuiGraphicsExtractor graphics) {
        float maxRadius = Math.min(Math.min(this.width, this.height) * 0.5f, 31.5f);
        int topLeft = Outline.packRadius(this.radiusTopLeft, maxRadius);
        int topRight = Outline.packRadius(this.radiusTopRight, maxRadius);
        int bottomRight = Outline.packRadius(this.radiusBottomRight, maxRadius);
        int bottomLeft = Outline.packRadius(this.radiusBottomLeft, maxRadius);
        int packedThickness = Math.clamp((long)Math.round(this.thickness * 16.0f), 0, 255);
        return new OutlineRenderState(DisplayBatcher.pose(graphics), this.x, this.y, this.width, this.height, topLeft | topRight << 6 | (packedThickness & 0xF) << 12, bottomLeft | bottomRight << 6 | packedThickness >> 4 << 12, this.applyAlpha(this.colorTopLeft), this.applyAlpha(this.colorTopRight), this.applyAlpha(this.colorBottomRight), this.applyAlpha(this.colorBottomLeft), graphics.scissorStack.peek());
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (this.width <= 0.0f || this.height <= 0.0f || this.thickness <= 0.0f || this.alpha <= 0.0f) {
            return;
        }
        DisplayBatcher.submit(graphics, this.build(graphics));
    }

    private int applyAlpha(int color) {
        int a = Math.round((float)(color >>> 24) * this.alpha * DisplayBatcher.getAlphaScale());
        return a << 24 | color & 0xFFFFFF;
    }

    private static int packRadius(float radius, float max) {
        return Math.clamp((long)Math.round(Math.clamp(radius, 0.0f, max) * 2.0f), 0, 63);
    }
}

