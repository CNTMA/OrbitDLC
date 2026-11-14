/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.world.item.ItemStack
 */
package brokke.cc.ui.clickgui.tab;

import brokke.cc.system.autobuy.AutoBuyCatalog;
import brokke.cc.system.autobuy.AutoBuyConfig;
import brokke.cc.system.autobuy.AutoBuyEnchantments;
import brokke.cc.system.autobuy.ItemEntry;
import brokke.cc.ui.clickgui.autobuy.AutoBuyPanel;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.ui.clickgui.tab.AutoBuyTab;
import brokke.cc.ui.clickgui.widget.CommonWidgets;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.outline.Outline;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.scissor.Scissor;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.TextAlign;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public final class AutoBuyItemsView
extends GuiComponent {
    private static int abFilter = 0;
    private float abFilterX = Float.NaN;
    private float abFilterW = Float.NaN;
    private float abEnabledBar = 0.0f;
    private int abSelected = -1;
    private int abInspLast = -1;

    public AutoBuyItemsView(ClickGuiContext ctx) {
        super(ctx);
    }

    public float renderAbItems(GuiGraphicsExtractor graphics, AutoBuyConfig cfg, float x, float y, float w, float a, Color accent, float top, float bottom, long now) {
        String string;
        List<ItemEntry> all = cfg.entries;
        int enabledCount = (int)all.stream().filter(e -> e.enabled).count();
        float h = 15.0f;
        String[] filters = new String[]{"\u0412\u0441\u0435", "\u0410\u043a\u0442\u0438\u0432\u043d\u044b\u0435", "\u0412\u044b\u043a\u043b\u044e\u0447\u0435\u043d\u043d\u044b\u0435"};
        float[] fw = new float[3];
        float filtersW = 0.0f;
        for (int i = 0; i < 3; ++i) {
            fw[i] = Text.of(Fonts.MEDIUM, filters[i]).size(4.8f).width() + 10.0f;
            filtersW += fw[i];
        }
        filtersW += 4.0f;
        CommonWidgets commonWidgets = this.ctx.widgets;
        AutoBuyTab cfr_ignored_0 = this.ctx.autoBuy;
        if (AutoBuyTab.abSearch.isEmpty()) {
            string = "\u041f\u043e\u0438\u0441\u043a \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432...";
        } else {
            AutoBuyTab cfr_ignored_1 = this.ctx.autoBuy;
            string = AutoBuyTab.abSearch;
        }
        AutoBuyTab cfr_ignored_2 = this.ctx.autoBuy;
        commonWidgets.renderAbField(graphics, "search", string, "M", x, y, w - filtersW - 4.0f, h, a, accent, top, bottom, now, AutoBuyTab.abSearch);
        float segX = x + w - filtersW;
        RoundedRect.of(segX, y, filtersW, h).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round(6.0f * a))).render(graphics);
        float selX = segX + 2.0f;
        for (int i = 0; i < abFilter; ++i) {
            selX += fw[i];
        }
        this.abFilterX = Float.isNaN(this.abFilterX) ? selX : this.ctx.smooth(this.abFilterX, selX, 16.0f);
        this.abFilterW = Float.isNaN(this.abFilterW) ? fw[abFilter] : this.ctx.smooth(this.abFilterW, fw[abFilter], 16.0f);
        RoundedRect.of(this.abFilterX, y + 2.0f, this.abFilterW, h - 4.0f).radius(3.0f).horizontalGradient(accent.withAlpha(0.7f * a), GuiDraw.getAccent2().withAlpha(0.7f * a)).render(graphics);
        float fx = segX + 2.0f;
        for (int i = 0; i < 3; ++i) {
            int idx = i;
            String key = "ab-filter-" + i;
            boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.ctx.hovered(fx, y, fw[i], h);
            float hv = this.ctx.anim(key, this.ctx.hov(key, hover), 8.0f);
            float sel = this.ctx.anim(String.valueOf(key) + "-sel", abFilter == i, 8.0f);
            Text.of(Fonts.MEDIUM, filters[i], fx + fw[i] * 0.5f, y + h * 0.5f, 4.8f).centered().color(GuiDraw.mix(Color.rgb(140, 145, 156), Color.rgb(255, 255, 255), Math.max(sel, hv * 0.6f)).withAlpha(a)).render(graphics);
            if (GuiDraw.canHit(y, h, top, bottom)) {
                this.hit(fx, y, fw[i], h, () -> {
                    if (abFilter != idx) {
                        abFilter = idx;
                        this.ctx.autoBuy.abSwitchTime = System.currentTimeMillis();
                        ClientSound.ui(ClientSound.Ui.CLICK);
                    }
                }, null);
            }
            fx += fw[i];
        }
        float barH = 3.0f;
        float k = all.isEmpty() ? 0.0f : (float)enabledCount / (float)all.size();
        this.abEnabledBar = this.ctx.smooth(this.abEnabledBar, k, 8.0f);
        Text.of(Fonts.MEDIUM, "\u0410\u043a\u0442\u0438\u0432\u043d\u043e " + enabledCount + " \u0438\u0437 " + all.size(), x + 1.0f, (y += h + 5.0f) + 4.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(150, 155, 166).withAlpha(a)).render(graphics);
        boolean anyOff = all.stream().anyMatch(e -> !e.enabled);
        String allText = anyOff ? "\u0412\u043a\u043b\u044e\u0447\u0438\u0442\u044c \u0432\u0441\u0435" : "\u0412\u044b\u043a\u043b\u044e\u0447\u0438\u0442\u044c \u0432\u0441\u0435";
        float aw = Text.of(Fonts.MEDIUM, allText).size(4.8f).width();
        boolean aHover = GuiDraw.canHit(y, 8.0f, top, bottom) && this.ctx.hovered(x + w - aw - 2.0f, y, aw + 2.0f, 8.0f);
        float ahv = this.ctx.anim("ab-all", this.ctx.hov("ab-all", aHover), 8.0f);
        Text.of(Fonts.MEDIUM, allText, x + w - 1.0f, y + 4.0f, 4.8f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(GuiDraw.mix(Color.rgb(150, 155, 166), accent, ahv).withAlpha(a)).render(graphics);
        if (GuiDraw.canHit(y, 8.0f, top, bottom)) {
            this.hit(x + w - aw - 2.0f, y, aw + 2.0f, 8.0f, () -> {
                for (ItemEntry e : all) {
                    e.enabled = anyOff;
                }
                this.ctx.saveAutoBuy();
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, null);
        }
        RoundedRect.of(x, y += 10.0f, w, barH).radius(1.5f).color(Color.rgba(255, 255, 255, Math.round(8.0f * a))).render(graphics);
        if (this.abEnabledBar > 0.005f) {
            RoundedRect.of(x, y, Math.max(barH, w * this.abEnabledBar), barH).radius(1.5f).horizontalGradient(accent.withAlpha(a), GuiDraw.getAccent2().withAlpha(a)).render(graphics);
        }
        y += barH + 7.0f;
        if (this.abSelected >= all.size()) {
            this.abSelected = -1;
        }
        float insp = this.ctx.anim("ab-insp", this.abSelected >= 0, 7.0f);
        if (this.abSelected >= 0) {
            this.abInspLast = this.abSelected;
        }
        if (insp > 0.01f && this.abInspLast >= 0 && this.abInspLast < all.size()) {
            float ih = all.get((int)this.abInspLast).configurableEnchantments ? 196.0f : 50.0f;
            float vis = ih * insp;
            float clipTop = Math.max(y, top);
            float clipBottom = Math.min(y + vis + 2.0f, bottom);
            if (clipBottom - clipTop >= 1.0f) {
                Scissor.push(graphics, x, clipTop, w, clipBottom - clipTop);
                graphics.pose().pushMatrix();
                graphics.pose().translate(x, y);
                graphics.pose().scale(1.0f, insp);
                graphics.pose().translate(-x, -y);
                this.renderAbInspector(graphics, all.get(this.abInspLast), this.abInspLast, x, y, w, ih, a * insp, accent, insp > 0.95f ? top : Float.MAX_VALUE, bottom, now);
                graphics.pose().popMatrix();
                Scissor.pop(graphics);
            }
            y += (ih + 6.0f) * insp;
        }
        AutoBuyTab cfr_ignored_3 = this.ctx.autoBuy;
        String query = AutoBuyTab.abSearch.trim().toLowerCase(Locale.ROOT);
        ArrayList<Integer> filtered = new ArrayList<Integer>();
        for (int i = 0; i < all.size(); ++i) {
            ItemEntry e2 = all.get(i);
            if (abFilter == 1 && !e2.enabled || abFilter == 2 && e2.enabled) continue;
            String name = e2.displayName != null && !e2.displayName.isBlank() ? e2.displayName : e2.itemId;
            String match = (name + " " + (e2.itemId != null ? e2.itemId : "") + " " + (e2.loreKeyword != null ? e2.loreKeyword : "")).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !match.contains(query)) continue;
            filtered.add(i);
        }
        if (filtered.isEmpty()) {
            Text.of(Fonts.MEDIUM, all.isEmpty() ? "\u0421\u043f\u0438\u0441\u043e\u043a \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432 \u043f\u0443\u0441\u0442" : "\u041d\u0438\u0447\u0435\u0433\u043e \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e", x + w * 0.5f, y + 14.0f, 5.4f).centered().color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
            return y + 30.0f;
        }
        filtered.sort((left, right) -> AutoBuyCatalog.ORDER.compare((ItemEntry)all.get((int)left), (ItemEntry)all.get((int)right)));
        int cols = 6;
        float gap = 4.0f;
        float tile = (w - gap * (float)(cols - 1)) / (float)cols;
        float tileH = tile + 6.0f;
        int groupStart = 0;
        while (groupStart < filtered.size()) {
            int groupEnd;
            int group = AutoBuyCatalog.group(all.get((Integer)filtered.get(groupStart)));
            for (groupEnd = groupStart + 1; groupEnd < filtered.size() && AutoBuyCatalog.group(all.get((Integer)filtered.get(groupEnd))) == group; ++groupEnd) {
            }
            if (y + 12.0f >= top && y <= bottom) {
                String label = AutoBuyCatalog.groupName(group);
                Text.of(Fonts.MEDIUM, label, x + 1.0f, y + 4.0f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(165, 170, 182).withAlpha(a)).render(graphics);
                float labelW = Text.of(Fonts.MEDIUM, label).size(5.0f).width();
                float lineX = x + labelW + 8.0f;
                if (lineX < x + w) {
                    RoundedRect.of(lineX, y + 4.0f, x + w - lineX, 0.5f).color(Color.rgba(255, 255, 255, Math.round(12.0f * a))).render(graphics);
                }
            }
            y += 12.0f;
            for (int n = groupStart; n < groupEnd; ++n) {
                int idx = (Integer)filtered.get(n);
                int cell = n - groupStart;
                float t = GuiDraw.easeOut((float)(now - this.ctx.autoBuy.abSwitchTime - (long)Math.min(n, 30) * 12L) / 320.0f);
                float tx = x + (float)(cell % cols) * (tile + gap);
                float ty = y + (float)(cell / cols) * (tileH + gap);
                if (!(ty + tileH >= top) || !(ty <= bottom)) continue;
                this.renderAbTile(graphics, all.get(idx), idx, tx, ty, tile, tileH, a, t, accent, top, bottom);
            }
            int rows = (groupEnd - groupStart + cols - 1) / cols;
            y += (float)rows * (tileH + gap) - gap + 9.0f;
            groupStart = groupEnd;
        }
        Text.of(Fonts.MEDIUM, "\u041b\u041a\u041c - \u0432\u043a\u043b/\u0432\u044b\u043a\u043b    \u0421\u041a\u041c - \u043f\u0430\u0440\u0441    \u041f\u041a\u041c - \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438", x + w * 0.5f, y + 3.0f, 4.4f).centered().color(Color.rgb(95, 100, 111).withAlpha(a)).render(graphics);
        return y + 8.0f;
    }

    private void renderAbTile(GuiGraphicsExtractor graphics, ItemEntry entry, int idx, float x, float y, float w, float h, float a, float t, Color accent, float top, float bottom) {
        ItemStack stack;
        String key = "ab-tile-" + idx;
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.ctx.hovered(x, y, w, h);
        float hv = this.ctx.anim(key, this.ctx.hov(key, hover), 8.0f);
        float on = this.ctx.anim(String.valueOf(key) + "-on", entry.enabled, 7.0f);
        float sel = this.ctx.anim(String.valueOf(key) + "-sel", this.abSelected == idx, 8.0f);
        float scale = 0.85f + 0.15f * t;
        float cx = x + w * 0.5f;
        float cy = y + h * 0.5f;
        float sw = w * scale;
        float sh = h * scale;
        float sx = cx - sw * 0.5f;
        float sy = cy - sh * 0.5f + (1.0f - t) * 4.0f;
        float ta = a * t;
        RoundedRect.of(sx, sy, sw, sh).radius(6.0f).color(Color.rgba(255, 255, 255, Math.round((5.0f + 5.0f * hv) * ta))).render(graphics);
        if (on > 0.01f) {
            RoundedRect.of(sx, sy, sw, sh).radius(6.0f).verticalGradient(accent.withAlpha(0.0f), accent.withAlpha(0.22f * on * ta)).render(graphics);
        }
        if (sel > 0.01f || on > 0.01f) {
            Outline.of(sx, sy, sw, sh).radius(6.0f).thickness(0.7f).color(GuiDraw.mix(accent, Color.rgb(255, 255, 255), sel).withAlpha((0.45f * on + 0.5f * sel) * ta)).render(graphics);
        }
        float icon = 18.0f + 2.0f * hv;
        float iy = sy + 9.0f - hv * 1.5f;
        if (ta > 0.3f && iy + icon >= top && iy <= bottom && !(stack = AutoBuyPanel.iconStack(entry.itemId, entry.displayName)).isEmpty()) {
            AutoBuyPanel.drawItemAt(graphics, stack, cx - icon * 0.5f, iy, icon);
        }
        if (on < 0.99f) {
            RoundedRect.of(cx - 11.0f, iy - 1.0f, 22.0f, 22.0f).radius(5.0f).color(Color.rgba(20, 22, 28, Math.round(110.0f * (1.0f - on) * ta))).render(graphics);
        }
        float pd = this.ctx.anim(String.valueOf(key) + "-parse", entry.parseEnabled, 9.0f);
        float ds = 3.5f;
        RoundedRect.of(sx + sw - 5.25f - ds * 0.5f, sy + 5.75f - ds * 0.5f, ds, ds).radius(ds * 0.5f).color(GuiDraw.mix(Color.rgb(238, 96, 96), Color.rgb(98, 214, 134), pd).withAlpha(ta)).render(graphics);
        String name = entry.displayName != null && !entry.displayName.isBlank() ? entry.displayName : entry.itemId;
        Text.of(Fonts.MEDIUM, GuiDraw.fit(name, 4.3f, sw - 5.0f), cx, sy + sh - 12.0f, 4.3f).centered().color(GuiDraw.mix(Color.rgb(140, 145, 156), Color.rgb(235, 238, 245), on).withAlpha(ta)).render(graphics);
        String price = entry.maxUnitPrice > 0L ? AutoBuyPanel.fmt(entry.maxUnitPrice) + "$" : "\u0446\u0435\u043d\u0430 \u043d\u0435 \u0437\u0430\u0434\u0430\u043d\u0430";
        Text.of(Fonts.MEDIUM, GuiDraw.fit(price, 4.0f, sw - 5.0f), cx, sy + sh - 5.5f, 4.0f).centered().color((entry.maxUnitPrice > 0L ? GuiDraw.mix(Color.rgb(120, 125, 136), accent, on) : Color.rgb(95, 100, 111)).withAlpha(ta)).render(graphics);
        if (GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(x, y, w, h, () -> {
                entry.enabled = !entry.enabled;
                this.ctx.saveAutoBuy();
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, () -> {
                this.ctx.focus.setAbField(null, "");
                this.abSelected = idx;
                this.abInspLast = idx;
                this.ctx.scrollbar.scrollDragging = false;
                this.ctx.scrollTarget = 0.0f;
                ClientSound.ui(ClientSound.Ui.EXPAND);
            });
            this.ctx.hits.middle(x, y, w, h, () -> {
                entry.parseEnabled = !entry.parseEnabled;
                this.ctx.saveAutoBuy();
                ClientSound.ui(ClientSound.Ui.CLICK);
            });
        }
    }

    private void renderAbInspector(GuiGraphicsExtractor graphics, ItemEntry entry, int idx, float x, float y, float w, float h, float a, Color accent, float top, float bottom, long now) {
        ItemStack stack;
        RoundedRect.of(x, y, w, h).radius(6.0f).horizontalGradient(accent.withAlpha(0.14f * a), GuiDraw.getAccent2().withAlpha(0.06f * a)).render(graphics);
        RoundedRect.of(x, y, w, h).radius(6.0f).color(Color.rgba(255, 255, 255, Math.round(5.0f * a))).render(graphics);
        float ib = 30.0f;
        float ix = x + 8.0f;
        float iy = y + 10.0f;
        RoundedRect.of(ix, iy, ib, ib).radius(6.0f).color(Color.rgba(0, 0, 0, Math.round(40.0f * a))).render(graphics);
        if (a > 0.3f && !(stack = AutoBuyPanel.iconStack(entry.itemId, entry.displayName)).isEmpty()) {
            AutoBuyPanel.drawItemAt(graphics, stack, ix + 3.0f, iy + 3.0f, 24.0f);
        }
        float tx = ix + ib + 8.0f;
        float on = this.ctx.anim("ab-insp-on-" + idx, entry.enabled, 8.0f);
        float swX = x + w - 16.0f - 9.0f;
        GuiDraw.renderSwitch(graphics, swX, y + 8.0f, 16.0f, 8.5f, on, accent, a);
        float closeX = swX - 12.0f;
        float closeY = y + 12.25f;
        boolean cHover = GuiDraw.canHit(y, h, top, bottom) && this.ctx.hovered(closeX - 4.0f, closeY - 4.0f, 8.0f, 8.0f);
        float chv = this.ctx.anim("ab-insp-close", this.ctx.hov("ab-insp-close", cHover), 8.0f);
        Color cc = GuiDraw.mix(Color.rgb(130, 135, 146), Color.rgb(245, 247, 252), chv).withAlpha(a);
        GuiDraw.stroke(graphics, closeX - 2.0f, closeY - 2.0f, closeX + 2.0f, closeY + 2.0f, 0.8f, cc);
        GuiDraw.stroke(graphics, closeX - 2.0f, closeY + 2.0f, closeX + 2.0f, closeY - 2.0f, 0.8f, cc);
        String name = entry.displayName != null && !entry.displayName.isBlank() ? entry.displayName : entry.itemId;
        Text.of(Fonts.MEDIUM, GuiDraw.fit(name, 6.4f, closeX - tx - 8.0f), tx, y + 12.0f, 6.4f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(245, 247, 252).withAlpha(a)).render(graphics);
        String market = "\u0420\u044b\u043d\u043e\u043a: " + (String)(entry.marketPrice > 0L ? AutoBuyPanel.fmt(entry.marketPrice) + "$" : "\u043d\u0435\u0442 \u0434\u0430\u043d\u043d\u044b\u0445");
        Text.of(Fonts.MEDIUM, market, tx, y + 21.0f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(140, 145, 156).withAlpha(a)).render(graphics);
        float rowY = y + 35.0f;
        float chipH = 11.0f;
        float cx = tx;
        String priceId = "price:" + idx;
        boolean editPrice = priceId.equals(this.ctx.focus.abField);
        String priceText = "\u0426\u0435\u043d\u0430: " + (String)(editPrice ? (this.ctx.focus.abInput.isEmpty() ? "0" : this.ctx.focus.abInput.toString()) + "$" : (entry.maxUnitPrice > 0L ? AutoBuyPanel.fmt(entry.maxUnitPrice) + "$" : "\u043d\u0435\u0442"));
        float pw = Text.of(Fonts.MEDIUM, priceText).size(4.6f).width() + 12.0f;
        this.ctx.widgets.renderAbChip(graphics, "ab-insp-price", priceText, cx, rowY, pw, chipH, a, accent, editPrice, entry.maxUnitPrice > 0L, top, bottom, () -> {
            this.ctx.focus.setAbField(priceId, entry.maxUnitPrice > 0L ? String.valueOf(entry.maxUnitPrice) : "");
            ClientSound.ui(ClientSound.Ui.CLICK);
        });
        cx += pw + 4.0f;
        if (entry.isDamageableEquipment()) {
            String durId = "dur:" + idx;
            boolean editDur = durId.equals(this.ctx.focus.abField);
            int dv = entry.minDurabilityPercent;
            String durText = "\u041f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u044c: " + (editDur ? (this.ctx.focus.abInput.isEmpty() ? "0" : this.ctx.focus.abInput.toString()) : String.valueOf(dv)) + "%";
            float dw = Text.of(Fonts.MEDIUM, durText).size(4.6f).width() + 12.0f;
            this.ctx.widgets.renderAbChip(graphics, "ab-insp-dur", durText, cx, rowY, dw, chipH, a, accent, editDur, entry.minDurabilityPercent > 0, top, bottom, () -> {
                this.ctx.focus.setAbField(durId, String.valueOf(dv));
                ClientSound.ui(ClientSound.Ui.CLICK);
            });
            cx += dw + 4.0f;
        }
        float parse = this.ctx.anim("ab-insp-parse-" + idx, entry.parseEnabled, 8.0f);
        float pl = Text.of(Fonts.MEDIUM, "\u0410\u0432\u0442\u043e-\u043f\u0430\u0440\u0441 \u0446\u0435\u043d\u044b").size(4.6f).width();
        float pgw = pl + 9.0f + 12.0f;
        boolean pHover = GuiDraw.canHit(rowY, chipH, top, bottom) && this.ctx.hovered(cx, rowY, pgw, chipH);
        float phv = this.ctx.anim("ab-insp-parse-h", this.ctx.hov("ab-insp-parse-h", pHover), 8.0f);
        RoundedRect.of(cx, rowY, pgw, chipH).radius(3.0f).color(Color.rgba(255, 255, 255, Math.round((10.0f + 6.0f * phv) * a))).render(graphics);
        GuiDraw.renderCheckBox(graphics, cx + 3.0f, rowY + 2.0f, 7.0f, parse, accent, a);
        Text.of(Fonts.MEDIUM, "\u0410\u0432\u0442\u043e-\u043f\u0430\u0440\u0441 \u0446\u0435\u043d\u044b", cx + 13.0f, rowY + chipH * 0.5f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(GuiDraw.mix(Color.rgb(140, 145, 156), Color.rgb(235, 238, 245), parse).withAlpha(a)).render(graphics);
        if (GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(swX - 2.0f, y + 6.0f, 20.0f, 12.5f, () -> {
                entry.enabled = !entry.enabled;
                this.ctx.saveAutoBuy();
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, null);
            this.hit(closeX - 4.0f, closeY - 4.0f, 8.0f, 8.0f, () -> {
                this.abSelected = -1;
                ClientSound.ui(ClientSound.Ui.COLLAPSE);
            }, null);
            float pcx = cx;
            this.hit(pcx, rowY, pgw, chipH, () -> {
                entry.parseEnabled = !entry.parseEnabled;
                this.ctx.saveAutoBuy();
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, null);
        }
        if (entry.configurableEnchantments) {
            float enchantY = y + 54.0f;
            Text.of(Fonts.MEDIUM, "\u041c\u0438\u043d\u0438\u043c\u0430\u043b\u044c\u043d\u044b\u0435 \u0443\u0440\u043e\u0432\u043d\u0438 \u0437\u0430\u0447\u0430\u0440\u043e\u0432\u0430\u043d\u0438\u0439 (0: \u043d\u0435 \u043f\u0440\u043e\u0432\u0435\u0440\u044f\u0442\u044c)", x + 8.0f, enchantY + 4.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(160, 165, 177).withAlpha(a)).render(graphics);
            enchantY += 14.0f;
            float enchantW = (w - 20.0f) * 0.5f;
            for (int n = 0; n < AutoBuyEnchantments.SWORD.length; ++n) {
                AutoBuyEnchantments.Enchantment enchantment = AutoBuyEnchantments.SWORD[n];
                int level = entry.requiredEnchantments.getOrDefault(enchantment.id(), 0);
                this.ctx.widgets.renderAbStepper(graphics, "ench-" + idx + "-" + enchantment.id(), enchantment.label(), (String)(level == 0 ? "\u041d\u0435\u0442" : level + "/" + enchantment.maxLevel()), x + 8.0f + (float)(n % 2) * (enchantW + 4.0f), enchantY + (float)(n / 2) * 20.0f, enchantW, a, accent, top, bottom, () -> entry.requiredEnchantments.put(enchantment.id(), Math.max(0, level - 1)), () -> entry.requiredEnchantments.put(enchantment.id(), Math.min(enchantment.maxLevel(), level + 1)));
            }
        }
    }
}

