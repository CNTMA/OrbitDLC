package com.orbitdlc.client;

import brokke.cc.util.render.display.rounded.RoundedRectRenderState;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.orbitdlc.OrbitDLC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;

public final class GlassBackdrop extends RoundedRectRenderState {
    private static final RenderPipeline PIPELINE = pipeline("liquid_glass");
    private static final RenderPipeline SATURATION = pipeline("world_saturation");
    private static final RenderPipeline WET_WORLD = pipeline("wet_world");

    private static RenderPipeline pipeline(String shader) {
        return RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(OrbitDLC.id("pipeline/" + shader))
                    .withVertexShader(OrbitDLC.id("core/liquid_glass"))
                    .withFragmentShader(OrbitDLC.id("core/" + shader))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR)
                    .build());
    }

    private static TextureTarget snapshot;
    private static boolean requested;
    private final TextureSetup texture;
    private final RenderPipeline effect;

    private GlassBackdrop(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int corners, RenderPipeline effect) {
        super(new Matrix3x2f(graphics.pose()), x, y, w, h, corners, corners, -1, -1, -1, -1, null);
        this.effect = effect;
        texture = TextureSetup.singleTexture(snapshot.getColorTextureView(),
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
    }

    static void init() {}

    static void submit(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        submit(graphics, x, y, w, h, 14);
    }

    static void submit(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int cornerRadius) {
        submit(graphics, x, y, w, h, cornerRadius, PIPELINE);
    }

    static void wetWorld(GuiGraphicsExtractor graphics) {
        submit(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight(), 0, WET_WORLD);
    }

    static void saturation(GuiGraphicsExtractor graphics) {
        submit(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight(), 0, SATURATION);
    }

    private static void submit(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int cornerRadius, RenderPipeline effect) {
        var main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        var source = main.getColorTexture();
        if (source == null || w <= 0 || h <= 0) return;
        if (snapshot == null) {
            snapshot = new TextureTarget("OrbitDLC glass background", main.width, main.height, source.getFormat(), null);
        } else if (snapshot.width != main.width || snapshot.height != main.height) {
            snapshot.resize(main.width, main.height);
        }
        int radius = Math.min(cornerRadius * 2, Math.min(w, h));
        requested = true;
        graphics.guiRenderState.addGuiElement(new GlassBackdrop(graphics, x, y, w, h, radius | radius << 8, effect));
    }

    public static void capture() {
        if (!requested) return;
        requested = false;
        var main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        snapshot.copyColorFrom(main);
    }

    public static void close() {
        requested = false;
        if (snapshot != null) {
            snapshot.destroyBuffers();
            snapshot = null;
        }
    }

    @Override
    public RenderPipeline pipeline() {
        return effect;
    }

    @Override
    public TextureSetup textureSetup() {
        return texture;
    }
}
