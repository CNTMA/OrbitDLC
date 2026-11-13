/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.world.item.ItemStack
 */
package brokke.cc.ui.clickgui.tab;

import brokke.cc.core.module.ModuleManager;
import brokke.cc.core.module.impl.misc.AutoBuy;
import brokke.cc.system.autobuy.AutoBuyConfig;
import brokke.cc.system.autobuy.HolyWorldStaffImporter;
import brokke.cc.system.autobuy.PurchaseRecord;
import brokke.cc.ui.clickgui.autobuy.AutoBuyPanel;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.TextAlign;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public final class AutoBuyTab
extends GuiComponent {
    public static String abSearch = "";
    private static int abTab = 0;
    private float abTabAnim = abTab;
    public long abSwitchTime = 0L;
    private PurchaseRecord abCopied = null;
    private long abCopiedAt = 0L;

    public AutoBuyTab(ClickGuiContext ctx) {
        super(ctx);
    }

    public float renderAutoBuy(GuiGraphicsExtractor graphics, float x, float y, float w, float prog, float top, float bottom, long now) {
        float startY = y;
        float appear = GuiDraw.easeOut((float)(now - this.ctx.switchTime) / 380.0f);
        float a = prog * appear;
        y += (1.0f - appear) * 8.0f;
        Color accent = GuiDraw.getAccent();
        AutoBuyConfig cfg = this.ctx.autoBuyConfig();
        String[] tabs = new String[]{"\u041f\u0440\u0435\u0434\u043c\u0435\u0442\u044b", "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438", "\u0418\u0441\u0442\u043e\u0440\u0438\u044f"};
        int[] counts = new int[]{cfg.entries.size(), -1, cfg.history.size()};
        float tabH = 14.0f;
        float tabW = (w - 4.0f) / (float)tabs.length;
        RoundedRect.of(x, y, w, tabH + 4.0f).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round(6.0f * a))).render(graphics);
        float tabPos = this.abTabAnim;
        this.abTabAnim = this.ctx.smooth(this.abTabAnim, abTab, 14.0f);
        RoundedRect.of(x + 2.0f + tabPos * tabW, y + 2.0f, tabW, tabH).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round(14.0f * a))).render(graphics);
        for (int i = 0; i < tabs.length; ++i) {
            int idx = i;
            float tx = x + 2.0f + (float)i * tabW;
            boolean hover = GuiDraw.canHit(y, tabH, top, bottom) && this.ctx.hovered(tx, y + 2.0f, tabW, tabH);
            String key = "ab-tab-" + i;
            float hv = this.ctx.anim(key, this.ctx.hov(key, hover), 7.0f);
            float act = Math.clamp(1.0f - Math.abs(tabPos - (float)i), 0.0f, 1.0f);
            float lw = Text.of(Fonts.MEDIUM, tabs[i]).size(5.4f).width();
            String count = counts[i] < 0 ? "" : String.valueOf(counts[i]);
            float cw = count.isEmpty() ? 0.0f : Text.of(Fonts.MEDIUM, count).size(4.6f).width() + 4.0f;
            float lx = tx + (tabW - lw - cw) * 0.5f;
            Text.of(Fonts.MEDIUM, tabs[i], lx, y + 2.0f + tabH * 0.5f, 5.4f).verticalAlign(VerticalAlign.MIDDLE).color(GuiDraw.mix(Color.rgb(135, 140, 150), Color.rgb(240, 242, 248), Math.max(act, hv * 0.5f)).withAlpha(a)).render(graphics);
            if (!count.isEmpty()) {
                Text.of(Fonts.MEDIUM, count, lx + lw + 4.0f, y + 2.0f + tabH * 0.5f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(GuiDraw.mix(Color.rgb(105, 110, 120), accent, act).withAlpha(a)).render(graphics);
            }
            if (!GuiDraw.canHit(y, tabH + 4.0f, top, bottom)) continue;
            this.hit(tx, y + 2.0f, tabW, tabH, () -> {
                if (abTab != idx) {
                    this.ctx.focus.setAbField(null, "");
                    abTab = idx;
                    this.abSwitchTime = System.currentTimeMillis();
                    this.ctx.scrollTarget = 0.0f;
                    ClientSound.ui(ClientSound.Ui.CLICK);
                }
            }, null);
        }
        y += tabH + 4.0f + 6.0f;
        y = abTab == 0 ? this.ctx.autoBuyItems.renderAbItems(graphics, cfg, x, y, w, a, accent, top, bottom, now) : (abTab == 1 ? this.renderAbSettings(graphics, cfg, x, y, w, a, accent, top, bottom, now) : this.renderAbHistory(graphics, cfg, x, y, w, a, accent, top, bottom, now));
        return y - startY;
    }

    private float renderAbToggle(GuiGraphicsExtractor graphics, String id, String label, String desc, boolean value, float x, float y, float w, float a, Color accent, float top, float bottom, Runnable toggle) {
        float h = desc == null ? 17.0f : 22.0f;
        String key = "ab-set-" + id;
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.ctx.hovered(x, y, w, h);
        float hv = this.ctx.anim(key, this.ctx.hov(key, hover), 7.0f);
        float on = this.ctx.anim(String.valueOf(key) + "-on", value, 8.0f);
        RoundedRect.of(x, y, w, h).radius(4.5f).color(Color.rgba(255, 255, 255, Math.round((6.0f + 4.0f * hv) * a))).render(graphics);
        Text.of(Fonts.MEDIUM, label, x + 7.0f, y + (desc == null ? h * 0.5f : 7.5f), 5.4f).verticalAlign(VerticalAlign.MIDDLE).color(GuiDraw.mix(Color.rgb(195, 199, 209), Color.rgb(245, 247, 252), on).withAlpha(a)).render(graphics);
        if (desc != null) {
            Text.of(Fonts.MEDIUM, GuiDraw.fit(desc, 4.5f, w - 40.0f), x + 7.0f, y + 15.5f, 4.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
        }
        if (toggle != null) {
            GuiDraw.renderSwitch(graphics, x + w - 23.0f, y + (h - 8.5f) * 0.5f, 16.0f, 8.5f, on, accent, a);
            if (GuiDraw.canHit(y, h, top, bottom)) {
                this.hit(x, y, w, h, () -> {
                    toggle.run();
                    this.ctx.saveAutoBuy();
                    ClientSound.ui(ClientSound.Ui.CLICK);
                }, null);
            }
        }
        return y + h + 3.0f;
    }

    private float renderAbSettings(GuiGraphicsExtractor graphics, AutoBuyConfig cfg, float x, float y, float w, float a, Color accent, float top, float bottom, long now) {
        float appear = GuiDraw.easeOut((float)(now - this.abSwitchTime) / 320.0f);
        y += (1.0f - appear) * 6.0f;
        y = GuiDraw.sectionTitle(graphics, "\u041e\u0441\u043d\u043e\u0432\u043d\u043e\u0435", x, y, a *= appear);
        y = this.renderAbToggle(graphics, "enabled", "\u0412\u043a\u043b\u044e\u0447\u0438\u0442\u044c AutoBuy", "\u041f\u043e\u043a\u0443\u043f\u0430\u0442\u044c \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u044b \u0438\u0437 \u0441\u043f\u0438\u0441\u043a\u0430 \u043d\u0430 \u0430\u0443\u043a\u0446\u0438\u043e\u043d\u0435", cfg.enabled, x, y, w, a, accent, top, bottom, () -> {
            AutoBuy autoBuy;
            boolean bl = cfg.enabled = !cfg.enabled;
            if (this.ctx.autoBuyOwner != null) {
                boolean enabled = cfg.enabled;
                this.ctx.autoBuyOwner.execute(() -> this.ctx.autoBuyOwner.setToggled(enabled));
            }
            AutoBuy autoBuy2 = autoBuy = this.ctx.autoBuyOwner == null ? ModuleManager.getModule(AutoBuy.class) : null;
            if (autoBuy != null && autoBuy.isEnabled() != cfg.enabled) {
                autoBuy.toggle();
            }
        });
        float colW = (w - 5.0f) * 0.5f;
        float ly = y;
        float ry = y;
        ly = this.renderAbToggle(graphics, "sell", "\u0410\u0432\u0442\u043e-\u043f\u0440\u043e\u0434\u0430\u0436\u0430", null, cfg.autoSell, x, ly, colW, a, accent, top, bottom, () -> {
            cfg.autoSell = !cfg.autoSell;
        });
        ry = this.renderAbToggle(graphics, "relist", "\u0410\u0432\u0442\u043e-\u0440\u0435\u043b\u0438\u0441\u0442", null, cfg.autoRelist, x + colW + 5.0f, ry, colW, a, accent, top, bottom, () -> {
            cfg.autoRelist = !cfg.autoRelist;
        });
        ly = this.renderAbToggle(graphics, "parse", "\u0410\u0432\u0442\u043e-\u043f\u0430\u0440\u0441 \u0446\u0435\u043d", null, cfg.autoParse, x, ly, colW, a, accent, top, bottom, () -> {
            cfg.autoParse = !cfg.autoParse;
        });
        ry = this.renderAbToggle(graphics, "pay", "\u0410\u0432\u0442\u043e-\u043e\u043f\u043b\u0430\u0442\u0430", null, cfg.autoPay, x + colW + 5.0f, ry, colW, a, accent, top, bottom, () -> {
            cfg.autoPay = !cfg.autoPay;
        });
        ly = this.renderAbToggle(graphics, "refresh", "\u0410\u0432\u0442\u043e-\u043e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u0435", null, cfg.autoRefresh, x, ly, colW, a, accent, top, bottom, () -> {
            cfg.autoRefresh = !cfg.autoRefresh;
        });
        ry = this.renderAbToggle(graphics, "minprice-on", "\u041c\u0438\u043d. \u043f\u043e\u0440\u043e\u0433 \u0446\u0435\u043d\u044b", null, cfg.minPriceEnabled, x + colW + 5.0f, ry, colW, a, accent, top, bottom, () -> {
            cfg.minPriceEnabled = !cfg.minPriceEnabled;
        });
        y = Math.max(ly, ry) + 4.0f;
        y = GuiDraw.sectionTitle(graphics, "\u0417\u043d\u0430\u0447\u0435\u043d\u0438\u044f", x, y, a);
        float rx = x + colW + 5.0f;
        this.ctx.widgets.renderAbStepper(graphics, "percent", "\u041e\u0442 \u0440\u044b\u043d\u043a\u0430", cfg.parsePercent + "%", x, y, colW, a, accent, top, bottom, () -> {
            cfg.parsePercent = Math.max(5, cfg.parsePercent - 5);
        }, () -> {
            cfg.parsePercent = Math.min(100, cfg.parsePercent + 5);
        });
        y = this.ctx.widgets.renderAbStepper(graphics, "relist", "\u0420\u0435\u043b\u0438\u0441\u0442", cfg.relistIntervalSeconds + " \u0441", rx, y, colW, a, accent, top, bottom, () -> {
            cfg.relistIntervalSeconds = Math.max(5, cfg.relistIntervalSeconds - 5);
        }, () -> {
            cfg.relistIntervalSeconds = Math.min(300, cfg.relistIntervalSeconds + 5);
        });
        float fh = 17.0f;
        float fw = 62.0f;
        this.renderAbToggle(graphics, "minprice-row", "\u041c\u0438\u043d. \u043f\u043e\u0440\u043e\u0433", null, cfg.minPriceEnabled, x, y, colW, a, accent, top, bottom, null);
        String mp = cfg.minPrice > 0L ? String.valueOf(cfg.minPrice) : "";
        this.ctx.widgets.renderAbField(graphics, "minprice", (String)(cfg.minPrice > 0L ? AutoBuyPanel.fmt(cfg.minPrice) + "$" : "\u0421\u0443\u043c\u043c\u0430..."), null, x + colW - fw - 3.0f, y + 2.5f, fw, fh - 5.0f, a, accent, top, bottom, now, mp);
        y += fh + 7.0f;
        y = GuiDraw.sectionTitle(graphics, "AutoPay", x, y, a);
        this.renderAbToggle(graphics, "nick-row", "\u041f\u043e\u043b\u0443\u0447\u0430\u0442\u0435\u043b\u044c", null, false, x, y, colW, a, accent, top, bottom, null);
        this.ctx.widgets.renderAbField(graphics, "nick", cfg.autoPayNick.isEmpty() ? "\u041d\u0438\u043a..." : cfg.autoPayNick, null, x + colW - fw - 3.0f, y + 2.5f, fw, fh - 5.0f, a, accent, top, bottom, now, cfg.autoPayNick);
        this.renderAbToggle(graphics, "keepbalance-row", "\u041e\u0441\u0442\u0430\u0432\u043b\u044f\u0442\u044c", null, false, rx, y, colW, a, accent, top, bottom, null);
        this.ctx.widgets.renderAbField(graphics, "keepbalance", AutoBuyPanel.fmt(cfg.autoPayKeepBalance) + "$", null, rx + colW - fw - 3.0f, y + 2.5f, fw, fh - 5.0f, a, accent, top, bottom, now, String.valueOf(cfg.autoPayKeepBalance));
        Text.of(Fonts.MEDIUM, "\u0411\u0430\u043b\u0430\u043d\u0441 \u0438\u0437 \u0441\u043a\u043e\u0440\u0431\u043e\u0440\u0434\u0430. \u041f\u043e\u043b\u0443\u0447\u0430\u0442\u0435\u043b\u044e \u043f\u0435\u0440\u0435\u0432\u043e\u0434\u0438\u0442\u0441\u044f \u0442\u043e\u043b\u044c\u043a\u043e \u0441\u0443\u043c\u043c\u0430 \u0441\u0432\u0435\u0440\u0445 \u043e\u0441\u0442\u0430\u0442\u043a\u0430.", x + 1.0f, (y += fh + 3.0f) + 3.0f, 4.4f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(125, 130, 142).withAlpha(a)).render(graphics);
        y += 12.0f;
        y = GuiDraw.sectionTitle(graphics, "\u0411\u0430\u043d-\u043b\u0438\u0441\u0442 \u043f\u0440\u043e\u0434\u0430\u0432\u0446\u043e\u0432 (" + cfg.banList.size() + ")", x, y, a);
        this.ctx.widgets.renderConfigButton(graphics, "ab-staff-download", HolyWorldStaffImporter.buttonLabel(cfg), "D", x, y, w, 15.0f, a, false, top, bottom, () -> HolyWorldStaffImporter.download(cfg));
        y += 19.0f;
        if (cfg.staffImportStatus != null && !cfg.staffImportStatus.isEmpty()) {
            Text.of(Fonts.MEDIUM, cfg.staffImportStatus, x + 4.0f, y + 5.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(150, 160, 175).withAlpha(a)).render(graphics);
            y += 13.0f;
        }
        y = this.renderAbToggle(graphics, "banlist", "\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c \u0431\u0430\u043d-\u043b\u0438\u0441\u0442", "\u041d\u0435 \u043f\u043e\u043a\u0443\u043f\u0430\u0442\u044c \u0443 \u043f\u0440\u043e\u0434\u0430\u0432\u0446\u043e\u0432 \u0438\u0437 \u0441\u043f\u0438\u0441\u043a\u0430", cfg.banListEnabled, x, y, w, a, accent, top, bottom, () -> {
            AutoBuy autoBuy;
            cfg.banListEnabled = !cfg.banListEnabled;
            AutoBuy autoBuy2 = autoBuy = this.ctx.autoBuyOwner == null ? ModuleManager.getModule(AutoBuy.class) : null;
            if (autoBuy != null) {
                autoBuy.getBanList().setState(cfg.banListEnabled);
            }
        });
        float addW = 52.0f;
        float ih = 15.0f;
        this.ctx.widgets.renderAbField(graphics, "ban", "\u041d\u0438\u043a \u043f\u0440\u043e\u0434\u0430\u0432\u0446\u0430...", "D", x, y, w - addW - 4.0f, ih, a, accent, top, bottom, now, "");
        float bx = x + w - addW;
        boolean bHover = GuiDraw.canHit(y, ih, top, bottom) && this.ctx.hovered(bx, y, addW, ih);
        float bhv = this.ctx.anim("ab-ban-add", this.ctx.hov("ab-ban-add", bHover), 7.0f);
        RoundedRect.of(bx, y, addW, ih).radius(4.0f).horizontalGradient(accent.withAlpha((0.55f + 0.35f * bhv) * a), GuiDraw.getAccent2().withAlpha((0.55f + 0.35f * bhv) * a)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c", bx + addW * 0.5f, y + ih * 0.5f, 5.2f).centered().color(Color.rgb(255, 255, 255).withAlpha(a)).render(graphics);
        if (GuiDraw.canHit(y, ih, top, bottom)) {
            this.hit(bx, y, addW, ih, () -> {
                if (!"ban".equals(this.ctx.focus.abClickPrev)) {
                    this.ctx.focus.setAbField("ban", "");
                }
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, null);
        }
        y += ih + 5.0f;
        if (cfg.banList.isEmpty()) {
            Text.of(Fonts.MEDIUM, "\u0421\u043f\u0438\u0441\u043e\u043a \u043f\u0443\u0441\u0442 - \u043f\u043e\u043a\u0443\u043f\u0430\u0435\u043c \u0443 \u0432\u0441\u0435\u0445", x + w * 0.5f, y + 6.0f, 4.8f).centered().color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
            y += 14.0f;
        } else {
            float chipH = 12.0f;
            float cx = x;
            for (String banned : new ArrayList<String>(cfg.banList)) {
                float cw = Text.of(Fonts.MEDIUM, banned).size(4.8f).width() + 17.0f;
                if (cx + cw > x + w && cx > x) {
                    cx = x;
                    y += chipH + 3.0f;
                }
                if (!GuiDraw.canHit(y, chipH, top, bottom)) {
                    cx += cw + 3.0f;
                    continue;
                }
                String key = "ab-ban-" + banned;
                boolean hover = GuiDraw.canHit(y, chipH, top, bottom) && this.ctx.hovered(cx, y, cw, chipH);
                float hv = this.ctx.anim(key, this.ctx.hov(key, hover), 8.0f);
                RoundedRect.of(cx, y, cw, chipH).radius(3.5f).color(Color.rgba(255, 255, 255, Math.round((9.0f + 5.0f * hv) * a))).render(graphics);
                if (hv > 0.01f) {
                    RoundedRect.of(cx, y, cw, chipH).radius(3.5f).color(Color.rgb(255, 80, 80).withAlpha(0.15f * hv * a)).render(graphics);
                }
                Text.of(Fonts.MEDIUM, banned, cx + 5.0f, y + chipH * 0.5f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(220, 224, 232).withAlpha(a)).render(graphics);
                float xc = cx + cw - 6.0f;
                float yc = y + chipH * 0.5f;
                Color xcol = GuiDraw.mix(Color.rgb(120, 125, 136), Color.rgb(255, 110, 110), hv).withAlpha(a);
                Text.of(Fonts.MEDIUM, "x", xc, yc, 6.0f).centered().color(xcol).render(graphics);
                if (GuiDraw.canHit(y, chipH, top, bottom)) {
                    this.hit(cx, y, cw, chipH, () -> {
                        cfg.removeBan(banned);
                        ClientSound.ui(ClientSound.Ui.CLICK);
                    }, null);
                }
                cx += cw + 3.0f;
            }
            this.ctx.widgets.renderConfigButton(graphics, "ab-ban-clear", "\u041e\u0447\u0438\u0441\u0442\u0438\u0442\u044c \u0431\u0430\u043d-\u043b\u0438\u0441\u0442", "x", x, y += chipH + 4.0f, w, 14.0f, a, true, top, bottom, cfg::clearBans);
            y += 17.0f;
        }
        this.ctx.widgets.renderConfigButton(graphics, "ab-reset", "\u0421\u0431\u0440\u043e\u0441\u0438\u0442\u044c \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438", "s", x, y += 6.0f, w, 15.0f, a, true, top, bottom, () -> {
            cfg.resetToDefaults();
            if (this.ctx.autoBuyOwner != null) {
                this.ctx.autoBuyOwner.execute(() -> this.ctx.autoBuyOwner.setToggled(false));
            }
        });
        return y + 15.0f;
    }

    private float renderAbHistory(GuiGraphicsExtractor graphics, AutoBuyConfig cfg, float x, float y, float w, float a, Color accent, float top, float bottom, long now) {
        List<PurchaseRecord> history = cfg.history;
        long ok = history.stream().filter(r -> r.success).count();
        long spent = history.stream().filter(r -> r.success).mapToLong(r -> r.totalPrice).sum();
        float statW = (w - 6.0f) / 3.0f;
        String[][] stats = new String[][]{{"\u041f\u043e\u043a\u0443\u043f\u043e\u043a", String.valueOf(ok)}, {"\u041e\u0448\u0438\u0431\u043e\u043a", String.valueOf((long)history.size() - ok)}, {"\u041f\u043e\u0442\u0440\u0430\u0447\u0435\u043d\u043e", AutoBuyPanel.fmt(spent) + "$"}};
        for (int i = 0; i < 3; ++i) {
            float sx = x + (float)i * (statW + 3.0f);
            RoundedRect.of(sx, y, statW, 22.0f).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round(6.0f * a))).render(graphics);
            Text.of(Fonts.MEDIUM, stats[i][1], sx + statW * 0.5f, y + 8.0f, 6.2f).centered().color((i == 1 && (long)history.size() > ok ? Color.rgb(235, 110, 110) : (i == 2 ? accent : Color.rgb(240, 242, 248))).withAlpha(a)).render(graphics);
            Text.of(Fonts.MEDIUM, stats[i][0], sx + statW * 0.5f, y + 16.5f, 4.4f).centered().color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
        }
        y += 28.0f;
        if (history.isEmpty()) {
            Text.of(Fonts.MEDIUM, "\u0418\u0441\u0442\u043e\u0440\u0438\u044f \u043f\u043e\u043a\u0443\u043f\u043e\u043a \u043f\u0443\u0441\u0442\u0430", x + w * 0.5f, y + 14.0f, 5.4f).centered().color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
            return y + 30.0f;
        }
        float rowH = 22.0f;
        float gap = 3.0f;
        for (int i = 0; i < history.size(); ++i) {
            PurchaseRecord r2 = history.get(i);
            float t = GuiDraw.easeOut((float)(now - this.abSwitchTime - (long)Math.min(i, 20) * 20L) / 320.0f);
            float ry = y + (1.0f - t) * 6.0f;
            if (ry + rowH >= top && ry <= bottom) {
                this.renderAbHistoryRow(graphics, r2, i, x, ry, w, rowH, a * t, accent, top, bottom, now);
            }
            y += rowH + gap;
        }
        this.ctx.widgets.renderConfigButton(graphics, "ab-hist-clear", "\u041e\u0447\u0438\u0441\u0442\u0438\u0442\u044c \u0438\u0441\u0442\u043e\u0440\u0438\u044e", "x", x, y += 3.0f, w, 15.0f, a, true, top, bottom, () -> {
            history.clear();
            this.ctx.saveAutoBuy();
        });
        return y + 15.0f;
    }

    private void renderAbHistoryRow(GuiGraphicsExtractor graphics, PurchaseRecord r, int i, float x, float y, float w, float h, float a, Color accent, float top, float bottom, long now) {
        String tail;
        ItemStack stack;
        String key = "ab-hist-" + i;
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        float hv = this.anim(key, this.hov(key, hover), 7.0f);
        RoundedRect.of(x, y, w, h).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round((6.0f + 4.0f * hv) * a))).render(graphics);
        Color mark = r.success ? Color.rgb(90, 210, 140) : Color.rgb(235, 110, 110);
        RoundedRect.of(x + 2.0f, y + 5.0f, 1.5f, h - 10.0f).radius(0.75f).color(mark.withAlpha(a)).render(graphics);
        if (a > 0.3f && !(stack = AutoBuyPanel.iconStack(r.itemId, r.label)).isEmpty()) {
            AutoBuyPanel.drawItemAt(graphics, stack, x + 7.0f, y + (h - 14.0f) * 0.5f, 14.0f);
        }
        float tx = x + 26.0f;
        String price = "x" + r.count + "  " + AutoBuyPanel.fmt(r.totalPrice) + "$";
        float pw = Text.of(Fonts.MEDIUM, price).size(5.0f).width();
        Text.of(Fonts.MEDIUM, price, x + w - 7.0f, y + h * 0.5f, 5.0f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color((r.success ? accent : Color.rgb(130, 135, 146)).withAlpha(a)).render(graphics);
        Text.of(Fonts.MEDIUM, GuiDraw.fit(r.getDisplayName(), 5.4f, w - (tx - x) - pw - 14.0f), tx, y + 7.5f, 5.4f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 238, 245).withAlpha(a)).render(graphics);
        boolean hasSeller = r.seller != null && !r.seller.isEmpty();
        boolean copied = hasSeller && this.abCopied == r && System.currentTimeMillis() - this.abCopiedAt < 1500L;
        String prefix = "\u041f\u0440\u043e\u0434\u0430\u0432\u0435\u0446: ";
        String nick = hasSeller ? r.seller : "-";
        float sy = y + h - 7.0f;
        float prefW = Text.of(Fonts.MEDIUM, prefix).size(4.6f).width();
        float nickW = Text.of(Fonts.MEDIUM, nick).size(4.6f).width();
        float nx = tx + prefW;
        boolean nickHover = hasSeller && GuiDraw.canHit(sy - 3.5f, 7.0f, top, bottom) && this.hovered(nx, sy - 3.5f, nickW, 7.0f);
        float nh = this.anim(String.valueOf(key) + "-nick", nickHover, 9.0f);
        Text.of(Fonts.MEDIUM, prefix, tx, sy, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
        Text.of(Fonts.MEDIUM, nick, nx, sy, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(GuiDraw.mix(Color.rgb(195, 199, 209), accent, copied ? 1.0f : nh).withAlpha(a)).render(graphics);
        tail = copied ? "  \u0441\u043a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u043d\u043e" : !r.success && r.failReason != null ? "  " + r.failReason : "";
        if (!tail.isEmpty()) {
            Text.of(Fonts.MEDIUM, GuiDraw.fit(tail, 4.6f, x + w - pw - 14.0f - nx - nickW), nx + nickW, sy, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color((copied ? accent : Color.rgb(115, 120, 132)).withAlpha(a)).render(graphics);
        }
        if (hasSeller && GuiDraw.canHit(sy - 3.5f, 7.0f, top, bottom)) {
            this.hit(nx, sy - 3.5f, nickW, 7.0f, () -> {
                this.ctx.minecraft().keyboardHandler.setClipboard(r.seller);
                this.abCopied = r;
                this.abCopiedAt = System.currentTimeMillis();
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, null);
        }
    }
}

