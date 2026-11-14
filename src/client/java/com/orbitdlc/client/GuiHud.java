package com.orbitdlc.client;

import brokke.cc.util.animation.Animation;
import brokke.cc.util.animation.Easing;
import brokke.cc.util.render.display.rounded.RoundedRectRenderState;
import com.orbitdlc.OrbitDLC;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

public final class GuiHud extends Module {
    private final KeyMapping shortcut;
    private final Animation entrance = new Animation();
    private final Animation slotSlide = new Animation();
    private final Animation targetHealth = new Animation();
    private final Animation targetEntrance = new Animation();
    private final Animation effectsEntrance = new Animation();
    private LivingEntity previousTarget;
    private boolean slotInitialized;
    private long hotbarShown;
    private static final net.minecraft.resources.Identifier MUSIC_COVER = OrbitDLC.id("music_cover");
    private String coverSource;
    private boolean coverReady;
    private final MusicPlayer music = new MusicPlayer();

    @Override
    protected void onEnable() {
        entrance.set(0f);
        targetEntrance.set(0f);
        effectsEntrance.set(0f);
        slotInitialized = false;
        hotbarShown = System.nanoTime();
        previousTarget = null;
    }

    @Override
    protected void onDisable() {
        stopMusic();
        previousTarget = null;
    }

    enum Widget {
        BRAND("Session status"), EFFECTS("Effects"), BINDS("Keybinds"), TARGET("Target health"),
        HOTBAR("Glass hotbar"), POSITION("Coordinates"), FPS("FPS"), CLOCK("Local time"),
        SPEED("Movement speed"), BIOME("Biome"), MUSIC("Music Player");

        final String label;

        Widget(String label) {
            this.label = label;
        }
    }

    private final java.util.EnumSet<Widget> visible = java.util.EnumSet.range(Widget.BRAND, Widget.HOTBAR);

    @Override
    public com.google.gson.JsonObject saveSettings() {
        var settings = super.saveSettings();
        var widgets = new com.google.gson.JsonArray();
        visible.forEach(widget -> widgets.add(widget.name()));
        settings.add("widgets", widgets);
        return settings;
    }

    @Override
    public void loadSettings(com.google.gson.JsonObject settings) {
        var widgets = java.util.EnumSet.noneOf(Widget.class);
        for (var widget : settings.getAsJsonArray("widgets")) {
            switch (widget.getAsString()) {
                case "ISLAND", "SERVER", "TPS" -> {}
                default -> widgets.add(Widget.valueOf(widget.getAsString()));
            }
        }
        if (!widgets.contains(Widget.MUSIC)) stopMusic();
        visible.clear();
        visible.addAll(widgets);
    }

    ModuleSettings settings(net.minecraft.client.gui.screens.Screen parent) {
        return new ModuleSettings("GUI widgets", parent) {
            @Override
            protected void init() {
                var widgets = Widget.values();
                choices(java.util.Arrays.stream(widgets).map(widget -> widget.label).toList(),
                        index -> visible.contains(widgets[index]), index -> {
                            Widget widget = widgets[index];
                            if (!visible.remove(widget)) visible.add(widget);
                            else if (widget == Widget.MUSIC) stopMusic();
                        });
            }

            @Override
            protected String summary() {
                return "Widgets · " + visible.size() + " / " + Widget.values().length;
            }
        };
    }


    GuiHud(KeyMapping shortcut) {
        super("gui", Category.VISUALS);
        this.shortcut = shortcut;
    }

    @Override
    public String description() {
        return "Glass HUD. Right-click to choose visible widgets.";
    }

