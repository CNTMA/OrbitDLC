/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.tab.market;

import brokke.cc.system.market.MarketCard;
import brokke.cc.system.market.MarketClient;
import brokke.cc.system.market.MarketInstaller;
import brokke.cc.system.market.MarketPage;
import brokke.cc.system.market.MarketState;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.ui.clickgui.tab.market.MarketTab;
import brokke.cc.ui.clickgui.tab.market.MarketWidgets;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class MarketCatalogView
extends GuiComponent {
    private static final String[] SORTS = new String[]{"popular", "new", "rating", "price_asc", "price_desc"};
    private static final String[] SORT_NAMES = new String[]{"\u041f\u043e\u043f\u0443\u043b\u044f\u0440\u043d\u044b\u0435", "\u041d\u043e\u0432\u044b\u0435", "\u041f\u043e \u0440\u0435\u0439\u0442\u0438\u043d\u0433\u0443", "\u0421\u043d\u0430\u0447\u0430\u043b\u0430 \u0434\u0435\u0448\u0451\u0432\u044b\u0435", "\u0421\u043d\u0430\u0447\u0430\u043b\u0430 \u0434\u043e\u0440\u043e\u0433\u0438\u0435"};
    private static final String[] FILTERS = new String[]{"all", "free", "paid", "owned", "mine"};
    private static final String[] FILTER_NAMES = new String[]{"\u0412\u0441\u0435", "\u0411\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u044b\u0435", "\u041f\u043b\u0430\u0442\u043d\u044b\u0435", "\u041a\u0443\u043f\u043b\u0435\u043d\u043d\u044b\u0435", "\u041c\u043e\u0438"};
    private static final int PAGE_SIZE = 20;
    private static int sort = 0;
    private static int filter = 0;
    private static String tag = "";
    private final MarketTab tab;
    private final List<MarketCard> items = new ArrayList<MarketCard>();
    private final Map<String, Integer> knownTags = new LinkedHashMap<String, Integer>();
    private int total = -1;
    private int nextPage = 0;
    private boolean loading;
    private String error;
    private int requestId;
    private String appliedQuery = "";
    private String typedQuery = "";
    private long typedAt;
    private boolean loadedOnce;

    public MarketCatalogView(ClickGuiContext ctx, MarketTab tab) {
        super(ctx);
        this.tab = tab;
    }

    public void ensureLoaded() {
        if (!this.loadedOnce) {
            this.reload();
        }
    }

    public void reload() {
        this.loadedOnce = true;
        this.items.clear();
        this.total = -1;
        this.nextPage = 0;
        this.loadMore();
    }

    private void loadMore() {
        int id = ++this.requestId;
        this.loading = true;
        this.error = null;
        this.appliedQuery = MarketTab.field("market:query").trim();
        MarketClient.browse(this.appliedQuery, SORTS[sort], FILTERS[filter], tag, this.nextPage, 20, r -> {
            if (id != this.requestId) {
                return;
            }
            this.loading = false;
            if (!r.ok()) {
                this.error = r.message();
                return;
            }
            this.items.addAll(((MarketPage)r.value()).items());
            this.total = ((MarketPage)r.value()).total();
            ++this.nextPage;
            for (MarketCard card : ((MarketPage)r.value()).items()) {
                MarketState.noteVersion(card.id(), card.version());
                MarketInstaller.rememberAuthor(card.id(), card.author());
                for (String t : card.tags()) {
                    this.knownTags.merge(t, 1, Integer::sum);
                }
            }
        });
    }

    private void watchQuery(long now) {
        if ("market:query".equals(this.ctx.focus.abField)) {
            String typed = this.ctx.focus.abInput.toString().trim();
            if (!typed.equals(this.typedQuery)) {
                this.typedQuery = typed;
                this.typedAt = now;
            }
            if (now - this.typedAt > 450L && !typed.equals(this.appliedQuery)) {
                MarketTab.setField("market:query", typed);
                this.reload();
            }
        } else {
            String committed;
            this.typedQuery = committed = MarketTab.field("market:query").trim();
            if (!committed.equals(this.appliedQuery) && this.loadedOnce) {
                this.reload();
            }
        }
    }

    public float render(GuiGraphicsExtractor graphics, float x, float y, float w, float a, float top, float bottom, long now) {
        this.watchQuery(now);
        MarketWidgets ui = this.tab.ui;
        float sortW = 74.0f;
        ui.field(graphics, "market:query", "\u041f\u043e\u0438\u0441\u043a \u0441\u043a\u0440\u0438\u043f\u0442\u043e\u0432...", x, y, w - sortW - 4.0f, 14.0f, a, top, bottom, now);
        ui.button(graphics, "market-sort", SORT_NAMES[sort], x + w - sortW, y, sortW, 14.0f, a, GuiDraw.getAccent(), false, top, bottom, () -> {
            sort = (sort + 1) % SORTS.length;
            this.reload();
        });
        y += 18.0f;
        float cx = x;
        for (int i = 0; i < FILTERS.length; ++i) {
            int idx = i;
            cx += ui.chip(graphics, "market-filter-" + i, FILTER_NAMES[i], cx, y, 10.0f, a, filter == i, top, bottom, () -> {
                filter = idx;
                this.reload();
            }) + 3.0f;
        }
        y += 13.0f;
        if (!this.knownTags.isEmpty() || !tag.isEmpty()) {
            String t;
            float tw;
            ArrayList<String> tags = new ArrayList<String>(this.knownTags.keySet());
            tags.sort((p, q) -> Integer.compare(this.knownTags.get(q), this.knownTags.get(p)));
            if (!tag.isEmpty() && !tags.contains(tag)) {
                tags.add(0, tag);
            }
            cx = x;
            float tagY = y;
            cx += ui.chip(graphics, "market-tag-all", "# \u0432\u0441\u0435", cx, tagY, 9.0f, a, tag.isEmpty(), top, bottom, () -> {
                tag = "";
                this.reload();
            }) + 3.0f;
            for (int i = 0; i < Math.min(tags.size(), 10) && !(cx + (tw = Text.of(Fonts.MEDIUM, "#" + (t = (String)tags.get(i))).size(4.8f).width() + 10.0f) > x + w); ++i) {
                String tag2 = t;
                cx += ui.chip(graphics, "market-tag-" + t, "#" + t, cx, tagY, 9.0f, a, t.equals(tag), top, bottom, () -> {
                    tag = tag2.equals(tag) ? "" : tag2;
                    this.reload();
                }) + 3.0f;
            }
            y += 13.0f;
        }
        if (this.error != null) {
            Text.of(Fonts.MEDIUM, GuiDraw.fit(this.error, 5.2f, w - 60.0f), x + 2.0f, y + 7.0f, 5.2f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.RED.withAlpha(a)).render(graphics);
            ui.button(graphics, "market-retry", "\u041f\u043e\u0432\u0442\u043e\u0440\u0438\u0442\u044c", x + w - 54.0f, y, 54.0f, 13.0f, a, GuiDraw.getAccent(), false, top, bottom, this::reload);
            y += 18.0f;
        }
        if (this.items.isEmpty()) {
            String text;
            text = this.loading ? "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430..." : this.error == null ? "\u041d\u0438\u0447\u0435\u0433\u043e \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e" : "";
            if (!text.isEmpty()) {
                Text.of(Fonts.MEDIUM, text, x + w * 0.5f, y + 20.0f, 5.6f).centered().color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
            }
            return y + 40.0f;
        }
        float gap = 5.0f;
        float colW = (w - gap) * 0.5f;
        float cardH = 46.0f;
        for (int i = 0; i < this.items.size(); ++i) {
            float ix = x + (float)(i % 2) * (colW + gap);
            float iy = y + (float)(i / 2) * (cardH + gap);
            float t = GuiDraw.easeOut((float)(now - this.tab.pageSwitchTime - (long)Math.min(i, 12) * 18L) / 300.0f);
            if (!(iy + cardH >= top - 4.0f) || !(iy <= bottom + 4.0f)) continue;
            this.renderCard(graphics, this.items.get(i), ix, iy + (1.0f - t) * 5.0f, colW, cardH, a * t, top, bottom);
        }
        y += (float)((this.items.size() + 1) / 2) * (cardH + gap);
        if (this.total > this.items.size()) {
            ui.button(graphics, "market-more", (String)(this.loading ? "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430..." : "\u041f\u043e\u043a\u0430\u0437\u0430\u0442\u044c \u0435\u0449\u0451 (" + (this.total - this.items.size()) + ")"), x, y, w, 14.0f, a, GuiDraw.getAccent(), false, top, bottom, this.loading ? null : this::loadMore);
            y += 18.0f;
        }
        return y;
    }

    private void renderCard(GuiGraphicsExtractor graphics, MarketCard card, float x, float y, float w, float h, float a, float top, float bottom) {
        MarketWidgets ui = this.tab.ui;
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        String key = "market-card-" + card.id();
        float hv = this.anim(key, this.hov(key, hover), 7.0f);
        RoundedRect.of(x, y, w, h).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round((6.0f + 5.0f * hv) * a))).render(graphics);
        float priceW = card.free() ? Text.of(Fonts.MEDIUM, "\u0411\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u043e").size(5.0f).width() : 30.0f;
        Text.of(Fonts.MEDIUM, GuiDraw.fit(card.name(), 6.0f, w - priceW - 18.0f), x + 6.0f, y + 7.5f, 6.0f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.TEXT.withAlpha(a)).render(graphics);
        ui.price(graphics, card, x + w - 6.0f, y + 7.5f, 5.0f, a);
        Text.of(Fonts.MEDIUM, GuiDraw.fit("\u043e\u0442 " + card.author() + "  v" + card.version(), 4.4f, w - 12.0f), x + 6.0f, y + 15.0f, 4.4f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
        Text.of(Fonts.MEDIUM, GuiDraw.fit(card.description(), 4.8f, w - 12.0f), x + 6.0f, y + 23.5f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.SOFT.withAlpha(a)).render(graphics);
        float rowY = y + h - 8.0f;
        float rx = x + 6.0f + ui.rating(graphics, x + 6.0f, rowY, card.rating(), -1, a) + 4.0f;
        Text.of(Fonts.MEDIUM, card.downloads() + " \u0437\u0430\u0433\u0440.", rx, rowY, 4.4f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
        float bx = x + w - 6.0f;
        ArrayList<String[]> badges = new ArrayList<String[]>();
        if (MarketInstaller.hasUpdate(card.id(), card.version())) {
            badges.add(new String[]{"\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c", "u"});
        } else if (MarketInstaller.installed(card.id()) != null) {
            badges.add(new String[]{"\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d", "i"});
        }
        if (card.mine()) {
            badges.add(new String[]{"\u0412\u0430\u0448", "m"});
        } else if (card.owned() || MarketState.owns(card.id())) {
            badges.add(new String[]{"\u041a\u0443\u043f\u043b\u0435\u043d\u043e", "o"});
        }
        for (String[] b : badges) {
            Color c = switch (b[1]) {
                case "u" -> MarketWidgets.GOLD;
                case "m" -> GuiDraw.getAccent();
                case "i" -> MarketWidgets.SOFT;
                default -> MarketWidgets.GREEN;
            };
            float bw = Text.of(Fonts.MEDIUM, b[0]).size(4.2f).width() + 6.0f;
            ui.badge(graphics, b[0], bx -= bw, rowY, c, a);
            bx -= 3.0f;
        }
        if (GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(x, y, w, h, () -> this.tab.openScript(card.id()), null);
        }
    }

    public void replace(MarketCard updated) {
        for (int i = 0; i < this.items.size(); ++i) {
            if (!this.items.get(i).id().equals(updated.id())) continue;
            this.items.set(i, updated);
        }
    }
}

