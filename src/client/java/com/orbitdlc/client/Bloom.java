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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import org.lwjgl.system.MemoryStack;

import java.util.Optional;

public final class Bloom extends Module {
    static final Bloom INSTANCE = new Bloom();
    private static final RenderPipeline EXTRACT = pipeline("bloom_extract", BindGroupLayout.builder()
            .withUniform("Scene", UniformType.COMBINED_IMAGE_SAMPLER).build());
    private static final RenderPipeline COMPOSITE = pipeline("bloom", BindGroupLayout.builder()
            .withUniform("Scene", UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("Highlights", UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("BloomSettings", UniformType.UNIFORM_BUFFER).build());

    private int intensity = 65;
    private TextureTarget scene;
    private TextureTarget highlights;
    private GpuBuffer uniforms;

    private Bloom() { super("bloom", Category.VISUALS); }

    private static RenderPipeline pipeline(String name, BindGroupLayout layout) {
        return RenderPipelines.register(RenderPipeline.builder()
                .withLocation(OrbitDLC.id("pipeline/" + name))
                .withVertexShader(OrbitDLC.id("core/wet_world"))
                .withFragmentShader(OrbitDLC.id("core/" + name))
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withColorTargetState(ColorTargetState.DEFAULT)
                .withCull(false).withBindGroupLayout(layout).build());
    }

    @Override public String description() { return "Soft glow around bright parts of the world."; }

    private static boolean shouldRender() {
        return INSTANCE.isEnabled() && INSTANCE.intensity > 0 && Optimization.level() < 3;
    }

    public static void render(RenderTarget target) {
        if (shouldRender() && target.getColorTextureView() != null) INSTANCE.draw(target);
    }

    private void draw(RenderTarget target) {
        try (var stack = MemoryStack.stackPush()) {
            var source = target.getColorTexture();
            int width = source.getWidth(0), height = source.getHeight(0);
            int lowWidth = Math.max(1, width / 4), lowHeight = Math.max(1, height / 4);
            if (scene != null && scene.getColorTexture().getFormat() != source.getFormat()) close();
            if (scene == null) scene = new TextureTarget("Bloom scene", width, height, source.getFormat(), null);
            else if (scene.width != width || scene.height != height) scene.resize(width, height);
            if (highlights == null)
                highlights = new TextureTarget("Bloom highlights", lowWidth, lowHeight, source.getFormat(), null);
            else if (highlights.width != lowWidth || highlights.height != lowHeight) highlights.resize(lowWidth, lowHeight);
            scene.copyColorFrom(target);

            var device = RenderSystem.getDevice();
            if (uniforms == null) uniforms = device.createBuffer(() -> "Bloom settings",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 16);
            var data = stack.malloc(16);
            data.putFloat(intensity / 100f).putFloat(0).putFloat(0).putFloat(0).flip();
            var encoder = device.createCommandEncoder();
            encoder.writeToBuffer(uniforms.slice(), data);
            var linear = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            try (var pass = encoder.createRenderPass(() -> "Extract bloom", highlights.getColorTextureView(), Optional.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(EXTRACT));
                pass.setUniform("Scene", scene.getColorTextureView(), linear);
                pass.draw(3, 1, 0, 0);
            }
            try (var pass = encoder.createRenderPass(() -> "Composite bloom", target.getColorTextureView(), Optional.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(COMPOSITE));
                pass.setUniform("Scene", scene.getColorTextureView(), linear);
                pass.setUniform("Highlights", highlights.getColorTextureView(), linear);
                pass.setUniform("BloomSettings", uniforms);
                pass.draw(3, 1, 0, 0);
            }
        } catch (RuntimeException error) {
            OrbitDLC.LOGGER.warn("Bloom could not render", error);
            setEnabled(false);
        }
    }

    @Override public JsonObject saveSettings() {
        var settings = new JsonObject();
        settings.addProperty("intensity", intensity);
        return settings;
    }

    @Override public void loadSettings(JsonObject settings) {
        intensity = settings.has("intensity") ? Math.clamp(settings.get("intensity").getAsInt(), 0, 200) : 65;
    }

    ModuleSettings settings(Screen parent) {
        return new ModuleSettings("Bloom", parent) {
            @Override protected void init() {
                slider("Intensity", "%", 0, 0, 200, () -> intensity, value -> intensity = value);
            }
        };
    }

    @Override protected void onDisable() { close(); }

    void close() {
        if (scene != null) scene.destroyBuffers();
        if (highlights != null) highlights.destroyBuffers();
        if (uniforms != null) uniforms.close();
        scene = null;
        highlights = null;
        uniforms = null;
    }
}
