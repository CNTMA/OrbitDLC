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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.util.Optional;

public final class MotionBlur extends Module {
    static final MotionBlur INSTANCE = new MotionBlur();
    private static final int UNIFORM_BYTES = 144;
    private static final RenderPipeline PIPELINE = RenderPipelines.register(RenderPipeline.builder()
            .withLocation(OrbitDLC.id("pipeline/motion_blur"))
            .withVertexShader(OrbitDLC.id("core/wet_world"))
            .withFragmentShader(OrbitDLC.id("core/motion_blur"))
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withCull(false)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withUniform("Scene", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("Depth", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("MotionSettings", UniformType.UNIFORM_BUFFER).build())
            .build());

    private final Matrix4f current = new Matrix4f();
    private final Matrix4f previous = new Matrix4f();
    private final Matrix4f inverse = new Matrix4f();
    private TextureTarget scene;
    private GpuBuffer uniforms;
    private ClientLevel previousLevel;
    private Vec3 previousPosition;
    private long previousFrame;
    private int previousWidth, previousHeight;
    private int strength = 45;

    private MotionBlur() { super("motion_blur", Category.VISUALS); }

    @Override public String description() { return "Soft directional blur during camera motion."; }

    public static void render(RenderTarget target, Camera camera) {
        if (!INSTANCE.isEnabled()) return;
        if (INSTANCE.strength == 0 || target.getColorTextureView() == null || target.getDepthTextureView() == null) {
            INSTANCE.previousPosition = null;
            return;
        }
        INSTANCE.draw(target, camera);
    }

    private void draw(RenderTarget target, Camera camera) {
        var position = camera.position();
        var level = Minecraft.getInstance().level;
        var source = target.getColorTexture();
        int width = source.getWidth(0);
        int height = source.getHeight(0);
        long now = System.nanoTime();
        long elapsed = now - previousFrame;
        camera.getViewRotationProjectionMatrix(current);
        try {
            if (previousPosition == null || level != previousLevel || width != previousWidth
                    || height != previousHeight || elapsed > 150_000_000L) return;
            var movement = position.subtract(previousPosition);
            if (movement.lengthSqr() > 16 || (movement.lengthSqr() < 1.0e-10 && current.equals(previous, 0.00001f))) return;
            float exposure = strength / 100f * Math.min(4f, 12_000_000f / Math.max(1L, elapsed));
            composite(target, movement, exposure);
        } catch (RuntimeException error) {
            OrbitDLC.LOGGER.warn("Motion Blur could not render", error);
            setEnabled(false);
        } finally {
            if (isEnabled()) {
                previous.set(current);
                previousPosition = position;
                previousLevel = level;
                previousFrame = now;
                previousWidth = width;
                previousHeight = height;
            }
        }
    }

    private void composite(RenderTarget target, Vec3 movement, float exposure) {
        var source = target.getColorTexture();
        int width = source.getWidth(0);
        int height = source.getHeight(0);
        if (scene != null && scene.getColorTexture().getFormat() != source.getFormat()) {
            scene.destroyBuffers();
            scene = null;
        }
        if (scene == null) scene = new TextureTarget("Motion Blur scene", width, height, source.getFormat(), null);
        else if (scene.width != width || scene.height != height) scene.resize(width, height);
        scene.copyColorFrom(target);
        var device = RenderSystem.getDevice();
        if (uniforms == null) uniforms = device.createBuffer(() -> "Motion Blur settings",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, UNIFORM_BYTES);

        try (var stack = MemoryStack.stackPush()) {
            var data = stack.malloc(UNIFORM_BYTES);
            inverse.set(current).invert().get(0, data);
            previous.get(64, data);
            data.position(128);
            // Pack the depth convention in the sign of the exposure.
            data.putFloat((float) movement.x).putFloat((float) movement.y).putFloat((float) movement.z)
                    .putFloat(device.getDeviceInfo().isZZeroToOne() ? exposure : -exposure).flip();
            var encoder = device.createCommandEncoder();
            encoder.writeToBuffer(uniforms.slice(), data);
            var samplers = RenderSystem.getSamplerCache();
            try (var pass = encoder.createRenderPass(() -> "Motion Blur", target.getColorTextureView(), Optional.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
                pass.setUniform("Scene", scene.getColorTextureView(), samplers.getClampToEdge(FilterMode.LINEAR));
                pass.setUniform("Depth", target.getDepthTextureView(), samplers.getClampToEdge(FilterMode.NEAREST));
                pass.setUniform("MotionSettings", uniforms);
                pass.draw(3, 1, 0, 0);
            }
        }
    }

    @Override public JsonObject saveSettings() {
        var settings = new JsonObject();
        settings.addProperty("strength", strength);
        return settings;
    }

    @Override public void loadSettings(JsonObject settings) {
        strength = settings.has("strength") ? Math.clamp(settings.get("strength").getAsInt(), 0, 100) : 45;
    }

    ModuleSettings settings(Screen parent) {
        return new ModuleSettings("Motion Blur", parent) {
            @Override protected void init() {
                slider("Strength", "%", 0, 0, 100, () -> strength, value -> strength = value);
            }
        };
    }

    @Override protected void onDisable() { close(); }

    void close() {
        if (scene != null) scene.destroyBuffers();
        if (uniforms != null) uniforms.close();
        scene = null;
        uniforms = null;
        previousPosition = null;
        previousLevel = null;
    }
}
