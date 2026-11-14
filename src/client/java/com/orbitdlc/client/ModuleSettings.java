package com.orbitdlc.client;

import brokke.cc.util.render.display.rounded.RoundedRectRenderState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

abstract class ModuleSettings extends Screen {
    private final Screen parent;
    private java.util.function.Consumer<Button> host;
    private int left;
    private int top;
    private int panelWidth;

    ModuleSettings(String title, Screen parent) {
        super(Component.literal(title));
        this.parent = parent;
    }

    final void embed(int x, int y, int width, java.util.function.Consumer<Button> host) {
        this.left = x;
        this.top = y;
        this.panelWidth = width;
        this.host = host;
        init();
    }

    protected final int contentX() { return left + 12; }
    protected final int contentY() { return top + 44; }
    protected final int contentWidth() { return panelWidth - 24; }

    protected final void settingWidget(Button button) { host.accept(button); }

    protected final void bind(String label, int row, net.minecraft.client.KeyMapping mapping) {
        chip(label + " · " + mapping.getTranslatedKeyMessage().getString(), row, button -> {
            ((ClickGui) parent).captureBind(mapping);
            button.setMessage(Component.literal("Press key or mouse button"));
        });
    }
    protected final void chip(String text, int row, Button.OnPress action) {
        int x = contentX();
        int y = contentY() + row * 24;
        settingWidget(new OrbitButton(Component.literal(text), x, y, contentWidth(), 19, action));
    }

    protected final void choices(java.util.List<String> labels,
                                 java.util.function.IntPredicate selected,
                                 java.util.function.IntConsumer toggle) {
        choicesAt(labels, selected, toggle, 0);
    }

    protected final int choicesAt(java.util.List<String> labels,
                                   java.util.function.IntPredicate selected,
                                   java.util.function.IntConsumer toggle, int offset) {
        int left = contentX();
        int x = left;
        int y = contentY() + offset;
        for (int index = 0; index < labels.size(); index++) {
            int choice = index;
            Component label = Component.literal(labels.get(index)).setStyle(OrbitButton.TYPE);
            int chipWidth = Math.min(contentWidth(), font.width(label) + 14);
            if (x + chipWidth > left + contentWidth()) {
                y += 19;
                x = left;
            }
            settingWidget(new OrbitButton(label, x, y, chipWidth, 16, button -> toggle.accept(choice),
                    () -> selected.test(choice)));
            x += chipWidth + 4;
        }
        return y - contentY() + 19;
    }

    protected String summary() {
        return "";
    }
    void drawSidebar(GuiGraphicsExtractor graphics, int height, int visibleWidth) {
        int x = left;
        int y = top;
        GlassBackdrop.submit(graphics, x, y, visibleWidth, height, 14);
        RoundedRectRenderState.submit(graphics, x, y, visibleWidth, height, 14, 0x30202638);
        graphics.text(font, title.copy().setStyle(OrbitButton.HEADING), x + 14, y + 13, 0xFFF6F3FF, false);
        String details = summary();
        if (!details.isEmpty()) {
            graphics.text(font, Component.literal(details).setStyle(OrbitButton.TYPE), x + 14, y + 29, 0xFFBDBDD0, false);
        }
    }

    @Override
    public void onClose() { minecraft.gui.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }
}
