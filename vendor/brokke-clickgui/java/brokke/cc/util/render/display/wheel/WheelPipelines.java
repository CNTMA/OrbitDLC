/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.renderpearl.api.pipeline.BindGroupLayout
 *  com.mojang.renderpearl.api.pipeline.BlendFunction
 *  com.mojang.renderpearl.api.pipeline.ColorTargetState
 *  com.mojang.renderpearl.api.pipeline.PrimitiveTopology
 *  com.mojang.renderpearl.api.pipeline.RenderPipeline
 *  com.mojang.renderpearl.api.pipeline.RenderPipeline$Snippet
 *  com.mojang.renderpearl.api.pipeline.UniformType
 *  net.minecraft.client.renderer.RenderPipelines
 *  net.minecraft.resources.Identifier
 */
package brokke.cc.util.render.display.wheel;

import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class WheelPipelines {
    private static final BindGroupLayout UNIFORMS_LAYOUT = BindGroupLayout.builder().withUniform("Uniforms", UniformType.UNIFORM_BUFFER).build();
    public static final RenderPipeline WHEEL = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(Identifier.fromNamespaceAndPath((String)"brokke", (String)"pipeline/wheel")).withVertexShader(Identifier.fromNamespaceAndPath((String)"brokke", (String)"core/wheel")).withFragmentShader(Identifier.fromNamespaceAndPath((String)"brokke", (String)"core/wheel")).withBindGroupLayout(UNIFORMS_LAYOUT).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).withPrimitiveTopology(PrimitiveTopology.TRIANGLES).build());

    private WheelPipelines() {
    }

    public static void init() {
    }
}

