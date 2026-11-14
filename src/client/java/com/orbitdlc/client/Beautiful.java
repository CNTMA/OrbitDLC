package com.orbitdlc.client;

import com.google.gson.JsonObject;
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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.util.Optional;

public final class Beautiful extends Module {
    static final Beautiful INSTANCE = new Beautiful();
    private static final int UNIFORM_BYTES = 80;
    private static final RenderPipeline PIPELINE = RenderPipelines.register(RenderPipeline.builder()
            .withLocation(OrbitDLC.id("pipeline/beautiful"))
            .withVertexShader(OrbitDLC.id("core/beautiful"))
            .withFragmentShader(OrbitDLC.id("core/beautiful"))
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withCull(false)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withUniform("Sampler0", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("Sampler1", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("BeautifulSettings", UniformType.UNIFORM_BUFFER).build())
            .build());

    private final Matrix4f inverseViewProjection = new Matrix4f();
    private int strength = 100;
    private TextureTarget scene;
    private GpuBuffer uniforms;

    private Beautiful() { super("beautiful", Category.VISUALS); }

    @Override
    public String description() { return "Cinematic color, soft bloom and atmospheric distance haze."; }

    public static void render(RenderTarget target, Camera camera) {
        if (INSTANCE.isEnabled() && Optimization.level() < 2 && target.getColorTextureView() != null && target.getDepthTextureView() != null) {
            INSTANCE.draw(target, camera);
        }
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
            if (scene == null) scene = new TextureTarget("OrbitDLC Beautiful scene", width, height, source.getFormat(), null);
            else if (scene.width != width || scene.height != height) scene.resize(width, height);
            scene.copyColorFrom(target);

            var device = RenderSystem.getDevice();
            if (uniforms == null) {
                uniforms = device.createBuffer(() -> "OrbitDLC Beautiful settings",
                        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, UNIFORM_BYTES);
            }
            var data = stack.malloc(UNIFORM_BYTES);
            camera.getViewRotationProjectionMatrix(inverseViewProjection).invert().get(0, data);
            data.position(64);
            data.putFloat(1f / width).putFloat(1f / height).putFloat(strength / 100f)
                    .putFloat(device.getDeviceInfo().isZZeroToOne() ? 1f : 0f).flip();

            var encoder = device.createCommandEncoder();
            encoder.writeToBuffer(uniforms.slice(), data);
            var samplers = RenderSystem.getSamplerCache();
            try (var pass = encoder.createRenderPass(() -> "OrbitDLC Beautiful",
                    target.getColorTextureView(), Optional.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
                pass.setUniform("Sampler0", scene.getColorTextureView(), samplers.getClampToEdge(FilterMode.LINEAR));
                pass.setUniform("Sampler1", target.getDepthTextureView(), samplers.getClampToEdge(FilterMode.NEAREST));
                pass.setUniform("BeautifulSettings", uniforms);
                pass.draw(3, 1, 0, 0);
            }
        } catch (RuntimeException error) {
            OrbitDLC.LOGGER.warn("Beautiful could not render", error);
            setEnabled(false);
        }
    }

    @Override
    public JsonObject saveSettings() {
        var settings = super.saveSettings();
        settings.addProperty("strength", strength);
        return settings;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        strength = settings.has("strength") ? Math.clamp(settings.get("strength").getAsInt(), 50, 150) : 100;
    }

    ModuleSettings settings(Screen parent) {
        return new ModuleSettings("Beautiful", parent) {
            @Override
            protected void init() {
                slider("Strength", "%", 0, 50, 150, () -> strength, value -> strength = value);
            }
        };
    }

    @Override
    protected void onDisable() { close(); }

    void close() {
        if (scene != null) {
            scene.destroyBuffers();
            scene = null;
        }
        if (uniforms != null) {
            uniforms.close();
            uniforms = null;
        }
    }
}
