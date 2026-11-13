/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.renderpearl.api.pipeline.ColorTargetState
 *  com.mojang.renderpearl.api.pipeline.PrimitiveTopology
 *  com.mojang.renderpearl.api.pipeline.RenderPipeline
 *  com.mojang.renderpearl.api.pipeline.RenderPipeline$Builder
 *  com.mojang.renderpearl.api.pipeline.RenderPipeline$Snippet
 *  net.minecraft.client.renderer.RenderPipelines
 *  net.minecraft.resources.Identifier
 */
package brokke.cc.util.render.display.blur;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class BlurPipelines {
    public static final float PADDING = 1.0f;
    public static final RenderPipeline DOWNSAMPLE = BlurPipelines.fullscreen("downsample", "blur_down", false, false);
    public static final RenderPipeline UPSAMPLE = BlurPipelines.fullscreen("upsample", "blur_up", false, false);
    public static final RenderPipeline GAUSSIAN_HORIZONTAL = BlurPipelines.fullscreen("gaussian_h", "blur_separable", false, false);
    public static final RenderPipeline GAUSSIAN_VERTICAL = BlurPipelines.fullscreen("gaussian_v", "blur_separable", true, false);
    public static final RenderPipeline BOX_HORIZONTAL = BlurPipelines.fullscreen("box_h", "blur_separable", false, true);
    public static final RenderPipeline BOX_VERTICAL = BlurPipelines.fullscreen("box_v", "blur_separable", true, true);
    public static final RenderPipeline PANEL = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[]{RenderPipelines.GUI_TEXTURED_SNIPPET}).withLocation(BlurPipelines.id("pipeline/blur_panel")).withVertexShader(BlurPipelines.id("core/blur_panel")).withFragmentShader(BlurPipelines.id("core/blur_panel")).withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR).build());

    private BlurPipelines() {
    }

    public static void init() {
    }

    private static RenderPipeline fullscreen(String name, String fragment, boolean vertical, boolean box) {
        RenderPipeline.Builder builder = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(BlurPipelines.id("pipeline/blur_" + name)).withVertexShader(BlurPipelines.id("core/blur_quad")).withFragmentShader(BlurPipelines.id("core/" + fragment)).withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withColorTargetState(ColorTargetState.DEFAULT);
        RenderPipelines.TRACY_BLIT.getBindGroupLayouts().forEach(arg_0 -> ((RenderPipeline.Builder)builder).withBindGroupLayout(arg_0));
        if (vertical) {
            builder.withShaderDefine("BLUR_VERTICAL");
        }
        if (box) {
            builder.withShaderDefine("KERNEL_BOX");
        }
        return RenderPipelines.register((RenderPipeline)builder.build());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath((String)"brokke", (String)path);
    }
}

