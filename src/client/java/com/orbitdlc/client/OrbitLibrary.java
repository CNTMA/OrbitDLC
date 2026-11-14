package com.orbitdlc.client;

import com.orbitdlc.client.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import java.util.ArrayList;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.List;

final class OrbitLibrary {
    private static final int INPUT_TOP = 34;
    private static final int INPUT_HEIGHT = 20;
    private static final int INPUT_TEXT_HEIGHT = 9;
    enum Tab {
        VISUALS("Visuals"), UTILITIES("Utilities"), FRIENDS("Friends"), CONFIGS("Configs"), THEMES("Themes"),
        COSMETICS("Cosmetics");

        final String label;

        Tab(String label) { this.label = label; }

        com.orbitdlc.client.module.Category category() {
            return switch (this) {
                case VISUALS -> com.orbitdlc.client.module.Category.VISUALS;
                case UTILITIES -> com.orbitdlc.client.module.Category.UTILITIES;
                default -> null;
            };
        }
    }

    private final ClickGui host;
    private final List<AbstractWidget> controls = new ArrayList<>();
    private Font font;
    private final ModuleManager modules;
    private final Tab tab;
    private int page;
    private int left, top, panelWidth, panelHeight;
    private EditBox profileName;
    private EditBox accentColor, glassColor;
    private String status = "";
    private List<String> files = List.of();
    private List<Cosmetics.Entry> cosmetics = List.of();
    private int selectedCosmetic;

    OrbitLibrary(ClickGui host, ModuleManager modules, Tab tab) {
        this.host = host;
        this.modules = modules;
        this.tab = tab;
        if (hasList()) refreshFiles();
        if (tab == Tab.COSMETICS) cosmetics = Cosmetics.catalog();
    }

    private boolean hasList() { return tab == Tab.CONFIGS || tab == Tab.FRIENDS; }

    private void refreshFiles() {
        try {
            files = tab == Tab.FRIENDS ? OrbitFriends.names() : OrbitProfiles.list(OrbitProfiles.PROFILES, ".json");
        } catch (IOException error) {
            status = errorMessage(error);
        }
    }

    void layout(Font font, int x, int y, int width, int height) {
        this.font = font;
        left = x;
        top = y;
        panelWidth = width;
        panelHeight = height;
        controls.clear();
        switch (tab) {
            case VISUALS, UTILITIES -> throw new IllegalStateException();
            case COSMETICS -> layoutCosmetics();
            case CONFIGS, FRIENDS -> {
                boolean friends = tab == Tab.FRIENDS;
                String hint = friends ? "Player name" : "Profile name";
                String previousName = profileName == null ? "" : profileName.getValue();
                int textY = top + INPUT_TOP + (INPUT_HEIGHT - INPUT_TEXT_HEIGHT) / 2;
                profileName = new EditBox(font, left + 14, textY, panelWidth - 107,
                        top + INPUT_TOP + INPUT_HEIGHT - textY, Component.literal(hint));
                profileName.setBordered(false);
                profileName.addFormatter((value, offset) -> Component.literal(value).setStyle(OrbitButton.TYPE).getVisualOrderText());
                profileName.setMaxLength(friends ? 16 : 32);
                profileName.setValue(previousName);
                profileName.setHint(Component.literal(hint).setStyle(OrbitButton.TYPE));
                addControl(profileName);
                button(friends ? "Add friend" : "New save", panelWidth - 84, 34, 74, () -> {
                    if (friends) {
                        perform(() -> {
                            OrbitFriends.add(profileName.getValue().strip());
                            profileName.setValue("");
                        }, "Friend added", true);
                    } else {
                        perform(() -> OrbitProfiles.save(OrbitProfiles.profile(profileName.getValue()), modules, false), "Profile created", true);
                    }
                });
            }
            case THEMES -> layoutThemes();
        }
        if (hasList()) addFileRows();
    }

