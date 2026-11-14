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
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayDeque;
import java.util.Optional;

public final class Trail extends Module {
    static final Trail INSTANCE = new Trail();
    private static final int LIMIT = 32;
    private static final int BYTES = 112 + LIMIT * 16;
    private static final RenderPipeline PIPELINE = RenderPipelines.register(RenderPipeline.builder()
            .withLocation(OrbitDLC.id("pipeline/trail"))
            .withVertexShader(OrbitDLC.id("core/wet_world"))
            .withFragmentShader(OrbitDLC.id("core/trail"))
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(ColorTargetState.DEFAULT).withCull(false)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withUniform("Scene", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("Depth", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("TrailSettings", UniformType.UNIFORM_BUFFER).build())
            .build());

    private record Point(Vec3 position, long at) {}

    private final ArrayDeque<Point> points = new ArrayDeque<>();
    private final Matrix4f projection = new Matrix4f();
    private final Vector4f clip = new Vector4f();
    private Object trackedPlayer;
    private int length = 1200;
    private int starCount = 14;
    private boolean stars = true;
    private boolean firstPerson = true;
    private VisualColor color = VisualColor.ORIGINAL;
    private TextureTarget scene;
    private GpuBuffer uniforms;

    private Trail() { super("trail", Category.VISUALS); }

    @Override public String description() { return "A fiery 3D ribbon with stars behind your player."; }

    void tick(Minecraft client) {
        var player = client.player;
        if (player != trackedPlayer || !isEnabled() || player == null || !player.isAlive()) {
            points.clear();
            trackedPlayer = player;
            return;
        }
        long now = System.nanoTime();
        while (!points.isEmpty() && now - points.getFirst().at() > length * 1_000_000L) points.removeFirst();
        var position = player.position().add(0, 0.55, 0);
        if (!points.isEmpty()) {
            double gap = position.distanceToSqr(points.getLast().position());
            if (gap < 0.0016) return;
            if (gap > 16) points.clear();
        }
        if (points.size() == LIMIT - 1) points.removeFirst();
        points.addLast(new Point(position, now));
    }

    public static void render(RenderTarget target, Camera camera) {
        var trail = INSTANCE;
        if (trail.isEnabled() && trail.points.size() >= 2 && Optimization.level() < 3
                && (trail.firstPerson || !Minecraft.getInstance().options.getCameraType().isFirstPerson())
                && target.getColorTextureView() != null && target.getDepthTextureView() != null)
            trail.draw(target, camera);
    }

    private void draw(RenderTarget target, Camera camera) {
        long now = System.nanoTime();
        var eye = camera.position();
        camera.getViewRotationProjectionMatrix(projection);
        float left = 1, right = 0, bottom = 1, top = 0;
        for (var point : points) {
            var p = point.position().subtract(eye);
            projection.transform(clip.set((float) p.x, (float) p.y, (float) p.z, 1));
            if (clip.w <= 0.7f) {
                // A segment can cross the near plane even with both ends outside the viewport.
                left = bottom = 0;
                right = top = 1;
                break;
            }
            float x = clip.x / clip.w * 0.5f + 0.5f, y = clip.y / clip.w * 0.5f + 0.5f;
            float marginX = Math.abs(projection.m00()) * 0.45f / clip.w;
            float marginY = Math.abs(projection.m11()) * 0.45f / clip.w;
            left = Math.min(left, x - marginX);
            right = Math.max(right, x + marginX);
            bottom = Math.min(bottom, y - marginY);
            top = Math.max(top, y + marginY);
        }
        if (left >= 1 || right <= 0 || bottom >= 1 || top <= 0) return;
        try (var stack = MemoryStack.stackPush()) {
            var source = target.getColorTexture();
            int width = source.getWidth(0), height = source.getHeight(0);
            if (scene != null && scene.getColorTexture().getFormat() != source.getFormat()) close();
            if (scene == null) scene = new TextureTarget("OrbitDLC trail scene", width, height, source.getFormat(), null);
            else if (scene.width != width || scene.height != height) scene.resize(width, height);
            scene.copyColorFrom(target);
            var device = RenderSystem.getDevice();
            if (uniforms == null) uniforms = device.createBuffer(() -> "Trail settings",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, BYTES);
            var data = stack.calloc(BYTES);
            projection.invert().get(0, data);
            data.position(64);
            var player = Minecraft.getInstance().player;
            // The interpolated player position closes the gap between client ticks.
            boolean head = player != null && player == trackedPlayer;
            data.putFloat(points.size() + (head ? 1 : 0)).putFloat(stars ? starCount : 0)
                    .putFloat((now % 12_000_000_000L) / 1e9f)
                    .putFloat(device.getDeviceInfo().isZZeroToOne() ? 1 : 0);
            int tint = color.isOriginal() ? 0xFF561A : color.renderColor();
            data.putFloat((tint >> 16 & 255) / 255f).putFloat((tint >> 8 & 255) / 255f)
                    .putFloat((tint & 255) / 255f).putFloat(0);
            data.putFloat(left).putFloat(bottom).putFloat(right).putFloat(top);
            for (var point : points) {
                var p = point.position().subtract(eye);
                data.putFloat((float) p.x).putFloat((float) p.y).putFloat((float) p.z)
                        .putFloat(Math.clamp((now - point.at()) / (length * 1_000_000f), 0, 1));
            }
            if (head) {
                float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
                var p = player.getPosition(partial).add(0, 0.55, 0).subtract(eye);
                data.putFloat((float) p.x).putFloat((float) p.y).putFloat((float) p.z).putFloat(0);
            }
            data.position(0);
            var encoder = device.createCommandEncoder();
            encoder.writeToBuffer(uniforms.slice(), data);
            var samplers = RenderSystem.getSamplerCache();
            try (var pass = encoder.createRenderPass(() -> "OrbitDLC fire trail", target.getColorTextureView(), Optional.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
                pass.setUniform("Scene", scene.getColorTextureView(), samplers.getClampToEdge(FilterMode.LINEAR));
                pass.setUniform("Depth", target.getDepthTextureView(), samplers.getClampToEdge(FilterMode.NEAREST));
                pass.setUniform("TrailSettings", uniforms);
                pass.draw(3, 1, 0, 0);
            }
        } catch (RuntimeException error) {
            OrbitDLC.LOGGER.warn("Trail could not render", error);
            setEnabled(false);
        }
    }

    @Override public JsonObject saveSettings() {
        var settings = new JsonObject();
        settings.addProperty("length", length);
        settings.addProperty("color", color.name());
        settings.addProperty("starCount", starCount);
        settings.addProperty("stars", stars);
        settings.addProperty("firstPerson", firstPerson);
        return settings;
    }

    @Override public void loadSettings(JsonObject settings) {
        length = settings.has("length") ? Math.clamp(settings.get("length").getAsInt(), 250, 1500) : 1200;
        color = VisualColor.load(settings);
        starCount = settings.has("starCount") ? Math.clamp(settings.get("starCount").getAsInt(), 0, 32) : 14;
        stars = !settings.has("stars") || settings.get("stars").getAsBoolean();
        firstPerson = !settings.has("firstPerson") || settings.get("firstPerson").getAsBoolean();
    }

    ModuleSettings settings(Screen parent) {
        return new ModuleSettings("Trail", parent) {
            @Override protected void init() {
                slider("Length", " ms", 0, 250, 1500, () -> length, value -> length = value);
                chip("Color · " + color.label, 1, button -> {
                    color = color.next();
                    button.setMessage(Component.literal("Color · " + color.label));
                });
                slider("Star count", "", 48, 0, 32, () -> starCount, value -> starCount = value);
                toggleAt("Stars", 72, () -> stars, () -> stars = !stars);
                toggleAt("First person", 96, () -> firstPerson, () -> firstPerson = !firstPerson);
            }
        };
    }

    @Override protected void onDisable() {
        points.clear();
        close();
    }

    void close() {
        if (scene != null) scene.destroyBuffers();
        if (uniforms != null) uniforms.close();
        scene = null;
        uniforms = null;
    }
}
