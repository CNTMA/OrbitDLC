/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.resources.Identifier
 */
package brokke.cc.util.render.display.image;

import brokke.cc.core.module.impl.render.GuiSettings;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.batch.DisplayBatcher;
import brokke.cc.util.render.display.image.ImageAsset;
import brokke.cc.util.render.display.image.ImageFit;
import brokke.cc.util.render.display.image.ImageRenderState;
import brokke.cc.util.render.display.image.ImageSize;
import brokke.cc.util.render.display.image.Images;
import brokke.cc.util.render.display.rounded.RoundedRect;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class Image {
    private static final float MAX_RADIUS = 63.5f;
    private Identifier texture;
    private float x;
    private float y;
    private float width;
    private float height;
    private float radiusTopLeft;
    private float radiusTopRight;
    private float radiusBottomRight;
    private float radiusBottomLeft;
    private boolean circle;
    private int colorTopLeft = -1;
    private int colorTopRight = -1;
    private int colorBottomRight = -1;
    private int colorBottomLeft = -1;
    private float alpha = 1.0f;
    private boolean flipX;
    private boolean flipY;
    private boolean smooth = true;
    private ImageFit fit = ImageFit.STRETCH;
    private int overlay;
    private float overlayAmount;

    private Image(Identifier texture) {
        this.texture = texture;
    }

    public static Image of(Identifier texture) {
        return new Image(texture);
    }

    public static Image of(ImageAsset asset) {
        return new Image(asset.texture());
    }

    public static Image of(Identifier texture, float x, float y, float width, float height) {
        return new Image(texture).bounds(x, y, width, height);
    }

    public static Image of(ImageAsset asset, float x, float y, float width, float height) {
        return new Image(asset.texture()).bounds(x, y, width, height);
    }

    public Image texture(Identifier texture) {
        this.texture = texture;
        return this;
    }

    public Image texture(ImageAsset asset) {
        return this.texture(asset.texture());
    }

    public Image position(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Image size(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public Image size(float size) {
        return this.size(size, size);
    }

    public Image bounds(float x, float y, float width, float height) {
        return this.position(x, y).size(width, height);
    }

    public Image fitWidth(float width) {
        return this.size(width, width / Images.size(this.texture).aspect());
    }

    public Image fitHeight(float height) {
        return this.size(height * Images.size(this.texture).aspect(), height);
    }

    public Image scale(float scale) {
        ImageSize size = Images.size(this.texture);
        return this.size((float)size.width() * scale, (float)size.height() * scale);
    }

    public Image fit(ImageFit fit) {
        this.fit = fit;
        return this;
    }

    public Image contain() {
        return this.fit(ImageFit.CONTAIN);
    }

    public Image circle() {
        this.circle = true;
        return this;
    }

    public Image radius(float radius) {
        return this.radius(radius, radius, radius, radius);
    }

    public Image radius(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        this.circle = false;
        this.radiusTopLeft = topLeft;
        this.radiusTopRight = topRight;
        this.radiusBottomRight = bottomRight;
        this.radiusBottomLeft = bottomLeft;
        return this;
    }

    public Image radiusTop(float radius) {
        this.circle = false;
        this.radiusTopLeft = radius;
        this.radiusTopRight = radius;
        return this;
    }

    public Image radiusBottom(float radius) {
        this.circle = false;
        this.radiusBottomLeft = radius;
        this.radiusBottomRight = radius;
        return this;
    }

    public Image radiusLeft(float radius) {
        this.circle = false;
        this.radiusTopLeft = radius;
        this.radiusBottomLeft = radius;
        return this;
    }

    public Image radiusRight(float radius) {
        this.circle = false;
        this.radiusTopRight = radius;
        this.radiusBottomRight = radius;
        return this;
    }

    public Image color(int argb) {
        return this.corners(argb, argb, argb, argb);
    }

    public Image verticalGradient(int top, int bottom) {
        return this.corners(top, top, bottom, bottom);
    }

    public Image horizontalGradient(int left, int right) {
        return this.corners(left, right, right, left);
    }

    public Image corners(int topLeft, int topRight, int bottomRight, int bottomLeft) {
        this.colorTopLeft = topLeft;
        this.colorTopRight = topRight;
        this.colorBottomRight = bottomRight;
        this.colorBottomLeft = bottomLeft;
        return this;
    }

    public Image color(Color color) {
        return this.color(color.argb());
    }

    public Image verticalGradient(Color top, Color bottom) {
        return this.verticalGradient(top.argb(), bottom.argb());
    }

    public Image horizontalGradient(Color left, Color right) {
        return this.horizontalGradient(left.argb(), right.argb());
    }

    public Image corners(Color topLeft, Color topRight, Color bottomRight, Color bottomLeft) {
        return this.corners(topLeft.argb(), topRight.argb(), bottomRight.argb(), bottomLeft.argb());
    }

    public Image mix(int argb, float amount) {
        this.overlay = argb;
        this.overlayAmount = Math.clamp(amount, 0.0f, 1.0f);
        return this;
    }

    public Image mix(Color color, float amount) {
        return this.mix(color.argb(), amount);
    }

    public Image alpha(float alpha) {
        this.alpha = Math.clamp(alpha, 0.0f, 1.0f);
        return this;
    }

    public Image flipX(boolean flipX) {
        this.flipX = flipX;
        return this;
    }

    public Image flipY(boolean flipY) {
        this.flipY = flipY;
        return this;
    }

    public Image smooth(boolean smooth) {
        this.smooth = smooth;
        return this;
    }

    public ImageSize size() {
        return Images.size(this.texture);
    }

    public float width() {
        return this.width;
    }

    public float height() {
        return this.height;
    }

    public ImageRenderState build(GuiGraphicsExtractor graphics) {
        float[] rect = this.rect();
        float maxRadius = Math.min(Math.min(rect[2], rect[3]) * 0.5f, 63.5f);
        int topLeft = Image.packRadius(this.circle ? maxRadius : this.radiusTopLeft, maxRadius);
        int topRight = Image.packRadius(this.circle ? maxRadius : this.radiusTopRight, maxRadius);
        int bottomRight = Image.packRadius(this.circle ? maxRadius : this.radiusBottomRight, maxRadius);
        int bottomLeft = Image.packRadius(this.circle ? maxRadius : this.radiusBottomLeft, maxRadius);
        int tl = this.colorTopLeft;
        int tr = this.colorTopRight;
        int br = this.colorBottomRight;
        int bl = this.colorBottomLeft;
        GuiSettings settings = GuiSettings.getInstance();
        if (settings != null && settings.isCustomTheme() && this.texture.getNamespace().equals("brokke") && this.texture.getPath().contains("icon")) {
            float phase = this.x * 0.002f + this.y * 0.001f;
            Color left = settings.getAnimatedThemeColor(phase);
            Color right = settings.getAnimatedThemeColor(phase + 0.12f);
            tl = left.withAlpha(tl >>> 24).argb();
            bl = left.withAlpha(bl >>> 24).argb();
            tr = right.withAlpha(tr >>> 24).argb();
            br = right.withAlpha(br >>> 24).argb();
        }
        return new ImageRenderState(DisplayBatcher.pose(graphics), rect[0], rect[1], rect[2], rect[3], topLeft | topRight << 7 | (this.flipX ? 1 : 0) << 14 | (this.flipY ? 1 : 0) << 15, bottomLeft | bottomRight << 7, this.applyAlpha(tl), this.applyAlpha(tr), this.applyAlpha(br), this.applyAlpha(bl), this.texture, this.smooth, graphics.scissorStack.peek());
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (this.width <= 0.0f || this.height <= 0.0f || this.alpha <= 0.0f) {
            return;
        }
        DisplayBatcher.submit(graphics, this.build(graphics));
        if (this.overlayAmount > 0.0f && this.overlay >>> 24 > 0) {
            float[] rect = this.rect();
            float maxRadius = Math.min(Math.min(rect[2], rect[3]) * 0.5f, 63.5f);
            RoundedRect.of(rect[0], rect[1], rect[2], rect[3]).radius(this.circle ? maxRadius : this.radiusTopLeft, this.circle ? maxRadius : this.radiusTopRight, this.circle ? maxRadius : this.radiusBottomRight, this.circle ? maxRadius : this.radiusBottomLeft).color(this.overlay).alpha(this.alpha * this.overlayAmount).render(graphics);
        }
    }

    private float[] rect() {
        if (this.fit == ImageFit.STRETCH) {
            return new float[]{this.x, this.y, this.width, this.height};
        }
        ImageSize size = Images.size(this.texture);
        float scale = Math.min(this.width / (float)size.width(), this.height / (float)size.height());
        float w = (float)size.width() * scale;
        float h = (float)size.height() * scale;
        return new float[]{this.x + (this.width - w) * 0.5f, this.y + (this.height - h) * 0.5f, w, h};
    }

    private int applyAlpha(int color) {
        int a = Math.round((float)(color >>> 24) * this.alpha * DisplayBatcher.getAlphaScale());
        return a << 24 | color & 0xFFFFFF;
    }

    private static int packRadius(float radius, float max) {
        return Math.clamp((long)Math.round(Math.clamp(radius, 0.0f, max) * 2.0f), 0, 127);
    }
}

