/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.screen;

import brokke.cc.core.module.impl.render.GuiSettings;
import brokke.cc.core.module.impl.render.Interface;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.blur.Blur;
import brokke.cc.util.render.display.blur.BlurType;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class GuiDraw {
    private GuiDraw() {
    }

    public static void renderPanelBackground(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float alpha) {
        if (alpha <= 0.01f) {
            return;
        }
        float radius = GuiDraw.panelRadius();
        if (Interface.isBlackBackgroundEnabled()) {
            RoundedRect.of(x, y, w, h).radius(radius).color(Color.rgb(0, 0, 0).withAlpha(alpha)).render(graphics);
            return;
        }
        Blur.of(x, y, w, h).radius(radius).type(BlurType.KAWASE).strength(GuiDraw.blurStrength()).tint(Color.rgba(12, 13, 18, 170)).alpha(alpha).render(graphics);
        RoundedRect.of(x, y, w, h).radius(radius).color(Color.rgba(12, 13, 18, Math.round(100.0f * alpha))).render(graphics);
    }

    public static Color getAccent2() {
        GuiSettings settings = GuiSettings.getInstance();
        return settings != null ? settings.getThemeSecondary() : Color.rgb(0, 225, 255);
    }

    public static float panelRadius() {
        GuiSettings settings = GuiSettings.getInstance();
        return settings != null ? (float)settings.cornerRadius.getValue() : 8.0f;
    }

    public static int blurStrength() {
        GuiSettings settings = GuiSettings.getInstance();
        return settings != null ? Math.clamp((long)settings.blur.getInt(), 1, 6) : 6;
    }

    public static Color getAccent() {
        GuiSettings settings = GuiSettings.getInstance();
        return settings != null ? settings.getThemePrimary() : Color.rgb(40, 140, 255);
    }

    public static float easeOut(float t) {
        if (!GuiDraw.guiAnimationsEnabled()) {
            return 1.0f;
        }
        t = Math.clamp(t, 0.0f, 1.0f);
        return 1.0f - (float)Math.pow(1.0f - t, 4.0);
    }

    public static boolean guiAnimationsEnabled() {
        GuiSettings settings = GuiSettings.getInstance();
        return settings == null || settings.guiAnimations.isEnabled();
    }

    public static float sectionTitle(GuiGraphicsExtractor graphics, String text, float x, float y, float a) {
        Text.of(Fonts.MEDIUM, text, x + 1.0f, y + 6.0f, 5.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(140, 145, 156).withAlpha(a)).render(graphics);
        return y + 13.0f;
    }

    public static void renderKnob(GuiGraphicsExtractor graphics, float cx, float cy, Color fill, float a, boolean active) {
        float r = active ? 4.0f : 3.4f;
        RoundedRect.of(cx - r - 0.8f, cy - r - 0.8f, (r + 0.8f) * 2.0f, (r + 0.8f) * 2.0f).radius(r + 0.8f).color(Color.rgba(0, 0, 0, Math.round(70.0f * a))).render(graphics);
        RoundedRect.of(cx - r, cy - r, r * 2.0f, r * 2.0f).radius(r).color(Color.rgb(255, 255, 255).withAlpha(a)).render(graphics);
        float inner = r - 1.2f;
        RoundedRect.of(cx - inner, cy - inner, inner * 2.0f, inner * 2.0f).radius(inner).color(Color.rgb(fill.red(), fill.green(), fill.blue()).withAlpha(a)).render(graphics);
    }

    public static void renderCheckBox(GuiGraphicsExtractor graphics, float x, float y, float s, float t, Color accent, float a) {
        RoundedRect.of(x, y, s, s).radius(2.5f).color(GuiDraw.mix(Color.rgb(60, 63, 72), accent, t).withAlpha(a)).render(graphics);
        if (t > 0.05f) {
            Text.of(Fonts.ICONS, "Q", x + s * 0.5f, y + s * 0.5f, s * 0.7f).centered().color(Color.rgb(255, 255, 255).withAlpha(a * t)).render(graphics);
        }
    }

    public static void renderSwitch(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float t, Color accent, float alpha) {
        RoundedRect.of(x, y, w, h).radius(h * 0.5f).horizontalGradient(GuiDraw.mix(Color.rgb(45, 48, 57), accent, t).withAlpha(alpha), GuiDraw.mix(Color.rgb(45, 48, 57), GuiDraw.getAccent2(), t).withAlpha(alpha)).render(graphics);
        float knob = h - 2.0f;
        RoundedRect.of(x + 1.0f + (w - knob - 2.0f) * t, y + 1.0f, knob, knob).radius(knob * 0.5f).color(Color.rgb(245, 246, 250).withAlpha(alpha)).render(graphics);
    }

    public static boolean canHit(float y, float h, float top, float bottom) {
        return h > 0.0f && bottom > top && y < bottom && y + h > top;
    }

    public static void renderCheck(GuiGraphicsExtractor graphics, float cx, float cy, Color color) {
        Text.of(Fonts.ICONS, "Q", cx, cy, 5.5f).centered().color(color).render(graphics);
    }

    public static void stroke(GuiGraphicsExtractor graphics, float x1, float y1, float x2, float y2, float t, Color color) {
        float len = (float)Math.hypot(x2 - x1, y2 - y1);
        int steps = Math.max(2, (int)Math.ceil(len / 0.2f));
        for (int i = 0; i <= steps; ++i) {
            float k = (float)i / (float)steps;
            float x = x1 + (x2 - x1) * k;
            float y = y1 + (y2 - y1) * k;
            RoundedRect.of(x - t * 0.5f, y - t * 0.5f, t, t).radius(t * 0.5f).color(color).render(graphics);
        }
    }

    public static void renderDropHeader(GuiGraphicsExtractor graphics, String text, float rightX, float cy, float open, float alpha, boolean hover) {
        float size = 5.3f;
        float tw = Text.of(Fonts.MEDIUM, text).size(size).width();
        float arrowX = rightX - 2.5f;
        Text.of(Fonts.ICONS, open > 0.5f ? "P" : "O", arrowX, cy, 5.0f).centered().color(Color.rgb(150, 155, 165).withAlpha(alpha)).render(graphics);
        Text.of(Fonts.MEDIUM, text, arrowX - 5.0f - tw, cy, size).verticalAlign(VerticalAlign.MIDDLE).color((hover ? Color.rgb(240, 242, 248) : Color.rgb(205, 209, 218)).withAlpha(alpha)).render(graphics);
    }

    public static float dropHeaderWidth(String text) {
        return Text.of(Fonts.MEDIUM, text).size(5.3f).width() + 9.0f;
    }

    public static String format(float value, float step) {
        if (step >= 1.0f) {
            return String.valueOf(Math.round(value));
        }
        if (step >= 0.1f) {
            return String.format(Locale.ROOT, "%.1f", Float.valueOf(value));
        }
        return String.format(Locale.ROOT, "%.2f", Float.valueOf(value));
    }

    public static String fit(String text, float size, float maxW) {
        if (text == null) {
            return "";
        }
        if (Text.of(Fonts.MEDIUM, text).size(size).width() <= maxW) {
            return text;
        }
        String s = text;
        while (s.length() > 1 && Text.of(Fonts.MEDIUM, s + "...").size(size).width() > maxW) {
            s = s.substring(0, s.length() - 1);
        }
        return s.trim() + "...";
    }

    public static Color mix(Color a, Color b, float t) {
        return Color.rgb(Math.round((float)a.red() + (float)(b.red() - a.red()) * t), Math.round((float)a.green() + (float)(b.green() - a.green()) * t), Math.round((float)a.blue() + (float)(b.blue() - a.blue()) * t));
    }
}

