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
 *  net.minecraft.client.renderer.texture.AbstractTexture
 *  net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
 *  org.joml.Matrix3x2f
 *  org.joml.Matrix3x2fc
 *  org.jspecify.annotations.Nullable
 */
package brokke.cc.util.render.display.text;

import brokke.cc.util.color.ColorMath;
import brokke.cc.util.render.display.text.ColorFunction;
import brokke.cc.util.render.display.text.TextLayout;
import brokke.cc.util.render.display.text.TextPipeline;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.render.display.text.font.MsdfFont;
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
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public final class TextRenderState
implements GuiElementRenderState {
    private static final float PADDING = 2.0f;
    private final TextLayout layout;
    private final MsdfFont font;
    private final Matrix3x2f pose;
    private final float x;
    private final float y;
    private final float size;
    private final int solidAlpha;
    private final @Nullable ColorFunction function;
    private final float alpha;
    private final int packedRange;
    private final @Nullable ScreenRectangle scissor;
    private final ScreenRectangle bounds;
    private @Nullable TextureSetup textureSetup;

    TextRenderState(TextLayout layout, Matrix3x2f pose, float x, float y, float size, int solid, @Nullable ColorFunction function, float alpha, @Nullable ScreenRectangle scissor) {
        this.layout = layout;
        this.font = layout.font();
        this.pose = pose;
        this.x = x;
        this.y = y;
        this.size = size;
        this.solidAlpha = ColorMath.multiplyAlpha(solid, alpha);
        this.function = function;
        this.alpha = alpha;
        this.packedRange = Math.clamp((long)Math.round(this.font.atlas().distanceRange() * 2.0f), 1, 255);
        this.scissor = scissor;
        this.bounds = TextRenderState.bounds(pose, x - 2.0f, y - 2.0f, layout.width() * size + 4.0f, layout.height() * size + 4.0f, scissor);
    }

    public void buildVertices(VertexConsumer consumer) {
        float[] quads = this.layout.quads();
        int[] indices = this.layout.indices();
        float invWidth = this.layout.width() > 0.0f ? 1.0f / this.layout.width() : 0.0f;
        float invHeight = this.layout.height() > 0.0f ? 1.0f / this.layout.height() : 0.0f;
        for (int g = 0; g < this.layout.glyphCount(); ++g) {
            int o = g * 8;
            float x0 = quads[o];
            float y0 = quads[o + 1];
            float x1 = quads[o + 2];
            float y1 = quads[o + 3];
            float u0 = quads[o + 4];
            float v0 = quads[o + 5];
            float u1 = quads[o + 6];
            float v1 = quads[o + 7];
            int index = indices[g];
            this.vertex(consumer, x0, y0, u0, v0, this.color(index, x0 * invWidth, y0 * invHeight));
            this.vertex(consumer, x0, y1, u0, v1, this.color(index, x0 * invWidth, y1 * invHeight));
            this.vertex(consumer, x1, y1, u1, v1, this.color(index, x1 * invWidth, y1 * invHeight));
            this.vertex(consumer, x1, y0, u1, v0, this.color(index, x1 * invWidth, y0 * invHeight));
        }
    }

    private int color(int index, float nx, float ny) {
        if (this.function == null) {
            return this.solidAlpha;
        }
        return ColorMath.multiplyAlpha(this.function.color(index, Math.clamp(nx, 0.0f, 1.0f), Math.clamp(ny, 0.0f, 1.0f)), this.alpha);
    }

    private void vertex(VertexConsumer consumer, float px, float py, float u, float v, int color) {
        consumer.addVertexWith2DPose((Matrix3x2fc)this.pose, this.x + px * this.size, this.y + py * this.size).setUv(u, v).setUv2(this.packedRange, this.font == Fonts.SEMIBOLD ? 56 : 0).setColor(color);
    }

    private static ScreenRectangle bounds(Matrix3x2f pose, float x, float y, float width, float height, @Nullable ScreenRectangle scissor) {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -3.4028235E38f;
        float maxY = -3.4028235E38f;
        for (int i = 0; i < 4; ++i) {
            float px = x + (float)(i & 1) * width;
            float py = y + (float)(i >> 1) * height;
            float tx = pose.m00() * px + pose.m10() * py + pose.m20();
            float ty = pose.m01() * px + pose.m11() * py + pose.m21();
            minX = Math.min(minX, tx);
            minY = Math.min(minY, ty);
            maxX = Math.max(maxX, tx);
            maxY = Math.max(maxY, ty);
        }
        ScreenRectangle raw = new ScreenRectangle((int)Math.floor(minX), (int)Math.floor(minY), (int)Math.ceil(maxX - minX) + 1, (int)Math.ceil(maxY - minY) + 1);
        ScreenRectangle clipped = scissor != null ? raw.intersection(scissor) : raw;
        return clipped != null ? clipped : ScreenRectangle.empty();
    }

    public RenderPipeline pipeline() {
        return TextPipeline.INSTANCE;
    }

    public TextureSetup textureSetup() {
        if (this.textureSetup == null) {
            GpuTextureView view = this.font.textureView();
            if (view == null) {
                AbstractTexture missingTex = Minecraft.getInstance().getTextureManager().getTexture(MissingTextureAtlasSprite.getLocation());
                view = missingTex != null ? missingTex.getTextureView() : null;
            }
            this.textureSetup = view != null ? TextureSetup.singleTexture((GpuTextureView)view, (GpuSampler)RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)) : TextureSetup.noTexture();
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