    private void layoutThemes() {
        int column = (panelWidth - 26) / 2;
        var themes = OrbitTheme.values();
        for (int i = 0; i < themes.length; i++) {
            var theme = themes[i];
            addControl(new OrbitButton(Component.literal(theme.label), left + 10 + (i % 2) * (column + 6),
                    top + 30 + (i / 2) * 22, column, 20,
                    control -> OrbitTheme.select(theme), () -> OrbitTheme.current() == theme));
        }
        button(OrbitTheme.flowing() ? "Color flow: On" : "Color flow: Off", 16 + column, 74, column, () -> {
            OrbitTheme.toggleFlow();
            host.refreshTab();
        });
        accentColor = themeField(accentColor, "Accent", 10, column, OrbitTheme.customAccent());
        glassColor = themeField(glassColor, "Glass tint", 16 + column, column, OrbitTheme.customTint());
        button("Apply custom colors", 10, 139, panelWidth - 20, () ->
                perform(() -> OrbitTheme.custom(accentColor.getValue(), glassColor.getValue()), "Custom colors applied", false));
    }

    private EditBox themeField(EditBox previous, String label, int x, int width, int color) {
        var field = new EditBox(font, left + x + 5, top + 121, width - 10, 14, Component.literal(label));
        field.setBordered(false);
        field.setMaxLength(7);
        field.addFormatter((value, offset) -> Component.literal(value).setStyle(OrbitButton.TYPE).getVisualOrderText());
        field.setValue(previous == null ? OrbitTheme.hex(color) : previous.getValue());
        return addControl(field);
    }

    private void layoutCosmetics() {
        if (cosmetics.isEmpty()) return;
        button("<", 10, panelHeight - 29, 28, () -> browseCosmetics(-1));
        button(">", panelWidth - 38, panelHeight - 29, 28, () -> browseCosmetics(1));
        var entry = cosmetics.get(selectedCosmetic);
        button(Cosmetics.equipped(entry) ? "Unequip" : "Equip", panelWidth - 82, panelHeight - 78, 72,
                () -> perform(() -> Cosmetics.toggle(entry), "", true));
    }

    private void browseCosmetics(int direction) {
        selectedCosmetic = Math.floorMod(selectedCosmetic + direction, cosmetics.size());
        status = "";
        host.refreshTab();
    }

    private void renderCosmetics(GuiGraphicsExtractor graphics) {
        if (cosmetics.isEmpty()) {
            draw(graphics, "No cosmetics found", 12, 40, 0xFFF4F5FA, panelWidth - 24);
            return;
        }
        var entry = cosmetics.get(selectedCosmetic);
        int previewHeight = panelHeight - 110;
        GlassBackdrop.submit(graphics, left + 10, top + 29, panelWidth - 20, previewHeight, 11);
        CosmeticPreview.draw(graphics, entry.id(), left + 20, top + 33, panelWidth - 40, previewHeight - 8);
        draw(graphics, entry.name(), 12, panelHeight - 74, 0xFFF4F5FA, panelWidth - 96);
        draw(graphics, entry.category(), 12, panelHeight - 58, 0xFFBAC7C8, panelWidth - 24);
        String counter = (selectedCosmetic + 1) + " / " + cosmetics.size();
        graphics.centeredText(font, Component.literal(counter).setStyle(OrbitButton.TYPE),
                left + panelWidth / 2, top + panelHeight - 23, 0xFFF4F5FA);
    }

