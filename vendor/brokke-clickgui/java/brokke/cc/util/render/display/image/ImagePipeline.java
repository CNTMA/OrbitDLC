/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.renderpearl.api.pipeline.RenderPipeline
 *  com.mojang.renderpearl.api.pipeline.RenderPipeline$Snippet
 *  net.minecraft.client.renderer.RenderPipelines
 *  net.minecraft.resources.Identifier
 */
package brokke.cc.util.render.display.image;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class ImagePipeline {
    public static final float PADDING = 1.0f;
    public static final RenderPipeline INSTANCE = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[]{RenderPipelines.GUI_TEXTURED_SNIPPET}).withLocation(Identifier.fromNamespaceAndPath((String)"brokke", (String)"pipeline/image")).withVertexShader(Identifier.fromNamespaceAndPath((String)"brokke", (String)"core/image")).withFragmentShader(Identifier.fromNamespaceAndPath((String)"brokke", (String)"core/image")).withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR).build());

    private ImagePipeline() {
    }

    public static void init() {
    }
}

