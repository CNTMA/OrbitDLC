/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.renderpearl.api.pipeline.RenderPipeline
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.gui.navigation.ScreenRectangle
 *  net.minecraft.client.gui.render.TextureSetup
 *  net.minecraft.client.renderer.state.gui.GuiElementRenderState
 *  org.joml.Matrix3x2f
 *  org.joml.Matrix3x2fStack
 *  org.joml.Matrix3x2fc
 *  org.jspecify.annotations.Nullable
 */
package brokke.cc.util.render.display.batch;

import brokke.cc.util.render.display.batch.BatchRenderState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public final class DisplayBatcher {
    private static final int MAX_BATCH_LOOKBACK = 64;
    private static final List<BatchRenderState> PENDING = new ArrayList<BatchRenderState>();
    private static Matrix3x2f snapshot = new Matrix3x2f();
    private static @Nullable GuiGraphicsExtractor session;
    private static float alphaScale;

    private DisplayBatcher() {
    }

    public static void setAlphaScale(float scale) {
        alphaScale = Math.clamp(scale, 0.0f, 1.0f);
    }

    public static float getAlphaScale() {
        return alphaScale;
    }

    public static void begin(GuiGraphicsExtractor graphics) {
        DisplayBatcher.flush();
        session = graphics;
    }

    public static void end() {
        DisplayBatcher.flush();
        session = null;
    }

    public static boolean active() {
        return session != null;
    }

    public static void flush() {
        GuiGraphicsExtractor active = session;
        if (active == null || PENDING.isEmpty()) {
            return;
        }
        int size = PENDING.size();
        for (int i = 0; i < size; ++i) {
            BatchRenderState batch = PENDING.get(i);
            batch.seal();
            active.guiRenderState.addGuiElement((GuiElementRenderState)batch);
        }
        PENDING.clear();
    }

    public static void submit(GuiGraphicsExtractor graphics, GuiElementRenderState element) {
        GuiGraphicsExtractor active = session;
        if (active == null || active.guiRenderState != graphics.guiRenderState) {
            graphics.guiRenderState.addGuiElement(element);
            return;
        }
        ScreenRectangle bounds = element.bounds();
        if (bounds == null) {
            DisplayBatcher.flush();
            graphics.guiRenderState.addGuiElement(element);
            return;
        }
        int x0 = bounds.left();
        int y0 = bounds.top();
        int x1 = x0 + bounds.width();
        int y1 = y0 + bounds.height();
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        RenderPipeline pipeline = element.pipeline();
        if (pipeline == null) {
            graphics.guiRenderState.addGuiElement(element);
            return;
        }
        TextureSetup textureSetup = element.textureSetup();
        ScreenRectangle scissor = element.scissorArea();
        BatchRenderState target = null;
        int end = Math.max(0, PENDING.size() - 64);
        for (int i = PENDING.size() - 1; i >= end; --i) {
            BatchRenderState batch = PENDING.get(i);
            if (batch.accepts(pipeline, textureSetup, scissor)) {
                target = batch;
                break;
            }
            if (batch.overlaps(x0, y0, x1, y1)) break;
        }
        if (target == null) {
            target = new BatchRenderState(pipeline, textureSetup, scissor);
            PENDING.add(target);
        }
        target.add(element, x0, y0, x1, y1);
    }

    public static Matrix3x2f pose(GuiGraphicsExtractor graphics) {
        Matrix3x2fStack current = graphics.pose();
        if (!snapshot.equals((Matrix3x2fc)current, 0.0f)) {
            snapshot = new Matrix3x2f((Matrix3x2fc)current);
        }
        return snapshot;
    }

    static {
        alphaScale = 1.0f;
    }
}

