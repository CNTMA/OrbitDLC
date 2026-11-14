/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.renderpearl.api.pipeline.RenderPipeline
 *  net.minecraft.client.gui.navigation.ScreenRectangle
 *  net.minecraft.client.gui.render.TextureSetup
 *  net.minecraft.client.renderer.state.gui.GuiElementRenderState
 */
package brokke.cc.util.render.display;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

public record ClippedGuiElement(GuiElementRenderState source, ScreenRectangle scissorArea, ScreenRectangle bounds) implements GuiElementRenderState
{
    public void buildVertices(VertexConsumer vertices) {
        this.source.buildVertices(vertices);
    }

    public RenderPipeline pipeline() {
        return this.source.pipeline();
    }

    public TextureSetup textureSetup() {
        return this.source.textureSetup();
    }
}

