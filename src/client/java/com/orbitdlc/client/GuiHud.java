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

    @Override
    protected void onEnable() {
        entrance.set(0f);
        targetEntrance.set(0f);
        effectsEntrance.set(0f);
        slotInitialized = false;
        previousTarget = null;
    }

    @Override
    protected void onDisable() {
        previousTarget = null;
    }

    private enum Widget {
        BRAND("Session status"), EFFECTS("Effects"), BINDS("Keybinds"), TARGET("Target health"),
        HOTBAR("Glass hotbar"), POSITION("Coordinates"), FPS("FPS"), CLOCK("Local time"),
        SPEED("Movement speed"), BIOME("Biome");

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
            widgets.add(Widget.valueOf(widget.getAsString()));
        }
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
        slotSlide.animate(selected, 0f, 13f, Easing.CUBIC_OUT);
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
                        draw(graphics, client, "·", x, bar.y() + 5, ACCENT);
                    }
                    x += 8;
                }
                graphics.disableScissor();
            }
        }

        int y = 36;
        if (visible.contains(Widget.BINDS) && (shortcut.isDown() || client.gui.screen() instanceof ClickGui)) {
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
            if (!effects.isEmpty()) {
                int cardWidth = Math.min(160, width - 24);
                if (cardWidth >= 60) {
                    effectsEntrance.animate(1f, 0f, 8f, Easing.CUBIC_OUT);
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
            infoY = info(graphics, client, infoY, pos.getX() + " / " + pos.getY() + " / " + pos.getZ());
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
        if (target == null && client.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen) {
            target = client.player;
        }
        if (visible.contains(Widget.TARGET) && target != null) {
            if (target != previousTarget) {
                targetEntrance.set(0f);
                targetHealth.set(0f);
                previousTarget = target;
            }
            targetEntrance.animate(1f, 0f, 8f, Easing.CUBIC_OUT);
            int x = width - 151 + Math.round(14f * (1f - targetEntrance.value()));
            int targetY = Math.max(38, height * 2 / 3);
            var box = HudDrag.position(HudDrag.Widget.TARGET, graphics, x, targetY, 136, 37);
            x = box.x();
            targetY = box.y();
            panel(graphics, x, targetY, 136, 37);
            graphics.text(client.font, text(target.getDisplayName().getString()), x + 10, targetY + 7, 0xFFF5F8FF, false);
            shape(graphics, x + 10, targetY + 22, 116, 5, 2, 0x6D8298BA);
            float health = Math.clamp(target.getHealth() / Math.max(1f, target.getMaxHealth()), 0f, 1f);
            targetHealth.animate(health, 0f, 10f, Easing.CUBIC_OUT);
            int fill = Math.round(116 * Math.clamp(targetHealth.value(), 0f, 1f));
            if (fill > 0) shape(graphics, x + 10, targetY + 22, fill, 5, 2, 0xFFE3EDFF);
        } else { previousTarget = null; }
    }

    private static final int TEXT = 0xFFF4F5FA;
    private static final int ACCENT = 0xFFF2CFE4;

    private static void card(GuiGraphicsExtractor graphics, Minecraft client,
                             int x, int y, int width, int rows, String title) {
        panel(graphics, x, y, width, 22 + rows * 12);
        draw(graphics, client, title, x + 8, y + 6, TEXT);
        shape(graphics, x + width - 12, y + 7, 4, 4, 2, ACCENT);
        graphics.fill(x + 7, y + 17, x + width - 7, y + 18, 0x34B8C5EE);
    }

    private static void row(GuiGraphicsExtractor graphics, Minecraft client,
                            int x, int y, int width, String label, String value) {
        int suffixWidth = client.font.width(text(value));
        String fitted = client.font.substrByWidth(text(label), Math.max(0, width - 22 - suffixWidth)).getString();
        draw(graphics, client, fitted, x + 8, y, TEXT);
        draw(graphics, client, value, x + width - 8 - suffixWidth, y, ACCENT);
    }

    private static void draw(GuiGraphicsExtractor graphics, Minecraft client, String label, int x, int y, int color) {
        graphics.text(client.font, text(label), x, y, color, false);
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
