package com.orbitdlc.client;

import brokke.cc.util.animation.Animation;
import brokke.cc.util.animation.Easing;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import com.orbitdlc.client.module.ModuleManager;
import com.orbitdlc.client.module.PlannedModule;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ClickGui extends Screen {
    private final Animation openAnim = new Animation();
    private boolean closing;
    private final ModuleManager modules;
    private final Screen parent;
    private final KeyMapping shortcut;
    private final List<Button> cards = new ArrayList<>();
    private Category category = Category.COMBAT;
    private EditBox search;
    private final Animation searchFocus = new Animation();
    private int page;
    private int pageCount;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;

    public ClickGui(ModuleManager modules, Screen parent, KeyMapping shortcut) {
        super(Component.literal("OrbitDLC"));
        this.modules = modules;
        this.parent = parent;
        this.shortcut = shortcut;
    }

    @Override
    protected void init() {
        String query = search == null ? "" : search.getValue();
        cards.clear();
        panelWidth = Math.min(ClickGuiLayout.WIDTH, width - 16);
        panelHeight = Math.min(ClickGuiLayout.HEIGHT, height - 16);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        Category[] order = {Category.COMBAT, Category.MOVEMENT, Category.VISUALS, Category.PLAYER, Category.MISC};
        for (int index = 0; index < order.length; index++) {
            Category item = order[index];
            String text = item == Category.VISUALS ? "Render" : item.label();
            var entry = new SidebarEntry(text, index, button -> {
                category = item;
                page = 0;
                updateCards();
            });
            addRenderableWidget(entry);
        }
        String[] extra = {"Configs", "Cosmetics", "Scripts", "Globals", "Events", "AutoBuy", "Bots", "Customs"};
        for (int index = 0; index < extra.length; index++) {
            var entry = new SidebarEntry(extra[index], index + order.length, button -> {});
            entry.active = false;
            entry.setTooltip(Tooltip.create(Component.literal("Brokke page pending integration")));
            addRenderableWidget(entry);
        }
        search = new EditBox(font, left + 105, top + 17, panelWidth - 124, 14, Component.literal("Search modules"));
        search.setBordered(false);
        search.setHint(uiText("Search modules"));
        search.addFormatter((text, offset) -> uiText(text).getVisualOrderText());
        search.setValue(query);
        search.setResponder(value -> {
            page = 0;
            updateCards();
        });
        addRenderableWidget(search);
        updateCards();
    }

    private final class SidebarEntry extends Button {
        private final Animation highlight = new Animation();
        private final Animation selection = new Animation();
        private SidebarEntry(String text, int index, OnPress onPress) {
            super(left + 5, top + 24 + index * 14 + (index >= 5 ? 7 : 0), 70, 13,
                    Component.literal(text), onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            String current = category == Category.VISUALS ? "Render" : category.label();
            boolean selected = active && getMessage().getString().equals(current);
            selection.animate(selected ? 1f : 0f, delta, 10f);
            highlight.animate(selected || isHoveredOrFocused() ? 1f : 0f, delta, 11f);
            if (selection.value() > 0.01f) {
                rounded(graphics, getX() - 3, getY() + 3, 2, 7, 1, OrbitButton.blend(0x00288CFF, 0xFF288CFF, selection.value()));
            }
            int color = OrbitButton.blend(0xFFADB4C2, 0xFFF0F2F8, highlight.value());
            graphics.text(font, uiText(getMessage().getString()), getX() + 7, getY() + 2, color, false);
        }
    }

    private void updateCards() {
        cards.forEach(this::removeWidget);
        cards.clear();
        String query = search.getValue().strip().toLowerCase(Locale.ROOT);
        var matches = modules.modules(category).stream()
                .filter(module -> name(module).contains(query)
                        || module.description().toLowerCase(Locale.ROOT).contains(query))
                .sorted(java.util.Comparator.comparing(module -> module instanceof PlannedModule))
                .toList();
        int columns = panelWidth >= 340 ? 2 : 1;
        int rows = Math.max(1, (panelHeight - 72) / 29);
        int capacity = rows * columns;
        pageCount = Math.max(1, (matches.size() + capacity - 1) / capacity);
        page = Math.clamp(page, 0, pageCount - 1);
        int cardWidth = (panelWidth - 110 - (columns - 1) * 6) / columns;
        int start = page * capacity;
        for (int i = start; i < Math.min(start + capacity, matches.size()); i++) {
            Module module = matches.get(i);
            int cell = i - start;
            Button card = new ModuleCard(module, left + 98 + cell % columns * (cardWidth + 6),
                    top + 38 + cell / columns * 29, cardWidth);
            card.setTooltip(Tooltip.create(Component.literal(module.description())));
            card.active = !(module instanceof PlannedModule);
            cards.add(addRenderableWidget(card));
        }
        if (pageCount > 1) {
            addPageButton("<", -1, left + panelWidth - 81);
            addPageButton(">", 1, left + panelWidth - 35);
        }
    }

    private final class ModuleCard extends Button {
        private final Module module;
        private final Animation toggle = new Animation();
        private final Animation hover = new Animation();
        private final Animation reveal = new Animation();

        private ModuleCard(Module module, int x, int y, int width) {
            super(x, y, width, 24, label(module), button -> {
                module.setEnabled(!module.isEnabled());
                button.setMessage(label(module));
            }, DEFAULT_NARRATION);
            this.module = module;
            toggle.set(module.isEnabled() ? 1f : 0f);
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT && isMouseOver(event.x(), event.y())) {
                Screen settings = switch (module) {
                    case ShaderSky sky -> sky.settings(ClickGui.this);
                    case Ambience ambience -> ambience.settings(ClickGui.this);
                    case GuiHud hud -> hud.settings(ClickGui.this);
                    default -> null;
                };
                if (settings == null) return false;
                minecraft.gui.setScreen(settings);
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }
        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            hover.animate(active && isHoveredOrFocused() ? 1f : 0f, delta, 11f);
            reveal.animate(1f, delta, 14f, Easing.CUBIC_OUT);
            float visibility = Math.max(0.1f, reveal.value());
            int surface = OrbitButton.blend(0xE91A1D25, 0xED323649, hover.value());
            rounded(graphics, x, y, w, 24, 5, OrbitButton.blend(surface & 0xFFFFFF, surface, visibility));
            int solid = active ? 0xFFF0F2F8 : 0xFFA9B0BE;
            int textColor = OrbitButton.blend(solid & 0xFFFFFF, solid, visibility);
            var title = font.substrByWidth(uiText(name(module)), Math.max(0, w - 39));
            graphics.text(font, uiText(title.getString()), x + 7, y + 8, textColor, false);
            if (module instanceof PlannedModule) {
                graphics.text(font, uiText("WIP"), x + w - 25, y + 8, textColor, false);
                return;
            }
            toggle.animate(module.isEnabled() ? 1f : 0f, delta, 8f);
            float progress = Math.clamp(toggle.value(), 0f, 1f);
            int track = OrbitButton.blend(0xFF2D3039, 0xFF288CFF, progress);
            int switchX = x + w - 27;
            rounded(graphics, switchX, y + 8, 20, 9, 5, track);
            rounded(graphics, switchX + 1 + Math.round(11f * progress), y + 9, 7, 7, 4, 0xFFF5F6FA);
        }
    }

    private static void rounded(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int radius, int color) {
        brokke.cc.util.render.display.rounded.RoundedRectRenderState.submit(graphics, x, y, w, h, radius, color);
    }

    private void addPageButton(String text, int direction, int x) {
        var button = new OrbitButton(Component.literal(text), x, top + panelHeight - 28, 23, 18, pressed -> {
            page += direction;
            updateCards();
        });
        button.active = page + direction >= 0 && page + direction < pageCount;
        cards.add(addRenderableWidget(button));
    }

    private static Component uiText(String text) {
        return Component.literal(text).setStyle(OrbitButton.TYPE);
    }

    private static String name(Module module) {
        return module.id().replace('_', ' ');
    }

    private static Component label(Module module) {
        String state = module instanceof PlannedModule ? "  [WIP]" : (module.isEnabled() ? "  ON" : "  OFF");
        return Component.literal(name(module) + state);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        openAnim.animate(closing ? 0f : 1f, delta, 2f, closing ? Easing.CUBIC_IN_OUT : Easing.EXPO_OUT);
        float progress = Math.clamp(openAnim.value(), 0f, 1f);
        float scale = 0.92f + 0.08f * progress;
        float slide = (1f - progress) * 8f;
        float cx = width * 0.5f;
        float cy = height * 0.5f;
        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(scale);
        graphics.pose().translate(-cx, -cy + slide);
        try {
            renderPanel(graphics, mouseX, mouseY, delta);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        GlassBackdrop.submit(graphics, left, top, panelWidth, panelHeight);
        searchFocus.animate(search.isFocused() ? 1f : 0f, delta, 10f);
        rounded(graphics, left + 97, top + 10, panelWidth - 108, 22, 7,
                OrbitButton.blend(0x004F6090, 0xBD687CBE, searchFocus.value()));
        rounded(graphics, left + 98, top + 11, panelWidth - 110, 20, 6, 0xEA1C1E2B);
        graphics.text(font, uiText(title.getString()), left + 12, top + 10, 0xFFE1CFFF);
        graphics.fill(left + 11 + ClickGuiLayout.SIDEBAR_WIDTH, top + 37, left + 12 + ClickGuiLayout.SIDEBAR_WIDTH, top + panelHeight - 12, 0x50D9CDEF);
        if (cards.isEmpty()) {
            graphics.text(font, uiText("No matching modules"), left + 100, top + 45, 0xFFB9B1C6);
        }
        if (pageCount > 1) {
            graphics.centeredText(font, uiText(Integer.toString(page + 1)), left + panelWidth - 46,
                    top + panelHeight - 23, 0xFFE1CFFF);
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (shortcut.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        closing = true;
    }

    @Override
    public void tick() {
        if (closing && openAnim.isDone() && openAnim.value() == 0f) {
            minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
