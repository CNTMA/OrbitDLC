package com.orbitdlc.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {
    private final Screen parent;

    // Цветовая схема OrbitDLC
    private static final int ACCENT = 0xFFA855F7;
    private static final int TEXT = 0xFFE5E5EA;
    private static final int MUTED = 0xFF8E8E93;
    private static final int PANEL_BG = 0xC2181026;
    private static final int SIDEBAR_BG = 0xCF231735;
    private static final int CARD_BG = 0xAE302245;
    private static final int TOGGLE_OFF = 0xFF4B3A60;

    private static final int CARD_H = 36;
    private static final int PAD = 8;

    // Кастомный шрифт (26.3)
    private static final Identifier CUSTOM_FONT_ID = Identifier.fromNamespaceAndPath("orbitdlc", "myfont");
    private static final FontDescription CUSTOM_FONT_DESC = new FontDescription.Resource(CUSTOM_FONT_ID);

    private final List<Category> categories = new ArrayList<>();
    private Category selected;
    private String search = "";
    private boolean searchFocused;
    private int scroll, maxScroll;
    private float openAnim, displayedScroll;
    private long lastFrame = System.nanoTime();
    private boolean closing;

    private int cLeft, cTop, cRight, cBottom;

    private record Ripple(float x, float y, long start) {}
    private final java.util.ArrayDeque<Ripple> ripples = new java.util.ArrayDeque<>();

    private static class Category {
        final String name;
        final List<Module> modules = new ArrayList<>();
        int x, y, w;
        Category(String name) { this.name = name; }
    }

    private static class Module {
        final String name;
        boolean enabled;
        boolean expanded;
        float expansion;
        float anim;
        int x, y, w, bottom;
        final List<Setting> settings = new ArrayList<>();
        Module(String name) { this.name = name; }
    }

    private static class Setting {
        final String name;
        float value;
        final java.util.function.Consumer<Float> onChange;
        int x, y, w;
        Setting(String name, float value, java.util.function.Consumer<Float> onChange) {
            this.name = name;
            this.value = value;
            this.onChange = onChange;
        }
    }

    public ClickGuiScreen(Screen parent) {
        super(Component.literal("OrbitDLC"));
        this.parent = parent;
        initModules();
        if (!categories.isEmpty()) selected = categories.getFirst();
    }

    private void initModules() {
        Category combat = new Category("Combat");
        Module aura = new Module("Aura");
        aura.settings.add(new Setting("Дистанция", 0.3f, v -> {}));
        aura.settings.add(new Setting("Скорость", 0.5f, v -> {}));
        combat.modules.add(aura);
        combat.modules.add(new Module("Velocity"));
        categories.add(combat);

        Category movement = new Category("Movement");
        movement.modules.add(new Module("Sprint"));
        Module speed = new Module("Speed");
        speed.settings.add(new Setting("Буст", 0.4f, v -> {}));
        movement.modules.add(speed);
        categories.add(movement);

        Category visuals = new Category("Visuals");
        Module esp = new Module("EntityESP");
        esp.settings.add(new Setting("Дальность", 0.6f, v -> {}));
        visuals.modules.add(esp);
        visuals.modules.add(new Module("FullBright"));
        categories.add(visuals);

        Category misc = new Category("Misc");
        misc.modules.add(new Module("AutoTotem"));
        categories.add(misc);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float seconds = Math.min(0.05f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;
        openAnim = approach(openAnim, closing ? 0f : 1f, seconds, 20f);
        if (closing && openAnim < 0.05f) {
            this.minecraft.gui.setScreen(parent);
            return;
        }

        g.fillGradient(0, 0, width, height, withAlpha(0x170B29, Math.round(74 * openAnim)), withAlpha(0x10091D, Math.round(117 * openAnim)));

        int panelW = Math.min(width - 12, Math.max(320, width * 62 / 100));
        int panelH = Math.min(height - 12, Math.max(180, height * 62 / 100));
        int left = (width - panelW) / 2;
        int top = (height - panelH) / 2 + Math.round((1f - openAnim) * 14);
        int sidebarW = Math.min(140, Math.max(90, panelW * 26 / 100));

        cLeft = left + sidebarW + PAD;
        cRight = left + panelW - PAD;
        cTop = top + 32;
        cBottom = top + panelH - PAD;

        roundedRect(g, left, top, panelW, panelH, 18, PANEL_BG);
        roundedRect(g, left, top, sidebarW, panelH, 18, SIDEBAR_BG);

        roundedRect(g, left + 8, top + 8, 20, 20, 6, 0xFF7345AE);
        text(g, "OrbitDLC", left + 34, top + 14, 1.1f, TEXT);

        int rowH = Math.min(28, (panelH - 44) / Math.max(1, categories.size()));
        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            cat.x = left + 6;
            cat.y = top + 40 + i * rowH;
            cat.w = sidebarW - 12;
            boolean active = cat == selected;
            if (active || inside(mouseX, mouseY, cat.x, cat.y, cat.w, rowH - 2)) {
                roundedRect(g, cat.x, cat.y, cat.w, rowH - 2, 6, active ? 0xC65E3B88 : 0x70402B59);
            }
            text(g, cat.name, cat.x + 8, cat.y + (rowH - 10) / 2, 1f, active ? ACCENT : TEXT);
        }

        int searchW = cRight - cLeft;
        roundedRect(g, cLeft, top + 8, searchW, 16, 5, searchFocused ? 0xFF43305F : 0xFF1D142C);
        text(g, search.isEmpty() ? "Поиск модулей" : search, cLeft + 6, top + 12, 1f, MUTED);

        if (selected == null) return;
        List<Module> mods = selected.modules.stream()
                .filter(m -> m.name.toLowerCase().contains(search.toLowerCase()))
                .toList();

        int columns = searchW >= 220 ? 2 : 1;
        int cardW = (searchW - PAD * (columns - 1)) / columns;

        for (Module m : mods) {
            m.expansion = approach(m.expansion, m.expanded ? 1f : 0f, seconds, 22f);
            m.anim = approach(m.anim, m.enabled ? 1f : 0f, seconds, 24f);
        }

        int totalH = 0;
        for (int i = 0; i < mods.size(); i += columns) {
            int rowH2 = CARD_H;
            for (int j = i; j < Math.min(i + columns, mods.size()); j++) {
                rowH2 = Math.max(rowH2, CARD_H + settingsHeight(mods.get(j)));
            }
            totalH += rowH2 + PAD;
        }
        maxScroll = Math.max(0, totalH - (cBottom - cTop));
        scroll = Math.min(scroll, maxScroll);
        displayedScroll = approach(displayedScroll, scroll, seconds, 24f);

        g.enableScissor(cLeft, cTop, cRight, cBottom);
        int y = cTop - Math.round(displayedScroll);
        for (int i = 0; i < mods.size(); i += columns) {
            int rowH2 = CARD_H;
            for (int j = i; j < Math.min(i + columns, mods.size()); j++) {
                Module m = mods.get(j);
                m.x = cLeft + (j - i) * (cardW + PAD);
                m.y = y;
                m.w = cardW;
                m.bottom = y + CARD_H + settingsHeight(m);
                rowH2 = Math.max(rowH2, m.bottom - y);
                if (m.bottom > cTop && m.y < cBottom) renderModule(g, m);
            }
            y += rowH2 + PAD;
        }
        g.disableScissor();

        long t = System.nanoTime();
        ripples.removeIf(r -> t - r.start() > 350_000_000L);
        for (Ripple r : ripples) {
            float p = (t - r.start()) / 350_000_000f;
            int rad = Math.round(3 + 14 * (1 - (1 - p) * (1 - p)));
            roundedRect(g, Math.round(r.x()) - rad, Math.round(r.y()) - rad, rad * 2, rad * 2, rad,
                    withAlpha(0xD4B5FF, Math.round(50 * (1 - p))));
        }
    }

    private void renderModule(GuiGraphicsExtractor g, Module m) {
        roundedRect(g, m.x, m.y, m.w, m.bottom - m.y, 7, CARD_BG);

        int tx = m.x + m.w - 34, ty = m.y + 7;
        roundedRect(g, tx, ty, 26, 11, 5, m.enabled ? 0xFF9B6CE2 : TOGGLE_OFF);
        roundedRect(g, tx + 1 + Math.round(11 * m.anim), ty + 1, 13, 9, 4, TEXT);

        text(g, m.name, m.x + 8, m.y + 8, 1.05f, TEXT);
        if (!m.settings.isEmpty()) {
            text(g, m.expanded ? "ПКМ · свернуть" : "ПКМ · настройки", m.x + 8, m.y + 22, 0.85f, MUTED);
        }

        if (m.bottom > m.y + CARD_H) {
            g.enableScissor(m.x, m.y + CARD_H, m.x + m.w, m.bottom);
            int sy = m.y + CARD_H + 2;
            for (Setting s : m.settings) {
                s.x = m.x + 4; s.y = sy; s.w = m.w - 8;
                text(g, s.name, s.x + 2, s.y, 0.85f, MUTED);
                g.fill(s.x + 2, s.y + 9, s.x + s.w - 2, s.y + 12, 0xFF30233F);
                g.fill(s.x + 2, s.y + 9, s.x + 2 + (int) ((s.w - 4) * s.value), s.y + 12, ACCENT);
                sy += 16;
            }
            g.disableScissor();
        }
    }

    private int settingsHeight(Module m) {
        if (m.expansion == 0f || m.settings.isEmpty()) return 0;
        return Math.round((m.settings.size() * 16 + 4) * m.expansion);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        if (inside(x, y, cLeft, cTop, cRight - cLeft, cBottom - cTop)) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.round(v * 24)));
            return true;
        }
        return super.mouseScrolled(x, y, h, v);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int btn = click.button();
        if (closing) return true;

        if (ripples.size() >= 8) ripples.removeFirst();
        ripples.addLast(new Ripple((float) mx, (float) my, System.nanoTime()));

        // 26.3: используем константы InputConstants вместо магических чисел
        boolean isLeft = btn == InputConstants.MOUSE_BUTTON_LEFT;
        boolean isRight = btn == InputConstants.MOUSE_BUTTON_RIGHT;

        searchFocused = isLeft && inside(mx, my, cLeft, cTop - 24, cRight - cLeft, 16);
        if (searchFocused) return true;

        if (selected != null) {
            int rowH = categories.size() > 1
                    ? categories.get(1).y - categories.getFirst().y
                    : 28;
            for (Category cat : categories) {
                if (isLeft && inside(mx, my, cat.x, cat.y, cat.w, rowH - 2)) {
                    if (selected != cat) {
                        selected = cat;
                        scroll = 0;
                        displayedScroll = 0;
                    }
                    return true;
                }
            }
        }

        if (selected == null || !inside(mx, my, cLeft, cTop, cRight - cLeft, cBottom - cTop)) {
            return super.mouseClicked(click, doubled);
        }

        List<Module> mods = selected.modules.stream()
                .filter(m -> m.name.toLowerCase().contains(search.toLowerCase()))
                .toList();
        for (Module m : mods) {
            boolean inCard = mx >= m.x && mx <= m.x + m.w && my >= m.y && my <= m.bottom;

            // Правая кнопка — раскрытие/сворачивание по всей площади карточки
            if (isRight && inCard && !m.settings.isEmpty()) {
                m.expanded = !m.expanded;
                return true;
            }

            // Левая кнопка по шапке — toggle модуля
            if (isLeft && mx >= m.x && mx <= m.x + m.w && my >= m.y && my <= m.y + CARD_H) {
                m.enabled = !m.enabled;
                return true;
            }

            // Левая кнопка по слайдерам внутри раскрытых настроек
            if (m.expanded && isLeft && my >= m.y + CARD_H && my <= m.bottom) {
                for (Setting s : m.settings) {
                    if (mx >= s.x && mx <= s.x + s.w && my >= s.y && my <= s.y + 14) {
                        s.value = clamp((float) ((mx - (s.x + 2)) / (s.w - 4)));
                        s.onChange.accept(s.value);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // 26.3: KEY_RSHIFT и KEY_ESCAPE из InputConstants
        if (event.key() == InputConstants.KEY_RSHIFT || event.key() == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (searchFocused && event.key() == InputConstants.KEY_BACKSPACE) {
            if (!search.isEmpty()) {
                search = search.substring(0, search.offsetByCodePoints(search.length(), -1));
                scroll = 0;
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchFocused && !Character.isISOControl(event.codepoint()) && search.length() < 48) {
            search += Character.toString(event.codepoint());
            scroll = 0;
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        closing = true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void roundedRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        color = withAlpha(color, Math.round(((color >>> 24) & 0xFF) * openAnim));
        r = Math.min(r, Math.min(w, h) / 2);
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int row = 0; row < r; row++) {
            double dy = r - row - 0.5;
            double edge = r - Math.sqrt(r * r - dy * dy);
            int inset = (int) Math.ceil(edge);
            if (inset > 0) {
                int rim = withAlpha(color, Math.round(((color >>> 24) & 0xFF) * (float) (inset - edge)));
                g.fill(x + inset - 1, y + row, x + inset, y + row + 1, rim);
                g.fill(x + w - inset, y + row, x + w - inset + 1, y + row + 1, rim);
                g.fill(x + inset - 1, y + h - row - 1, x + inset, y + h - row, rim);
                g.fill(x + w - inset, y + h - row - 1, x + w - inset + 1, y + h - row, rim);
            }
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
            g.fill(x + inset, y + h - row - 1, x + w - inset, y + h - row, color);
        }
    }

    // 26.3: рендер текста с кастомным шрифтом через Component и Style
    private void text(GuiGraphicsExtractor g, String s, int x, int y, float scale, int color) {
        Component component = Component.literal(s)
                .setStyle(Style.EMPTY.withFont(CUSTOM_FONT_DESC));

        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        g.text(this.font, component, 0, 0,
                withAlpha(color, Math.round(((color >>> 24) & 0xFF) * openAnim)), false);
        pose.popMatrix();
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static float approach(float cur, float target, float sec, float rate) {
        float next = target + (cur - target) * (float) Math.exp(-rate * sec);
        return Math.abs(next - target) < 0.002f ? target : next;
    }

    private static boolean inside(double x, double y, int left, int top, int w, int h) {
        return x >= left && x < left + w && y >= top && y < top + h;
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}