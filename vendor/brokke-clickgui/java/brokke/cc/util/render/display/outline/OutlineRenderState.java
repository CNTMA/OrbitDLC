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
package brokke.cc.util.render.display.outline;

import brokke.cc.util.render.display.outline.OutlinePipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public final class OutlineRenderState
implements GuiElementRenderState {
    private final Matrix3x2f pose;
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final int packedTop;
    private final int packedBottom;
    private final int topLeft;
    private final int topRight;
    private final int bottomRight;
    private final int bottomLeft;
    private final @Nullable ScreenRectangle scissor;
    private final ScreenRectangle bounds;

    OutlineRenderState(Matrix3x2f pose, float x, float y, float width, float height, int packedTop, int packedBottom, int topLeft, int topRight, int bottomRight, int bottomLeft, @Nullable ScreenRectangle scissor) {
        this.pose = pose;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.packedTop = packedTop;
        this.packedBottom = packedBottom;
        this.topLeft = topLeft;
        this.topRight = topRight;
        this.bottomRight = bottomRight;
        this.bottomLeft = bottomLeft;
        this.scissor = scissor;
        float pad = 1.0f;
        ScreenRectangle raw = new ScreenRectangle((int)Math.floor(x - pad), (int)Math.floor(y - pad), (int)Math.ceil(width + pad * 2.0f) + 1, (int)Math.ceil(height + pad * 2.0f) + 1).transformAxisAligned((Matrix3x2fc)pose);
        ScreenRectangle clipped = scissor != null ? raw.intersection(scissor) : raw;
        this.bounds = clipped != null ? clipped : ScreenRectangle.empty();
    }

    public void buildVertices(VertexConsumer consumer) {
        float pad = 1.0f;
        float hw = this.width * 0.5f + pad;
        float hh = this.height * 0.5f + pad;
        float x0 = this.x - pad;
        float y0 = this.y - pad;
        float x1 = this.x + this.width + pad;
        float y1 = this.y + this.height + pad;
        this.vertex(consumer, x0, y0, -hw, -hh, this.topLeft);
        this.vertex(consumer, x0, y1, -hw, hh, this.bottomLeft);
        this.vertex(consumer, x1, y1, hw, hh, this.bottomRight);
        this.vertex(consumer, x1, y0, hw, -hh, this.topRight);
    }

    private void vertex(VertexConsumer consumer, float px, float py, float u, float v, int color) {
        consumer.addVertexWith2DPose((Matrix3x2fc)this.pose, px, py).setUv(u, v).setUv2(this.packedTop, this.packedBottom).setColor(color);
    }

    public RenderPipeline pipeline() {
        return OutlinePipeline.INSTANCE;
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

