package com.orbitdlc.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.EnumMap;

final class HudDrag {
    enum Widget { STATUS, KEYS, EFFECTS, TARGET, HOTBAR, METRICS, MUSIC, ISLAND, COORDS, SERVER, TPS }
    record Bounds(int x, int y, int w, int h) {
        boolean contains(double px, double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    private static final EnumMap<Widget, Bounds> bounds = new EnumMap<>(Widget.class);
    private static final EnumMap<Widget, Bounds> offsets = new EnumMap<>(Widget.class);
    private static Widget selected;
    private static double startX, startY;
    private static int offsetX, offsetY, screenWidth, screenHeight;

    static com.google.gson.JsonObject savePositions() {
        var positions = new com.google.gson.JsonObject();
        offsets.forEach((widget, offset) -> {
            var position = new com.google.gson.JsonArray();
            position.add(offset.x());
            position.add(offset.y());
            positions.add(widget.name(), position);
        });
        return positions;
    }

    static void loadPositions(com.google.gson.JsonObject positions) {
        var restored = new EnumMap<Widget, Bounds>(Widget.class);
        for (var widget : Widget.values()) {
            if (!positions.has(widget.name())) continue;
            var position = positions.getAsJsonArray(widget.name());
            restored.put(widget, new Bounds(Math.clamp(position.get(0).getAsInt(), -8192, 8192),
                    Math.clamp(position.get(1).getAsInt(), -8192, 8192), 0, 0));
        }
        offsets.clear();
        offsets.putAll(restored);
    }

    static void register(GuiHud hud) {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof ChatScreen)) return;
            selected = null;
            bounds.clear();
            ScreenMouseEvents.allowMouseClick(screen).register((current, event) -> {
                if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;
                return !startDrag(hud, event.x(), event.y());
            });
            ScreenMouseEvents.allowMouseRelease(screen).register((current, event) -> {
                if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || selected == null) return true;
                selected = null;
                return false;
            });
            ScreenEvents.remove(screen).register(current -> {
                selected = null;
                bounds.clear();
            });
        });
    }

    static boolean startDrag(GuiHud hud, double x, double y) {
        for (var entry : bounds.entrySet()) {
            if (!entry.getValue().contains(x, y)) continue;
            if (entry.getKey() == Widget.MUSIC && hud.clickMusic(entry.getValue(), x, y)) return true;
            selected = entry.getKey();
            var offset = offsets.getOrDefault(selected, new Bounds(0, 0, 0, 0));
            offsetX = offset.x();
            offsetY = offset.y();
            startX = x;
            startY = y;
            return true;
        }
        return false;
    }

    static boolean finishDrag() {
        if (selected == null) return false;
        selected = null;
        return true;
    }

    static void updateFrame(net.minecraft.client.Minecraft client) {
        if (selected == null || !(client.gui.screen() instanceof ChatScreen || client.gui.screen() instanceof ClickGui)) return;
        var window = client.getWindow();
        update(client.mouseHandler.getScaledXPos(window), client.mouseHandler.getScaledYPos(window));
    }
    static void update(double x, double y) {
        if (selected == null) return;
        var box = bounds.get(selected);
        if (box == null) return;
        var previous = offsets.getOrDefault(selected, new Bounds(0, 0, 0, 0));
        int baseX = box.x() - previous.x();
        int baseY = box.y() - previous.y();
        int dx = Math.clamp(offsetX + (int) Math.round(x - startX), -baseX,
                Math.max(0, screenWidth - box.w()) - baseX);
        int dy = Math.clamp(offsetY + (int) Math.round(y - startY), -baseY,
                Math.max(0, screenHeight - box.h() - 16) - baseY);
        offsets.put(selected, new Bounds(dx, dy, 0, 0));
    }

    static Bounds position(Widget widget, GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        screenWidth = graphics.guiWidth();
        screenHeight = graphics.guiHeight();
        var offset = offsets.get(widget);
        if (offset != null) {
            x = Math.clamp(x + offset.x(), 0, Math.max(0, screenWidth - w));
            y = Math.clamp(y + offset.y(), 0, Math.max(0, screenHeight - h));
        }
        var result = new Bounds(x, y, w, h);
        bounds.put(widget, result);
        return result;
    }

    static void beginPanels() {
        bounds.keySet().removeIf(widget -> widget != Widget.HOTBAR);
    }

    static void noHotbar() {
        bounds.remove(Widget.HOTBAR);
    }
}