    private void addFileRows() {
        int capacity = Math.max(0, (panelHeight - 108) / 23);
        if (capacity == 0) return;
        int last = Math.max(0, (files.size() - 1) / capacity);
        page = Math.clamp(page, 0, last);
        for (int i = page * capacity; i < Math.min((page + 1) * capacity, files.size()); i++) {
            String file = files.get(i);
            int y = 59 + (i % capacity) * 23;
            if (tab == Tab.FRIENDS) {
                button("Remove", panelWidth - 70, y, 60,
                        () -> perform(() -> OrbitFriends.remove(file), "Friend removed", true));
                continue;
            }
            var path = OrbitProfiles.PROFILES.resolve(file);
            button("Load", panelWidth - 105, y, 44, () -> perform(() -> OrbitProfiles.load(path, modules), "Loaded " + file, false));
            button("Save", panelWidth - 57, y, 47, () -> perform(() -> OrbitProfiles.save(path, modules, true), "Saved " + file, false));
        }
        if (last > 0) {
            button("<", 10, panelHeight - 25, 24, () -> movePage(-1)).active = page > 0;
            button(">", 38, panelHeight - 25, 24, () -> movePage(1)).active = page < last;
        }
    }

    private void movePage(int direction) {
        page += direction;
        host.refreshTab();
    }

    private <T extends AbstractWidget> T addControl(T widget) {
        controls.add(widget);
        return host.addTabControl(widget);
    }

    private OrbitButton button(String label, int x, int y, int width, Runnable action) {
        return addControl(new OrbitButton(Component.literal(label), left + x, top + y, width, 20,
                button -> action.run()));
    }

    private void perform(FileAction action, String success, boolean refresh) {
        try {
            action.run();
            status = success;
            if (refresh && hasList()) refreshFiles();
        } catch (IOException | RuntimeException error) {
            status = errorMessage(error);
        }
        if (refresh) host.refreshTab();
    }

    private String errorMessage(Throwable error) {
        if (error instanceof java.nio.file.FileAlreadyExistsException) return "This file already exists";
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage().lines().findFirst().orElse("Error");
    }

    @FunctionalInterface
    private interface FileAction { void run() throws IOException; }

    void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (hasList()) GlassBackdrop.submit(graphics, left + 10, top + INPUT_TOP, panelWidth - 99, INPUT_HEIGHT, 6);
        draw(graphics, tab.label, 12, 12, OrbitTheme.accent(), panelWidth - 24);
        if (tab == Tab.COSMETICS) renderCosmetics(graphics);
        if (tab == Tab.THEMES) {
            int column = (panelWidth - 26) / 2;
            draw(graphics, "Accent", 12, 103, OrbitTheme.accent(), column);
            draw(graphics, "Glass tint", 18 + column, 103, OrbitTheme.accent(), column);
            GlassBackdrop.submit(graphics, left + 10, top + 116, column, 19, 6);
            GlassBackdrop.submit(graphics, left + 16 + column, top + 116, column, 19, 6);
        }
        if (hasList()) {
            int capacity = Math.max(0, (panelHeight - 108) / 23);
            for (int i = page * capacity; i < Math.min((page + 1) * capacity, files.size()); i++) {
                draw(graphics, files.get(i), 12, 65 + (i % capacity) * 23, 0xFFF4F5FA,
                        panelWidth - (tab == Tab.FRIENDS ? 86 : 123));
            }
            if (files.isEmpty()) {
                draw(graphics, tab == Tab.FRIENDS ? "No friends added" : "No saved profiles",
                        12, 65, 0xFFF4F5FA, panelWidth - 24);
            }
        }
        if (tab == Tab.COSMETICS) draw(graphics, status, 12, panelHeight - 44, OrbitTheme.accent(), panelWidth - 24);
        else draw(graphics, status, 69, panelHeight - 19, OrbitTheme.accent(), panelWidth - 79);
        controls.forEach(widget -> widget.extractRenderState(graphics, mouseX, mouseY, delta));
    }

    private void draw(GuiGraphicsExtractor graphics, String value, int x, int y, int color, int maxWidth) {
        var text = Component.literal(value).setStyle(OrbitButton.TYPE);
        var clipped = font.substrByWidth(text, Math.max(0, maxWidth));
        graphics.text(font, Component.literal(clipped.getString()).setStyle(OrbitButton.TYPE),
                left + x, top + y, color, false);
    }
}
