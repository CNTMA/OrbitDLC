package com.orbitdlc.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.orbitdlc.OrbitDLC;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.system.MemoryStack;

import java.util.Optional;

public final class ShaderHand extends Module {
    static final ShaderHand INSTANCE = new ShaderHand();
    private static final int UNIFORM_BYTES = 48;
    private static final RenderPipeline PIPELINE = RenderPipelines.register(RenderPipeline.builder()
            .withLocation(OrbitDLC.id("pipeline/shader_hand"))
            .withVertexShader(OrbitDLC.id("core/wet_world"))
            .withFragmentShader(OrbitDLC.id("core/shader_hand"))
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withCull(false)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withUniform("Sampler0", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("Sampler1", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("HandFire", UniformType.UNIFORM_BUFFER).build())
            .build());

    private TextureTarget scene;
    private GpuBuffer uniforms;
    private VisualColor flameColor = VisualColor.ORIGINAL;
    private int heightPercent = 100;
    private int bloomPercent = 50;
    private boolean smoke = true;
    private long previousFrame;
    private float previousYaw, previousPitch;
    private float driftX, driftY;

    private ShaderHand() { super("shader_hand", Category.VISUALS); }

    @Override
    public String description() { return "Animated flames and sparks around the first-person hand."; }

    public static void render(RenderTarget target, GpuTextureView handDepth, float yaw, float pitch) {
        if (INSTANCE.isEnabled() && Optimization.level() < 3 && handDepth != null) INSTANCE.draw(target, handDepth, yaw, pitch);
    }

    private void updateDrift(long now, float yaw, float pitch) {
        float elapsed = (now - previousFrame) * 1e-9f;
        if (previousFrame == 0 || elapsed > 0.25f) {
            driftX = driftY = 0f;
        } else if (elapsed > 0f) {
            float yawChange = net.minecraft.util.Mth.wrapDegrees(yaw - previousYaw);
            float targetX = Math.clamp(yawChange / elapsed * 0.85f, -290f, 290f);
            float targetY = Math.clamp((previousPitch - pitch) / elapsed * 0.65f, -125f, 125f);
            driftX = softenDrift(driftX, targetX, elapsed);
            driftY = softenDrift(driftY, targetY, elapsed);
        }
        previousFrame = now;
        previousYaw = yaw;
        previousPitch = pitch;
    }

    private static float softenDrift(float current, float target, float elapsed) {
        float speed = Math.abs(target) > Math.abs(current) || current * target < 0f ? 18f : 3.2f;
        return current + (target - current) * (float) -Math.expm1(-speed * elapsed);
    }

    private void draw(RenderTarget target, GpuTextureView depth, float yaw, float pitch) {
        var color = target.getColorTexture();
        if (color == null || target.getColorTextureView() == null) return;
        try (var stack = MemoryStack.stackPush()) {
            int width = color.getWidth(0);
            int height = color.getHeight(0);
            if (scene == null) scene = new TextureTarget("OrbitDLC hand scene", width, height, color.getFormat(), null);
            else if (scene.width != width || scene.height != height) scene.resize(width, height);
            scene.copyColorFrom(target);
            var device = RenderSystem.getDevice();
            if (uniforms == null) {
                uniforms = device.createBuffer(() -> "OrbitDLC hand fire uniforms",
                        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, UNIFORM_BYTES);
            }
            var data = stack.malloc(UNIFORM_BYTES);
            long now = System.nanoTime();
            updateDrift(now, yaw, pitch);
            data.putFloat(1f / width).putFloat(1f / height)
                    .putFloat((now % 60_000_000_000L) * 1e-9f).putFloat(heightPercent / 100f);
            int tint = flameColor.isOriginal() ? 0xFF6612 : flameColor.renderColor();
            data.putFloat((tint >> 16 & 255) / 255f).putFloat((tint >> 8 & 255) / 255f)
                    .putFloat((tint & 255) / 255f).putFloat(bloomPercent / 100f);
            data.putFloat(driftX).putFloat(driftY).putFloat(smoke ? 1f : 0f).putFloat(0f).flip();
            var encoder = device.createCommandEncoder();
            encoder.writeToBuffer(uniforms.slice(), data);
            var samplers = RenderSystem.getSamplerCache();
            try (var pass = encoder.createRenderPass(() -> "OrbitDLC ShaderHand",
                    target.getColorTextureView(), Optional.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
                pass.setUniform("Sampler0", scene.getColorTextureView(), samplers.getClampToEdge(FilterMode.LINEAR));
                pass.setUniform("Sampler1", depth, samplers.getClampToEdge(FilterMode.NEAREST));
                pass.setUniform("HandFire", uniforms);
                pass.draw(3, 1, 0, 0);
            }
        } catch (RuntimeException error) {
            OrbitDLC.LOGGER.warn("ShaderHand could not render", error);
            setEnabled(false);
        }
    }

    @Override
    public JsonObject saveSettings() {
        var settings = super.saveSettings();
        settings.addProperty("color", flameColor.name());
        settings.addProperty("height", heightPercent);
        settings.addProperty("bloom", bloomPercent);
        settings.addProperty("smoke", smoke);
        return settings;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        flameColor = VisualColor.load(settings);
        heightPercent = settings.has("height") ? Math.clamp(settings.get("height").getAsInt(), 50, 250) : 100;
        bloomPercent = settings.has("bloom") ? Math.clamp(settings.get("bloom").getAsInt(), 0, 150) : 50;
        smoke = !settings.has("smoke") || settings.get("smoke").getAsBoolean();
    }

    ModuleSettings settings(Screen parent) {
        return new ModuleSettings("ShaderHand", parent) {
            @Override
            protected void init() {
                chip("Color · " + flameColor.label, 0, button -> {
                    flameColor = flameColor.next();
                    button.setMessage(Component.literal("Color · " + flameColor.label));
                });
                slider("Height", "%", 24, 50, 250, () -> heightPercent, value -> heightPercent = value);
                slider("Bloom", "%", 48, 0, 150, () -> bloomPercent, value -> bloomPercent = value);
                chip(smoke ? "Smoke · ON" : "Smoke · OFF", 3, button -> {
                    smoke = !smoke;
                    button.setMessage(Component.literal(smoke ? "Smoke · ON" : "Smoke · OFF"));
                });
            }
        };
    }

    @Override
    protected void onDisable() { close(); }

    void close() {
        previousFrame = 0;
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
