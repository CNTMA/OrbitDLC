/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.tab;

import brokke.cc.core.setting.ConfigManager;
import brokke.cc.ui.clickgui.globals.CloudConfigs;
import brokke.cc.ui.clickgui.globals.CloudConfigsView;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.outline.Outline;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.scissor.Scissor;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ConfigsTab
extends GuiComponent {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    public final StringBuilder configInput = new StringBuilder();
    public final List<Long> configCharTimes = new ArrayList<Long>();
    private long configErrorTime = 0L;
    private static String lastConfig = null;
    public final CloudConfigsView cloud;
    public final Map<String, String> cloudFields;
    public int mode = 0;

    public ConfigsTab(ClickGuiContext ctx) {
        super(ctx);
        this.cloud = new CloudConfigsView(ctx);
        this.cloudFields = this.cloud.fields;
    }

    public void redeemCode() {
        this.cloud.redeemCode();
    }

    private float renderModeSwitch(GuiGraphicsExtractor graphics, float x, float y, float w, float a, Color accent, float top, float bottom) {
        float tabH = 14.0f;
        float tabW = (w - 4.0f) * 0.5f;
        RoundedRect.of(x, y, w, tabH + 4.0f).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round(6.0f * a))).render(graphics);
        float pos = this.anim("configs-mode", this.mode == 1, 6.0f);
        RoundedRect.of(x + 2.0f + pos * tabW, y + 2.0f, tabW, tabH).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round(14.0f * a))).render(graphics);
        String[] labels = new String[]{"\u041b\u043e\u043a\u0430\u043b\u044c\u043d\u044b\u0435", "\u0421\u0435\u0440\u0432\u0435\u0440\u043d\u044b\u0435"};
        String[] counts = new String[]{String.valueOf(ConfigManager.getAvailableConfigs().size()), String.valueOf(CloudConfigs.own().size() + CloudConfigs.shared().size())};
        for (int i = 0; i < 2; ++i) {
            int idx = i;
            float tx = x + 2.0f + (float)i * tabW;
            boolean hover = GuiDraw.canHit(y, tabH, top, bottom) && this.hovered(tx, y + 2.0f, tabW, tabH);
            String key = "configs-mode-" + i;
            float hv = this.anim(key, this.hov(key, hover), 7.0f);
            float act = i == 1 ? pos : 1.0f - pos;
            float lw = Text.of(Fonts.MEDIUM, labels[i]).size(5.4f).width();
            float cw = Text.of(Fonts.MEDIUM, counts[i]).size(4.6f).width();
            float lx = tx + (tabW - lw - 4.0f - cw) * 0.5f;
            Text.of(Fonts.MEDIUM, labels[i], lx, y + 2.0f + tabH * 0.5f, 5.4f).verticalAlign(VerticalAlign.MIDDLE).color(GuiDraw.mix(Color.rgb(135, 140, 150), Color.rgb(240, 242, 248), Math.max(act, hv * 0.5f)).withAlpha(a)).render(graphics);
            Text.of(Fonts.MEDIUM, counts[i], lx + lw + 4.0f, y + 2.0f + tabH * 0.5f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(GuiDraw.mix(Color.rgb(105, 110, 120), accent, act).withAlpha(a)).render(graphics);
            if (!GuiDraw.canHit(y, tabH + 4.0f, top, bottom)) continue;
            this.hit(tx, y + 2.0f, tabW, tabH, () -> {
                if (this.mode != idx) {
                    this.mode = idx;
                    this.ctx.switchTime = System.currentTimeMillis();
                    if (idx == 1) {
                        CloudConfigs.refresh();
                    }
                    ClientSound.ui(ClientSound.Ui.CLICK);
                }
            }, null);
        }
        return y + tabH + 4.0f + 6.0f;
    }

    public void submitConfig() {
        String name = this.configInput.toString().trim();
        if (name.isEmpty()) {
            this.configErrorTime = System.currentTimeMillis();
            ClientSound.checkbox(false);
            return;
        }
        if (ConfigManager.saveConfig(name)) {
            this.configInput.setLength(0);
            this.configCharTimes.clear();
            lastConfig = name;
            this.ctx.configToast("\u041a\u043e\u043d\u0444\u0438\u0433 " + name + " \u0441\u043e\u0445\u0440\u0430\u043d\u0451\u043d", Color.rgb(90, 210, 140));
            ClientSound.checkbox(true);
        } else {
            this.configErrorTime = System.currentTimeMillis();
            this.ctx.configToast("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u043a\u043e\u043d\u0444\u0438\u0433", Color.rgb(235, 90, 90));
            ClientSound.checkbox(false);
        }
    }

    public float renderConfigs(GuiGraphicsExtractor graphics, float x, float y, float w, float prog, float top, float bottom, long now) {
        float startY = y;
        float appear = GuiDraw.easeOut((float)(now - this.ctx.switchTime) / 380.0f);
        float a = prog * appear;
        y += (1.0f - appear) * 8.0f;
        Color accent = GuiDraw.getAccent();
        y = this.renderModeSwitch(graphics, x, y, w, a, accent, top, bottom);
        if (this.mode == 1) {
            return this.cloud.render(graphics, x, y, w, a, accent, top, bottom, now) - startY;
        }
        y = this.renderConfigInput(graphics, x, y, w, a, accent, top, bottom, now);
        float toast = 1.0f - GuiDraw.easeOut(Math.max(0.0f, (float)(now - this.ctx.configToastTime - 1800L) / 500.0f));
        float toastIn = GuiDraw.easeOut((float)(now - this.ctx.configToastTime) / 250.0f);
        if (this.ctx.configToastText != null && toast > 0.01f) {
            Text.of(Fonts.MEDIUM, this.ctx.configToastText, x + 2.0f + (1.0f - toastIn) * 4.0f, y + 3.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(this.ctx.configToastColor.withAlpha(a * toast * toastIn)).render(graphics);
        }
        y += 9.0f;
        List<String> configs = ConfigManager.getAvailableConfigs();
        configs.sort(String.CASE_INSENSITIVE_ORDER);
        y = GuiDraw.sectionTitle(graphics, "\u041a\u043e\u043d\u0444\u0438\u0433\u0438 (" + configs.size() + ")", x, y, a);
        if (configs.isEmpty()) {
            Text.of(Fonts.MEDIUM, "\u041a\u043e\u043d\u0444\u0438\u0433\u043e\u0432 \u043f\u043e\u043a\u0430 \u043d\u0435\u0442", x + w * 0.5f, y + 14.0f, 5.4f).centered().color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
            y += 30.0f;
        }
        float rowH = 20.0f;
        float rowGap = 3.0f;
        for (int i = 0; i < configs.size(); ++i) {
            String name = configs.get(i);
            float t = GuiDraw.easeOut((float)(now - this.ctx.switchTime - (long)i * 25L) / 320.0f);
            this.renderConfigRow(graphics, name, x, y + (1.0f - t) * 6.0f, w, rowH, a * t, accent, top, bottom);
            y += rowH + rowGap;
        }
        float bw = (w - 5.0f) * 0.5f;
        float bh = 15.0f;
        this.ctx.widgets.renderConfigButton(graphics, "folder", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c \u043f\u0430\u043f\u043a\u0443", "F", x, y += 5.0f, bw, bh, a, false, top, bottom, () -> {
            ConfigManager.openConfigsFolder();
            this.ctx.configToast("\u041f\u0430\u043f\u043a\u0430 \u043a\u043e\u043d\u0444\u0438\u0433\u043e\u0432 \u043e\u0442\u043a\u0440\u044b\u0442\u0430", Color.rgb(200, 205, 215));
        });
        this.ctx.widgets.renderConfigButton(graphics, "reset", "\u0421\u0431\u0440\u043e\u0441\u0438\u0442\u044c \u043c\u043e\u0434\u0443\u043b\u0438", "s", x + bw + 5.0f, y, bw, bh, a, true, top, bottom, () -> {
            ConfigManager.resetConfig();
            lastConfig = null;
            this.ctx.configToast("\u0412\u0441\u0435 \u043c\u043e\u0434\u0443\u043b\u0438 \u0441\u0431\u0440\u043e\u0448\u0435\u043d\u044b", Color.rgb(235, 140, 140));
        });
        return (y += bh) - startY;
    }

    private float renderConfigInput(GuiGraphicsExtractor graphics, float x, float y, float w, float a, Color accent, float top, float bottom, long now) {
        float err;
        float shift;
        int start;
        float h = 15.0f;
        float btnW = 56.0f;
        float fw = w - btnW - 4.0f;
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, fw, h);
        float hv = this.anim("config-input", this.hov("config-input", hover), 7.0f);
        float fc = this.anim("config-input-focus", this.ctx.focus.configFocused, 6.0f);
        RoundedRect.of(x, y, fw, h).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round((8.0f + 4.0f * hv + 6.0f * fc) * a))).render(graphics);
        if (fc > 0.01f) {
            Outline.of(x, y, fw, h).radius(4.0f).thickness(0.7f).color(accent.withAlpha(0.6f * fc * a)).render(graphics);
        }
        Text.of(Fonts.ICONS, "F", x + 8.0f, y + h * 0.5f, 6.0f).centered().color(GuiDraw.mix(Color.rgb(130, 135, 146), Color.rgb(225, 228, 236), fc).withAlpha(a)).render(graphics);
        String text = this.configInput.toString();
        float tx = x + 16.0f;
        float cy = y + h * 0.5f;
        float maxW = fw - 20.0f;
        for (start = 0; start < text.length() - 1 && Text.of(Fonts.MEDIUM, text.substring(start)).size(5.4f).width() > maxW; ++start) {
        }
        float f = shift = start > 0 ? Text.of(Fonts.MEDIUM, text.substring(0, start)).size(5.4f).width() : 0.0f;
        if (text.isEmpty()) {
            Text.of(Fonts.MEDIUM, "\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 \u043a\u043e\u043d\u0444\u0438\u0433\u0430...", tx, cy, 5.4f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(120, 125, 136).withAlpha(a)).render(graphics);
        } else {
            Scissor.push(graphics, tx - 1.0f, y, maxW + 2.0f, h);
            for (int i = start; i < text.length(); ++i) {
                float cx = tx + Text.of(Fonts.MEDIUM, text.substring(0, i)).size(5.4f).width() - shift;
                long born = i < this.configCharTimes.size() ? this.configCharTimes.get(i) : 0L;
                float t = GuiDraw.easeOut((float)(now - born) / 220.0f);
                Text.of(Fonts.MEDIUM, String.valueOf(text.charAt(i)), cx, cy + (1.0f - t) * 4.0f, 5.4f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 238, 245).withAlpha(a * t)).render(graphics);
            }
            Scissor.pop(graphics);
        }
        if (this.ctx.focus.configFocused) {
            float blink = 0.5f + 0.5f * (float)Math.cos((double)now / 170.0);
            float caretX = tx + (text.isEmpty() ? 0.0f : Text.of(Fonts.MEDIUM, text).size(5.4f).width() - shift + 0.5f);
            RoundedRect.of(caretX, y + 4.0f, 0.6f, h - 8.0f).color(Color.rgb(235, 238, 245).withAlpha(a * blink)).render(graphics);
        }
        if ((err = 1.0f - GuiDraw.easeOut((float)(now - this.configErrorTime) / 600.0f)) > 0.01f) {
            RoundedRect.of(x, y, fw, h).radius(4.0f).color(Color.rgba(255, 70, 70, Math.round(40.0f * err * a))).render(graphics);
        }
        float bx = x + fw + 4.0f;
        boolean bHover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(bx, y, btnW, h);
        float bh = this.anim("config-save", this.hov("config-save", bHover), 7.0f);
        RoundedRect.of(bx, y, btnW, h).radius(4.0f).horizontalGradient(accent.withAlpha((0.55f + 0.35f * bh) * a), GuiDraw.getAccent2().withAlpha((0.55f + 0.35f * bh) * a)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c", bx + btnW * 0.5f, cy, 5.2f).centered().color(Color.rgb(255, 255, 255).withAlpha(a)).render(graphics);
        if (GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(x, y, fw, h, () -> {
                this.ctx.focus.setConfigFocus(true);
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, () -> {
                this.configInput.setLength(0);
                this.configCharTimes.clear();
            });
            this.hit(bx, y, btnW, h, this::submitConfig, null);
        }
        return y + h + 3.0f;
    }

    private void renderConfigRow(GuiGraphicsExtractor graphics, String name, float x, float y, float w, float h, float a, Color accent, float top, float bottom) {
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.ctx.hovered(x, y, w, h);
        String key = "config-row-" + name;
        float hv = this.ctx.anim(key, this.ctx.hov(key, hover), 7.0f);
        float active = this.ctx.anim(String.valueOf(key) + "-active", name.equalsIgnoreCase(lastConfig == null ? "" : lastConfig), 6.0f);
        RoundedRect.of(x, y, w, h).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round((6.0f + 4.0f * hv) * a))).render(graphics);
        if (active > 0.01f) {
            RoundedRect.of(x + 1.5f, y + 5.0f, 1.2f, h - 10.0f).radius(0.6f).verticalGradient(accent.withAlpha(active * a), GuiDraw.getAccent2().withAlpha(active * a)).render(graphics);
        }
        float iconBox = 12.0f;
        float ix = x + 5.0f;
        float iy = y + (h - iconBox) * 0.5f;
        RoundedRect.of(ix, iy, iconBox, iconBox).radius(3.5f).color(GuiDraw.mix(Color.rgb(60, 63, 72), accent, active).withAlpha((0.5f + 0.3f * active) * a)).render(graphics);
        Text.of(Fonts.ICONS, "F", ix + iconBox * 0.5f, iy + iconBox * 0.5f, 6.0f).centered().color(Color.rgb(235, 238, 245).withAlpha(a)).render(graphics);
        float nx = ix + iconBox + 5.0f;
        Text.of(Fonts.MEDIUM, GuiDraw.fit(name, 5.6f, w - 110.0f), nx, y + h * 0.5f - 2.5f, 5.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(232, 235, 242).withAlpha(a)).render(graphics);
        File file = new File(ConfigManager.getConfigsDir(), name + ".json");
        String date = file.exists() ? DATE_FORMAT.format(new Date(file.lastModified())) : "";
        Text.of(Fonts.MEDIUM, (String)(active > 0.5f ? "\u0410\u043a\u0442\u0438\u0432\u043d\u044b\u0439 - " + date : date), nx, y + h * 0.5f + 3.5f, 4.3f).verticalAlign(VerticalAlign.MIDDLE).color(GuiDraw.mix(Color.rgb(110, 115, 126), accent, active).withAlpha(a)).render(graphics);
        float delS = 12.0f;
        float delX = x + w - delS - 4.0f;
        float delY = y + (h - delS) * 0.5f;
        boolean delHover = GuiDraw.canHit(y, h, top, bottom) && this.ctx.hovered(delX, delY, delS, delS);
        float dh = this.ctx.anim(String.valueOf(key) + "-del", delHover, 8.0f);
        RoundedRect.of(delX, delY, delS, delS).radius(3.5f).color(Color.rgb(255, 85, 85).withAlpha((0.08f + 0.22f * dh) * a)).render(graphics);
        Color glyph = GuiDraw.mix(Color.rgb(150, 155, 165), Color.rgb(255, 95, 95), dh).withAlpha(a * Math.max(0.5f, hv));
        float c = delS * 0.5f;
        GuiDraw.stroke(graphics, delX + c - 2.2f, delY + c - 2.2f, delX + c + 2.2f, delY + c + 2.2f, 0.9f, glyph);
        GuiDraw.stroke(graphics, delX + c - 2.2f, delY + c + 2.2f, delX + c + 2.2f, delY + c - 2.2f, 0.9f, glyph);
        float loadW = 42.0f;
        float loadH = 12.0f;
        float loadX = delX - loadW - 4.0f;
        float loadY = y + (h - loadH) * 0.5f;
        boolean loadHover = GuiDraw.canHit(y, h, top, bottom) && this.ctx.hovered(loadX, loadY, loadW, loadH);
        float lh = this.ctx.anim(String.valueOf(key) + "-load", loadHover, 8.0f);
        RoundedRect.of(loadX, loadY, loadW, loadH).radius(3.5f).color(Color.rgba(255, 255, 255, Math.round(10.0f * (1.0f - lh) * a))).render(graphics);
        if (lh > 0.01f) {
            RoundedRect.of(loadX, loadY, loadW, loadH).radius(3.5f).horizontalGradient(accent.withAlpha(0.7f * lh * a), GuiDraw.getAccent2().withAlpha(0.7f * lh * a)).render(graphics);
        }
        Text.of(Fonts.MEDIUM, "\u0417\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c", loadX + loadW * 0.5f, loadY + loadH * 0.5f, 4.8f).centered().color(GuiDraw.mix(Color.rgb(200, 204, 214), Color.rgb(255, 255, 255), lh).withAlpha(a)).render(graphics);
        if (GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(loadX, loadY, loadW, loadH, () -> {
                if (ConfigManager.loadConfig(name)) {
                    lastConfig = name;
                    this.ctx.configToast("\u041a\u043e\u043d\u0444\u0438\u0433 " + name + " \u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043d", Color.rgb(90, 210, 140));
                    ClientSound.checkbox(true);
                } else {
                    this.ctx.configToast("\u041e\u0448\u0438\u0431\u043a\u0430 \u0437\u0430\u0433\u0440\u0443\u0437\u043a\u0438 \u043a\u043e\u043d\u0444\u0438\u0433\u0430", Color.rgb(235, 90, 90));
                    ClientSound.checkbox(false);
                }
            }, null);
            this.hit(delX, delY, delS, delS, () -> {
                ConfigManager.deleteConfig(name);
                if (name.equalsIgnoreCase(lastConfig == null ? "" : lastConfig)) {
                    lastConfig = null;
                }
                this.ctx.configToast("\u041a\u043e\u043d\u0444\u0438\u0433 " + name + " \u0443\u0434\u0430\u043b\u0451\u043d", Color.rgb(235, 140, 140));
                ClientSound.checkbox(false);
            }, null);
        }
    }
}

