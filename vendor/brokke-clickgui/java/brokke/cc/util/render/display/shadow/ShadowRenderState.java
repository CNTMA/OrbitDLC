/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.renderpearl.api.pipeline.RenderPipeline
 *  net.minecraft.client.gui.navigation.ScreenRectangle
 *  net.minecraft.client.gui.render.TextureSetup
 *  net.minecraft.client.renderer.state.gui.GuiElementRenderState
 *  org.joml.Matrix3x2f
 *  org.joml.Matrix3x2fc
 *  org.jspecify.annotations.Nullable
 */
package brokke.cc.util.render.display.shadow;

import brokke.cc.util.render.display.shadow.ShadowPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public final class ShadowRenderState
implements GuiElementRenderState {
    private final Matrix3x2f pose;
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final float pad;
    private final int packedFirst;
    private final int packedSecond;
    private final int topLeft;
    private final int topRight;
    private final int bottomRight;
    private final int bottomLeft;
    private final @Nullable ScreenRectangle scissor;
    private final ScreenRectangle bounds;

    ShadowRenderState(Matrix3x2f pose, float x, float y, float width, float height, float blur, int packedFirst, int packedSecond, int topLeft, int topRight, int bottomRight, int bottomLeft, @Nullable ScreenRectangle scissor) {
        this.pose = pose;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.pad = blur + 1.0f;
        this.packedFirst = packedFirst;
        this.packedSecond = packedSecond;
        this.topLeft = topLeft;
        this.topRight = topRight;
        this.bottomRight = bottomRight;
        this.bottomLeft = bottomLeft;
        this.scissor = scissor;
        ScreenRectangle raw = new ScreenRectangle((int)Math.floor(x - this.pad), (int)Math.floor(y - this.pad), (int)Math.ceil(width + this.pad * 2.0f) + 1, (int)Math.ceil(height + this.pad * 2.0f) + 1).transformMaxBounds((Matrix3x2fc)pose);
        ScreenRectangle clipped = scissor != null ? raw.intersection(scissor) : raw;
        this.bounds = clipped != null ? clipped : ScreenRectangle.empty();
    }

    public void buildVertices(VertexConsumer consumer) {
        float hw = this.width * 0.5f + this.pad;
        float hh = this.height * 0.5f + this.pad;
        float x0 = this.x - this.pad;
        float y0 = this.y - this.pad;
        float x1 = this.x + this.width + this.pad;
        float y1 = this.y + this.height + this.pad;
        this.vertex(consumer, x0, y0, -hw, -hh, this.topLeft);
        this.vertex(consumer, x0, y1, -hw, hh, this.bottomLeft);
        this.vertex(consumer, x1, y1, hw, hh, this.bottomRight);
        this.vertex(consumer, x1, y0, hw, -hh, this.topRight);
    }

    private void vertex(VertexConsumer consumer, float px, float py, float u, float v, int color) {
        consumer.addVertexWith2DPose((Matrix3x2fc)this.pose, px, py).setUv(u, v).setUv2(this.packedFirst, this.packedSecond).setColor(color);
    }

    public RenderPipeline pipeline() {
        return ShadowPipeline.INSTANCE;
    }

    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    public @Nullable ScreenRectangle scissorArea() {
        return this.scissor;
    }

    public @Nullable ScreenRectangle bounds() {
        return this.bounds;
    }
}

