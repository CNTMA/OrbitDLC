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

public final class CustomFog extends Module {
    static final CustomFog INSTANCE = new CustomFog();
    private static final int UNIFORM_BYTES = 112;
    private static final RenderPipeline PIPELINE = RenderPipelines.register(RenderPipeline.builder()
            .withLocation(OrbitDLC.id("pipeline/custom_fog"))
            .withVertexShader(OrbitDLC.id("core/wet_world"))
            .withFragmentShader(OrbitDLC.id("core/custom_fog"))
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withCull(false)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withUniform("Sampler0", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("Sampler1", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("CustomFogSettings", UniformType.UNIFORM_BUFFER).build())
            .build());

    private final Matrix4f inverseViewProjection = new Matrix4f();
    private final long animationStart = System.nanoTime();
    private int density = 50;
    private VisualColor fogColor = VisualColor.ORIGINAL;
    private TextureTarget scene;
    private GpuBuffer uniforms;

    private CustomFog() { super("custom_fog", Category.VISUALS); }

    @Override
    public String description() { return "Soft volumetric fog with adjustable color and density."; }

    public static void render(RenderTarget target, Camera camera) {
        if (INSTANCE.isEnabled() && Optimization.level() < 3 && target.getColorTextureView() != null && target.getDepthTextureView() != null) {
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
            if (scene == null) scene = new TextureTarget("OrbitDLC CustomFog scene", width, height, source.getFormat(), null);
            else if (scene.width != width || scene.height != height) scene.resize(width, height);
            scene.copyColorFrom(target);

            var device = RenderSystem.getDevice();
            if (uniforms == null) {
                uniforms = device.createBuffer(() -> "OrbitDLC CustomFog settings",
                        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, UNIFORM_BYTES);
            }
            var data = stack.malloc(UNIFORM_BYTES);
            camera.getViewRotationProjectionMatrix(inverseViewProjection).invert().get(0, data);
            data.position(64);
            var position = camera.position();
            data.putFloat((float) position.x).putFloat((float) position.y).putFloat((float) position.z)
                    .putFloat(device.getDeviceInfo().isZZeroToOne() ? 1f : 0f);
            int tint = fogColor.isOriginal() ? 0xAABACB : fogColor.renderColor();
            data.putFloat((tint >> 16 & 255) / 255f).putFloat((tint >> 8 & 255) / 255f)
                    .putFloat((tint & 255) / 255f).putFloat(density / 100f);
            double seconds = (System.nanoTime() - animationStart) * 1e-9;
            data.putFloat((float) (seconds * 0.055 % Math.TAU))
                    .putFloat((float) (seconds * 0.021 % Math.TAU))
                    .putFloat((float) (seconds * 0.036 % Math.TAU)).putFloat(0f).flip();

            var encoder = device.createCommandEncoder();
            encoder.writeToBuffer(uniforms.slice(), data);
            var samplers = RenderSystem.getSamplerCache();
            try (var pass = encoder.createRenderPass(() -> "OrbitDLC CustomFog",
                    target.getColorTextureView(), Optional.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
                pass.setUniform("Sampler0", scene.getColorTextureView(), samplers.getClampToEdge(FilterMode.LINEAR));
                pass.setUniform("Sampler1", target.getDepthTextureView(), samplers.getClampToEdge(FilterMode.NEAREST));
                pass.setUniform("CustomFogSettings", uniforms);
                pass.draw(3, 1, 0, 0);
            }
        } catch (RuntimeException error) {
            OrbitDLC.LOGGER.warn("CustomFog could not render", error);
            setEnabled(false);
        }
    }

    @Override
    public JsonObject saveSettings() {
        var settings = super.saveSettings();
        settings.addProperty("density", density);
        settings.addProperty("color", fogColor.name());
        return settings;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        density = settings.has("density") ? Math.clamp(settings.get("density").getAsInt(), 25, 100) : 50;
        fogColor = VisualColor.load(settings);
    }

    ModuleSettings settings(Screen parent) {
        return new ModuleSettings("CustomFog", parent) {
            @Override
            protected void init() {
                slider("Density", "%", 0, 25, 100, () -> density, value -> density = value);
                chip("Color · " + fogColor.label, 1, button -> {
                    fogColor = fogColor.next();
                    button.setMessage(Component.literal("Color · " + fogColor.label));
                });
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
