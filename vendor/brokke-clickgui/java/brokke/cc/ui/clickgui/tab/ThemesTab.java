/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.tab;

import brokke.cc.core.module.impl.render.GuiSettings;
import brokke.cc.core.setting.ColorSetting;
import brokke.cc.core.setting.ConfigManager;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.color.Color;
import brokke.cc.util.color.Theme;
import brokke.cc.util.render.display.outline.Outline;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.scissor.Scissor;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.TextAlign;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ThemesTab
extends GuiComponent {
    public int editingSlot = 0;
    public int pickerDrag = 0;

    public ThemesTab(ClickGuiContext ctx) {
        super(ctx);
    }

    public float renderThemes(GuiGraphicsExtractor graphics, float x, float y, float w, float prog, float top, float bottom, long now) {
        GuiSettings gs = GuiSettings.getInstance();
        if (gs == null) {
            return 0.0f;
        }
        float startY = y;
        float appear = GuiDraw.easeOut((float)(now - this.ctx.switchTime) / 380.0f);
        float a = prog * appear;
        y += (1.0f - appear) * 8.0f;
        y = GuiDraw.sectionTitle(graphics, "\u0422\u0435\u043c\u044b", x, y, a);
        List<String> names = gs.theme.getModes();
        int cols = Math.max(4, Math.round(w / 62.0f));
        float gap = 4.0f;
        float cardW = (w - gap * (float)(cols - 1)) / (float)cols;
        float cardH = 22.0f;
        String current = (String)gs.theme.get();
        for (int i = 0; i < names.size(); ++i) {
            String name = names.get(i);
            float cx = x + (float)(i % cols) * (cardW + gap);
            float cy = y + (float)(i / cols) * (cardH + gap);
            float ca = a * GuiDraw.easeOut((float)(now - this.ctx.switchTime - (long)i * 30L) / 380.0f);
            this.renderThemeCard(graphics, gs, name, cx, cy, cardW, cardH, ca, name.equals(current), top, bottom);
        }
        y += (float)((names.size() + cols - 1) / cols) * (cardH + gap) + 2.0f;
        float customOpen = this.anim("custom-open", "Custom".equals(current), 5.0f);
        if (customOpen > 0.01f) {
            float pickerH = this.customPickerHeight();
            float visibleH = pickerH * customOpen;
            float visTop = Math.max(y, top);
            float visBottom = Math.min(y + visibleH, bottom);
            if (visBottom - visTop >= 1.5f) {
                Scissor.push(graphics, x - 1.0f, visTop, w + 2.0f, visBottom - visTop);
                this.renderCustomPicker(graphics, gs, x, y, w, a * customOpen, customOpen > 0.95f ? top : Float.MAX_VALUE, visBottom);
                Scissor.pop(graphics);
            }
            y += visibleH;
        }
        y += this.ctx.settings.renderSetting(graphics, gs.newHud, x + 8.0f, y, w - 16.0f, a, GuiDraw.getAccent(), top, bottom);
        y = GuiDraw.sectionTitle(graphics, "\u041e\u043a\u043d\u043e", x, y, a);
        float boxH = this.ctx.settings.settingHeight(gs.cornerRadius) + this.ctx.settings.settingHeight(gs.blur) + this.ctx.settings.settingHeight(gs.guiSize) + 40.0f;
        RoundedRect.of(x, y, w, boxH).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round(6.0f * a))).render(graphics);
        float sy = y + 4.0f;
        sy += this.ctx.settings.renderSetting(graphics, gs.cornerRadius, x + 8.0f, sy, w - 16.0f, a, GuiDraw.getAccent(), top, bottom);
        sy += this.ctx.settings.renderSetting(graphics, gs.blur, x + 8.0f, sy, w - 16.0f, a, GuiDraw.getAccent(), top, bottom);
        sy += this.ctx.settings.renderSetting(graphics, gs.guiSize, x + 8.0f, sy, w - 16.0f, a, GuiDraw.getAccent(), top, bottom);
        float animationToggle = this.anim("gui-animations-enabled", gs.guiAnimations.isEnabled(), 8.0f);
        Text.of(Fonts.MEDIUM, "\u0410\u043d\u0438\u043c\u0430\u0446\u0438\u0438 \u0433\u0443\u0438", x + 8.0f, sy + 8.0f, 5.3f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(205, 209, 218).withAlpha(a)).render(graphics);
        GuiDraw.renderSwitch(graphics, x + w - 24.0f, sy + 4.0f, 16.0f, 8.0f, animationToggle, GuiDraw.getAccent(), a);
        if (GuiDraw.canHit(sy, 16.0f, top, bottom)) {
            this.hit(x + 8.0f, sy, w - 16.0f, 16.0f, () -> {
                gs.guiAnimations.toggle();
                ClientSound.checkbox(gs.guiAnimations.isEnabled());
                ConfigManager.saveConfig("backup");
            }, null);
        }
        float mute = this.anim("gui-sounds-muted", !gs.sounds.isEnabled(), 8.0f);
        Text.of(Fonts.MEDIUM, "\u0412\u044b\u043a\u043b\u044e\u0447\u0438\u0442\u044c \u0433\u0443\u0438 \u0437\u0432\u0443\u043a\u0438", x + 8.0f, (sy += 16.0f) + 8.0f, 5.3f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(205, 209, 218).withAlpha(a)).render(graphics);
        GuiDraw.renderSwitch(graphics, x + w - 24.0f, sy + 4.0f, 16.0f, 8.0f, mute, GuiDraw.getAccent(), a);
        if (GuiDraw.canHit(sy, 16.0f, top, bottom)) {
            this.hit(x + 8.0f, sy, w - 16.0f, 16.0f, () -> {
                gs.sounds.toggle();
                ClientSound.checkbox(gs.sounds.isEnabled());
                ConfigManager.saveConfig("backup");
            }, null);
        }
        return (y += boxH) - startY;
    }

    private void renderThemeCard(GuiGraphicsExtractor graphics, GuiSettings gs, String name, float x, float y, float w, float h, float a, boolean active, float top, float bottom) {
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        String key = "theme-card-" + name;
        float hv = this.anim(key, this.hov(key, hover), 7.0f);
        float sel = this.anim(String.valueOf(key) + "-sel", active, 6.0f);
        Color[] colors = this.themeColors(gs, name);
        RoundedRect.of(x, y, w, h).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round((6.0f + 4.0f * hv) * a))).render(graphics);
        if (sel > 0.01f) {
            Outline.of(x, y, w, h).radius(5.0f).thickness(0.8f).color(colors[0].withAlpha(0.9f * sel * a)).render(graphics);
        }
        float gx = x + 3.0f;
        float gy = y + 3.0f;
        float gw = w - 6.0f;
        float gh = 8.0f;
        RoundedRect.of(gx, gy, gw, gh).radius(3.0f).horizontalGradient(colors[0].withAlpha(a), colors[1].withAlpha(a)).render(graphics);
        if (sel > 0.01f) {
            Text.of(Fonts.ICONS, "Q", gx + gw - 4.5f, gy + gh * 0.5f, 4.5f).centered().color(Color.rgb(255, 255, 255).withAlpha(a * sel)).render(graphics);
        }
        String label = "Custom".equals(name) ? "Custom" : name;
        Text.of(Fonts.MEDIUM, GuiDraw.fit(label, 4.3f, w - 6.0f), x + 3.5f, y + h - 5.5f, 4.3f).verticalAlign(VerticalAlign.MIDDLE).color(GuiDraw.mix(Color.rgb(165, 170, 180), Color.rgb(240, 242, 248), Math.max(sel, hv * 0.6f)).withAlpha(a)).render(graphics);
        if (GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(x, y, w, h, () -> {
                if (!name.equals(gs.theme.get())) {
                    gs.theme.set(name);
                    ClientSound.ui(ClientSound.Ui.MODE);
                }
            }, null);
        }
    }

    private Color[] themeColors(GuiSettings gs, String name) {
        if ("Custom".equals(name)) {
            return new Color[]{gs.customPrimary.getColor(), gs.customSecondary.getColor()};
        }
        Theme theme = Theme.fromName(name);
        return new Color[]{theme.getPrimary(), theme.getSecondary()};
    }

    private float customPickerHeight() {
        return 106.0f;
    }

    private void renderCustomPicker(GuiGraphicsExtractor graphics, GuiSettings gs, float x, float y, float w, float a, float top, float bottom) {
        y = GuiDraw.sectionTitle(graphics, "\u0421\u0432\u043e\u0438 \u0446\u0432\u0435\u0442\u0430", x, y, a);
        float boxH = this.customPickerHeight() - 13.0f - 3.0f;
        RoundedRect.of(x, y, w, boxH).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round(6.0f * a))).render(graphics);
        ColorSetting[] slots = new ColorSetting[]{gs.customPrimary, gs.customSecondary};
        String[] labels = new String[]{"\u0426\u0432\u0435\u0442 1", "\u0426\u0432\u0435\u0442 2"};
        float pad = 6.0f;
        float slotGap = 5.0f;
        float slotW = (w - pad * 2.0f - slotGap) * 0.5f;
        float slotH = 13.0f;
        float sy = y + pad;
        for (int i = 0; i < 2; ++i) {
            int idx = i;
            ColorSetting cs = slots[i];
            float sx = x + pad + (float)i * (slotW + slotGap);
            boolean hover = GuiDraw.canHit(sy, slotH, top, bottom) && this.hovered(sx, sy, slotW, slotH);
            String key = "slot-" + i;
            float hv = this.anim(key, this.hov(key, hover), 7.0f);
            float sel = this.anim(String.valueOf(key) + "-sel", this.editingSlot == i, 6.0f);
            RoundedRect.of(sx, sy, slotW, slotH).radius(3.5f).color(Color.rgba(255, 255, 255, Math.round((7.0f + 5.0f * hv + 6.0f * sel) * a))).render(graphics);
            if (sel > 0.01f) {
                Outline.of(sx, sy, slotW, slotH).radius(3.5f).thickness(0.7f).color(Color.rgba(255, 255, 255, Math.round(45.0f * sel * a))).render(graphics);
            }
            float dot = 7.0f;
            RoundedRect.of(sx + 3.5f, sy + (slotH - dot) * 0.5f, dot, dot).radius(dot * 0.5f).color(cs.getColor().withAlpha(a)).render(graphics);
            Text.of(Fonts.MEDIUM, labels[i], sx + 14.0f, sy + slotH * 0.5f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(GuiDraw.mix(Color.rgb(165, 170, 180), Color.rgb(240, 242, 248), Math.max(sel, hv * 0.6f)).withAlpha(a)).render(graphics);
            Text.of(Fonts.MEDIUM, "#" + cs.getHexDigits().substring(0, 6), sx + slotW - 4.0f, sy + slotH * 0.5f, 4.4f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(120, 125, 136).withAlpha(a)).render(graphics);
            if (!GuiDraw.canHit(sy, slotH, top, bottom)) continue;
            this.hit(sx, sy, slotW, slotH, () -> {
                if (this.editingSlot != idx) {
                    this.editingSlot = idx;
                    ClientSound.ui(ClientSound.Ui.CLICK);
                }
            }, null);
        }
        ColorSetting cs = slots[this.editingSlot];
        float bx = x + pad;
        float bw = w - pad * 2.0f;
        float svY = sy + slotH + 6.0f;
        float hueY = svY + 52.0f + 5.0f;
        if (this.pickerDrag == 1) {
            float s = Math.clamp((this.getMouseX() - bx) / bw, 0.0f, 1.0f);
            float b = 1.0f - Math.clamp((this.getMouseY() - svY) / 52.0f, 0.0f, 1.0f);
            cs.setSV(s, b);
        } else if (this.pickerDrag == 2) {
            cs.setHue(Math.clamp((this.getMouseX() - bx) / bw, 0.0f, 0.999f));
        }
        float hue = cs.getHue();
        Color pure = Color.hsb(hue, 1.0f, 1.0f);
        RoundedRect.of(bx, svY, bw, 52.0f).radius(4.0f).horizontalGradient(Color.rgb(255, 255, 255).withAlpha(a), pure.withAlpha(a)).render(graphics);
        RoundedRect.of(bx, svY, bw, 52.0f).radius(4.0f).verticalGradient(Color.rgba(0, 0, 0, 0), Color.rgba(0, 0, 0, Math.round(255.0f * a))).render(graphics);
        float kx = bx + bw * cs.getSaturation();
        float ky = svY + 52.0f * (1.0f - cs.getBrightness());
        GuiDraw.renderKnob(graphics, kx, ky, cs.getColor(), a, this.pickerDrag == 1);
        float segW = bw / 6.0f;
        for (int i = 0; i < 6; ++i) {
            Color c1 = Color.hsb((float)i / 6.0f, 1.0f, 1.0f);
            Color c2 = Color.hsb((float)(i + 1) / 6.0f, 1.0f, 1.0f);
            float r = 3.0f;
            RoundedRect rect = RoundedRect.of(bx + (float)i * segW - (i > 0 ? 0.3f : 0.0f), hueY, segW + (i > 0 ? 0.3f : 0.0f), 6.0f).horizontalGradient(c1.withAlpha(a), c2.withAlpha(a));
            if (i == 0) {
                rect.radius(r, 0.0f, 0.0f, r);
            } else if (i == 5) {
                rect.radius(0.0f, r, r, 0.0f);
            }
            rect.render(graphics);
        }
        GuiDraw.renderKnob(graphics, bx + bw * hue, hueY + 3.0f, pure, a, this.pickerDrag == 2);
        if (GuiDraw.canHit(svY, 52.0f, top, bottom)) {
            this.hit(bx - 2.0f, svY - 2.0f, bw + 4.0f, 56.0f, () -> {
                this.pickerDrag = 1;
            }, null);
        }
        if (GuiDraw.canHit(hueY, 6.0f, top, bottom)) {
            this.hit(bx - 2.0f, hueY - 3.0f, bw + 4.0f, 12.0f, () -> {
                this.pickerDrag = 2;
            }, null);
        }
    }
}

