package com.orbitdlc.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;

final class AccountSwitcher extends Screen {
    private final MainMenu parent;
    private EditBox name;
    private String status = "";
    private int left, top, panelWidth;

    AccountSwitcher(MainMenu parent) {
        super(Component.literal("Account Switcher"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(270, width - 16);
        left = (width - panelWidth) / 2;
        top = Math.max(4, (height - 144) / 2);
        var current = LocalAccounts.active();
        String draft = name == null ? (current == null ? "" : current.getName()) : name.getValue();
        name = new EditBox(font, left + 16, top + 53, panelWidth - 32, 18, Component.literal("Local username"));
        name.setBordered(false);
        name.setMaxLength(16);
        name.setValue(draft);
        name.setHint(Component.literal("Local username").setStyle(OrbitButton.TYPE));
        name.addFormatter((text, offset) -> Component.literal(text).setStyle(OrbitButton.TYPE).getVisualOrderText());
        addRenderableWidget(name);
        int half = (panelWidth - 28) / 2;
        button("Use local profile", left + 11, top + 78, half, () -> apply(name.getValue().strip()));
        button("Launcher account", left + 17 + half, top + 78, half, () -> apply(null));
        button("Back", left + panelWidth - 57, top + 8, 46, this::onClose);
    }

    private void button(String label, int x, int y, int width, Runnable action) {
        addRenderableWidget(new OrbitButton(Component.literal(label), x, y, width, 18, ignored -> action.run()) {
            @Override
            protected void background(GuiGraphicsExtractor graphics, int color) {
                GlassBackdrop.menu(graphics, getX(), getY(), getWidth(), getHeight());
            }
        });
    }

    private void apply(String username) {
        if (username != null && !LocalAccounts.valid(username)) {
            status = "Use 3–16 letters, digits or underscores";
            return;
        }
        try {
            LocalAccounts.select(username);
            onClose();
        } catch (IOException error) {
            status = "Could not save the selected profile";
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        parent.extractBackground(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        GlassBackdrop.menu(graphics, left, top, panelWidth, 144);
        GlassBackdrop.menu(graphics, left + 11, top + 48, panelWidth - 22, 21);
        graphics.text(font, title.copy().setStyle(OrbitButton.HEADING), left + 12, top + 13, 0xFFF5F2FF, false);
        graphics.text(font, Component.literal("Online servers require a launcher account").setStyle(OrbitButton.TYPE),
                left + 12, top + 32, 0xFFE0DDE7, false);
        graphics.text(font, Component.literal(status).setStyle(OrbitButton.TYPE), left + 12, top + 107, 0xFFFFAAAA, false);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() { minecraft.gui.setScreen(parent); }
}
