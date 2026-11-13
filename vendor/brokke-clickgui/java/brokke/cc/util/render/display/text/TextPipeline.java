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
package brokke.cc.util.render.display.text;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class TextPipeline {
    public static final RenderPipeline INSTANCE = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[]{RenderPipelines.GUI_TEXTURED_SNIPPET}).withLocation(Identifier.fromNamespaceAndPath((String)"brokke", (String)"pipeline/text")).withVertexShader(Identifier.fromNamespaceAndPath((String)"brokke", (String)"core/text")).withFragmentShader(Identifier.fromNamespaceAndPath((String)"brokke", (String)"core/text")).withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR).build());

    private TextPipeline() {
    }

    public static void init() {
    }
}

