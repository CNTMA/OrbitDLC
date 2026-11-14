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
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.util.List;
import java.util.Optional;

public final class BlockOverlay extends Module {
    static final BlockOverlay INSTANCE = new BlockOverlay();
    private static final int BYTES = 112;
    private static final RenderPipeline PIPELINE = RenderPipelines.register(RenderPipeline.builder()
            .withLocation(OrbitDLC.id("pipeline/block_overlay"))
            .withVertexShader(OrbitDLC.id("core/wet_world"))
            .withFragmentShader(OrbitDLC.id("core/block_overlay"))
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withCull(false)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withUniform("Scene", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("Depth", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("OverlaySettings", UniformType.UNIFORM_BUFFER).build())
            .build());

    private enum Shader { GLASS, GLOW, SOLID }
    private Shader shader = Shader.GLASS;
    private final Matrix4f inverse = new Matrix4f();
    private TextureTarget scene;
    private GpuBuffer uniforms;

    private BlockOverlay() { super("block_overlay", Category.VISUALS); }

    @Override public String description() { return "Shade the block under the crosshair."; }

    @Override public JsonObject saveSettings() {
        var settings = new JsonObject();
        settings.addProperty("shader", shader.name());
        return settings;
    }

    @Override public void loadSettings(JsonObject settings) {
        shader = settings.has("shader") ? Shader.valueOf(settings.get("shader").getAsString()) : Shader.GLASS;
    }

    ModuleSettings settings(Screen parent) {
        return new ModuleSettings("BlockOverlay", parent) {
            @Override protected void init() {
                choices(List.of("Liquid Glass", "Glow", "Solid"), i -> shader.ordinal() == i,
                        i -> shader = Shader.values()[i]);
            }
            @Override protected String summary() { return "Shader"; }
        };
    }

    public static void render(RenderTarget target, Camera camera) {
        var client = Minecraft.getInstance();
        if (INSTANCE.isEnabled() && client.level != null
                && client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && target.getColorTextureView() != null && target.getDepthTextureView() != null) {
            INSTANCE.draw(target, camera, hit);
        }
    }

    private void draw(RenderTarget target, Camera camera, BlockHitResult hit) {
        try (var stack = MemoryStack.stackPush()) {
            var source = target.getColorTexture();
            int width = source.getWidth(0);
            int height = source.getHeight(0);
            if (scene != null && scene.getColorTexture().getFormat() != source.getFormat()) {
                scene.destroyBuffers();
                scene = null;
            }
            if (scene == null) scene = new TextureTarget("OrbitDLC BlockOverlay scene", width, height, source.getFormat(), null);
            else if (scene.width != width || scene.height != height) scene.resize(width, height);
            scene.copyColorFrom(target);

            var device = RenderSystem.getDevice();
            if (uniforms == null) uniforms = device.createBuffer(() -> "OrbitDLC BlockOverlay settings",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, BYTES);
            var data = stack.malloc(BYTES);
            camera.getViewRotationProjectionMatrix(inverse).invert().get(0, data);
            data.position(64);
            var pos = hit.getBlockPos();
            var eye = camera.position();
            data.putFloat((float) (pos.getX() - eye.x)).putFloat((float) (pos.getY() - eye.y))
                    .putFloat((float) (pos.getZ() - eye.z)).putFloat(shader.ordinal());
            int color = OrbitTheme.accent();
            data.putFloat((color >> 16 & 255) / 255f).putFloat((color >> 8 & 255) / 255f)
                    .putFloat((color & 255) / 255f).putFloat(device.getDeviceInfo().isZZeroToOne() ? 1f : 0f);
            data.putFloat(1f / width).putFloat(1f / height)
                    .putFloat((System.nanoTime() % 8_000_000_000L) / 8_000_000_000f).putFloat(0f);
            data.flip();
            var encoder = device.createCommandEncoder();
            encoder.writeToBuffer(uniforms.slice(), data);
            var samplers = RenderSystem.getSamplerCache();
            try (var pass = encoder.createRenderPass(() -> "OrbitDLC BlockOverlay",
                    target.getColorTextureView(), Optional.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
                pass.setUniform("Scene", scene.getColorTextureView(), samplers.getClampToEdge(FilterMode.LINEAR));
                pass.setUniform("Depth", target.getDepthTextureView(), samplers.getClampToEdge(FilterMode.NEAREST));
                pass.setUniform("OverlaySettings", uniforms);
                pass.draw(3, 1, 0, 0);
            }
        } catch (RuntimeException error) {
            OrbitDLC.LOGGER.warn("BlockOverlay could not render", error);
            setEnabled(false);
        }
    }

    @Override protected void onDisable() { close(); }

    void close() {
        if (scene != null) scene.destroyBuffers();
        if (uniforms != null) uniforms.close();
        scene = null;
        uniforms = null;
    }
}
