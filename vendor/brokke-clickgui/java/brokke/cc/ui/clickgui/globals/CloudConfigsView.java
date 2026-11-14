/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.globals;

import brokke.cc.core.setting.ConfigManager;
import brokke.cc.ui.clickgui.globals.CloudConfigs;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.outline.Outline;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.TextAlign;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class CloudConfigsView
extends GuiComponent {
    private static final SimpleDateFormat DATE = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    public final Map<String, String> fields = new HashMap<String, String>();
    private String expanded = null;
    private boolean uploadOpen = false;
    private final Consumer<CloudConfigs.Result> toast = r -> {
        this.ctx.configToast(r.message(), r.ok() ? Color.rgb(90, 210, 140) : Color.rgb(235, 90, 90));
        ClientSound.checkbox(r.ok());
    };

    public CloudConfigsView(ClickGuiContext ctx) {
        super(ctx);
    }

    public void redeemCode() {
        String code = this.fields.getOrDefault("cloudcfg:code", "").trim();
        if (code.isEmpty()) {
            this.toast.accept(new CloudConfigs.Result(false, "\u0412\u0432\u0435\u0434\u0438 \u043a\u043b\u044e\u0447"));
            return;
        }
        this.fields.put("cloudcfg:code", "");
        CloudConfigs.redeem(code, this.toast);
    }

    public float render(GuiGraphicsExtractor graphics, float x, float y, float w, float a, Color accent, float top, float bottom, long now) {
        CloudConfigs.refreshIfStale();
        float h = 15.0f;
        float btnW = 62.0f;
        this.renderField(graphics, "cloudcfg:code", "\u041a\u043b\u044e\u0447 \u043a\u043e\u043d\u0444\u0438\u0433\u0430 (XXXX-XXXX-XXXX)", "K", x, y, w - btnW - 4.0f, h, a, accent, top, bottom, now);
        this.button(graphics, "cc-redeem", "\u0410\u043a\u0442\u0438\u0432\u0438\u0440\u043e\u0432\u0430\u0442\u044c", x + w - btnW, y, btnW, h, a, accent, true, top, bottom, () -> {
            if ("cloudcfg:code".equals(this.ctx.focus.abField)) {
                this.ctx.focus.setAbField(null, "");
            }
            this.redeemCode();
        });
        y += h + 3.0f;
        y = this.toastLine(graphics, x, y, a, now);
        if (!CloudConfigs.isLoaded()) {
            String s = CloudConfigs.isConnected() ? "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430 \u043e\u0431\u043b\u0430\u043a\u0430..." : "\u0421\u0435\u0440\u0432\u0435\u0440 \u043a\u043e\u043d\u0444\u0438\u0433\u043e\u0432 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d";
            Text.of(Fonts.MEDIUM, s, x + w * 0.5f, y + 14.0f, 5.4f).centered().color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
            return y + 30.0f;
        }
        List<CloudConfigs.Config> own = CloudConfigs.own();
        y = GuiDraw.sectionTitle(graphics, "\u041c\u043e\u0438 \u043e\u0431\u043b\u0430\u0447\u043d\u044b\u0435 (" + own.size() + ")", x, y, a);
        float upW = 70.0f;
        this.button(graphics, "cc-upload-toggle", this.uploadOpen ? "\u0421\u043a\u0440\u044b\u0442\u044c" : "+ \u0412\u044b\u0433\u0440\u0443\u0437\u0438\u0442\u044c", x + w - upW, y - 11.0f, upW, 10.0f, a, accent, !this.uploadOpen, top, bottom, () -> {
            this.uploadOpen = !this.uploadOpen;
        });
        if (this.uploadOpen) {
            y = this.renderUploadList(graphics, x, y, w, a, accent, top, bottom);
        }
        if (own.isEmpty()) {
            Text.of(Fonts.MEDIUM, "\u0412\u044b\u0433\u0440\u0443\u0437\u0438 \u043b\u043e\u043a\u0430\u043b\u044c\u043d\u044b\u0439 \u043a\u043e\u043d\u0444\u0438\u0433, \u0447\u0442\u043e\u0431\u044b \u0440\u0430\u0437\u0434\u0430\u0432\u0430\u0442\u044c \u0435\u0433\u043e \u043f\u043e \u043a\u043b\u044e\u0447\u0430\u043c", x + w * 0.5f, y + 8.0f, 5.0f).centered().color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
            y += 20.0f;
        }
        for (CloudConfigs.Config cfg : own) {
            y = this.renderRow(graphics, cfg, x, y, w, a, accent, top, bottom, now);
        }
        List<CloudConfigs.Config> shared = CloudConfigs.shared();
        y += 4.0f;
        y = GuiDraw.sectionTitle(graphics, "\u041f\u043e\u043b\u0443\u0447\u0435\u043d\u043d\u044b\u0435 \u043f\u043e \u043a\u043b\u044e\u0447\u0443 (" + shared.size() + ")", x, y, a);
        if (shared.isEmpty()) {
            Text.of(Fonts.MEDIUM, "\u0410\u043a\u0442\u0438\u0432\u0438\u0440\u0443\u0439 \u043a\u043b\u044e\u0447, \u0447\u0442\u043e\u0431\u044b \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0447\u0443\u0436\u043e\u0439 \u043a\u043e\u043d\u0444\u0438\u0433", x + w * 0.5f, y + 8.0f, 5.0f).centered().color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
            y += 20.0f;
        }
        for (CloudConfigs.Config cfg : shared) {
            y = this.renderRow(graphics, cfg, x, y, w, a, accent, top, bottom, now);
        }
        return y;
    }

    private float toastLine(GuiGraphicsExtractor graphics, float x, float y, float a, long now) {
        float toast = 1.0f - GuiDraw.easeOut(Math.max(0.0f, (float)(now - this.ctx.configToastTime - 2500L) / 500.0f));
        if (this.ctx.configToastText != null && toast > 0.01f) {
            Text.of(Fonts.MEDIUM, this.ctx.configToastText, x + 2.0f, y + 3.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(this.ctx.configToastColor.withAlpha(a * toast)).render(graphics);
        }
        return y + 9.0f;
    }

    private float renderUploadList(GuiGraphicsExtractor graphics, float x, float y, float w, float a, Color accent, float top, float bottom) {
        List<String> local = ConfigManager.getAvailableConfigs();
        local.sort(String.CASE_INSENSITIVE_ORDER);
        if (local.isEmpty()) {
            Text.of(Fonts.MEDIUM, "\u041d\u0435\u0442 \u043b\u043e\u043a\u0430\u043b\u044c\u043d\u044b\u0445 \u043a\u043e\u043d\u0444\u0438\u0433\u043e\u0432", x + 4.0f, y + 5.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
            return y + 12.0f;
        }
        float chipH = 12.0f;
        float cx = x;
        for (String name : local) {
            float cw = Text.of(Fonts.MEDIUM, name).size(4.8f).width() + 16.0f;
            if (cx + cw > x + w) {
                cx = x;
                y += chipH + 3.0f;
            }
            this.button(graphics, "cc-up-" + name, name, cx, y, cw, chipH, a, accent, false, top, bottom, () -> CloudConfigs.upload(name, this.toast));
            cx += cw + 3.0f;
        }
        return y + chipH + 6.0f;
    }

    private float renderRow(GuiGraphicsExtractor graphics, CloudConfigs.Config cfg, float x, float y, float w, float a, Color accent, float top, float bottom, long now) {
        float h = 20.0f;
        String key = "cc-row-" + cfg.id();
        boolean open = cfg.own() && cfg.id().equals(this.expanded);
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        float hv = this.anim(key, this.hov(key, hover), 7.0f);
        float op = this.anim(key + "-open", open, 6.0f);
        RoundedRect.of(x, y, w, h).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round((6.0f + 4.0f * hv + 3.0f * op) * a))).render(graphics);
        float ib = 12.0f;
        float ix = x + 5.0f;
        float iy = y + (h - ib) * 0.5f;
        RoundedRect.of(ix, iy, ib, ib).radius(3.5f).horizontalGradient(accent.withAlpha(0.7f * a), GuiDraw.getAccent2().withAlpha(0.7f * a)).render(graphics);
        Text.of(Fonts.ICONS, cfg.own() ? "F" : "K", ix + ib * 0.5f, iy + ib * 0.5f, 6.0f).centered().color(Color.rgb(255, 255, 255).withAlpha(a)).render(graphics);
        float nx = ix + ib + 5.0f;
        Text.of(Fonts.MEDIUM, GuiDraw.fit(cfg.name(), 5.6f, w - 140.0f), nx, y + h * 0.5f - 2.5f, 5.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(232, 235, 242).withAlpha(a)).render(graphics);
        String sub = cfg.own() ? DATE.format(new Date(cfg.updated())) + "  |  \u0441\u043a\u0430\u0447\u0430\u043d " + cfg.downloads() + " \u0440\u0430\u0437" : "\u043e\u0442 " + cfg.author() + "  |  " + DATE.format(new Date(cfg.updated()));
        Text.of(Fonts.MEDIUM, sub, nx, y + h * 0.5f + 3.5f, 4.3f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(110, 115, 126).withAlpha(a)).render(graphics);
        float bh = 12.0f;
        float by = y + (h - bh) * 0.5f;
        float bx = x + w - bh - 4.0f;
        this.iconButton(graphics, key + "-del", bx, by, bh, a, Color.rgb(255, 85, 85), top, bottom, () -> {
            CloudConfigs.delete(cfg, this.toast);
            if (cfg.id().equals(this.expanded)) {
                this.expanded = null;
            }
        });
        float lw = 42.0f;
        this.button(graphics, key + "-load", "\u0417\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c", bx -= lw + 4.0f, by, lw, bh, a, accent, true, top, bottom, () -> CloudConfigs.apply(cfg, this.toast));
        if (cfg.own()) {
            float kw = 34.0f;
            this.button(graphics, key + "-keys", open ? "\u0421\u043a\u0440\u044b\u0442\u044c" : "\u041a\u043b\u044e\u0447\u0438", bx -= kw + 4.0f, by, kw, bh, a, accent, false, top, bottom, () -> {
                String string = this.expanded = open ? null : cfg.id();
                if (!open) {
                    CloudConfigs.requestCodes(cfg.id());
                }
                ClientSound.ui(ClientSound.Ui.CLICK);
            });
        }
        y += h + 3.0f;
        if (open) {
            y = this.renderKeys(graphics, cfg, x + 8.0f, y, w - 8.0f, a, accent, top, bottom, now);
        }
        return y;
    }

    private float renderKeys(GuiGraphicsExtractor graphics, CloudConfigs.Config cfg, float x, float y, float w, float a, Color accent, float top, float bottom, long now) {
        float h = 13.0f;
        float bw = 66.0f;
        String usesId = "cloudcfg:uses";
        this.renderField(graphics, usesId, "\u041a\u043e\u043b-\u0432\u043e \u0430\u043a\u0442\u0438\u0432\u0430\u0446\u0438\u0439 (1)", "F", x, y, w - bw - 4.0f, h, a, accent, top, bottom, now);
        this.button(graphics, "cc-mk-" + cfg.id(), "\u0421\u043e\u0437\u0434\u0430\u0442\u044c \u043a\u043b\u044e\u0447", x + w - bw, y, bw, h, a, accent, true, top, bottom, () -> {
            int uses;
            if (usesId.equals(this.ctx.focus.abField)) {
                this.ctx.focus.setAbField(null, "");
            }
            try {
                uses = Math.max(1, Integer.parseInt(this.fields.getOrDefault(usesId, "1").trim()));
            }
            catch (NumberFormatException e) {
                uses = 1;
            }
            CloudConfigs.createCode(cfg, uses, this.toast);
        });
        y += h + 3.0f;
        List<CloudConfigs.Code> codes = CloudConfigs.codes(cfg.id());
        if (codes.isEmpty()) {
            Text.of(Fonts.MEDIUM, "\u0410\u043a\u0442\u0438\u0432\u043d\u044b\u0445 \u043a\u043b\u044e\u0447\u0435\u0439 \u043d\u0435\u0442", x + 4.0f, y + 4.0f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
            return y + 12.0f;
        }
        for (CloudConfigs.Code code : codes) {
            float rh = 13.0f;
            RoundedRect.of(x, y, w, rh).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round(5.0f * a))).render(graphics);
            Text.of(Fonts.MEDIUM, code.code(), x + 5.0f, y + rh * 0.5f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(232, 235, 242).withAlpha(a)).render(graphics);
            Text.of(Fonts.MEDIUM, "\u043e\u0441\u0442\u0430\u043b\u043e\u0441\u044c " + code.left() + " / " + code.max(), x + w - 72.0f, y + rh * 0.5f, 4.4f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(accent.withAlpha(a)).render(graphics);
            float bs = 10.0f;
            float bx = x + w - bs - 2.0f;
            this.iconButton(graphics, "cc-code-del-" + code.code(), bx, y + 1.5f, bs, a, Color.rgb(255, 85, 85), top, bottom, () -> CloudConfigs.deleteCode(cfg.id(), code.code(), this.toast));
            this.button(graphics, "cc-code-copy-" + code.code(), "\u041a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u0442\u044c", bx - 52.0f, y + 1.5f, 48.0f, bs, a, accent, false, top, bottom, () -> {
                this.ctx.minecraft().keyboardHandler.setClipboard(code.code());
                this.toast.accept(new CloudConfigs.Result(true, "\u041a\u043b\u044e\u0447 \u0441\u043a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u043d"));
            });
            y += rh + 2.0f;
        }
        return y + 3.0f;
    }

    private void renderField(GuiGraphicsExtractor graphics, String id, String placeholder, String icon, float x, float y, float w, float h, float a, Color accent, float top, float bottom, long now) {
        boolean focused = id.equals(this.ctx.focus.abField);
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        float hv = this.anim(id, this.hov(id, hover), 7.0f);
        float fc = this.anim(id + "-focus", focused, 6.0f);
        RoundedRect.of(x, y, w, h).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round((8.0f + 4.0f * hv + 6.0f * fc) * a))).render(graphics);
        if (fc > 0.01f) {
            Outline.of(x, y, w, h).radius(4.0f).thickness(0.7f).color(accent.withAlpha(0.6f * fc * a)).render(graphics);
        }
        Text.of(Fonts.ICONS, icon, x + 8.0f, y + h * 0.5f, 6.0f).centered().color(GuiDraw.mix(Color.rgb(130, 135, 146), Color.rgb(225, 228, 236), fc).withAlpha(a)).render(graphics);
        String text = focused ? this.ctx.focus.abInput.toString() : this.fields.getOrDefault(id, "");
        float tx = x + 16.0f;
        float cy = y + h * 0.5f;
        if (text.isEmpty()) {
            Text.of(Fonts.MEDIUM, placeholder, tx, cy, 5.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(120, 125, 136).withAlpha(a)).render(graphics);
        } else {
            Text.of(Fonts.MEDIUM, GuiDraw.fit(text, 5.2f, w - 22.0f), tx, cy, 5.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 238, 245).withAlpha(a)).render(graphics);
        }
        if (focused) {
            float blink = 0.5f + 0.5f * (float)Math.cos((double)now / 170.0);
            float cx = tx + (text.isEmpty() ? 0.0f : Math.min(w - 22.0f, Text.of(Fonts.MEDIUM, text).size(5.2f).width()) + 0.5f);
            RoundedRect.of(cx, y + 3.5f, 0.6f, h - 7.0f).color(Color.rgb(235, 238, 245).withAlpha(a * blink)).render(graphics);
        }
        if (GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(x, y, w, h, () -> {
                this.ctx.focus.setAbField(id, this.fields.getOrDefault(id, ""));
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, () -> {
                this.fields.put(id, "");
                if (focused) {
                    this.ctx.focus.clearAbText();
                }
            });
        }
    }

    private void button(GuiGraphicsExtractor graphics, String key, String label, float x, float y, float w, float h, float a, Color accent, boolean primary, float top, float bottom, Runnable action) {
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        float hv = this.anim(key, this.hov(key, hover), 8.0f);
        if (primary) {
            RoundedRect.of(x, y, w, h).radius(3.5f).horizontalGradient(accent.withAlpha((0.55f + 0.35f * hv) * a), GuiDraw.getAccent2().withAlpha((0.55f + 0.35f * hv) * a)).render(graphics);
        } else {
            RoundedRect.of(x, y, w, h).radius(3.5f).color(Color.rgba(255, 255, 255, Math.round((10.0f + 10.0f * hv) * a))).render(graphics);
        }
        Text.of(Fonts.MEDIUM, label, x + w * 0.5f, y + h * 0.5f, Math.min(5.0f, h * 0.42f)).centered().color(GuiDraw.mix(Color.rgb(205, 209, 218), Color.rgb(255, 255, 255), primary ? 1.0f : hv).withAlpha(a)).render(graphics);
        if (GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(x, y, w, h, action, null);
        }
    }

    private void iconButton(GuiGraphicsExtractor graphics, String key, float x, float y, float s, float a, Color color, float top, float bottom, Runnable action) {
        boolean hover = GuiDraw.canHit(y, s, top, bottom) && this.hovered(x, y, s, s);
        float hv = this.anim(key, this.hov(key, hover), 8.0f);
        RoundedRect.of(x, y, s, s).radius(3.5f).color(color.withAlpha((0.08f + 0.22f * hv) * a)).render(graphics);
        Color glyph = GuiDraw.mix(Color.rgb(150, 155, 165), color, hv).withAlpha(a);
        float c = s * 0.5f;
        float d = s * 0.18f;
        GuiDraw.stroke(graphics, x + c - d, y + c - d, x + c + d, y + c + d, 0.9f, glyph);
        GuiDraw.stroke(graphics, x + c - d, y + c + d, x + c + d, y + c - d, 0.9f, glyph);
        if (GuiDraw.canHit(y, s, top, bottom)) {
            this.hit(x, y, s, s, action, null);
        }
    }
}