    void registerHud() {
        HudElementRegistry.replaceElement(VanillaHudElements.HOTBAR, vanilla -> (graphics, delta) -> {
            var client = Minecraft.getInstance();
            if (!isEnabled() || !visible.contains(Widget.HOTBAR) || client.player == null || client.player.isSpectator()) {
                HudDrag.noHotbar();
                vanilla.extractRenderState(graphics, delta);
                return;
            }
            HudDrag.updateFrame(client);
            hotbar(graphics, client);
        });
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, OrbitDLC.id("glass_hud"), (graphics, delta) -> {
            var client = Minecraft.getInstance();
            HudDrag.updateFrame(client);
            HudDrag.beginPanels();
            if (isEnabled() && client.player != null && !client.player.isSpectator()) {
                entrance.animate(1f, 0f, 8f, Easing.CUBIC_OUT);
                graphics.pose().pushMatrix();
                graphics.pose().translate(-18f * (1f - entrance.value()), 0f);
                try {
                    panels(graphics, client);
                } finally {
                    graphics.pose().popMatrix();
                }
            }
        });
    }

    private void hotbar(GuiGraphicsExtractor graphics, Minecraft client) {
        int slot = Math.min(23, Math.max(17, (graphics.guiWidth() - 20) / 9));
        int barWidth = slot * 9 + 8;
        int x = (graphics.guiWidth() - barWidth) / 2;
        int y = graphics.guiHeight() - slot - 8;
        if (Animations.plays(Animations.Part.HOTBAR))
            y += Math.round(12 * Animations.remaining(hotbarShown, 400));
        var bar = HudDrag.position(HudDrag.Widget.HOTBAR, graphics, x, y, barWidth, slot + 6);
        x = bar.x();
        y = bar.y();
        panel(graphics, x, y, barWidth, slot + 6);
        var inventory = client.player.getInventory();
        int selected = inventory.getSelectedSlot();
        if (!slotInitialized) {
            slotSlide.set(selected);
            slotInitialized = true;
        }
        if (Animations.plays(Animations.Part.SLOT)) slotSlide.animate(selected, 0f, 8f, Easing.CUBIC_OUT);
        else slotSlide.set(selected);
        for (int i = 0; i < 9; i++) {
            shape(graphics, x + 4 + slot * i, y + 3, slot - 2, slot, 7, 0x456C5665);
        }
        int highlight = x + 4 + Math.round(slot * Math.clamp(slotSlide.value(), 0f, 8f));
        shape(graphics, highlight, y + 3, slot - 2, slot, 7, 0xA1D6ACC6);
        shape(graphics, highlight + 4, y + 4, slot - 10, 1, 1, 0xC9F7DFEE);
        for (int i = 0; i < 9; i++) {
            int cell = x + 4 + slot * i;
            var stack = inventory.getItem(i);
            int itemX = cell + (slot - 18) / 2;
            int itemY = y + (slot - 16) / 2 + 3;
            graphics.item(stack, itemX, itemY);
            graphics.itemDecorations(client.font, stack, itemX, itemY);
        }
    }

    private void panels(GuiGraphicsExtractor graphics, Minecraft client) {
        if (visible.contains(Widget.MUSIC)) musicPanel(graphics, client);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (visible.contains(Widget.BRAND)) {
            var server = client.getCurrentServer();
            var connection = client.getConnection();
            var playerInfo = connection == null ? null : connection.getPlayerInfo(client.player.getUUID());
            var fields = new java.util.ArrayList<String>();
            fields.add(client.player.getGameProfile().name());
            fields.add(server == null ? "Local world" : server.ip);
            fields.add(client.getFps() + " FPS");
            if (server != null && playerInfo != null) fields.add(playerInfo.getLatency() + " ms");
            int total = 18;
            for (String field : fields) total += client.font.width(text(field)) + 12;
            int barWidth = Math.min(width - 16, total);
            if (barWidth > 12) {
                var bar = HudDrag.position(HudDrag.Widget.STATUS, graphics, 8, 8, barWidth, 19);
                panel(graphics, bar.x(), bar.y(), barWidth, 19);
                graphics.enableScissor(bar.x(), bar.y(), bar.x() + barWidth - 4, bar.y() + 19);
                graphics.blit(OrbitDLC.id("black_hole.png"), bar.x() + 4, bar.y() + 2, bar.x() + 19, bar.y() + 17, 0f, 1f, 0f, 1f);
                int x = bar.x() + 20;
                for (String field : fields) {
                    draw(graphics, client, field, x, bar.y() + 5, TEXT);
                    x += client.font.width(text(field)) + 4;
                    if (!field.equals(fields.getLast())) {
                        draw(graphics, client, "·", x, bar.y() + 5, Theme.color(ACCENT));
                    }
                    x += 8;
                }
                graphics.disableScissor();
            }
        }

        int y = 36;
        if (visible.contains(Widget.BINDS) && (shortcut.isDown() || editing(client))) {
            String key = shortcut.getTranslatedKeyMessage().getString();
            int cardWidth = Math.max(110, client.font.width(text(key)) + client.font.width(text("ClickGUI")) + 25);
            var box = HudDrag.position(HudDrag.Widget.KEYS, graphics, 12, y, cardWidth, 34);
            card(graphics, client, box.x(), box.y(), cardWidth, 1, "Keybinds");
            row(graphics, client, box.x(), box.y() + 20, cardWidth, "ClickGUI", key);
            y += 42;
        }

        if (visible.contains(Widget.EFFECTS)) {
            var effects = client.player.getActiveEffects().stream()
                    .sorted(java.util.Comparator.comparing(effect -> effect.getEffect().value().getDescriptionId()))
                    .limit(Math.clamp((height - y - 65) / 12, 0, 8)).toList();
            if (!effects.isEmpty() || editing(client)) {
                int cardWidth = Math.min(160, width - 24);
                if (cardWidth >= 60) {
                    if (Animations.plays(Animations.Part.EFFECTS)) effectsEntrance.animate(1f, 0f, 5f, Easing.CUBIC_OUT);
                    else effectsEntrance.set(1f);
                    int cardX = 12 - Math.round(10f * (1f - effectsEntrance.value()));
                    var box = HudDrag.position(HudDrag.Widget.EFFECTS, graphics, cardX, y, cardWidth, 22 + effects.size() * 12);
                    cardX = box.x();
                    card(graphics, client, cardX, box.y(), cardWidth, effects.size(), "Effects");
                    int rowY = box.y() + 20;
                    for (var effect : effects) {
                        String name = effect.getEffect().value().getDisplayName().getString();
                        if (effect.getAmplifier() > 0) name += " " + (effect.getAmplifier() + 1);
                        int seconds = Math.max(0, effect.getDuration() / 20);
                        String duration = effect.isInfiniteDuration() ? "∞" :
                                String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
                        row(graphics, client, cardX, rowY, cardWidth, name, duration);
                        rowY += 12;
                    }
                }
            } else { effectsEntrance.set(0f); }
        }

        int infoY = 8;
        var player = client.player;
        if (visible.contains(Widget.FPS) && !visible.contains(Widget.BRAND)) infoY = info(graphics, client, infoY, client.getFps() + " FPS");
        if (visible.contains(Widget.CLOCK)) {
            infoY = info(graphics, client, infoY, java.time.LocalTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        }
        if (visible.contains(Widget.POSITION)) {
            var pos = player.blockPosition();
            infoY = stat(graphics, client, HudDrag.Widget.COORDS, infoY,
                    pos.getX() + " / " + pos.getY() + " / " + pos.getZ());
        }
        if (visible.contains(Widget.SPEED)) {
            double speed = player.getDeltaMovement().horizontalDistance() * 20;
            infoY = info(graphics, client, infoY, String.format(java.util.Locale.ROOT, "%.1f blocks/s", speed));
        }
        if (visible.contains(Widget.BIOME) && client.level != null) {
            var biome = client.level.getBiome(player.blockPosition()).unwrapKey();
            if (biome.isPresent()) info(graphics, client, infoY, biome.get().identifier().getPath().replace('_', ' '));
        }

        LivingEntity target = client.hitResult instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity living ? living : null;
        if (target == null && editing(client)) {
            target = client.player;
        }
        if (visible.contains(Widget.TARGET) && target != null) {
            if (target != previousTarget) {
                targetEntrance.set(0f);
                targetHealth.set(0f);
                previousTarget = target;
            }
            if (Animations.plays(Animations.Part.TARGET)) targetEntrance.animate(1f, 0f, 5f, Easing.CUBIC_OUT);
            else targetEntrance.set(1f);
            int x = width - 151 + Math.round(14f * (1f - targetEntrance.value()));
            int targetY = Math.max(38, height * 2 / 3);
            var box = HudDrag.position(HudDrag.Widget.TARGET, graphics, x, targetY, 136, 37);
            x = box.x();
            targetY = box.y();
            panel(graphics, x, targetY, 136, 37);
            graphics.text(client.font, text(target.getDisplayName().getString()), x + 10, targetY + 7, 0xFFF5F8FF, false);
            shape(graphics, x + 10, targetY + 22, 116, 5, 2, 0x6D8298BA);
            float health = Math.clamp(target.getHealth() / Math.max(1f, target.getMaxHealth()), 0f, 1f);
            if (Animations.plays(Animations.Part.TARGET)) targetHealth.animate(health, 0f, 8f, Easing.CUBIC_OUT);
            else targetHealth.set(health);
            int fill = Math.round(116 * Math.clamp(targetHealth.value(), 0f, 1f));
            if (fill > 0) shape(graphics, x + 10, targetY + 22, fill, 5, 2, OrbitTheme.accent());
        } else { previousTarget = null; }
    }

    private static final int TEXT = 0xFFF4F5FA;
    private static final int ACCENT = 0xFFF2CFE4;

    private static void card(GuiGraphicsExtractor graphics, Minecraft client,
                             int x, int y, int width, int rows, String title) {
        panel(graphics, x, y, width, 22 + rows * 12);
        draw(graphics, client, title, x + 8, y + 6, TEXT);
        shape(graphics, x + width - 12, y + 7, 4, 4, 2, Theme.color(ACCENT));
        graphics.fill(x + 7, y + 17, x + width - 7, y + 18, 0x34B8C5EE);
    }

    private void musicPanel(GuiGraphicsExtractor graphics, Minecraft client) {
        int width = Math.min(172, graphics.guiWidth() - 16);
        if (width < 122) return;
        var track = music.poll();
        boolean editing = editing(client);
        updateMusicCover(client, track.cover());
        if (!track.hasTitle() && !editing) return;
        var box = HudDrag.position(HudDrag.Widget.MUSIC, graphics, 12, graphics.guiHeight() - 112, width, 62);
        int x = box.x();
        int y = box.y();
        panel(graphics, x, y, width, 62);
        graphics.enableScissor(x + 7, y + 3, x + width - 7, y + 59);
        try {
            draw(graphics, client, "Music Player", x + 9, y + 6, Theme.color(ACCENT));
            if (coverReady) {
                graphics.blit(MUSIC_COVER, x + 9, y + 20, x + 46, y + 57, 0f, 1f, 0f, 1f);
            } else {
                shape(graphics, x + 9, y + 20, 37, 37, 6, 0x665F5260);
                musicIcon(graphics, client, "note", x + 19, y + 30, 17);
            }
            String title = track.hasTitle() ? track.title() : track.error() ? "Media service unavailable" : "No active track";
            draw(graphics, client, title, x + 53, y + 19, TEXT);
            if (track.artist() != null) draw(graphics, client, track.artist(), x + 53, y + 30, TEXT);
            musicButton(graphics, client, x + 53, y + 43, 18, "previous", track.previous(), false);
            musicButton(graphics, client, x + 74, y + 43, 19, track.playing() ? "pause" : "play", track.toggle(), true);
            musicButton(graphics, client, x + 96, y + 43, 18, "next", track.next(), false);
        } finally {
            graphics.disableScissor();
        }
    }

    private void stopMusic() {
        music.stop();
        updateMusicCover(Minecraft.getInstance(), null);
    }

    private void updateMusicCover(Minecraft client, String encoded) {
        if (java.util.Objects.equals(coverSource, encoded)) return;
        var textures = client.getTextureManager();
        if (coverReady) textures.release(MUSIC_COVER);
        coverReady = false;
        coverSource = encoded;
        if (encoded == null || encoded.length() > 64_000) return;
        try {
            var pixels = com.mojang.blaze3d.platform.NativeImage.read(java.util.Base64.getDecoder().decode(encoded));
            textures.register(MUSIC_COVER, new net.minecraft.client.renderer.texture.DynamicTexture(
                    () -> "OrbitDLC music cover", pixels));
            coverReady = true;
        } catch (java.io.IOException | IllegalArgumentException ignored) {
            // Keep showing the music icon if the application supplies a malformed image.
        }
    }

    private static void musicButton(GuiGraphicsExtractor graphics, Minecraft client, int x, int y, int w,
                                    String icon, boolean enabled, boolean primary) {
        shape(graphics, x, y, w, 14, 5, enabled ? (primary ? 0x99765872 : 0x665F5260) : 0x333E3942);
        musicIcon(graphics, client, icon, x + (w - 10) / 2, y + 2, 10);
    }

    private static void musicIcon(GuiGraphicsExtractor graphics, Minecraft client, String icon, int x, int y, int size) {
        var texture = client.getTextureManager().getTexture(OrbitDLC.id("music/" + icon + ".png"));
        var sampler = com.mojang.blaze3d.systems.RenderSystem.getSamplerCache()
                .getClampToEdge(com.mojang.renderpearl.api.textures.FilterMode.LINEAR);
        graphics.blit(texture.getTextureView(), sampler, x, y, x + size, y + size, 0f, 1f, 0f, 1f);
    }

    boolean clickMusic(HudDrag.Bounds box, double x, double y) {
        if (!isEnabled() || !visible.contains(Widget.MUSIC)) return false;
        double localX = x - box.x();
        double localY = y - box.y();
        if (localY < 43 || localY >= 57) return false;
        var track = music.current();
        if (localX >= 53 && localX < 71 && track.previous()) music.control("previous");
        else if (localX >= 74 && localX < 93 && track.toggle()) music.control("toggle");
        else if (localX >= 96 && localX < 114 && track.next()) music.control("next");
        else return false;
        return true;
    }

    private static void row(GuiGraphicsExtractor graphics, Minecraft client,
                            int x, int y, int width, String label, String value) {
        int suffixWidth = client.font.width(text(value));
        String fitted = client.font.substrByWidth(text(label), Math.max(0, width - 22 - suffixWidth)).getString();
        draw(graphics, client, fitted, x + 8, y, TEXT);
        draw(graphics, client, value, x + width - 8 - suffixWidth, y, Theme.color(ACCENT));
    }

    private static void draw(GuiGraphicsExtractor graphics, Minecraft client, String label, int x, int y, int color) {
        graphics.text(client.font, text(label), x, y, color, false);
    }
    private static boolean editing(Minecraft client) {
        return client.gui.screen() instanceof ClickGui
                || client.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen;
    }

    private static int stat(GuiGraphicsExtractor graphics, Minecraft client, HudDrag.Widget widget, int y, String value) {
        int w = Math.min(graphics.guiWidth() - 16, client.font.width(text(value)) + 18);
        if (w < 18) return y;
        var box = HudDrag.position(widget, graphics, graphics.guiWidth() - w - 8, y, w, 20);
        panel(graphics, box.x(), box.y(), w, 20);
        draw(graphics, client, client.font.substrByWidth(text(value), w - 14).getString(), box.x()+7, box.y()+5, TEXT);
        return y + 24;
    }

    private static int info(GuiGraphicsExtractor graphics, Minecraft client, int y, String value) {
        int w = client.font.width(text(value)) + 22;
        int x = graphics.guiWidth() - w - 12;
        panel(graphics, x, y, w, 22);
        graphics.text(client.font, text(value), x + 11, y + 7, 0xFFF5F8FF, false);
        return y + 27;
    }
    private static Component text(String value) {
        return Component.literal(value).setStyle(OrbitButton.TYPE);
    }

    private static void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        int radius = Math.min(11, h / 2);
        shape(graphics, x, y + 2, w, h, radius, 0x52090B13);
        GlassBackdrop.submit(graphics, x, y, w, h, radius);
    }

    private static void shape(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int radius, int color) {
        RoundedRectRenderState.submit(graphics, x, y, w, h, radius, color);
    }
}
