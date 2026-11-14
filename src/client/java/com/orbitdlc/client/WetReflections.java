package com.orbitdlc.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.orbitdlc.OrbitDLC;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.util.Optional;

public final class WetReflections {
    private static final int BYTES = 176 + WorldParticles.REFLECTION_LIMIT * 16;
    private static final RenderPipeline PIPELINE = RenderPipelines.register(RenderPipeline.builder()
            .withLocation(OrbitDLC.id("pipeline/wet_reflections"))
            .withVertexShader(OrbitDLC.id("core/wet_world"))
            .withFragmentShader(OrbitDLC.id("core/wet_world"))
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withCull(false)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withUniform("Sampler0", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("Sampler1", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("WetWorld", UniformType.UNIFORM_BUFFER).build())
            .build());
    private static final Matrix4f projection = new Matrix4f();
    private static final Matrix4f inverse = new Matrix4f();
    private static TextureTarget output;
    private static GpuBuffer uniforms;
    private static boolean failed;

    private WetReflections() {}

    static void init() {}

    public static void apply(RenderTarget main, Camera camera) {
        if (Optimization.level() >= 2 || !Ambience.wetWorldEnabled() || failed || main.getDepthTextureView() == null
                || main.getColorTextureView() == null) return;
        try (var stack = MemoryStack.stackPush()) {
            var device = RenderSystem.getDevice();
            var color = main.getColorTexture();
            int width = color.getWidth(0);
            int height = color.getHeight(0);
            if (output == null) {
                output = new TextureTarget("OrbitDLC reflections", width, height,
                        color.getFormat(), null);
            } else if (output.width != width || output.height != height) {
                output.resize(width, height);
            }
            if (uniforms == null) {
                uniforms = device.createBuffer(() -> "OrbitDLC reflection uniforms",
                        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, BYTES);
            }
            camera.getViewRotationProjectionMatrix(projection);
            inverse.set(projection).invert();
            var position = camera.position();
            var data = stack.calloc(BYTES);
            projection.get(0, data);
            inverse.get(64, data);
            data.position(128);
            data.putFloat((float) position.x).putFloat((float) position.y).putFloat((float) position.z).putFloat(0);
            data.putFloat((System.nanoTime() % 600_000_000_000L) / 1_000_000_000f);
            data.putFloat(device.getDeviceInfo().isZZeroToOne() ? 1f : 0f).putFloat(0).putFloat(0);
            var particles = WorldParticles.INSTANCE;
            int tint = particles.tint();
            data.putFloat((tint >> 16 & 255) / 255f).putFloat((tint >> 8 & 255) / 255f)
                    .putFloat((tint & 255) / 255f).putFloat(0f);
            int lightCount = particles.writeReflectionLights(data, camera);
            data.putFloat(172, lightCount);
            data.position(0);
            var encoder = device.createCommandEncoder();
            encoder.writeToBuffer(uniforms.slice(), data);
            var samplers = RenderSystem.getSamplerCache();
            try (var pass = encoder.createRenderPass(() -> "OrbitDLC reflections",
                    output.getColorTextureView(), Optional.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
                pass.setUniform("Sampler0", main.getColorTextureView(), samplers.getClampToEdge(FilterMode.LINEAR));
                pass.setUniform("Sampler1", main.getDepthTextureView(), samplers.getClampToEdge(FilterMode.NEAREST));
                pass.setUniform("WetWorld", uniforms);
                pass.draw(3, 1, 0, 0);
            }
            encoder.copyTextureToTexture(output.getColorTexture(), color, 0, 0, 0, 0, 0, width, height);
        } catch (RuntimeException error) {
            failed = true;
            OrbitDLC.LOGGER.warn("Wet reflections could not render", error);
        }
    }

    static void close() {
        if (output != null) {
            output.destroyBuffers();
            output = null;
        }
        if (uniforms != null) {
            uniforms.close();
            uniforms = null;
        }
        failed = false;
    }
}
