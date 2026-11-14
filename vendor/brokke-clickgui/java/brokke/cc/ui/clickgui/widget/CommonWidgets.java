/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.widget;

import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.outline.Outline;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class CommonWidgets
extends GuiComponent {
    public CommonWidgets(ClickGuiContext ctx) {
        super(ctx);
    }

    public void renderConfigButton(GuiGraphicsExtractor graphics, String id, String label, String icon, float x, float y, float w, float h, float a, boolean danger, float top, float bottom, Runnable action) {
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        String key = "config-btn-" + id;
        float hv = this.anim(key, this.hov(key, hover), 7.0f);
        Color tint = danger ? Color.rgb(255, 90, 90) : GuiDraw.getAccent();
        RoundedRect.of(x, y, w, h).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round(8.0f * a))).render(graphics);
        if (hv > 0.01f) {
            RoundedRect.of(x, y, w, h).radius(4.0f).color(tint.withAlpha(0.18f * hv * a)).render(graphics);
        }
        float tw = Text.of(Fonts.MEDIUM, label).size(5.0f).width();
        float total = tw + 9.0f;
        float sx = x + (w - total) * 0.5f;
        Color c = GuiDraw.mix(Color.rgb(190, 194, 204), danger ? Color.rgb(255, 130, 130) : Color.rgb(245, 247, 252), hv).withAlpha(a);
        float icx = sx + 3.0f;
        float icy = y + h * 0.5f;
        if (icon.equals("x")) {
            GuiDraw.stroke(graphics, icx - 1.8f, icy - 1.8f, icx + 1.8f, icy + 1.8f, 0.85f, c);
            GuiDraw.stroke(graphics, icx - 1.8f, icy + 1.8f, icx + 1.8f, icy - 1.8f, 0.85f, c);
        } else if (icon.equals(">")) {
            GuiDraw.stroke(graphics, icx - 1.0f, icy - 2.2f, icx + 1.2f, icy, 0.9f, c);
            GuiDraw.stroke(graphics, icx + 1.2f, icy, icx - 1.0f, icy + 2.2f, 0.9f, c);
        } else {
            Text.of(Fonts.ICONS, icon, icx, icy, 5.5f).centered().color(c).render(graphics);
        }
        Text.of(Fonts.MEDIUM, label, sx + 9.0f, y + h * 0.5f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(c).render(graphics);
        if (GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(x, y, w, h, () -> {
                ClientSound.ui(ClientSound.Ui.CLICK);
                action.run();
            }, null);
        }
    }

    public void renderEventPill(GuiGraphicsExtractor graphics, String id, String label, String icon, float x, float y, float w, float h, float a, boolean accentHover, float top, float bottom, Runnable action) {
        boolean hover = action != null && GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        String key = "event-pill-" + id;
        float hv = this.anim(key, this.hov(key, hover), 7.0f);
        RoundedRect.of(x, y, w, h).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round((8.0f + 5.0f * hv) * a))).render(graphics);
        if (accentHover && hv > 0.01f) {
            RoundedRect.of(x, y, w, h).radius(4.0f).color(GuiDraw.getAccent().withAlpha(0.2f * hv * a)).render(graphics);
        }
        Color c = GuiDraw.mix(Color.rgb(170, 175, 185), Color.rgb(245, 247, 252), hv).withAlpha(action == null ? a * 0.6f : a);
        if (icon.equals("<") || icon.equals(">")) {
            float dir = icon.equals("<") ? 1.0f : -1.0f;
            float ix = x + 7.0f;
            float iy = y + h * 0.5f;
            GuiDraw.stroke(graphics, ix + 1.0f * dir, iy - 2.2f, ix - 1.2f * dir, iy, 0.9f, c);
            GuiDraw.stroke(graphics, ix - 1.2f * dir, iy, ix + 1.0f * dir, iy + 2.2f, 0.9f, c);
        } else {
            Text.of(Fonts.ICONS, icon, x + 7.0f, y + h * 0.5f, 5.5f).centered().color(c).render(graphics);
        }
        Text.of(Fonts.MEDIUM, label, x + 13.0f, y + h * 0.5f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(c).render(graphics);
        if (action != null && GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(x, y, w, h, action, null);
        }
    }

    public void renderMcpButton(GuiGraphicsExtractor graphics, Object key, String label, float x, float y, float w, float h, float a, Color tint, float top, float bottom, Runnable action) {
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        float hv = this.anim(key, this.hov(key, hover), 8.0f);
        RoundedRect.of(x, y, w, h).radius(3.0f).color(Color.rgba(255, 255, 255, Math.round(12.0f * (1.0f - hv) * a))).render(graphics);
        if (hv > 0.01f) {
            RoundedRect.of(x, y, w, h).radius(3.0f).color(tint.withAlpha(0.75f * hv * a)).render(graphics);
        }
        Text.of(Fonts.MEDIUM, label, x + w * 0.5f, y + h * 0.5f, 4.6f).centered().color(GuiDraw.mix(Color.rgb(205, 209, 218), Color.rgb(255, 255, 255), hv).withAlpha(a)).render(graphics);
        if (GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(x, y, w, h, () -> {
                ClientSound.ui(ClientSound.Ui.CLICK);
                action.run();
            }, null);
        }
    }

    public float renderAbField(GuiGraphicsExtractor graphics, String id, String placeholder, String icon, float x, float y, float w, float h, float a, Color accent, float top, float bottom, long now, String initial) {
        boolean shown;
        boolean focused = id.equals(this.ctx.focus.abField);
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        String key = "ab-field-" + id;
        float hv = this.anim(key, this.hov(key, hover), 7.0f);
        float fc = this.anim(String.valueOf(key) + "-focus", focused, 6.0f);
        RoundedRect.of(x, y, w, h).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round((8.0f + 4.0f * hv + 6.0f * fc) * a))).render(graphics);
        if (fc > 0.01f) {
            Outline.of(x, y, w, h).radius(4.0f).thickness(0.7f).color(accent.withAlpha(0.6f * fc * a)).render(graphics);
        }
        float tx = x + 5.0f;
        if (icon != null) {
            Text.of(Fonts.ICONS, icon, x + 8.0f, y + h * 0.5f, 6.0f).centered().color(GuiDraw.mix(Color.rgb(130, 135, 146), Color.rgb(225, 228, 236), fc).withAlpha(a)).render(graphics);
            tx = x + 16.0f;
        }
        boolean bl = shown = !focused && (id.startsWith("bot:") || id.startsWith("bot-setting:") || id.startsWith("module-setting:")) && !initial.isEmpty();
        String text = focused ? this.ctx.focus.abInput.toString() : (shown ? GuiDraw.fit(initial, 5.2f, w - (tx - x) - 4.0f) : "");
        float cy = y + h * 0.5f;
        float size = 5.2f;
        if (text.isEmpty()) {
            Text.of(Fonts.MEDIUM, GuiDraw.fit(placeholder, size, w - (tx - x) - 4.0f), tx, cy, size).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(120, 125, 136).withAlpha(a)).render(graphics);
        } else {
            for (int i = 0; i < text.length(); ++i) {
                float cx = tx + Text.of(Fonts.MEDIUM, text.substring(0, i)).size(size).width();
                long born = !shown && i < this.ctx.focus.abCharTimes.size() ? this.ctx.focus.abCharTimes.get(i) : 0L;
                float t = GuiDraw.easeOut((float)(now - born) / 220.0f);
                Text.of(Fonts.MEDIUM, String.valueOf(text.charAt(i)), cx, cy + (1.0f - t) * 4.0f, size).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 238, 245).withAlpha(a * t)).render(graphics);
            }
        }
        if (focused) {
            float blink = 0.5f + 0.5f * (float)Math.cos((double)now / 170.0);
            float caretX = tx + (text.isEmpty() ? 0.0f : Text.of(Fonts.MEDIUM, text).size(size).width() + 0.5f);
            RoundedRect.of(caretX, y + 3.5f, 0.6f, h - 7.0f).color(Color.rgb(235, 238, 245).withAlpha(a * blink)).render(graphics);
        }
        if (GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(x, y, w, h, () -> {
                this.ctx.focus.setAbField(id, initial);
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, () -> {
                if (id.equals(this.ctx.focus.abField)) {
                    this.ctx.focus.abInput.setLength(0);
                    this.ctx.focus.abCharTimes.clear();
                }
            });
        }
        return y + h;
    }

    public void renderAbChip(GuiGraphicsExtractor graphics, Object key, String text, float x, float y, float w, float h, float a, Color accent, boolean editing, boolean active, float top, float bottom, Runnable action) {
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        float hv = this.anim(key, this.hov(key, hover), 8.0f);
        float ed = this.anim(String.valueOf(key) + "-ed", editing, 8.0f);
        RoundedRect.of(x, y, w, h).radius(3.0f).color(Color.rgba(255, 255, 255, Math.round((10.0f + 6.0f * hv) * a))).render(graphics);
        if (ed > 0.01f) {
            RoundedRect.of(x, y, w, h).radius(3.0f).color(accent.withAlpha(0.25f * ed * a)).render(graphics);
            Outline.of(x, y, w, h).radius(3.0f).thickness(0.6f).color(accent.withAlpha(0.8f * ed * a)).render(graphics);
        }
        Color c = editing ? Color.rgb(250, 250, 255) : (active ? Color.rgb(220, 224, 232) : Color.rgb(130, 135, 146));
        Text.of(Fonts.MEDIUM, text, x + w * 0.5f, y + h * 0.5f, 4.6f).centered().color(GuiDraw.mix(c, Color.rgb(250, 250, 255), hv * 0.5f).withAlpha(a)).render(graphics);
        if (action != null && GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(x, y, w, h, action, null);
        }
    }

    public float renderAbStepper(GuiGraphicsExtractor graphics, String id, String label, String value, float x, float y, float w, float a, Color accent, float top, float bottom, Runnable minus, Runnable plus) {
        float h = 17.0f;
        String key = "ab-step-" + id;
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.ctx.hovered(x, y, w, h);
        float hv = this.ctx.anim(key, this.ctx.hov(key, hover), 7.0f);
        RoundedRect.of(x, y, w, h).radius(4.5f).color(Color.rgba(255, 255, 255, Math.round((6.0f + 4.0f * hv) * a))).render(graphics);
        Text.of(Fonts.MEDIUM, label, x + 7.0f, y + h * 0.5f, 5.4f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(215, 219, 228).withAlpha(a)).render(graphics);
        float bs = 11.0f;
        float vw = 34.0f;
        float px = x + w - bs - 6.0f;
        float vx = px - vw;
        float mx = vx - bs;
        float by = y + (h - bs) * 0.5f;
        this.renderAbChip(graphics, String.valueOf(key) + "-minus", "-", mx, by, bs, bs, a, accent, false, true, top, bottom, () -> {
            minus.run();
            this.ctx.saveAutoBuy();
            ClientSound.ui(ClientSound.Ui.HOVER);
        });
        Text.of(Fonts.MEDIUM, value, vx + vw * 0.5f, y + h * 0.5f, 5.0f).centered().color(accent.withAlpha(a)).render(graphics);
        this.renderAbChip(graphics, String.valueOf(key) + "-plus", "+", px, by, bs, bs, a, accent, false, true, top, bottom, () -> {
            plus.run();
            this.ctx.saveAutoBuy();
            ClientSound.ui(ClientSound.Ui.HOVER);
        });
        return y + h + 3.0f;
    }
}

