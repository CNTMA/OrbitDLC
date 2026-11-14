/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.tab.market;

import brokke.cc.system.market.MarketCard;
import brokke.cc.system.market.MarketClient;
import brokke.cc.system.market.MarketDetails;
import brokke.cc.system.market.MarketInstaller;
import brokke.cc.system.market.MarketPublishRequest;
import brokke.cc.system.market.MarketScript;
import brokke.cc.system.market.MarketState;
import brokke.cc.system.scripting.runtime.ScriptInstance;
import brokke.cc.system.scripting.runtime.ScriptManager;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.ui.clickgui.tab.market.MarketTab;
import brokke.cc.ui.clickgui.tab.market.MarketWidgets;
import brokke.cc.util.Log;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.TextAlign;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class MarketPublishView
extends GuiComponent {
    private static final int MAX_CODE = 262144;
    private final MarketTab tab;
    private String selectedFile;
    private String editingId;
    private boolean busy;
    private String unpublishArmed;
    private long unpublishArmedAt;

    public MarketPublishView(ClickGuiContext ctx, MarketTab tab) {
        super(ctx);
        this.tab = tab;
    }

    public void onOpen() {
        MarketState.refresh(null);
    }

    public void edit(MarketCard card, MarketScript script) {
        this.editingId = card.id();
        MarketTab.setField("market:name", card.name());
        MarketTab.setField("market:description", card.description());
        MarketTab.setField("market:long", script != null ? script.longDescription() : "");
        MarketTab.setField("market:tags", String.join((CharSequence)", ", card.tags()));
        MarketTab.setField("market:price", String.valueOf(card.price()));
        MarketTab.setField("market:version", card.version());
        MarketTab.setField("market:changelog", "");
        MarketInstaller.Installed local = MarketInstaller.installed(card.id());
        if (local != null) {
            this.selectedFile = local.file().getName();
        }
        if (script == null) {
            String id = card.id();
            MarketClient.script(id, r -> {
                if (r.ok() && id.equals(this.editingId) && MarketTab.field("market:long").isEmpty()) {
                    MarketTab.setField("market:long", ((MarketDetails)r.value()).script().longDescription());
                }
            });
        }
    }

    private void resetForm() {
        this.editingId = null;
        for (String id : new String[]{"name", "description", "long", "tags", "price", "version", "changelog"}) {
            MarketTab.setField("market:" + id, "");
        }
    }

    private List<File> localScripts() {
        ArrayList<File> out = new ArrayList<File>();
        try {
            for (ScriptInstance s : ScriptManager.get().all()) {
                if (s.file == null || !s.file.exists()) continue;
                out.add(s.file);
            }
        }
        catch (Throwable ignored) {
            Log.swallow(ignored);
        }
        out.sort((p, q) -> p.getName().compareToIgnoreCase(q.getName()));
        return out;
    }

    private void select(File file) {
        this.selectedFile = file.getName();
        if (MarketTab.field("market:name").isBlank()) {
            String n = file.getName();
            MarketTab.setField("market:name", n.endsWith(".kts") ? n.substring(0, n.length() - 4) : n);
        }
        if (MarketTab.field("market:version").isBlank()) {
            for (ScriptInstance s : ScriptManager.get().all()) {
                if (s.file == null || !s.file.getName().equals(file.getName()) || s.version.isEmpty()) continue;
                MarketTab.setField("market:version", s.version);
            }
        }
        if (MarketTab.field("market:description").isBlank()) {
            for (ScriptInstance s : ScriptManager.get().all()) {
                if (s.file == null || !s.file.getName().equals(file.getName()) || s.description.isEmpty()) continue;
                String d = s.description;
                MarketTab.setField("market:description", d.length() > 200 ? d.substring(0, 200) : d);
            }
        }
    }

    private void publish() {
        int price;
        this.ctx.focus.setAbField(null, "");
        File file = null;
        for (File f : this.localScripts()) {
            if (!f.getName().equals(this.selectedFile)) continue;
            file = f;
        }
        if (file == null) {
            this.tab.toast("\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u043b\u043e\u043a\u0430\u043b\u044c\u043d\u044b\u0439 \u0441\u043a\u0440\u0438\u043f\u0442", false);
            return;
        }
        String code = MarketInstaller.readCode(file);
        if (code == null || code.isBlank()) {
            this.tab.toast(MarketClient.message("bad_code"), false);
            return;
        }
        if (code.getBytes(StandardCharsets.UTF_8).length > 262144) {
            this.tab.toast(MarketClient.message("too_large"), false);
            return;
        }
        String name = MarketTab.field("market:name").trim();
        if (name.length() < 3 || name.length() > 32) {
            this.tab.toast(MarketClient.message("bad_name"), false);
            return;
        }
        ArrayList<String> tags = new ArrayList<String>();
        for (String t : MarketTab.field("market:tags").split("[,\\s]+")) {
            String clean = t.trim().replace("#", "").toLowerCase(Locale.ROOT);
            if (clean.isEmpty() || tags.contains(clean)) continue;
            tags.add(clean.length() > 16 ? clean.substring(0, 16) : clean);
        }
        if (tags.size() > 5) {
            this.tab.toast(MarketClient.message("bad_tags"), false);
            return;
        }
        try {
            String raw = MarketTab.field("market:price").trim();
            price = raw.isEmpty() ? 0 : Integer.parseInt(raw);
        }
        catch (NumberFormatException e) {
            price = -1;
        }
        if (price < 0 || price > 1000000) {
            this.tab.toast(MarketClient.message("bad_price"), false);
            return;
        }
        String version = MarketTab.field("market:version").trim();
        if (version.isEmpty()) {
            version = "1.0";
        }
        MarketPublishRequest request = new MarketPublishRequest(this.editingId, name, MarketTab.field("market:description").trim(), MarketTab.field("market:long").trim(), tags, price, version, MarketTab.field("market:changelog").trim(), code);
        this.busy = true;
        boolean update = this.editingId != null;
        MarketClient.publish(request, r -> {
            this.busy = false;
            if (!r.ok()) {
                this.tab.toast(r.message(), false);
                return;
            }
            this.tab.toast(update ? "\u041d\u043e\u0432\u0430\u044f \u0432\u0435\u0440\u0441\u0438\u044f \u043e\u043f\u0443\u0431\u043b\u0438\u043a\u043e\u0432\u0430\u043d\u0430" : "\u0421\u043a\u0440\u0438\u043f\u0442 \u043e\u043f\u0443\u0431\u043b\u0438\u043a\u043e\u0432\u0430\u043d", true);
            MarketState.noteVersion(((MarketCard)r.value()).id(), ((MarketCard)r.value()).version());
            this.resetForm();
            MarketState.refresh(null);
            this.tab.catalog.reload();
        });
    }

    private void unpublish(MarketCard card) {
        long now = System.currentTimeMillis();
        if (!card.id().equals(this.unpublishArmed) || now - this.unpublishArmedAt > 3000L) {
            this.unpublishArmed = card.id();
            this.unpublishArmedAt = now;
            return;
        }
        this.unpublishArmed = null;
        this.busy = true;
        MarketClient.unpublish(card.id(), r -> {
            this.busy = false;
            if (!r.ok()) {
                this.tab.toast(r.message(), false);
                return;
            }
            this.tab.toast("\u00ab" + card.name() + "\u00bb \u0441\u043d\u044f\u0442 \u0441 \u043f\u0440\u043e\u0434\u0430\u0436\u0438", true);
            if (card.id().equals(this.editingId)) {
                this.resetForm();
            }
            MarketState.refresh(null);
            this.tab.catalog.reload();
        });
    }

    public float render(GuiGraphicsExtractor graphics, float x, float y, float w, float a, float top, float bottom, long now) {
        float bw;
        String longText;
        MarketWidgets ui = this.tab.ui;
        y = GuiDraw.sectionTitle(graphics, this.editingId != null ? "\u041e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u043a\u0440\u0438\u043f\u0442\u0430" : "\u041e\u043f\u0443\u0431\u043b\u0438\u043a\u043e\u0432\u0430\u0442\u044c \u0441\u043a\u0440\u0438\u043f\u0442", x, y, a);
        y = ui.label(graphics, "\u041b\u043e\u043a\u0430\u043b\u044c\u043d\u044b\u0439 \u0441\u043a\u0440\u0438\u043f\u0442", x, y, a);
        List<File> files = this.localScripts();
        if (files.isEmpty()) {
            Text.of(Fonts.MEDIUM, "\u0412 \u043f\u0430\u043f\u043a\u0435 \u0441\u043a\u0440\u0438\u043f\u0442\u043e\u0432 \u043f\u0443\u0441\u0442\u043e", x + 2.0f, y + 5.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
            y += 12.0f;
        } else {
            float cx = x;
            for (File f : files) {
                String label = f.getName();
                float cw = Text.of(Fonts.MEDIUM, label).size(4.8f).width() + 10.0f;
                if (cx + cw > x + w) {
                    cx = x;
                    y += 13.0f;
                }
                cx += ui.chip(graphics, "market-local-" + label, label, cx, y, 10.0f, a, label.equals(this.selectedFile), top, bottom, () -> this.select(f)) + 3.0f;
            }
            y += 14.0f;
        }
        float half = (w - 4.0f) * 0.5f;
        y = ui.label(graphics, "\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 (3-32)", x, y, a);
        ui.field(graphics, "market:name", "\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435", x, y, half, 14.0f, a, top, bottom, now);
        ui.field(graphics, "market:version", "\u0412\u0435\u0440\u0441\u0438\u044f, \u043d\u0430\u043f\u0440\u0438\u043c\u0435\u0440 1.0", x + half + 4.0f, y, half, 14.0f, a, top, bottom, now);
        y += 17.0f;
        y = ui.label(graphics, "\u041a\u0440\u0430\u0442\u043a\u043e\u0435 \u043e\u043f\u0438\u0441\u0430\u043d\u0438\u0435 (\u0434\u043e 200)", x, y, a);
        ui.field(graphics, "market:description", "\u041e\u0434\u043d\u0430-\u0434\u0432\u0435 \u0441\u0442\u0440\u043e\u043a\u0438 \u043e \u0441\u043a\u0440\u0438\u043f\u0442\u0435", x, y, w, 14.0f, a, top, bottom, now);
        y += 17.0f;
        y = ui.label(graphics, "\u041f\u043e\u043b\u043d\u043e\u0435 \u043e\u043f\u0438\u0441\u0430\u043d\u0438\u0435 (\u0434\u043e 4000)", x, y, a);
        ui.field(graphics, "market:long", "\u041f\u043e\u0434\u0440\u043e\u0431\u043d\u043e\u0435 \u043e\u043f\u0438\u0441\u0430\u043d\u0438\u0435, \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438, \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438", x, y, w, 14.0f, a, top, bottom, now);
        y += 17.0f;
        String string = longText = "market:long".equals(this.ctx.focus.abField) ? this.ctx.focus.abInput.toString() : MarketTab.field("market:long");
        if (!longText.isBlank()) {
            y = ui.paragraph(graphics, longText, x + 2.0f, y, w - 4.0f, 4.6f, MarketWidgets.SOFT, a * 0.8f, top, bottom) + 3.0f;
        }
        y = ui.label(graphics, "\u0422\u0435\u0433\u0438 \u0447\u0435\u0440\u0435\u0437 \u0437\u0430\u043f\u044f\u0442\u0443\u044e (\u0434\u043e 5)  /  \u0426\u0435\u043d\u0430 \u0432 BC (0 = \u0431\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u043e)", x, y, a);
        ui.field(graphics, "market:tags", "pvp, render, utility", x, y, half, 14.0f, a, top, bottom, now);
        ui.field(graphics, "market:price", "0", x + half + 4.0f, y, half, 14.0f, a, top, bottom, now);
        y += 17.0f;
        y = ui.label(graphics, "\u0427\u0442\u043e \u043d\u043e\u0432\u043e\u0433\u043e (\u0434\u043e 500)", x, y, a);
        ui.field(graphics, "market:changelog", "\u0421\u043f\u0438\u0441\u043e\u043a \u0438\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0439 \u044d\u0442\u043e\u0439 \u0432\u0435\u0440\u0441\u0438\u0438", x, y, w, 14.0f, a, top, bottom, now);
        Text.of(Fonts.MEDIUM, "\u0421 \u043a\u0430\u0436\u0434\u043e\u0439 \u043f\u0440\u043e\u0434\u0430\u0436\u0438 \u0443\u0434\u0435\u0440\u0436\u0438\u0432\u0430\u0435\u0442\u0441\u044f \u043a\u043e\u043c\u0438\u0441\u0441\u0438\u044f 5%", x + 2.0f, (y += 18.0f) + 4.0f, 4.4f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
        float f = bw = this.editingId != null ? (w - 4.0f) * 0.5f : w;
        ui.button(graphics, "market-publish", this.busy ? "\u041e\u0442\u043f\u0440\u0430\u0432\u043a\u0430..." : (this.editingId != null ? "\u041e\u043f\u0443\u0431\u043b\u0438\u043a\u043e\u0432\u0430\u0442\u044c \u043d\u043e\u0432\u0443\u044e \u0432\u0435\u0440\u0441\u0438\u044e" : "\u041e\u043f\u0443\u0431\u043b\u0438\u043a\u043e\u0432\u0430\u0442\u044c"), x, y += 10.0f, bw, 15.0f, a, GuiDraw.getAccent(), true, top, bottom, this.busy ? null : this::publish);
        if (this.editingId != null) {
            ui.button(graphics, "market-publish-cancel", "\u041e\u0442\u043c\u0435\u043d\u0430", x + bw + 4.0f, y, bw, 15.0f, a, GuiDraw.getAccent(), false, top, bottom, this::resetForm);
        }
        return this.renderListings(graphics, x, y += 22.0f, w, a, top, bottom, now);
    }

    private float renderListings(GuiGraphicsExtractor graphics, float x, float y, float w, float a, float top, float bottom, long now) {
        MarketWidgets ui = this.tab.ui;
        y = GuiDraw.sectionTitle(graphics, "\u041c\u043e\u0438 \u0441\u043a\u0440\u0438\u043f\u0442\u044b", x, y, a);
        List<MarketCard> listings = MarketState.listings();
        if (listings.isEmpty()) {
            Text.of(Fonts.MEDIUM, MarketState.loading() ? "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430..." : "\u0412\u044b \u0435\u0449\u0451 \u043d\u0438\u0447\u0435\u0433\u043e \u043d\u0435 \u043e\u043f\u0443\u0431\u043b\u0438\u043a\u043e\u0432\u0430\u043b\u0438", x + 2.0f, y + 5.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
            return y + 14.0f;
        }
        Map<String, MarketInstaller.Installed> installed = MarketInstaller.installed();
        for (MarketCard card : listings) {
            float h = 24.0f;
            RoundedRect.of(x, y, w, h).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round(6.0f * a))).render(graphics);
            Text.of(Fonts.MEDIUM, GuiDraw.fit(card.name() + "  v" + card.version(), 5.6f, w - 150.0f), x + 6.0f, y + 7.5f, 5.6f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.TEXT.withAlpha(a)).render(graphics);
            String stats = card.sales() + " \u043f\u0440\u043e\u0434\u0430\u0436   " + card.downloads() + " \u0437\u0430\u0433\u0440\u0443\u0437\u043e\u043a   " + (card.ratings() > 0 ? String.format(Locale.ROOT, "%.1f", Float.valueOf(card.rating())) : "-");
            Text.of(Fonts.MEDIUM, stats, x + 6.0f, y + 16.5f, 4.4f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
            ui.price(graphics, card, x + w - 140.0f, y + 7.5f, 5.0f, a);
            if (installed.containsKey(card.id())) {
                Text.of(Fonts.MEDIUM, "\u0443\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d", x + w - 140.0f, y + 16.5f, 4.2f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
            }
            float bw = 42.0f;
            float by = y + 5.0f;
            ui.button(graphics, "market-mine-open-" + card.id(), "\u041e\u0442\u043a\u0440\u044b\u0442\u044c", x + w - bw * 3.0f - 12.0f, by, bw, 14.0f, a, GuiDraw.getAccent(), false, top, bottom, () -> this.tab.openScript(card.id()));
            ui.button(graphics, "market-mine-edit-" + card.id(), "\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c", x + w - bw * 2.0f - 9.0f, by, bw, 14.0f, a, GuiDraw.getAccent(), false, top, bottom, () -> {
                this.edit(card, null);
                this.ctx.scrollTarget = 0.0f;
            });
            boolean armed = card.id().equals(this.unpublishArmed) && now - this.unpublishArmedAt <= 3000L;
            ui.button(graphics, "market-mine-remove-" + card.id(), armed ? "\u0422\u043e\u0447\u043d\u043e?" : "\u0421\u043d\u044f\u0442\u044c", x + w - bw - 6.0f, by, bw, 14.0f, a, MarketWidgets.RED, armed, top, bottom, this.busy ? null : () -> this.unpublish(card));
            y += h + 4.0f;
        }
        return y;
    }
}

