/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.renderpearl.api.pipeline.RenderPipeline
 *  net.minecraft.client.gui.navigation.ScreenRectangle
 *  net.minecraft.client.gui.render.TextureSetup
 *  net.minecraft.client.renderer.state.gui.GuiElementRenderState
 *  org.jspecify.annotations.Nullable
 */
package brokke.cc.util.render.display.batch;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.jspecify.annotations.Nullable;

public final class BatchRenderState
implements GuiElementRenderState {
    private static final int MAX_MEMBERS = 4096;
    private final List<GuiElementRenderState> members = new ArrayList<GuiElementRenderState>(32);
    private int[] rects = new int[128];
    private final RenderPipeline pipeline;
    private final TextureSetup textureSetup;
    private final @Nullable ScreenRectangle scissor;
    private @Nullable ScreenRectangle bounds;
    private int minX = Integer.MAX_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int maxY = Integer.MIN_VALUE;

    public BatchRenderState(RenderPipeline pipeline, TextureSetup textureSetup, @Nullable ScreenRectangle scissor) {
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline cannot be null");
        this.textureSetup = textureSetup;
        this.scissor = scissor;
    }

    boolean accepts(RenderPipeline pipeline, TextureSetup textureSetup, @Nullable ScreenRectangle scissor) {
        return this.members.size() < 4096 && this.pipeline == pipeline && Objects.equals(this.scissor, scissor) && Objects.equals(this.textureSetup, textureSetup);
    }

    boolean overlaps(int x0, int y0, int x1, int y1) {
        if (x0 >= this.maxX || x1 <= this.minX || y0 >= this.maxY || y1 <= this.minY) {
            return false;
        }
        int[] rects = this.rects;
        int end = this.members.size() << 2;
        for (int i = 0; i < end; i += 4) {
            if (x0 >= rects[i + 2] || x1 <= rects[i] || y0 >= rects[i + 3] || y1 <= rects[i + 1]) continue;
            return true;
        }
        return false;
    }

    void add(GuiElementRenderState element, int x0, int y0, int x1, int y1) {
        int offset = this.members.size() << 2;
        if (offset + 4 > this.rects.length) {
            this.rects = Arrays.copyOf(this.rects, this.rects.length << 1);
        }
        this.rects[offset] = x0;
        this.rects[offset + 1] = y0;
        this.rects[offset + 2] = x1;
        this.rects[offset + 3] = y1;
        this.members.add(element);
        this.minX = Math.min(this.minX, x0);
        this.minY = Math.min(this.minY, y0);
        this.maxX = Math.max(this.maxX, x1);
        this.maxY = Math.max(this.maxY, y1);
    }

    void seal() {
        this.bounds = new ScreenRectangle(this.minX, this.minY, Math.max(0, this.maxX - this.minX), Math.max(0, this.maxY - this.minY));
    }

    public void buildVertices(VertexConsumer consumer) {
        List<GuiElementRenderState> members = this.members;
        int size = members.size();
        for (int i = 0; i < size; ++i) {
            members.get(i).buildVertices(consumer);
        }
    }

    public RenderPipeline pipeline() {
        return this.pipeline;
    }

    public TextureSetup textureSetup() {
        return this.textureSetup;
    }

    public @Nullable ScreenRectangle scissorArea() {
        return this.scissor;
    }

    public @Nullable ScreenRectangle bounds() {
        return this.bounds;
    }
}

