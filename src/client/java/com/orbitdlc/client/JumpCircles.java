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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayDeque;
import java.util.Optional;

final class JumpCircles extends Module {
    static final JumpCircles INSTANCE = new JumpCircles();
    private static final int LIMIT = 16;
    private static final int BYTES = 160 + LIMIT * 48;
    private static final RenderPipeline PIPELINE = RenderPipelines.register(RenderPipeline.builder()
            .withLocation(OrbitDLC.id("pipeline/jump_circles"))
            .withVertexShader(OrbitDLC.id("core/wet_world"))
            .withFragmentShader(OrbitDLC.id("core/jump_circles"))
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withCull(false)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withUniform("DepthSampler", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("SceneSampler", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("SealSampler", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("WorldJumpCircleUniforms", UniformType.UNIFORM_BUFFER).build())
            .build());

    private record Ring(Vec3 origin, long started) {}

    private final ArrayDeque<Ring> rings = new ArrayDeque<>();
    private final Matrix4f inverse = new Matrix4f();
    private LocalPlayer previousPlayer;
    private Vec3 ground;
    private boolean grounded;
    private float radius = 2f;
    private long lifetime = 850_000_000L;
    private GpuBuffer uniforms;
    private TextureTarget scene;
    private boolean seal;
    private VisualColor color = VisualColor.ORIGINAL;

    private JumpCircles() { super("jump_circles", Category.VISUALS); }

    @Override
    public String description() { return "Expanding ground rings when you jump. Right-click for size and duration."; }

    void tick(Minecraft client) {
        var player = client.player;
        if (!isEnabled() || player == null || player != previousPlayer) {
            rings.clear();
            previousPlayer = player;
            grounded = false;
        }
        if (!isEnabled() || player == null) return;
        long now = System.nanoTime();
        rings.removeIf(ring -> now - ring.started() >= lifetime);
        if (player.onGround()) {
            ground = player.position();
        } else if (grounded && player.getDeltaMovement().y > 0) {
            if (rings.size() == LIMIT) rings.removeFirst();
            rings.addLast(new Ring(ground, now));
        }
        grounded = player.onGround();
    }

    void render(Minecraft client, RenderTarget target) {
        if (!isEnabled() || rings.isEmpty() || client.player != previousPlayer
                || target.getDepthTextureView() == null || target.getColorTextureView() == null) return;
        var camera = client.gameRenderer.mainCamera();
        var frustum = Optimization.frustum(camera);
        java.util.Collection<Ring> visibleRings = rings;
        if (frustum != null) {
            // Include rotated seal corners and the shockwave glow outside the ring.
            double extent = radius * Math.sqrt(2) + 3;
            visibleRings = rings.stream().filter(ring -> {
                var origin = ring.origin();
                return frustum.isVisible(new net.minecraft.world.phys.AABB(
                        origin.x - extent, origin.y - 1.5, origin.z - extent,
                        origin.x + extent, origin.y + 1.5, origin.z + extent));
            }).toList();
            if (visibleRings.isEmpty()) return;
        }
        var device = RenderSystem.getDevice();
        try (var stack = MemoryStack.stackPush()) {
            if (uniforms == null) {
                uniforms = device.createBuffer(() -> "Jump circles", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, BYTES);
            }
            var color = target.getColorTexture();
            int width = color.getWidth(0);
            int height = color.getHeight(0);
            if (scene == null) {
                scene = new TextureTarget("Jump Circles scene", width, height, color.getFormat(), null);
            } else if (scene.width != width || scene.height != height) {
                scene.resize(width, height);
            }
            scene.copyColorFrom(target);

            var data = stack.calloc(BYTES);
            camera.getViewRotationProjectionMatrix(inverse).invert().get(0, data);
            inverse.identity().get(64, data);
            data.position(128);
            data.putFloat(0f).putFloat(0f).putFloat(0f);
            data.putFloat(device.getDeviceInfo().isZZeroToOne() ? 1f : 0f);
            data.putFloat(1f / width).putFloat(1f / height).putFloat(-1f).putFloat(visibleRings.size());
            var cameraPosition = camera.position();
            long now = System.nanoTime();
            for (Ring ring : visibleRings) {
                var relative = ring.origin().subtract(cameraPosition);
                data.putFloat((float) relative.x).putFloat((float) relative.y).putFloat((float) relative.z);
                data.putFloat(Math.clamp((float) (now - ring.started()) / lifetime, 0f, 1f));
                if (this.color.isOriginal()) {
                    data.putFloat(0.83f).putFloat(0.65f).putFloat(0.93f);
                } else {
                    int tint = this.color.renderColor();
                    data.putFloat((tint >> 16 & 255) / 255f).putFloat((tint >> 8 & 255) / 255f)
                            .putFloat((tint & 255) / 255f);
                }
                data.putFloat(radius);
                data.putFloat(0.75f).putFloat(0.7f).putFloat(0.22f).putFloat(seal ? -1f : 1.5f);
            }
            data.position(0);
            var encoder = device.createCommandEncoder();
            encoder.writeToBuffer(uniforms.slice(), data);
            var texture = client.getTextureManager().getTexture(OrbitDLC.id(seal
                    ? "textures/effects/jump_seal.png"
                    : "textures/effects/jump_circle.png"));
            var samplers = RenderSystem.getSamplerCache();
            try (var pass = encoder.createRenderPass(() -> "OrbitDLC jump circles", target.getColorTextureView(), Optional.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
                pass.setUniform("DepthSampler", target.getDepthTextureView(), samplers.getClampToEdge(FilterMode.NEAREST));
                pass.setUniform("SealSampler", texture.getTextureView(), samplers.getClampToEdge(FilterMode.LINEAR));
                pass.setUniform("SceneSampler", scene.getColorTextureView(), samplers.getClampToEdge(FilterMode.LINEAR));
                pass.setUniform("WorldJumpCircleUniforms", uniforms);
                pass.draw(3, 1, 0, 0);
            }
        } catch (RuntimeException error) {
            OrbitDLC.LOGGER.warn("Jump Circles could not render", error);
            setEnabled(false);
        }
    }

    @Override
    protected void onDisable() {
        rings.clear();
        grounded = false;
    }

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

    @Override
    public com.google.gson.JsonObject saveSettings() {
        var settings = super.saveSettings();
        settings.addProperty("radius", radius);
        settings.addProperty("lifetime", lifetime);
        settings.addProperty("seal", seal);
        settings.addProperty("color", color.name());
        return settings;
    }

    @Override
    public void loadSettings(com.google.gson.JsonObject settings) {
        float value = settings.get("radius").getAsFloat();
        if (!Float.isFinite(value)) throw new IllegalArgumentException("Invalid circle radius");
        radius = Math.clamp(value, 1.5f, 3f);
        lifetime = Math.clamp(settings.get("lifetime").getAsLong(), 550_000_000L, 1_150_000_000L);
        seal = settings.get("seal").getAsBoolean();
        color = VisualColor.load(settings);
    }

    ModuleSettings settings(Screen parent) {
        return new ModuleSettings("Jump Circles", parent) {
            @Override
            protected void init() {
                chip("Color · " + color.label, 3, button -> {
                    color = color.next();
                    button.setMessage(Component.literal("Color · " + color.label));
                });
                chip(seal ? "Style · Seal" : "Style · Shockwave", 2, button -> {
                    seal = !seal;
                    button.setMessage(Component.literal(seal ? "Style · Seal" : "Style · Shockwave"));
                });
                slider("Radius", " cm", 0, 150, 300, () -> Math.round(radius * 100), value -> radius = value / 100f);
                slider("Duration", " ms", 24, 550, 1150, () -> (int) (lifetime / 1_000_000), value -> lifetime = value * 1_000_000L);
            }
        };
    }
}
