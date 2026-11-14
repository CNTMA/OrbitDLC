package com.orbitdlc.client;


import brokke.cc.util.animation.Animation;
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
    protected final int contentY() { return top + (summary().isEmpty() ? 26 : 39); }
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
        settingWidget(new SettingRow(text, x, y, contentWidth(), 20, action, () -> false, false));
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
            settingWidget(new SettingRow(labels.get(index), x, y, chipWidth, 16, button -> toggle.accept(choice),
                    () -> selected.test(choice), true));
            x += chipWidth + 4;
        }
        return y - contentY() + 19;
    }

    protected final void slider(String label, String unit, int offset, int min, int max,
                                java.util.function.IntSupplier read, java.util.function.IntConsumer write) {
        settingWidget(new OrbitButton(Component.literal(label), contentX(), contentY() + offset,
                contentWidth(), 22, button -> {}) {
            private void move(double x) {
                double fraction = Math.clamp((x - getX() - 6) / Math.max(1, getWidth() - 12), 0, 1);
                write.accept(min + (int) Math.round(fraction * (max - min)));
            }

            @Override public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
                move(event.x());
            }

            @Override protected void onDrag(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
                move(event.x());
            }

            @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
                int direction = switch (event.key()) {
                    case com.mojang.blaze3d.platform.InputConstants.KEY_LEFT -> -1;
                    case com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT -> 1;
                    default -> 0;
                };
                if (direction == 0) return false;
                write.accept(Math.clamp(read.getAsInt() + direction, min, max));
                return true;
            }

            @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
                int x = getX(), y = getY(), w = getWidth();
                if (w <= 14) return;
                int value = read.getAsInt();
                var amount = fitted(value + unit, w / 2);
                int textWidth = font.width(amount);
                graphics.text(font, fitted(label, w - textWidth - 15), x + 6, y + 2, OrbitTheme.foreground(), false);
                graphics.text(font, amount, x + w - textWidth - 6, y + 2, OrbitTheme.accent(), false);
                int track = w - 12;
                int fill = Math.round(track * Math.clamp((float) (value - min) / (max - min), 0f, 1f));
                RoundedRectRenderState.submit(graphics, x + 6, y + 15, track, 3, 2, 0x904E4258);
                if (fill > 0) RoundedRectRenderState.submit(graphics, x + 6, y + 15, fill, 3, 2, OrbitTheme.accent());
                int knob = isHoveredOrFocused() ? 7 : 5;
                RoundedRectRenderState.submit(graphics, x + 6 + fill - knob / 2, y + 16 - knob / 2,
                        knob, knob, 4, OrbitTheme.foreground());
            }
        });
    }

    protected final void toggleAt(String label, int offset, java.util.function.BooleanSupplier enabled, Runnable toggle) {
        settingWidget(new SettingRow(label, contentX(), contentY() + offset, contentWidth(), 20,
                button -> toggle.run(), enabled, false) {
            @Override protected boolean switchRow() { return true; }
        });
    }

    private class SettingRow extends OrbitButton {
        private final boolean choice;
        private final java.util.function.BooleanSupplier selected;
        private final Animation hover = new Animation();
        private final Animation state;

        SettingRow(String label, int x, int y, int w, int h, OnPress action,
                   java.util.function.BooleanSupplier selected, boolean choice) {
            super(Component.literal(label), x, y, w, h, action);
            this.selected = selected;
            this.choice = choice;
            this.state = new Animation(selected.getAsBoolean() ? 1f : 0f);
        }

        protected boolean switchRow() { return false; }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            hover.animate(isHoveredOrFocused() ? 1f : 0f, delta, 9f);
            state.animate(selected.getAsBoolean() ? 1f : 0f, delta, 8f);
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            int fill = blend(choice ? 0x551B1722 : 0x30191620, OrbitTheme.tint(0x80),
                    Math.max(hover.value() * 0.65f, choice ? state.value() : 0f));
            RoundedRectRenderState.submit(graphics, x, y, w, h, 5, fill);
            String value = getMessage().getString();
            int separator = choice || switchRow() ? -1 : value.indexOf(" · ");
            int textY = y + (h - 9) / 2;
            int room = w - 12;
            if (switchRow()) {
                int trackX = x + w - 23;
                RoundedRectRenderState.submit(graphics, trackX, y + 6, 17, 8, 4,
                        blend(0xFF443B4D, OrbitTheme.accent(), state.value()));
                RoundedRectRenderState.submit(graphics, trackX + 1 + Math.round(9 * state.value()),
                        y + 7, 6, 6, 3, 0xFFFDF7FF);
                room -= 21;
            } else if (separator >= 0) {
                var right = fitted(value.substring(separator + 3), Math.max(0, w / 2 - 5));
                int width = font.width(right);
                graphics.text(font, right, x + w - width - 6, textY, OrbitTheme.accent(), false);
                room -= width + 5;
                value = value.substring(0, separator);
            }
            var label = fitted(value, room);
            graphics.text(font, label, choice ? x + (w - font.width(label)) / 2 : x + 6,
                    textY, OrbitTheme.foreground(), false);
        }
    }

    private Component fitted(String label, int width) {
        var text = Component.literal(label).setStyle(OrbitButton.TYPE);
        return Component.literal(font.substrByWidth(text, Math.max(0, width)).getString()).setStyle(OrbitButton.TYPE);
    }

    protected String summary() {
        return "";
    }
    void drawHeading(GuiGraphicsExtractor graphics) {
        int x = left, y = top;
        graphics.text(font, Component.literal(font.substrByWidth(title.copy().setStyle(OrbitButton.HEADING), contentWidth()).getString()).setStyle(OrbitButton.HEADING), x + 12, y + 8, 0xFFF6F3FF);
        String details = summary();
        if (!details.isEmpty()) {
            graphics.text(font, Component.literal(font.substrByWidth(Component.literal(details).setStyle(OrbitButton.TYPE), contentWidth()).getString()).setStyle(OrbitButton.TYPE), x + 12, y + 23, 0xFFBDBDD0);
        }
    }

    @Override
    public void onClose() { minecraft.gui.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }
}
