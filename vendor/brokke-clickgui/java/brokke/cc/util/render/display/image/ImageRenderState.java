/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.renderpearl.api.pipeline.RenderPipeline
 *  com.mojang.renderpearl.api.textures.FilterMode
 *  com.mojang.renderpearl.api.textures.GpuSampler
 *  com.mojang.renderpearl.api.textures.GpuTextureView
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.navigation.ScreenRectangle
 *  net.minecraft.client.gui.render.TextureSetup
 *  net.minecraft.client.renderer.state.gui.GuiElementRenderState
 *  net.minecraft.resources.Identifier
 *  org.joml.Matrix3x2f
 *  org.joml.Matrix3x2fc
 *  org.jspecify.annotations.Nullable
 */
package brokke.cc.util.render.display.image;

import brokke.cc.util.render.display.image.ImagePipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public final class ImageRenderState
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
    private final Identifier texture;
    private final boolean smooth;
    private final @Nullable ScreenRectangle scissor;
    private final ScreenRectangle bounds;
    private @Nullable TextureSetup textureSetup;

    ImageRenderState(Matrix3x2f pose, float x, float y, float width, float height, int packedTop, int packedBottom, int topLeft, int topRight, int bottomRight, int bottomLeft, Identifier texture, boolean smooth, @Nullable ScreenRectangle scissor) {
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
        this.texture = texture;
        this.smooth = smooth;
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
        return ImagePipeline.INSTANCE;
    }

    public TextureSetup textureSetup() {
        if (this.textureSetup == null) {
            this.textureSetup = TextureSetup.singleTexture((GpuTextureView)Minecraft.getInstance().getTextureManager().getTexture(this.texture).getTextureView(), (GpuSampler)RenderSystem.getSamplerCache().getClampToEdge(this.smooth ? FilterMode.LINEAR : FilterMode.NEAREST));
        }
        return this.textureSetup;
    }

    public @Nullable ScreenRectangle scissorArea() {
        return this.scissor;
    }

    public @Nullable ScreenRectangle bounds() {
        return this.bounds;
    }
}

