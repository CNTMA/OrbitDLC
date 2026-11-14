package com.orbitdlc.client;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.orbitdlc.OrbitDLC;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public final class SkyPng extends Module {
    static final SkyPng INSTANCE = new SkyPng();
    private static final RenderPipeline PIPELINE = RenderPipelines.register(RenderPipeline.builder()
            .withLocation(OrbitDLC.id("pipeline/sky_png"))
            .withVertexShader(OrbitDLC.id("core/wet_world"))
            .withFragmentShader(OrbitDLC.id("core/sky_png"))
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withUniform("SkyImage", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("SkyPngSettings", UniformType.UNIFORM_BUFFER).build())
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withCull(false).build());

    private final Matrix4f inverse = new Matrix4f();
    private Path path;
    private SkyImage image;
    private DynamicTexture texture;
    private GpuBuffer uniforms;
    private int uploaded = -1;
    private int generation;
    private long started;
    private String status = "No image selected";

    private SkyPng() { super("skypng", Category.VISUALS); }

    @Override
    public String description() { return "Use an image or animated GIF as the sky."; }

    @Override
    public JsonObject saveSettings() {
        var settings = new JsonObject();
        if (path != null) settings.addProperty("path", path.toString());
        return settings;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        clear();
        if (settings.has("path")) {
            path = Path.of(settings.get("path").getAsString());
            load(path, false);
        }
    }

    private void load(Path selected, boolean activate) {
        int request = ++generation;
        status = "Loading " + selected.getFileName();
        var client = Minecraft.getInstance();
        Thread.startVirtualThread(() -> {
            try {
                var decoded = SkyImage.read(selected);
                client.execute(() -> {
                    if (request != generation) return;
                    closeTexture();
                    path = selected;
                    image = decoded;
                    started = System.nanoTime();
                    status = path.getFileName().toString();
                    if (activate) setEnabled(true);
                });
            } catch (IOException | RuntimeException error) {
                client.execute(() -> {
                    if (request == generation)
                        status = error.getMessage() == null ? "Cannot open image" : error.getMessage();
                });
            }
        });
    }

    private void choose(Screen parent) {
        Minecraft.getInstance().gui.setScreen(new Screen(net.minecraft.network.chat.Component.literal("SkyPng")) {
            private net.minecraft.client.gui.components.EditBox file;
            private String error = "";

            @Override
            protected void init() {
                int w = Math.min(320, width - 24);
                int x = (width - w) / 2;
                int y = height / 2;
                String previous = file == null ? (path == null ? "" : path.toString()) : file.getValue();
                file = new net.minecraft.client.gui.components.EditBox(font, x, y, w, 20,
                        net.minecraft.network.chat.Component.literal("Image path"));
                file.setMaxLength(4096);
                file.setValue(previous);
                file.setHint(net.minecraft.network.chat.Component.literal("Paste the path to an image or GIF"));
                addRenderableWidget(file);
                addRenderableWidget(new OrbitButton(net.minecraft.network.chat.Component.literal("Load"),
                        x, y + 28, w / 2 - 3, 20, button -> acceptPath()));
                addRenderableWidget(new OrbitButton(net.minecraft.network.chat.Component.literal("Back"),
                        x + w / 2 + 3, y + 28, w - w / 2 - 3, 20, button -> onClose()));
                setInitialFocus(file);
            }

            @Override
            public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
                super.extractRenderState(graphics, mouseX, mouseY, delta);
                graphics.centeredText(font, title, width / 2, height / 2 - 38, 0xFFFFFFFF);
                graphics.centeredText(font, net.minecraft.network.chat.Component.literal(
                        "Drag an image here or paste its path"), width / 2, height / 2 - 20, 0xFFDCD5E7);
                graphics.centeredText(font, net.minecraft.network.chat.Component.literal(error),
                        width / 2, height / 2 + 58, 0xFFFFA7AE);
            }

            private void acceptPath() {
                String value = file.getValue().strip();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\""))
                    value = value.substring(1, value.length() - 1);
                try {
                    if (value.isBlank()) {
                        error = "Enter an image path";
                        return;
                    }
                    importFile(Path.of(value));
                } catch (java.nio.file.InvalidPathException invalid) {
                    error = "Invalid file path";
                }
            }

            @Override
            public void onFilesDrop(java.util.List<Path> files) {
                if (files.size() == 1) importFile(files.getFirst());
                else error = "Drop one image at a time";
            }

            private void importFile(Path selected) {
                if (!java.nio.file.Files.isRegularFile(selected)) {
                    error = "Image file not found";
                    return;
                }
                load(selected.toAbsolutePath().normalize(), true);
                onClose();
            }

            @Override public void onClose() { minecraft.gui.setScreen(parent); }
            @Override public boolean isPauseScreen() { return false; }
        });
    }

    private void clear() {
        generation++;
        closeTexture();
        image = null;
        path = null;
        status = "No image selected";
    }

    ModuleSettings settings(Screen parent) {
        return new ModuleSettings("SkyPng", parent) {
            @Override protected void init() {
                chip("Choose image or GIF", 0, button -> choose(parent));
                chip("Clear image", 1, button -> clear());
            }

            @Override protected String summary() {
                return status.length() > 26 ? status.substring(0, 25) + "…" : status;
            }
        };
    }

    public static boolean render(SkyRenderState sky, RenderTarget target) {
        return INSTANCE.isEnabled() && INSTANCE.image != null && sky.skybox != null
                && sky.skybox.name().equals("OVERWORLD") && INSTANCE.draw(target);
    }

    private boolean draw(RenderTarget target) {
        if (target.getColorTextureView() == null) return false;
        try (var stack = MemoryStack.stackPush()) {
            int frame = image.frameAt((System.nanoTime() - started) / 1_000_000L);
            if (texture == null)
                texture = new DynamicTexture(() -> "OrbitDLC sky image", image.width(), image.height(), false);
            if (frame != uploaded) {
                var pixels = texture.getPixels();
                var colors = image.frames()[frame];
                for (int y = 0; y < image.height(); y++)
                    for (int x = 0; x < image.width(); x++)
                        pixels.setPixel(x, y, colors[y * image.width() + x]);
                texture.upload();
                uploaded = frame;
            }

            var device = RenderSystem.getDevice();
            if (uniforms == null) uniforms = device.createBuffer(() -> "OrbitDLC SkyPng settings",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 64);
            var camera = Minecraft.getInstance().gameRenderer.mainCamera();
            var buffer = stack.malloc(64);
            camera.getViewRotationProjectionMatrix(inverse).invert().get(0, buffer);
            var encoder = device.createCommandEncoder();
            encoder.writeToBuffer(uniforms.slice(), buffer);
            try (var pass = encoder.createRenderPass(() -> "OrbitDLC SkyPng", target.getColorTextureView(), Optional.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
                pass.setUniform("SkyImage", texture.getTextureView(),
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                pass.setUniform("SkyPngSettings", uniforms);
                pass.draw(3, 1, 0, 0);
            }
            return true;
        } catch (RuntimeException error) {
            OrbitDLC.LOGGER.warn("SkyPng could not render", error);
            status = "Sky rendering failed";
            setEnabled(false);
            return false;
        }
    }

    private void closeTexture() {
        if (texture != null) texture.close();
        texture = null;
        uploaded = -1;
    }

    @Override protected void onDisable() { close(); }

    void close() {
        closeTexture();
        if (uniforms != null) uniforms.close();
        uniforms = null;
    }
}
