/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.multiplayer.PlayerInfo
 *  net.minecraft.client.player.AbstractClientPlayer
 *  net.minecraft.resources.Identifier
 *  net.minecraft.world.entity.player.PlayerSkin
 */
package brokke.cc.util.render.display.head;

import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.batch.DisplayBatcher;
import brokke.cc.util.render.display.head.HeadSkins;
import brokke.cc.util.render.display.head.PlayerHeadRenderState;
import brokke.cc.util.render.display.rounded.RoundedRect;
import com.mojang.authlib.GameProfile;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

public final class PlayerHead {
    private static final float MAX_RADIUS = 63.5f;
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
    private boolean hat = true;
    private boolean smooth;
    private int overlay;
    private float overlayAmount;
    private Supplier<Identifier> source = () -> HeadSkins.lookup(Minecraft.getInstance().getGameProfile()).get().body().texturePath();

    private PlayerHead() {
    }

    public static PlayerHead create() {
        return new PlayerHead();
    }

    public static PlayerHead of(float x, float y, float size) {
        return new PlayerHead().position(x, y).size(size);
    }

    public static PlayerHead of(float x, float y, float width, float height) {
        return new PlayerHead().bounds(x, y, width, height);
    }

    public PlayerHead position(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public PlayerHead size(float size) {
        return this.size(size, size);
    }

    public PlayerHead size(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public PlayerHead bounds(float x, float y, float width, float height) {
        return this.position(x, y).size(width, height);
    }

    public PlayerHead skin(PlayerSkin skin) {
        this.source = () -> skin.body().texturePath();
        return this;
    }

    public PlayerHead skin(Supplier<PlayerSkin> skin) {
        this.source = () -> ((PlayerSkin)skin.get()).body().texturePath();
        return this;
    }

    public PlayerHead profile(GameProfile profile) {
        return this.skin(HeadSkins.lookup(profile));
    }

    public PlayerHead player(AbstractClientPlayer player) {
        return this.skin(() -> ((AbstractClientPlayer)player).getSkin());
    }

    public PlayerHead info(PlayerInfo info) {
        return this.skin(() -> ((PlayerInfo)info).getSkin());
    }

    public PlayerHead self() {
        return this.profile(Minecraft.getInstance().getGameProfile());
    }

    public PlayerHead texture(Identifier texture) {
        this.source = () -> texture;
        return this;
    }

    public PlayerHead hat(boolean hat) {
        this.hat = hat;
        return this;
    }

    public PlayerHead smooth(boolean smooth) {
        this.smooth = smooth;
        return this;
    }

    public PlayerHead circle() {
        this.circle = true;
        return this;
    }

    public PlayerHead radius(float radius) {
        return this.radius(radius, radius, radius, radius);
    }

    public PlayerHead radius(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        this.circle = false;
        this.radiusTopLeft = topLeft;
        this.radiusTopRight = topRight;
        this.radiusBottomRight = bottomRight;
        this.radiusBottomLeft = bottomLeft;
        return this;
    }

    public PlayerHead radiusTop(float radius) {
        this.circle = false;
        this.radiusTopLeft = radius;
        this.radiusTopRight = radius;
        return this;
    }

    public PlayerHead radiusBottom(float radius) {
        this.circle = false;
        this.radiusBottomLeft = radius;
        this.radiusBottomRight = radius;
        return this;
    }

    public PlayerHead radiusLeft(float radius) {
        this.circle = false;
        this.radiusTopLeft = radius;
        this.radiusBottomLeft = radius;
        return this;
    }

    public PlayerHead radiusRight(float radius) {
        this.circle = false;
        this.radiusTopRight = radius;
        this.radiusBottomRight = radius;
        return this;
    }

    public PlayerHead color(int argb) {
        return this.corners(argb, argb, argb, argb);
    }

    public PlayerHead verticalGradient(int top, int bottom) {
        return this.corners(top, top, bottom, bottom);
    }

    public PlayerHead horizontalGradient(int left, int right) {
        return this.corners(left, right, right, left);
    }

    public PlayerHead corners(int topLeft, int topRight, int bottomRight, int bottomLeft) {
        this.colorTopLeft = topLeft;
        this.colorTopRight = topRight;
        this.colorBottomRight = bottomRight;
        this.colorBottomLeft = bottomLeft;
        return this;
    }

    public PlayerHead color(Color color) {
        return this.color(color.argb());
    }

    public PlayerHead verticalGradient(Color top, Color bottom) {
        return this.verticalGradient(top.argb(), bottom.argb());
    }

    public PlayerHead horizontalGradient(Color left, Color right) {
        return this.horizontalGradient(left.argb(), right.argb());
    }

    public PlayerHead corners(Color topLeft, Color topRight, Color bottomRight, Color bottomLeft) {
        return this.corners(topLeft.argb(), topRight.argb(), bottomRight.argb(), bottomLeft.argb());
    }

    public PlayerHead mix(int argb, float amount) {
        this.overlay = argb;
        this.overlayAmount = Math.clamp(amount, 0.0f, 1.0f);
        return this;
    }

    public PlayerHead mix(Color color, float amount) {
        return this.mix(color.argb(), amount);
    }

    public PlayerHead alpha(float alpha) {
        this.alpha = Math.clamp(alpha, 0.0f, 1.0f);
        return this;
    }

    public PlayerHeadRenderState build(GuiGraphicsExtractor graphics) {
        float maxRadius = Math.min(Math.min(this.width, this.height) * 0.5f, 63.5f);
        int topLeft = PlayerHead.packRadius(this.circle ? maxRadius : this.radiusTopLeft, maxRadius);
        int topRight = PlayerHead.packRadius(this.circle ? maxRadius : this.radiusTopRight, maxRadius);
        int bottomRight = PlayerHead.packRadius(this.circle ? maxRadius : this.radiusBottomRight, maxRadius);
        int bottomLeft = PlayerHead.packRadius(this.circle ? maxRadius : this.radiusBottomLeft, maxRadius);
        return new PlayerHeadRenderState(DisplayBatcher.pose(graphics), this.x, this.y, this.width, this.height, topLeft | topRight << 7 | (this.hat ? 1 : 0) << 14, bottomLeft | bottomRight << 7, this.applyAlpha(this.colorTopLeft), this.applyAlpha(this.colorTopRight), this.applyAlpha(this.colorBottomRight), this.applyAlpha(this.colorBottomLeft), this.source.get(), this.smooth, graphics.scissorStack.peek());
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (this.width <= 0.0f || this.height <= 0.0f || this.alpha <= 0.0f) {
            return;
        }
        DisplayBatcher.submit(graphics, this.build(graphics));
        if (this.overlayAmount > 0.0f && this.overlay >>> 24 > 0) {
            float maxRadius = Math.min(Math.min(this.width, this.height) * 0.5f, 63.5f);
            RoundedRect.of(this.x, this.y, this.width, this.height).radius(this.circle ? maxRadius : this.radiusTopLeft, this.circle ? maxRadius : this.radiusTopRight, this.circle ? maxRadius : this.radiusBottomRight, this.circle ? maxRadius : this.radiusBottomLeft).color(this.overlay).alpha(this.alpha * this.overlayAmount).render(graphics);
        }
    }

    private int applyAlpha(int color) {
        int a = Math.round((float)(color >>> 24) * this.alpha * DisplayBatcher.getAlphaScale());
        return a << 24 | color & 0xFFFFFF;
    }

    private static int packRadius(float radius, float max) {
        return Math.clamp((long)Math.round(Math.clamp(radius, 0.0f, max) * 2.0f), 0, 127);
    }
}

