/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.util.render.display.blur;

import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.batch.DisplayBatcher;
import brokke.cc.util.render.display.blur.BlurKey;
import brokke.cc.util.render.display.blur.BlurRenderState;
import brokke.cc.util.render.display.blur.BlurType;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Blur {
    private static final float MAX_RADIUS = 31.5f;
    private static final float OPACITY_STEPS = 31.0f;
    private float x;
    private float y;
    private float width;
    private float height;
    private float radiusTopLeft;
    private float radiusTopRight;
    private float radiusBottomRight;
    private float radiusBottomLeft;
    private BlurType type = BlurType.KAWASE;
    private int strength = 3;
    private int tintTopLeft;
    private int tintTopRight;
    private int tintBottomRight;
    private int tintBottomLeft;
    private float alpha = 1.0f;

    private Blur() {
    }

    public static Blur create() {
        return new Blur();
    }

    public static Blur of(float x, float y, float width, float height) {
        return new Blur().bounds(x, y, width, height);
    }

    public Blur position(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Blur size(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public Blur bounds(float x, float y, float width, float height) {
        return this.position(x, y).size(width, height);
    }

    public Blur radius(float radius) {
        return this.radius(radius, radius, radius, radius);
    }

    public Blur radius(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        this.radiusTopLeft = topLeft;
        this.radiusTopRight = topRight;
        this.radiusBottomRight = bottomRight;
        this.radiusBottomLeft = bottomLeft;
        return this;
    }

    public Blur radiusTop(float radius) {
        this.radiusTopLeft = radius;
        this.radiusTopRight = radius;
        return this;
    }

    public Blur radiusBottom(float radius) {
        this.radiusBottomLeft = radius;
        this.radiusBottomRight = radius;
        return this;
    }

    public Blur radiusLeft(float radius) {
        this.radiusTopLeft = radius;
        this.radiusBottomLeft = radius;
        return this;
    }

    public Blur radiusRight(float radius) {
        this.radiusTopRight = radius;
        this.radiusBottomRight = radius;
        return this;
    }

    public Blur type(BlurType type) {
        this.type = type;
        return this;
    }

    public Blur strength(int strength) {
        this.strength = Math.clamp((long)strength, 1, 6);
        return this;
    }

    public Blur tint(int argb) {
        return this.corners(argb, argb, argb, argb);
    }

    public Blur tint(Color color) {
        return this.tint(color.argb());
    }

    public Blur verticalGradient(Color top, Color bottom) {
        return this.corners(top.argb(), top.argb(), bottom.argb(), bottom.argb());
    }

    public Blur horizontalGradient(Color left, Color right) {
        return this.corners(left.argb(), right.argb(), right.argb(), left.argb());
    }

    public Blur corners(int topLeft, int topRight, int bottomRight, int bottomLeft) {
        this.tintTopLeft = topLeft;
        this.tintTopRight = topRight;
        this.tintBottomRight = bottomRight;
        this.tintBottomLeft = bottomLeft;
        return this;
    }

    public Blur alpha(float alpha) {
        this.alpha = Math.clamp(alpha, 0.0f, 1.0f);
        return this;
    }

    public BlurRenderState build(GuiGraphicsExtractor graphics) {
        float maxRadius = Math.min(Math.min(this.width, this.height) * 0.5f, 31.5f);
        int topLeft = Blur.packRadius(this.radiusTopLeft, maxRadius);
        int topRight = Blur.packRadius(this.radiusTopRight, maxRadius);
        int bottomRight = Blur.packRadius(this.radiusBottomRight, maxRadius);
        int bottomLeft = Blur.packRadius(this.radiusBottomLeft, maxRadius);
        int opacity = Math.round(this.alpha * 31.0f);
        BlurKey key = new BlurKey(this.type, this.strength);
        return new BlurRenderState(key, DisplayBatcher.pose(graphics), this.x, this.y, this.width, this.height, topLeft | topRight << 6 | this.type.outputLevel(this.strength) << 12 | (opacity & 1) << 15, bottomLeft | bottomRight << 6 | opacity >> 1 << 12, this.tintTopLeft, this.tintTopRight, this.tintBottomRight, this.tintBottomLeft, graphics.scissorStack.peek());
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (this.width <= 0.0f || this.height <= 0.0f || this.alpha <= 0.0f) {
            return;
        }
        DisplayBatcher.submit(graphics, this.build(graphics));
    }

    private static int packRadius(float radius, float max) {
        return Math.clamp((long)Math.round(Math.clamp(radius, 0.0f, max) * 2.0f), 0, 63);
    }
}

