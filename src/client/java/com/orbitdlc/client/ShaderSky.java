package com.orbitdlc.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.*;
import com.orbitdlc.OrbitDLC;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.util.Locale;
import java.util.Optional;

public final class ShaderSky extends Module {
    static final ShaderSky INSTANCE = new ShaderSky();

    private enum Preset {
        SUNSET, NEBULA, CAUSTIC, RADIANT, MOONLIT, METEORS;

        final RenderPipeline pipeline = createPipeline(name().toLowerCase(Locale.ROOT));
    }

    private Preset preset = Preset.SUNSET;
    private float speed = 1f;
    private VisualColor color = VisualColor.ORIGINAL;
    private final long start = System.nanoTime();
    private final Matrix4f inverse = new Matrix4f();
    private GpuBuffer uniforms;

    private ShaderSky() {
        super("shadersky", Category.VISUALS);
    }

    static void init() {
        Preset.values();
    }

    private static RenderPipeline createPipeline(String name) {
        return RenderPipelines.register(RenderPipeline.builder()
                .withLocation(OrbitDLC.id("pipeline/sky_" + name))
                .withVertexShader(OrbitDLC.id("core/orbit_sky"))
                .withFragmentShader(OrbitDLC.id("core/orbit_sky_" + name))
                .withBindGroupLayout(BindGroupLayout.builder()
                        .withUniform("SkyUniforms", UniformType.UNIFORM_BUFFER).build())
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withColorTargetState(ColorTargetState.DEFAULT)
                .withCull(false).build());
    }

    @Override
    public String description() {
        return "Animated sky shaders. Right-click to choose a preset and speed.";
    }

    @Override
    protected void onDisable() {
        close();
    }

    void close() {
        if (uniforms != null) {
            uniforms.close();
            uniforms = null;
        }
    }

    public static boolean render(SkyRenderState sky, com.mojang.blaze3d.pipeline.RenderTarget skyTarget) {
        return INSTANCE.draw(sky, skyTarget);
    }

    private boolean draw(SkyRenderState sky, com.mojang.blaze3d.pipeline.RenderTarget skyTarget) {
        if (!isEnabled() || sky.skybox == null || !sky.skybox.name().equals("OVERWORLD")) return false;
        var renderer = Minecraft.getInstance().gameRenderer;
        var target = skyTarget.getColorTextureView();
        if (target == null) return false;
        try (var stack = MemoryStack.stackPush()) {
            if (uniforms == null) {
                uniforms = RenderSystem.getDevice().createBuffer(() -> "OrbitDLC sky uniforms",
                        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 112);
            }
            var buffer = stack.malloc(112);
            renderer.mainCamera().getViewRotationProjectionMatrix(inverse).invert().get(0, buffer);
            float seconds = ((System.nanoTime() - start) / 1_000_000_000f) * speed;
            buffer.position(64);
            buffer.putFloat(seconds).putFloat(0).putFloat(1).putFloat(1);
            buffer.putFloat(1).putFloat(sky.rainBrightness).putFloat(1).putFloat(0);
            if (color.isOriginal()) {
                buffer.putFloat(0.67f).putFloat(0.52f).putFloat(1f).putFloat(0f);
            } else {
                int tint = color.renderColor();
                buffer.putFloat((tint >> 16 & 255) / 255f).putFloat((tint >> 8 & 255) / 255f)
                        .putFloat((tint & 255) / 255f).putFloat(1f);
            }
            buffer.flip();
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.writeToBuffer(uniforms.slice(), buffer);
            try (var pass = encoder.createRenderPass(() -> "OrbitDLC ShaderSky", target, Optional.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(preset.pipeline));
                pass.setUniform("SkyUniforms", uniforms);
                pass.draw(3, 1, 0, 0);
            }
            return true;
        } catch (RuntimeException error) {
            OrbitDLC.LOGGER.warn("ShaderSky disabled after a rendering error", error);
            setEnabled(false);
            return false;
        }
    }

    @Override
    public com.google.gson.JsonObject saveSettings() {
        var settings = super.saveSettings();
        settings.addProperty("preset", preset.name());
        settings.addProperty("speed", speed);
        settings.addProperty("color", color.name());
        return settings;
    }

    @Override
    public void loadSettings(com.google.gson.JsonObject settings) {
        preset = Preset.valueOf(settings.get("preset").getAsString());
        float value = settings.get("speed").getAsFloat();
        if (!Float.isFinite(value)) throw new IllegalArgumentException("Invalid sky speed");
        speed = Math.clamp(value, 0.5f, 2f);
        color = VisualColor.load(settings);
    }

    ModuleSettings settings(Screen parent) {
        return new ModuleSettings("ShaderSky", parent) {
            @Override
            protected void init() {
                chip("Color · " + color.label, 0, button -> {
                    color = color.next();
                    button.setMessage(Component.literal("Color · " + color.label));
                });
                var presets = Preset.values();
                int speedRow = choicesAt(java.util.Arrays.stream(presets)
                                .map(value -> value.name().charAt(0) + value.name().substring(1).toLowerCase(Locale.ROOT))
                                .toList(),
                        index -> preset == presets[index], index -> preset = presets[index], 25);
                slider("Speed", "%", speedRow + 5, 50, 200, () -> Math.round(speed * 100), value -> speed = value / 100f);
            }

        };
    }
}
