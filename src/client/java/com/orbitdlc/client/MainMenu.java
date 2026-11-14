package com.orbitdlc.client;

import com.orbitdlc.OrbitDLC;
import com.orbitdlc.client.module.ModuleManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

public final class MainMenu extends Screen {
    static final net.minecraft.resources.Identifier BACKGROUND = OrbitDLC.id("textures/menu/singleplayer.png");
    private final ModuleManager modules;
    private final KeyMapping shortcut;
    private final brokke.cc.util.animation.Animation entrance = new brokke.cc.util.animation.Animation();
    private int left;
    private int top;
    private int panelWidth;

    public MainMenu(ModuleManager modules, KeyMapping shortcut) {
        super(Component.literal("OrbitDLC"));
        this.modules = modules;
        this.shortcut = shortcut;
    }

    @Override
    protected void init() {
        entrance.set(0f);
        panelWidth = Math.min(width - 32, Math.clamp(width / 4, 150, 290));
        left = Math.max(16, width / 32);

        int row = Math.clamp(height / 15, 20, 36);
        int gap = 6;
        int stackHeight = 31 + 5 * row + 4 * gap;
        top = Math.max(8, Math.min(height * 3 / 8, height - stackHeight - 12));
        int y = top + 31;
        add("menu.singleplayer", left, y, panelWidth, row,
                () -> minecraft.gui.setScreen(new SelectWorldScreen(this)));
        var multiplayer = add("menu.multiplayer", left, y + row + gap, panelWidth, row,
                () -> minecraft.gui.setScreen(new JoinMultiplayerScreen(this)));
        multiplayer.active = minecraft.allowsMultiplayer();
        add("menu.options", left, y + 2 * (row + gap), panelWidth, row,
                () -> minecraft.gui.setScreen(new OptionsScreen(this, minecraft.options)));
        add("Account Switcher", left, y + 3 * (row + gap), panelWidth, row,
                () -> minecraft.gui.setScreen(new AccountSwitcher(this)));
        int lastY = y + 4 * (row + gap);
        int half = (panelWidth - gap) / 2;
        add("menu.quit", left, lastY, half, row, minecraft::stop);
        add("ClickGUI", left + half + gap, lastY, panelWidth - half - gap, row,
                () -> ClickGuiShortcut.open(minecraft, modules, this, shortcut));
    }

    private Button add(String label, int x, int y, int w, int height, Runnable action) {
        Component text = label.startsWith("menu.") ? Component.translatable(label) : Component.literal(label);
        return addRenderableWidget(new OrbitButton(text, x, y, w, height, button -> action.run()) {
            @Override
            protected void background(GuiGraphicsExtractor graphics, int color) {
                GlassBackdrop.menu(graphics, getX(), getY(), getWidth(), getHeight());
                brokke.cc.util.render.display.rounded.RoundedRectRenderState.submit(
                        graphics, getX(), getY(), getWidth(), getHeight(), 18,
                        active ? color & 0x32FFFFFF : 0x6620222D);
            }
        });
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        GlassBackdrop.menuBackground(graphics, width, height);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        entrance.animate(1f, delta, 6f, brokke.cc.util.animation.Easing.EXPO_OUT);
        graphics.pose().pushMatrix();
        graphics.pose().translate(0f, 8f * (1f - Math.clamp(entrance.value(), 0f, 1f)));
        try {
            renderPanel(graphics, mouseX, mouseY, delta);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.pose().pushMatrix();
        graphics.blit(OrbitDLC.id("black_hole.png"), left, top - 50, left + 47, top - 3, 0f, 1f, 0f, 1f);
        graphics.pose().translate(left, top);
        graphics.pose().scale(1.8f);
        graphics.text(font, Component.literal("Welcome to OrbitDLC").setStyle(OrbitButton.HEADING), 0, 0, 0xFFF9FAFF, false);
        graphics.pose().popMatrix();
        var user = Component.literal(minecraft.getUser().getName()).setStyle(OrbitButton.TYPE);
        int chipWidth = Math.max(62, font.width(user) + 24);
        int chipX = width - chipWidth - 12;
        brokke.cc.util.render.display.rounded.RoundedRectRenderState.submit(graphics, chipX, 10, chipWidth, 23, 11, 0xA6233249);
        graphics.centeredText(font, user, chipX + chipWidth / 2, 17, 0xFFF1F6FF);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (shortcut.matches(event)) {
            ClickGuiShortcut.open(minecraft, modules, this, shortcut);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
