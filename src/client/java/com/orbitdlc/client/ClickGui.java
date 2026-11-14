package com.orbitdlc.client;

import brokke.cc.util.animation.Animation;
import brokke.cc.util.animation.Easing;
import brokke.cc.util.render.display.rounded.RoundedRectRenderState;
import com.mojang.blaze3d.platform.InputConstants;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import com.orbitdlc.client.module.ModuleManager;
import com.orbitdlc.client.module.PlannedModule;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ClickGui extends Screen {
    private enum Column { COMBAT, MOVEMENT, VISUALS, PLAYER, OTHER }

    private static final int ROW = 20;
    private static final int HEADER = 26;
    private static final int GAP = 8;
    private final ModuleManager modules;
    private final Screen parent;
    private final GuiHud hud;
    private final KeyMapping shortcut;
    private final Animation entrance = new Animation();
    private final ColumnPanel[] columns = java.util.Arrays.stream(Column.values())
            .map(ColumnPanel::new).toArray(ColumnPanel[]::new);
    private final java.util.EnumMap<OrbitLibrary.Tab, OrbitLibrary> libraries =
            new java.util.EnumMap<>(OrbitLibrary.Tab.class);
    private OrbitLibrary.Tab library;
    private KeyMapping pendingBind;
    private EditBox search;
    private String query = "";
    private boolean searching;
    private boolean closing;
    private int columnWidth, panelHeight, left, top, libraryX, libraryY, libraryWidth, libraryHeight;

    private static final class ColumnPanel {
        final Column category;
        final List<AbstractWidget> controls = new ArrayList<>();
        final Animation content = new Animation(1f);
        Module selected;
        ModuleSettings settings;
        final Animation scrollMotion = new Animation();
        final List<Integer> originalY = new ArrayList<>();
        int x, scroll, maxScroll;

        void resetScroll() {
            scroll = 0;
            scrollMotion.set(0f);
        }

        ColumnPanel(Column category) { this.category = category; }
    }

    public ClickGui(ModuleManager modules, Screen parent, KeyMapping shortcut) {
        super(Component.literal("OrbitDLC"));
        this.modules = modules;
        this.parent = parent;
        this.hud = modules.modules().stream().filter(GuiHud.class::isInstance)
                .map(GuiHud.class::cast).findFirst().orElseThrow();
        this.shortcut = shortcut;
    }

    @Override protected void init() {
        columnWidth = Math.min(130, (width - 24 - GAP * 4) / 5);
        panelHeight = Math.min(252, height - 63);
        left = (width - 5 * columnWidth - 4 * GAP) / 2;
        top = (height - panelHeight) / 2;
        refreshTab();
    }

    void refreshTab() {
        clearWidgets();
        int barY = height - 28;
        var tabs = List.of(OrbitLibrary.Tab.FRIENDS, OrbitLibrary.Tab.CONFIGS,
                OrbitLibrary.Tab.COSMETICS, OrbitLibrary.Tab.THEMES);
        int buttonWidth = Math.min(68, (width - 24) / tabs.size());
        for (int i = 0; i < tabs.size(); i++) {
            var tab = tabs.get(i);
            addRenderableWidget(new OrbitButton(Component.literal(tab.label),
                    width - 8 - (tabs.size() - i) * buttonWidth, barY, buttonWidth - 4, 20, button -> {
                        library = library == tab ? null : tab;
                        if (library != null) libraries.computeIfAbsent(tab, key -> new OrbitLibrary(this, modules, key));
                        refreshTab();
                    }, () -> library == tab));
        }

        if (columnWidth > 26 && panelHeight > HEADER + 1) {
            for (int i = 0; i < columns.length; i++) {
                var panel = columns[i];
                panel.x = left + i * (columnWidth + GAP);
                layoutColumn(panel);
            }
        }
        if (library != null) {
            libraryWidth = Math.min(library == OrbitLibrary.Tab.THEMES ? 194 : 343, width - 16);
            libraryHeight = Math.min(library == OrbitLibrary.Tab.THEMES ? 232 : 250, height - 42);
            libraryX = width - libraryWidth - 9;
            libraryY = height - libraryHeight - 34;
            libraries.get(library).layout(font, libraryX, libraryY, libraryWidth, libraryHeight);
        }
        if (searching && library == null) {
            int w = Math.min(200, width - 24);
            search = new GlassInput(font, (width - w) / 2, 5, w, 21, "Search modules");
            search.setValue(query);
            search.setResponder(value -> {
                query = value;
                for (var panel : columns) { panel.selected = null; panel.resetScroll(); }
                rebuildColumns();
            });
            addRenderableWidget(search);
            setInitialFocus(search);
        }
    }

    private void rebuildColumns() {
        for (var panel : columns) {
            for (var widget : panel.controls) removeWidget(widget);
            if (columnWidth > 26 && panelHeight > HEADER + 1) layoutColumn(panel);
        }
    }

    <T extends AbstractWidget> T addTabControl(T widget) { return addWidget(widget); }

    private void layoutColumn(ColumnPanel panel) {
        panel.controls.clear();
        panel.originalY.clear();
        panel.settings = null;
        int bodyHeight = panelHeight - HEADER;
        if (panel.selected != null) {
            panel.settings = settingsFor(panel.selected);
            if (panel.settings != null) {
                var settings = panel.settings;
                settings.embed(panel.x, top + HEADER, columnWidth, panel.controls::add);
                int contentBottom = panel.controls.stream().mapToInt(w -> w.getY() + w.getHeight())
                        .max().orElse(top + HEADER);
                panel.maxScroll = Math.max(0, contentBottom - top - panelHeight + 6);
                registerScrollingControls(panel);
                var back = new OrbitButton(Component.literal("‹"), panel.x + columnWidth - 21,
                        top + 4, 16, 17, button -> showModules(panel));
                panel.controls.add(back);
                if (library == null) addWidget(back);
                return;
            }
        }

        String filter = query.strip().toLowerCase(Locale.ROOT);
        var list = modules.modules().stream()
                .filter(module -> !(module instanceof PlannedModule) && columnFor(module) == panel.category)
                .filter(module -> name(module).toLowerCase(Locale.ROOT).contains(filter)
                        || module.id().replace('_', ' ').contains(filter))
                .sorted(Comparator.comparing(ClickGui::name, String.CASE_INSENSITIVE_ORDER)).toList();
        panel.maxScroll = Math.max(0, list.size() * ROW - bodyHeight + 1);
        for (int i = 0; i < list.size(); i++) {
            panel.controls.add(new ModuleRow(list.get(i), panel, top + HEADER + i * ROW));
        }
        registerScrollingControls(panel);
    }

    private void registerScrollingControls(ColumnPanel panel) {
        panel.scroll = Math.clamp(panel.scroll, 0, panel.maxScroll);
        for (var widget : panel.controls) {
            panel.originalY.add(widget.getY());
            if (library == null) addWidget(widget);
        }
        positionScrollingControls(panel);
    }

    private void positionScrollingControls(ColumnPanel panel) {
        int offset = Math.round(Math.clamp(panel.scrollMotion.value(), 0f, panel.maxScroll));
        for (int i = 0; i < panel.originalY.size(); i++) {
            var widget = panel.controls.get(i);
            widget.setY(panel.originalY.get(i) - offset);
            widget.visible = insideBody(widget);
        }
    }

    private boolean insideBody(AbstractWidget widget) {
        return widget.getY() >= top + HEADER && widget.getY() + widget.getHeight() <= top + panelHeight;
    }

    private void showModules(ColumnPanel panel) {
        pendingBind = null;
        panel.selected = null;
        panel.content.set(0f);
        panel.resetScroll();
        refreshTab();
    }

    private static Column columnFor(Module module) {
        return switch (module.id()) {
            case "mace_swap", "mace_helper", "hitboxes", "target_esp" -> Column.COMBAT;
            case "trail", "elytra_swap", "quick_rocket", "jump_circles", "jump_particles", "landing_particles", "particle_trails",
                    "steady_view" -> Column.MOVEMENT;
            case "skin", "nametags", "viewmodel", "swing_animations", "shader_hand", "chinahat", "click_pearl", "held_durability" -> Column.PLAYER;
            case "theme", "gui", "no_render", "aspectratio" -> Column.OTHER;
            default -> module.category() == Category.VISUALS ? Column.VISUALS : Column.OTHER;
        };
    }

    private ModuleSettings settingsFor(Module module) {
        return switch (module) {
            case BlockOverlay overlay -> overlay.settings(this);
            case ShaderSky sky -> sky.settings(this);
            case ShaderHand hand -> hand.settings(this);
            case Skin skin -> skin.settings(this);
            case ElytraSwap elytra -> elytra.settings(this);
            case MaceSwap swap -> swap.settings(this);
            case MaceHelper helper -> helper.settings(this);
            case UtilityModules.QuickUse use -> use.settings(this);
            case AspectRatio ratio -> ratio.settings(this);
            case Optimization optimization -> optimization.settings(this);
            case Animations animations -> animations.settings(this);
            case Trail trail -> trail.settings(this);
            case Bloom bloom -> bloom.settings(this);
            case MotionBlur motionBlur -> motionBlur.settings(this);
            case Beautiful beautiful -> beautiful.settings(this);
            case CustomFog fog -> fog.settings(this);
            case NoRender noRender -> noRender.settings(this);
            case Viewmodel viewmodel -> viewmodel.settings(this);
            case SwingAnimations swingAnimations -> swingAnimations.settings(this);
            case Ambience ambience -> ambience.settings(this);
            case GuiHud hud -> hud.settings(this);
            case JumpCircles circles -> circles.settings(this);
            case WorldParticles particles -> particles.settings(this);
            case TargetEsp target -> target.settings(this);
            case Hitboxes hitboxes -> hitboxes.settings(this);
            default -> null;
        };
    }

    private final class ModuleRow extends Button {
        private final Module module;
        private final ColumnPanel panel;
        private final Animation hover = new Animation();
        private final Animation enabled = new Animation();

        ModuleRow(Module module, ColumnPanel panel, int y) {
            super(panel.x + 4, y, columnWidth - 8, ROW, Component.literal(name(module)), button -> {
                module.setEnabled(!module.isEnabled());
                GuiAudio.toggle(module.isEnabled());
            }, DEFAULT_NARRATION);
            this.module = module;
            enabled.set(module.isEnabled() ? 1f : 0f);
            this.panel = panel;
        }

        @Override public void playDownSound(net.minecraft.client.sounds.SoundManager sounds) {}

        @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT && isMouseOver(event.x(), event.y())) {
                if (settingsFor(module) == null) return false;
                panel.selected = module;
                panel.content.set(0f);
                panel.resetScroll();
                refreshTab();
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            hover.animate(isHoveredOrFocused() ? 1f : 0f, delta, 10f);
            enabled.animate(module.isEnabled() ? 1f : 0f, delta, 5f, Easing.CUBIC_OUT);
            float active = Math.clamp(enabled.value(), 0f, 1f);
            int bg = OrbitButton.blend(OrbitTheme.tint(Math.round(active * 52)), OrbitTheme.tint(0x76), hover.value());
            RoundedRectRenderState.submit(graphics, getX(), getY() + 1, getWidth(), ROW - 2, 5, bg);
            var text = font.substrByWidth(ui(name(module)), Math.max(0, getWidth() - 22));
            graphics.text(font, ui(text.getString()), getX() + 5, getY() + 6,
                    module.isEnabled() ? OrbitTheme.foreground() : OrbitButton.blend(OrbitTheme.foreground(), OrbitTheme.surface(), 0.22f), false);
            int switchX = getX() + getWidth() - 18;
            RoundedRectRenderState.submit(graphics, switchX, getY() + 7, 15, 7, 4,
                    OrbitButton.blend(0x65493B52, OrbitTheme.accent(), active));
            RoundedRectRenderState.submit(graphics, switchX + 1 + Math.round(active * 8), getY() + 8,
                    5, 5, 3, 0xFFFDF7FF);
        }
    }

    private static final net.minecraft.network.chat.Style ICON_STYLE = net.minecraft.network.chat.Style.EMPTY
            .withFont(new net.minecraft.network.chat.FontDescription.Resource(com.orbitdlc.OrbitDLC.id("icons")));

    private static Component ui(String text) { return Component.literal(text).setStyle(OrbitButton.TYPE); }

    private static String name(Module module) {
        return switch (module.id()) {
            case "shadersky" -> "ShaderSky";
            case "shadercrystal" -> "ShaderCrystal";
            case "chinahat" -> "ChinaHat";
            case "aspectratio" -> "AspectRatio";
            case "block_overlay" -> "BlockOverlay";
            case "bad_trip" -> "BadTrip";
            case "swing_animations" -> "SwingAnimations";
            default -> java.util.Arrays.stream(module.id().split("_"))
                    .map(word -> switch (word) {
                        case "esp", "gui", "hud", "fps", "fov" -> word.toUpperCase(Locale.ROOT);
                        default -> Character.toUpperCase(word.charAt(0)) + word.substring(1);
                    }).collect(java.util.stream.Collectors.joining(" "));
        };
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        entrance.animate(closing ? 0f : 1f, delta, closing ? 4.5f : 2.35f, Easing.LINEAR);
        float slide = 6f * (1f - Math.clamp(entrance.value(), 0f, 1f));
        if (parent instanceof MainMenu) GlassBackdrop.menuBackground(graphics, width, height);
        else GlassBackdrop.clickGuiBackground(graphics);
        graphics.nextStratum();
        graphics.pose().pushMatrix();
        graphics.pose().translate(0, -slide);
        try {
            if (columnWidth > 26 && panelHeight > HEADER + 1) {
                for (var panel : columns) {
                    float progress = Math.clamp((entrance.value() - panel.category.ordinal() * 0.075f) / 0.70f, 0f, 1f);
                    progress = 1f - (float) Math.pow(1f - progress, 3);
                    graphics.pose().pushMatrix();
                    try {
                        graphics.pose().translate(panel.x + columnWidth / 2f, top + 36f * (1f - progress));
                        graphics.pose().scale(0.88f + 0.12f * progress);
                        graphics.pose().translate(-panel.x - columnWidth / 2f, -top);
                        drawColumn(graphics, panel, mouseX, mouseY, delta);
                    } finally {
                        graphics.pose().popMatrix();
                    }
                }
            }
            if (library != null && libraryHeight > 0) {
                GlassBackdrop.submit(graphics, libraryX, libraryY, libraryWidth, libraryHeight, 13);
                RoundedRectRenderState.submit(graphics, libraryX, libraryY, libraryWidth, libraryHeight, 13, (OrbitTheme.surface() & 0xFFFFFF) | 0x39000000);
                libraries.get(library).render(graphics, mouseX, mouseY, delta);
            }
            super.extractRenderState(graphics, mouseX, mouseY, delta);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private void drawColumn(GuiGraphicsExtractor graphics, ColumnPanel panel, int mx, int my, float delta) {
        GlassBackdrop.clickGuiPanel(graphics, panel.x, top, columnWidth, panelHeight, 13);
        RoundedRectRenderState.submit(graphics, panel.x, top, columnWidth, panelHeight, 13, (OrbitTheme.surface() & 0xFFFFFF) | 0x39000000);
        String category = panel.category.name().toLowerCase(Locale.ROOT);
        String title = Character.toUpperCase(category.charAt(0)) + category.substring(1);
        graphics.text(font, Component.literal(title).setStyle(OrbitButton.HEADING), panel.x + 9, top + 8, OrbitTheme.foreground(), false);
        if (panel.settings == null) drawCategoryIcon(graphics, panel);
        graphics.fill(panel.x + 7, top + HEADER - 1, panel.x + columnWidth - 7, top + HEADER, (OrbitTheme.outline() & 0xFFFFFF) | 0xAA000000);
        if (panel.settings != null) panel.controls.getLast().extractRenderState(graphics, mx, my, delta);
        graphics.enableScissor(panel.x + 2, top + HEADER, panel.x + columnWidth - 2, top + panelHeight - 1);
        panel.scrollMotion.animate(panel.scroll, delta, 6.5f, Easing.CUBIC_OUT);
        positionScrollingControls(panel);
        graphics.pose().pushMatrix();
        try {
            panel.content.animate(1f, delta, 4.2f, Easing.CUBIC_OUT);
            graphics.pose().translate(14f * (1f - panel.content.value()), 0);
            if (panel.settings != null) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(0, -Math.clamp(panel.scrollMotion.value(), 0f, panel.maxScroll));
                panel.settings.drawHeading(graphics);
                graphics.pose().popMatrix();
            }
            for (int i = 0; i < panel.originalY.size(); i++) {
                var widget = panel.controls.get(i);
                if (widget.getY() >= top + panelHeight || widget.getY() + widget.getHeight() <= top + HEADER) continue;
                boolean interactive = widget.visible;
                widget.visible = true;
                try {
                    widget.extractRenderState(graphics, interactive ? mx : -1, interactive ? my : -1, delta);
                } finally {
                    widget.visible = interactive;
                }
            }
        } finally {
            graphics.pose().popMatrix();
            graphics.disableScissor();
        }
    }

    private void drawCategoryIcon(GuiGraphicsExtractor graphics, ColumnPanel panel) {
        int x = panel.x + columnWidth - 15, y = top + 8;
        int color = OrbitTheme.foreground();
        String glyph = switch (panel.category) {
            case COMBAT -> "V";
            case MOVEMENT -> "g";
            case VISUALS -> "W";
            case PLAYER -> "n";
            case OTHER -> "%";
        };
        var icon = Component.literal(glyph).setStyle(ICON_STYLE);
        graphics.centeredText(font, icon, x + 3, y, color);
    }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (library != null && libraries.get(library).scroll(x, y, vertical)) return true;
        if (library == null && y >= top && y < top + panelHeight && vertical != 0) {
            for (var panel : columns) {
                if (x < panel.x || x >= panel.x + columnWidth) continue;
                int step = (int) Math.round(Math.clamp(vertical, -6, 6) * 36);
                panel.scroll = Math.clamp(panel.scroll - step, 0, panel.maxScroll);
                return true;
            }
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (pendingBind != null) {
            assignBind(InputConstants.Type.MOUSE.getOrCreate(event.button()));
            return true;
        }
        if (closing) return false;
        if (super.mouseClicked(event, doubleClick)) return true;
        return library == null && event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && HudDrag.startDrag(hud, event.x(), event.y());
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        return (event.button() == InputConstants.MOUSE_BUTTON_LEFT && HudDrag.finishDrag()) || super.mouseReleased(event);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (pendingBind != null) {
            if (event.key() == InputConstants.KEY_ESCAPE || shortcut.matches(event)) {
                pendingBind = null;
                refreshTab();
            } else assignBind(event.key() == InputConstants.KEY_BACKSPACE ? InputConstants.UNKNOWN : InputConstants.getKey(event));
            return true;
        }
        if (event.hasControlDown() && event.key() == InputConstants.KEY_F) {
            library = null;
            searching = !searching;
            if (!searching) query = "";
            refreshTab();
            return true;
        }
        if (shortcut.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    void captureBind(KeyMapping mapping) { pendingBind = mapping; }

    private void assignBind(InputConstants.Key key) {
        pendingBind.setKey(key);
        KeyMapping.resetMapping();
        minecraft.options.save();
        pendingBind = null;
        refreshTab();
    }

    @Override public void onClose() {
        if (!closing) {
            closing = true;
            HudDrag.finishDrag();
            GuiAudio.close();
        }
    }

    @Override public void tick() {
        if (closing && entrance.isDone() && entrance.value() == 0f) minecraft.gui.setScreen(parent);
    }

    @Override public boolean isPauseScreen() { return false; }
}
