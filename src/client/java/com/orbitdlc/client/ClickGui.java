package com.orbitdlc.client;

import brokke.cc.util.animation.Animation;
import brokke.cc.util.animation.Easing;
import com.orbitdlc.client.module.Module;
import com.orbitdlc.client.module.ModuleManager;
import com.orbitdlc.client.module.PlannedModule;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ClickGui extends Screen {
    private KeyMapping pendingBind;
    private final Animation openAnim = new Animation();
    private boolean closing;
    private final ModuleManager modules;
    private final Screen parent;
    private final KeyMapping shortcut;
    private final List<Button> cards = new ArrayList<>();
    private final java.util.EnumMap<OrbitLibrary.Tab, OrbitLibrary> libraries =
            new java.util.EnumMap<>(OrbitLibrary.Tab.class);
    private OrbitLibrary.Tab tab = OrbitLibrary.Tab.VISUALS;
    private final Animation tabReveal = new Animation();
    private final Animation tabMarker = new Animation();
    private EditBox search;
    private ModuleSettings sidebar;
    private final Animation settingsReveal = new Animation();
    private final List<Button> settingControls = new ArrayList<>();
    private int settingsWidth;
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
        settingsWidth = sidebar == null ? 0 : Math.min(Math.max(0, width - 164),
                Math.min(200, Math.max(120, width / 3)));
        panelWidth = Math.min(panelWidth, width - 16 - (sidebar == null ? 0 : settingsWidth + 8));
        left = (width - panelWidth - (sidebar == null ? 0 : settingsWidth + 8)) / 2;
        top = (height - panelHeight) / 2;
        for (OrbitLibrary.Tab entry : OrbitLibrary.Tab.values()) {
            addRenderableWidget(new SidebarEntry(entry));
        }
        settingControls.clear();
        if (tab.category() == null) {
            libraries.get(tab).layout(font, left + 98, top, panelWidth - 110, panelHeight);
            return;
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
        addTabControl(search);
        if (sidebar != null) {
            sidebar.embed(left + panelWidth + 8, top, settingsWidth, this::addSettingControl);
            addSettingControl(new OrbitButton(Component.literal("×"), left + panelWidth + settingsWidth - 12,
                    top + 7, 14, 14, button -> closeSettings()));
        }
        updateCards();
    }

    private void selectTab(OrbitLibrary.Tab next) {
        if (next == tab) return;
        tab = next;
        page = 0;
        sidebar = null;
        if (next.category() == null) {
            libraries.computeIfAbsent(next, selected -> new OrbitLibrary(this, modules, selected));
        }
        tabReveal.set(0f);
        refreshTab();
    }

    void refreshTab() {
        rebuildWidgets();
    }

    <T extends AbstractWidget> T addTabControl(T widget) {
        return addWidget(widget);
    }

    private void addSettingControl(Button button) {
        settingControls.add(addWidget(button));
    }
    private final class SidebarEntry extends Button {
        @Override
        public void playDownSound(net.minecraft.client.sounds.SoundManager sounds) {}

        private final Animation highlight = new Animation();
        private final OrbitLibrary.Tab entry;

        private SidebarEntry(OrbitLibrary.Tab entry) {
            super(left + 5, top + 27 + entry.ordinal() * 17, 77, 15,
                    Component.literal(entry.label), button -> selectTab(entry), DEFAULT_NARRATION);
            this.entry = entry;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            boolean selected = entry == tab;
            highlight.animate(selected || isHoveredOrFocused() ? 1f : 0f, delta, 11f);
            rounded(graphics, getX(), getY(), getWidth(), getHeight(), 5,
                    OrbitButton.blend(0, OrbitTheme.tint(0x48), highlight.value()));
            int color = OrbitButton.blend(0xFFADB4C2, OrbitTheme.accent(), highlight.value());
            graphics.text(font, uiText(getMessage().getString()), getX() + 7, getY() + 2, color, false);
        }
    }

    private void updateCards() {
        cards.forEach(this::removeWidget);
        cards.clear();
        String query = search.getValue().strip().toLowerCase(Locale.ROOT);
        var matches = modules.modules(tab.category()).stream()
                .filter(module -> module.id().replace('_', ' ').contains(query)
                        || name(module).toLowerCase(Locale.ROOT).contains(query)
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
            card.active = !(module instanceof PlannedModule);
            cards.add(addTabControl(card));
        }
        if (pageCount > 1) {
            addPageButton("<", -1, left + panelWidth - 81);
            addPageButton(">", 1, left + panelWidth - 35);
        }
    }

    private final class ModuleCard extends Button {
        @Override
        public void playDownSound(net.minecraft.client.sounds.SoundManager sounds) {}

        private final Module module;
        private final Animation toggle = new Animation();
        private final Animation hover = new Animation();
        private final Animation reveal = new Animation();

        private ModuleCard(Module module, int x, int y, int width) {
            super(x, y, width, 24, label(module), button -> {
                module.setEnabled(!module.isEnabled());
                GuiAudio.toggle(module.isEnabled());
                button.setMessage(label(module));
            }, DEFAULT_NARRATION);
            this.module = module;
            toggle.set(module.isEnabled() ? 1f : 0f);
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT && isMouseOver(event.x(), event.y())) {
                ModuleSettings settings = switch (module) {
                    case ShaderSky sky -> sky.settings(ClickGui.this);
                    case ShaderHand hand -> hand.settings(ClickGui.this);
                    case MaceSwap swap -> swap.settings(ClickGui.this);
                    case MaceHelper helper -> helper.settings(ClickGui.this);
                    case UtilityModules.QuickUse use -> use.settings(ClickGui.this);
                    case Beautiful beautiful -> beautiful.settings(ClickGui.this);
                    case CustomFog fog -> fog.settings(ClickGui.this);
                    case NoRender noRender -> noRender.settings(ClickGui.this);
                    case Viewmodel viewmodel -> viewmodel.settings(ClickGui.this);
                    case Ambience ambience -> ambience.settings(ClickGui.this);
                    case GuiHud hud -> hud.settings(ClickGui.this);
                    case JumpCircles circles -> circles.settings(ClickGui.this);
                    case WorldParticles particles -> particles.settings(ClickGui.this);
                    case TargetEsp target -> target.settings(ClickGui.this);
                    case Hitboxes hitboxes -> hitboxes.settings(ClickGui.this);
                    default -> null;
                };
                if (settings == null) return false;
                sidebar = settings;
                settingsReveal.set(0f);
                rebuildWidgets();
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
            GlassBackdrop.submit(graphics, x, y, w, 24, 7);
            int surface = OrbitButton.blend(0x38201A20, OrbitTheme.tint(0x62), hover.value());
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
            int track = OrbitButton.blend(0xFF443A44, OrbitTheme.accent(), progress);
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
        cards.add(addTabControl(button));
    }

    private static Component uiText(String text) {
        return Component.literal(text).setStyle(OrbitButton.TYPE);
    }

    private static String name(Module module) {
        return switch (module.id()) {
            case "shadersky" -> "ShaderSky";
            case "fullbright" -> "FullBright";
            default -> {
                var name = new StringBuilder();
                for (String word : module.id().split("_+")) {
                    name.append(switch (word) {
                        case "esp", "gui", "hud", "fps", "rpc", "fov" -> word.toUpperCase(Locale.ROOT);
                        default -> Character.toUpperCase(word.charAt(0)) + word.substring(1);
                    });
                }
                yield name.toString();
            }
        };
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
        graphics.text(font, title.copy().setStyle(OrbitButton.HEADING), left + 12, top + 10, OrbitTheme.accent());
        int divider = left + 11 + ClickGuiLayout.SIDEBAR_WIDTH;
        graphics.fill(divider, top + 24, divider + 1, top + panelHeight - 12, 0x50D9CDEF);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        tabMarker.animate(tab.ordinal(), delta, 9f, Easing.CUBIC_OUT);
        rounded(graphics, left + 3, top + 31 + Math.round(tabMarker.value() * 17), 2, 7, 1, OrbitTheme.accent());
        tabReveal.animate(1f, delta, 8f, Easing.CUBIC_OUT);
        int reveal = tabRevealHeight();
        if (reveal > 0 && panelWidth > 103) {
            graphics.enableScissor(left + 97, top, left + panelWidth - 6, top + reveal);
            try {
                if (brokke.cc.util.render.display.rounded.RoundedRectRenderState.hasDrawableScissor(graphics)) {
                    if (tab.category() != null) renderVisuals(graphics, mouseX, mouseY, delta);
                    else libraries.get(tab).render(graphics, mouseX, mouseY, delta);
                }
            } finally {
                graphics.disableScissor();
            }
        }
        renderSettings(graphics, mouseX, mouseY, delta);
    }

    private int tabRevealHeight() {
        return Math.round((panelHeight - 8) * Math.clamp(tabReveal.value(), 0f, 1f));
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (pendingBind != null) {
            assignBind(com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(event.button()));
            return true;
        }
        if (event.x() >= left + 97 && event.x() < left + panelWidth
                && event.y() >= top + tabRevealHeight() && event.y() < top + panelHeight) {
            return false;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void renderVisuals(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        GlassBackdrop.submit(graphics, left + 98, top + 11, panelWidth - 110, 20, 7);
        searchFocus.animate(search.isFocused() ? 1f : 0f, delta, 10f);
        rounded(graphics, left + 97, top + 10, panelWidth - 108, 22, 7,
                OrbitButton.blend(0x00705A6A, 0xBDE8C7DF, searchFocus.value()));
        rounded(graphics, left + 98, top + 11, panelWidth - 110, 20, 6, 0x30261C25);
        if (cards.isEmpty()) {
            graphics.text(font, uiText("No matching modules"), left + 100, top + 45, 0xFFB9B1C6);
        }
        if (pageCount > 1) {
            graphics.centeredText(font, uiText(Integer.toString(page + 1)), left + panelWidth - 46,
                    top + panelHeight - 23, 0xFFE1CFFF);
        }
        search.extractRenderState(graphics, mouseX, mouseY, delta);
        cards.forEach(card -> card.extractRenderState(graphics, mouseX, mouseY, delta));
    }

    private void renderSettings(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (sidebar != null && settingsWidth > 0 && panelHeight > 0) {
            settingsReveal.animate(1f, delta, 7f, Easing.CUBIC_OUT);
            int x = left + panelWidth + 8;
            int visibleWidth = Math.min(settingsWidth, Math.round(settingsWidth * settingsReveal.value()));
            if (visibleWidth < 2) return;
            graphics.enableScissor(x, top, x + visibleWidth, top + panelHeight);
            try {
                if (!brokke.cc.util.render.display.rounded.RoundedRectRenderState.hasDrawableScissor(graphics)) return;
                sidebar.drawSidebar(graphics, panelHeight, visibleWidth);
                for (Button button : settingControls) {
                    if (button.getX() + button.getWidth() <= x + visibleWidth) {
                        button.extractRenderState(graphics, mouseX, mouseY, delta);
                    }
                }
            } finally {
                graphics.disableScissor();
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (pendingBind != null) {
            var input = com.mojang.blaze3d.platform.InputConstants.getKey(event);
            if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE || shortcut.matches(event)) {
                pendingBind = null;
                refreshTab();
            } else {
                assignBind(event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_BACKSPACE
                        ? com.mojang.blaze3d.platform.InputConstants.UNKNOWN : input);
            }
            return true;
        }
        if (shortcut.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (closing) return;
        if (sidebar != null) {
            closeSettings();
            return;
        }
        closing = true;
        GuiAudio.close();
    }

    private void closeSettings() {
        pendingBind = null;
        sidebar = null;
        rebuildWidgets();
    }

    void captureBind(KeyMapping mapping) {
        pendingBind = mapping;
    }

    private void assignBind(com.mojang.blaze3d.platform.InputConstants.Key key) {
        pendingBind.setKey(key);
        KeyMapping.resetMapping();
        minecraft.options.save();
        pendingBind = null;
        refreshTab();
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
