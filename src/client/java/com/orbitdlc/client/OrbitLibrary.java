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
    enum Tab {
        VISUALS("Visuals"), CONFIGS("Configs"), THEMES("Themes");

        final String label;

        Tab(String label) { this.label = label; }
    }

    private final ClickGui host;
    private final List<AbstractWidget> controls = new ArrayList<>();
    private Font font;
    private final ModuleManager modules;
    private final Tab tab;
    private int page;
    private int left, top, panelWidth, panelHeight;
    private EditBox profileName;
    private String status = "";
    private List<String> files = List.of();

    OrbitLibrary(ClickGui host, ModuleManager modules, Tab tab) {
        this.host = host;
        this.modules = modules;
        this.tab = tab;
        if (tab == Tab.CONFIGS) refreshFiles();
    }

    private void refreshFiles() {
        try {
            files = OrbitProfiles.list(OrbitProfiles.PROFILES, ".json");
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
            case VISUALS -> throw new IllegalStateException();
            case CONFIGS -> {
                String previousName = profileName == null ? "" : profileName.getValue();
                profileName = new EditBox(font, left + 14, top + 35, panelWidth - 107, 18, Component.literal("Profile name"));
                profileName.setBordered(false);
                profileName.addFormatter((value, offset) -> Component.literal(value).setStyle(OrbitButton.TYPE).getVisualOrderText());
                profileName.setMaxLength(32);
                profileName.setValue(previousName);
                profileName.setHint(Component.literal("Profile name").setStyle(OrbitButton.TYPE));
                addControl(profileName);
                button("New save", panelWidth - 84, 34, 74,
                        () -> perform(() -> OrbitProfiles.save(OrbitProfiles.profile(profileName.getValue()), modules, false), "Profile created", true));
            }
            case THEMES -> {
                int rowY = 34;
                for (OrbitTheme theme : OrbitTheme.values()) {
                    addControl(new OrbitButton(Component.literal(theme.label), left + 10, top + rowY, panelWidth - 20, 20,
                            button -> OrbitTheme.select(theme), () -> OrbitTheme.current() == theme));
                    rowY += 22;
                }
            }
        }
        if (tab == Tab.CONFIGS) addFileRows();
    }

    private void addFileRows() {
        int capacity = Math.max(0, (panelHeight - 108) / 23);
        if (capacity == 0) return;
        int last = Math.max(0, (files.size() - 1) / capacity);
        page = Math.clamp(page, 0, last);
        for (int i = page * capacity; i < Math.min((page + 1) * capacity, files.size()); i++) {
            String file = files.get(i);
            int y = 59 + (i % capacity) * 23;
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
            if (refresh) refreshFiles();
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
        if (tab == Tab.CONFIGS) GlassBackdrop.submit(graphics, left + 10, top + 34, panelWidth - 99, 20, 6);
        draw(graphics, tab.label, 12, 12, OrbitTheme.accent(), panelWidth - 24);
        if (tab == Tab.CONFIGS) {
            int capacity = Math.max(0, (panelHeight - 108) / 23);
            for (int i = page * capacity; i < Math.min((page + 1) * capacity, files.size()); i++) {
                draw(graphics, files.get(i), 12, 65 + (i % capacity) * 23, 0xFFF4F5FA,
                        panelWidth - 123);
            }
            if (files.isEmpty()) {
                draw(graphics, "No saved profiles", 12, 65, 0xFFF4F5FA, panelWidth - 24);
            }
        }
        draw(graphics, status, 69, panelHeight - 19, OrbitTheme.accent(), panelWidth - 79);
        controls.forEach(widget -> widget.extractRenderState(graphics, mouseX, mouseY, delta));
    }

    private void draw(GuiGraphicsExtractor graphics, String value, int x, int y, int color, int maxWidth) {
        var text = Component.literal(value).setStyle(OrbitButton.TYPE);
        var clipped = font.substrByWidth(text, Math.max(0, maxWidth));
        graphics.text(font, Component.literal(clipped.getString()).setStyle(OrbitButton.TYPE),
                left + x, top + y, color, false);
    }
}
