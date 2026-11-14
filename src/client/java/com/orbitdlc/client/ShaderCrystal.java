package com.orbitdlc.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.orbitdlc.OrbitDLC;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.Optional;

public final class ShaderCrystal extends Module {
    static final ShaderCrystal INSTANCE = new ShaderCrystal();
    private static final int LIMIT = 8;
    private static final int UNIFORM_BYTES = 96 + LIMIT * 16;
    private static final RenderPipeline PIPELINE = RenderPipelines.register(RenderPipeline.builder()
            .withLocation(OrbitDLC.id("pipeline/shader_crystal"))
            .withVertexShader(OrbitDLC.id("core/wet_world"))
            .withFragmentShader(OrbitDLC.id("core/shader_crystal"))
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withCull(false)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withUniform("Sampler0", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("Sampler1", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("CrystalSettings", UniformType.UNIFORM_BUFFER).build())
            .build());

    private record Crystal(float x, float y, float z, float rotation) {}
    private final ArrayList<Crystal> crystals = new ArrayList<>(LIMIT);
    private final Matrix4f inverseProjection = new Matrix4f();
    private TextureTarget scene;
    private GpuBuffer uniforms;

    private ShaderCrystal() { super("shadercrystal", Category.VISUALS); }

    @Override
    public String description() { return "Refractive liquid glass for End Crystals."; }

    public static void beginFrame() { INSTANCE.crystals.clear(); }

    public static boolean replace(EndCrystalRenderState crystal) {
        if (!INSTANCE.isEnabled() || Optimization.level() >= 3 || INSTANCE.crystals.size() >= LIMIT
                || crystal.distanceToCameraSq > 4096 || crystal.isInvisible || crystal.appearsGlowing()) return false;
        var camera = net.minecraft.client.Minecraft.getInstance().gameRenderer.mainCamera().position();
        float height = 1.0f + EndCrystalRenderer.getY(crystal.ageInTicks) * 0.4f;
        INSTANCE.crystals.add(new Crystal((float) (crystal.x - camera.x), (float) (crystal.y - camera.y) + height,
                (float) (crystal.z - camera.z), crystal.ageInTicks * 0.05236f));
        return true;
    }

    public static void render(RenderTarget target, Camera camera) {
        if (!INSTANCE.crystals.isEmpty() && target.getColorTextureView() != null
                && target.getDepthTextureView() != null) INSTANCE.draw(target, camera);
    }

    private void draw(RenderTarget target, Camera camera) {
        try (var stack = MemoryStack.stackPush()) {
            var source = target.getColorTexture();
            int width = source.getWidth(0);
            int height = source.getHeight(0);
            if (scene != null && scene.getColorTexture().getFormat() != source.getFormat()) {
                scene.destroyBuffers();
                scene = null;
            }
            if (scene == null) scene = new TextureTarget("OrbitDLC crystal scene", width, height, source.getFormat(), null);
            else if (scene.width != width || scene.height != height) scene.resize(width, height);
            scene.copyColorFrom(target);

            var device = RenderSystem.getDevice();
            if (uniforms == null) uniforms = device.createBuffer(() -> "OrbitDLC CrystalSettings",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, UNIFORM_BYTES);
            var data = stack.calloc(UNIFORM_BYTES);
            camera.getViewRotationProjectionMatrix(inverseProjection).invert().get(0, data);
            data.position(64);
            data.putFloat(crystals.size()).putFloat(device.getDeviceInfo().isZZeroToOne() ? 1f : 0f)
                    .putFloat(1f / width).putFloat(1f / height);
            int tint = Theme.color(0xFFE1ABFF);
            data.putFloat((tint >> 16 & 255) / 255f).putFloat((tint >> 8 & 255) / 255f)
                    .putFloat((tint & 255) / 255f).putFloat(0f);
            for (var crystal : crystals) {
                data.putFloat(crystal.x()).putFloat(crystal.y()).putFloat(crystal.z()).putFloat(crystal.rotation());
            }
            data.position(0);
            var encoder = device.createCommandEncoder();
            encoder.writeToBuffer(uniforms.slice(), data);
            var samplers = RenderSystem.getSamplerCache();
            try (var pass = encoder.createRenderPass(() -> "OrbitDLC Liquid Glass crystals",
                    target.getColorTextureView(), Optional.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
                pass.setUniform("Sampler0", scene.getColorTextureView(), samplers.getClampToEdge(FilterMode.LINEAR));
                pass.setUniform("Sampler1", target.getDepthTextureView(), samplers.getClampToEdge(FilterMode.NEAREST));
                pass.setUniform("CrystalSettings", uniforms);
                pass.draw(3, 1, 0, 0);
            }
        } catch (RuntimeException error) {
            OrbitDLC.LOGGER.warn("ShaderCrystal could not render", error);
            setEnabled(false);
        }
    }

    void close() {
        if (scene != null) scene.destroyBuffers();
        if (uniforms != null) uniforms.close();
    }
}
