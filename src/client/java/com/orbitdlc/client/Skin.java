package com.orbitdlc.client;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.orbitdlc.OrbitDLC;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Skin extends Module {
    static final Skin INSTANCE = new Skin();
    private static final ClientAsset.ResourceTexture TEXTURE = new ClientAsset.ResourceTexture(OrbitDLC.id("local_skin"));
    private byte[] png;
    private boolean registered;
    private String filename = "";
    private PlayerModelType arms = PlayerModelType.WIDE;

    private Skin() { super("skin", Category.VISUALS); }

    @Override public String description() { return "Choose your local skin and arm model."; }

    public static PlayerSkin apply(PlayerSkin original) {
        var skin = INSTANCE;
        if (!skin.isEnabled()) return original;
        if (skin.png != null && !skin.registered) skin.upload();
        return new PlayerSkin(skin.registered ? TEXTURE : original.body(), original.cape(),
                original.elytra(), skin.arms, original.secure() && !skin.registered);
    }

    private void upload() {
        try {
            var pixels = NativeImage.read(png);
            Minecraft.getInstance().getTextureManager().register(TEXTURE.texturePath(),
                    new DynamicTexture(() -> "OrbitDLC skin", pixels));
            registered = true;
        } catch (IOException | RuntimeException error) {
            release();
            png = null;
            OrbitDLC.LOGGER.warn("Could not upload local skin", error);
        }
    }

    private void release() {
        if (registered) Minecraft.getInstance().getTextureManager().release(TEXTURE.texturePath());
        registered = false;
    }

    @Override protected void onDisable() { release(); }

    @Override public JsonObject saveSettings() {
        var settings = new JsonObject();
        settings.addProperty("arms", arms.name());
        if (png != null) {
            settings.addProperty("png", java.util.Base64.getEncoder().encodeToString(png));
            settings.addProperty("name", filename);
        }
        return settings;
    }

    @Override public void loadSettings(JsonObject settings) {
        var model = settings.has("arms") ? PlayerModelType.valueOf(settings.get("arms").getAsString()) : PlayerModelType.WIDE;
        byte[] data = null;
        if (settings.has("png")) {
            String encoded = settings.get("png").getAsString();
            if (encoded.length() > 90_000) throw new IllegalArgumentException("Skin PNG is too large");
            data = java.util.Base64.getDecoder().decode(encoded);
            verify(data);
        }
        var name = settings.has("name") ? settings.get("name").getAsString() : "";
        release();
        png = data;
        arms = model;
        filename = name;
    }

    private static void verify(byte[] data) {
        if (data.length < 33 || data.length > 65536) throw new IllegalArgumentException("Choose a 64×64 PNG");
        var header = java.nio.ByteBuffer.wrap(data);
        if (header.getLong() != 0x89504E470D0A1A0AL || header.getInt(12) != 0x49484452
                || header.getInt(16) != 64 || header.getInt(20) != 64) {
            throw new IllegalArgumentException("Choose a 64×64 PNG");
        }
        try (var ignored = NativeImage.read(data)) {
            // Validate the complete image before replacing the current skin.
        } catch (IOException error) {
            throw new IllegalArgumentException("Invalid skin PNG", error);
        }
    }

    ModuleSettings settings(ClickGui parent) {
        return new ModuleSettings("Skin", parent) {
            @Override protected void init() {
                chip("Choose skin PNG", 0, button -> Minecraft.getInstance().gui.setScreen(new ImportScreen(parent)));
                choicesAt(List.of("Classic", "Alex"), i -> (arms == PlayerModelType.WIDE) == (i == 0),
                        i -> arms = i == 0 ? PlayerModelType.WIDE : PlayerModelType.SLIM, 26);
                chip("Reset skin", 2, button -> {
                    release();
                    png = null;
                    filename = "";
                });
            }

            @Override protected String summary() { return png == null ? "Account skin" : filename; }
        };
    }

    private final class ImportScreen extends Screen {
        private final Screen parent;
        private GlassInput field;
        private String error = "";

        ImportScreen(Screen parent) {
            super(Component.literal("Choose skin PNG"));
            this.parent = parent;
        }

        @Override protected void init() {
            int w = Math.min(300, width - 20), x = (width - w) / 2, y = height / 2;
            field = new GlassInput(font, x, y, w, 22, "PNG path");
            field.setMaxLength(4096);
            addRenderableWidget(field);
            addRenderableWidget(new OrbitButton(Component.literal("Load"), x, y + 28, w / 2 - 3, 20, button -> {
                try {
                    String input = field.getValue().strip();
                    if (input.length() >= 2 && input.startsWith("\"") && input.endsWith("\""))
                        input = input.substring(1, input.length() - 1);
                    importPng(Path.of(input));
                } catch (java.nio.file.InvalidPathException invalid) {
                    error = "Invalid file path";
                }
            }));
            addRenderableWidget(new OrbitButton(Component.literal("Back"), x + w / 2 + 3, y + 28,
                    w - w / 2 - 3, 20, button -> onClose()));
            setInitialFocus(field);
        }

        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float delta) {
            super.extractRenderState(graphics, x, y, delta);
            graphics.centeredText(font, Component.literal("Drop a 64×64 PNG or paste its path").setStyle(OrbitButton.TYPE),
                    width / 2, height / 2 - 22, OrbitTheme.foreground());
            graphics.centeredText(font, Component.literal(error).setStyle(OrbitButton.TYPE),
                    width / 2, height / 2 + 57, 0xFFFF9AAB);
        }

        @Override public void onFilesDrop(List<Path> files) {
            if (files.size() == 1) importPng(files.getFirst());
            else error = "Choose one PNG";
        }

        private void importPng(Path path) {
            try {
                if (!Files.isRegularFile(path) || Files.size(path) > 65536) {
                    error = "Choose a 64×64 PNG";
                    return;
                }
                byte[] data;
                try (var stream = Files.newInputStream(path)) { data = stream.readNBytes(65537); }
                verify(data);
                release();
                png = data;
                filename = path.getFileName().toString();
                Skin.this.setEnabled(true);
                onClose();
            } catch (IOException | IllegalArgumentException failure) {
                error = failure.getMessage();
            }
        }

        @Override public void onClose() { minecraft.gui.setScreen(parent); }
        @Override public boolean isPauseScreen() { return false; }
    }
}
