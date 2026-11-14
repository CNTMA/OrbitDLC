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
package brokke.cc.util.render.display.shadow;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class ShadowPipeline {
    public static final float PADDING = 1.0f;
    public static final RenderPipeline INSTANCE = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[]{RenderPipelines.GUI_SNIPPET}).withLocation(Identifier.fromNamespaceAndPath((String)"brokke", (String)"pipeline/shadow")).withVertexShader(Identifier.fromNamespaceAndPath((String)"brokke", (String)"core/shadow")).withFragmentShader(Identifier.fromNamespaceAndPath((String)"brokke", (String)"core/shadow")).withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR).build());

    private ShadowPipeline() {
    }

    public static void init() {
    }
}

