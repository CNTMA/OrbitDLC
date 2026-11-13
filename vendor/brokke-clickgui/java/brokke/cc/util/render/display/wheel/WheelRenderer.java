/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.buffers.Std140Builder
 *  com.mojang.blaze3d.buffers.Std140SizeCalculator
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.renderpearl.api.buffers.GpuBuffer
 *  com.mojang.renderpearl.api.commands.CommandEncoder
 *  com.mojang.renderpearl.api.commands.RenderPass
 *  com.mojang.renderpearl.api.pipeline.RenderPipeline
 *  net.minecraft.client.Minecraft
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.system.MemoryStack
 */
package brokke.cc.util.render.display.wheel;

import brokke.cc.util.render.display.wheel.WheelPipelines;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.nio.ByteBuffer;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;

public final class WheelRenderer {
    private static final int DATA_UBO_SIZE = new Std140SizeCalculator().putMat4f().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().get();
    private static GpuBuffer uniformBuffer;

    private WheelRenderer() {
    }

    private static void ensureBuffer() {
        if (uniformBuffer == null || uniformBuffer.isClosed()) {
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "wheel_pipeline_uniforms", 136, (long)DATA_UBO_SIZE);
        }
    }

    public static boolean draw(Matrix4f matrix, float cx, float cy, float innerR, float outerR, float gap, float cornerR, int slotCount, int[] slotColors, float baseAlpha, float z) {
        try {
            RenderTarget target;
            WheelRenderer.ensureBuffer();
            if (uniformBuffer == null) {
                return false;
            }
            float pad = Math.max(cornerR, 3.0f) + 2.0f;
            float halfSize = outerR + pad;
            float size = halfSize * 2.0f;
            float x = cx - halfSize;
            float y = cy - halfSize;
            try (MemoryStack stack = MemoryStack.stackPush();){
                Std140Builder builder = Std140Builder.onStack((MemoryStack)stack, (int)DATA_UBO_SIZE).putMat4f((Matrix4fc)matrix).putVec4(x, y, size, size).putVec4(innerR, outerR, gap, cornerR).putVec4((float)slotCount, baseAlpha, z, 0.0f);
                for (int i = 0; i < 8; ++i) {
                    int argb = i < slotCount && i < slotColors.length ? slotColors[i] : 0;
                    float r = (float)(argb >> 16 & 0xFF) / 255.0f;
                    float g = (float)(argb >> 8 & 0xFF) / 255.0f;
                    float b = (float)(argb & 0xFF) / 255.0f;
                    float a = (float)(argb >> 24 & 0xFF) / 255.0f;
                    builder.putVec4(r, g, b, a);
                }
                ByteBuffer buf = builder.get();
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(uniformBuffer.slice(), buf);
            }
            Minecraft mc = Minecraft.getInstance();
            RenderTarget renderTarget = target = mc.gameRenderer != null ? mc.gameRenderer.mainRenderTarget() : null;
            if (target == null || target.getColorTextureView() == null) {
                return false;
            }
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            try (RenderPass pass = encoder.createRenderPass(() -> "WheelPipeline", target.getColorTextureView(), Optional.empty());){
                pass.setPipeline(RenderSystem.getCompiledPipeline((RenderPipeline)WheelPipelines.WHEEL));
                pass.setUniform("Uniforms", uniformBuffer);
                pass.draw(6, 1, 0, 0);
            }
            return true;
        }
        catch (Throwable t) {
            return false;
        }
    }
}

