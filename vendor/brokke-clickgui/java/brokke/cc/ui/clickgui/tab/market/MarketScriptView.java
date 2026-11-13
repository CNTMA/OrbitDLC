/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.tab.market;

import brokke.cc.system.market.MarketCard;
import brokke.cc.system.market.MarketClient;
import brokke.cc.system.market.MarketDetails;
import brokke.cc.system.market.MarketDownload;
import brokke.cc.system.market.MarketInstaller;
import brokke.cc.system.market.MarketPurchase;
import brokke.cc.system.market.MarketScript;
import brokke.cc.system.market.MarketState;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.ui.clickgui.tab.market.MarketTab;
import brokke.cc.ui.clickgui.tab.market.MarketWidgets;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.TextAlign;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import com.google.gson.JsonObject;
import java.io.File;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class MarketScriptView
extends GuiComponent {
    private static final String WARNING = "\u0421\u043a\u0440\u0438\u043f\u0442\u044b \u043f\u043e\u043b\u0443\u0447\u0430\u044e\u0442 \u043f\u043e\u043b\u043d\u044b\u0439 \u0434\u043e\u0441\u0442\u0443\u043f \u043a \u0438\u0433\u0440\u0435 \u0438 \u0430\u043a\u043a\u0430\u0443\u043d\u0442\u0443. \u041f\u0435\u0440\u0435\u0434 \u0432\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435\u043c \u043e\u0431\u044f\u0437\u0430\u0442\u0435\u043b\u044c\u043d\u043e \u043f\u0440\u043e\u0441\u043c\u043e\u0442\u0440\u0438\u0442\u0435 \u043a\u043e\u0434. \u0423\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u043d\u044b\u0435 \u0438\u0437 \u043c\u0430\u0440\u043a\u0435\u0442\u0430 \u0441\u043a\u0440\u0438\u043f\u0442\u044b \u0437\u0430\u0433\u0440\u0443\u0436\u0430\u044e\u0442\u0441\u044f \u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d\u043d\u044b\u043c\u0438.";
    private final MarketTab tab;
    private String id;
    private MarketDetails details;
    private boolean loading;
    private String error;
    private long buyArmedAt;
    private boolean busy;
    private boolean codeOpen;
    private boolean codeLoading;
    private String code;
    private int reviewStars;
    private long removeArmedAt;
    private long banArmedAt;

    public MarketScriptView(ClickGuiContext ctx, MarketTab tab) {
        super(ctx);
        this.tab = tab;
    }

    public void open(String scriptId) {
        if (!scriptId.equals(this.id)) {
            this.details = null;
            this.code = null;
            this.codeOpen = false;
            this.reviewStars = 0;
            MarketTab.setField("market:review", "");
        }
        this.id = scriptId;
        this.buyArmedAt = 0L;
        this.load();
    }

    private void load() {
        String requested = this.id;
        this.loading = true;
        this.error = null;
        MarketClient.script(requested, r -> {
            if (!requested.equals(this.id)) {
                return;
            }
            this.loading = false;
            if (!r.ok()) {
                this.error = r.message();
                return;
            }
            this.details = (MarketDetails)r.value();
            MarketCard card = this.details.script().card();
            MarketState.noteVersion(card.id(), card.version());
            MarketInstaller.rememberAuthor(card.id(), card.author());
            this.tab.catalog.replace(card);
            if (this.details.myReview() != null && this.reviewStars == 0) {
                this.reviewStars = this.details.myReview().stars();
                MarketTab.setField("market:review", this.details.myReview().text());
            }
        });
    }

    private void buy(MarketCard card) {
        long now = System.currentTimeMillis();
        if (now - this.buyArmedAt > 3000L) {
            this.buyArmedAt = now;
            return;
        }
        this.buyArmedAt = 0L;
        this.busy = true;
        MarketClient.buy(card.id(), r -> {
            this.busy = false;
            if (!r.ok()) {
                this.tab.toast(r.message(), false);
                return;
            }
            MarketState.applyPurchase((MarketPurchase)r.value());
            this.tab.toast("\u0421\u043a\u0440\u0438\u043f\u0442 \u00ab" + card.name() + "\u00bb \u043a\u0443\u043f\u043b\u0435\u043d", true);
            this.load();
        });
    }

    private void install(MarketCard card) {
        this.busy = true;
        MarketInstaller.rememberAuthor(card.id(), card.author());
        MarketInstaller.install(card.id(), r -> {
            this.busy = false;
            if (!r.ok()) {
                this.tab.toast(r.message(), false);
                return;
            }
            this.tab.toast("\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d " + ((File)r.value()).getName() + " (\u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d, \u0432\u043a\u043b\u044e\u0447\u0438\u0442\u0435 \u0432\u043e \u0432\u043a\u043b\u0430\u0434\u043a\u0435 Scripts)", true);
            this.load();
        });
    }

    private void toggleCode(String scriptId) {
        boolean bl = this.codeOpen = !this.codeOpen;
        if (!this.codeOpen || this.code != null || this.codeLoading) {
            return;
        }
        this.codeLoading = true;
        MarketClient.download(scriptId, r -> {
            this.codeLoading = false;
            if (!scriptId.equals(this.id)) {
                return;
            }
            if (!r.ok()) {
                this.codeOpen = false;
                this.tab.toast(r.message(), false);
                return;
            }
            String expected = ((MarketDownload)r.value()).sha256();
            if (expected == null || !expected.equalsIgnoreCase(MarketInstaller.sha256(((MarketDownload)r.value()).code()))) {
                this.codeOpen = false;
                this.tab.toast(MarketClient.message("sha_mismatch"), false);
                return;
            }
            this.code = ((MarketDownload)r.value()).code();
        });
    }

    private void sendReview(MarketCard card) {
        if (this.reviewStars < 1) {
            this.tab.toast("\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u043e\u0446\u0435\u043d\u043a\u0443 \u043e\u0442 1 \u0434\u043e 5", false);
            return;
        }
        this.ctx.focus.setAbField(null, "");
        this.busy = true;
        MarketClient.review(card.id(), this.reviewStars, MarketTab.field("market:review").trim(), r -> {
            this.busy = false;
            if (!r.ok()) {
                this.tab.toast(r.message(), false);
                return;
            }
            this.tab.toast("\u041e\u0442\u0437\u044b\u0432 \u0441\u043e\u0445\u0440\u0430\u043d\u0451\u043d", true);
            this.load();
        });
    }

    public float render(GuiGraphicsExtractor graphics, float x, float y, float w, float a, float top, float bottom, long now) {
        MarketWidgets ui = this.tab.ui;
        this.ctx.widgets.renderEventPill(graphics, "market-back", "\u041d\u0430\u0437\u0430\u0434", "<", x, y, 44.0f, 13.0f, a, false, top, bottom, () -> this.tab.openPage(0));
        y += 18.0f;
        if (this.details == null) {
            String text = this.error != null ? this.error : "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430...";
            Text.of(Fonts.MEDIUM, GuiDraw.fit(text, 5.6f, w), x + w * 0.5f, y + 20.0f, 5.6f).centered().color((this.error != null ? MarketWidgets.RED : MarketWidgets.MUTED).withAlpha(a)).render(graphics);
            if (this.error != null && !this.loading) {
                ui.button(graphics, "market-script-retry", "\u041f\u043e\u0432\u0442\u043e\u0440\u0438\u0442\u044c", x + w * 0.5f - 30.0f, y + 30.0f, 60.0f, 13.0f, a, GuiDraw.getAccent(), false, top, bottom, this::load);
            }
            return y + 50.0f;
        }
        MarketScript script = this.details.script();
        MarketCard card = script.card();
        boolean owned = card.owned() || MarketState.owns(card.id());
        boolean access = card.free() || owned || card.mine();
        MarketInstaller.Installed local = MarketInstaller.installed(card.id());
        float headH = 40.0f;
        RoundedRect.of(x, y, w, headH).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round(6.0f * a))).render(graphics);
        Text.of(Fonts.MEDIUM, GuiDraw.fit(card.name(), 7.4f, w - 90.0f), x + 7.0f, y + 9.0f, 7.4f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.TEXT.withAlpha(a)).render(graphics);
        ui.price(graphics, card, x + w - 7.0f, y + 9.0f, 6.0f, a);
        Text.of(Fonts.MEDIUM, GuiDraw.fit("\u0410\u0432\u0442\u043e\u0440: " + card.author() + "   \u0412\u0435\u0440\u0441\u0438\u044f " + card.version() + "   \u041e\u0431\u043d\u043e\u0432\u043b\u0451\u043d " + MarketWidgets.date(card.updatedAt()), 4.6f, w - 14.0f), x + 7.0f, y + 19.0f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
        float sx = x + 7.0f + ui.rating(graphics, x + 7.0f, y + 29.0f, card.rating(), card.ratings(), a) + 6.0f;
        String stats = card.downloads() + " \u0437\u0430\u0433\u0440\u0443\u0437\u043e\u043a   " + card.sales() + " \u043f\u0440\u043e\u0434\u0430\u0436   " + Math.max(1, script.size() / 1024) + " \u041a\u0411";
        Text.of(Fonts.MEDIUM, stats, sx, y + 29.0f, 4.4f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
        float bx = x + w - 7.0f;
        if (card.mine()) {
            bx -= ui.badge(graphics, "\u0412\u0430\u0448", bx - Text.of(Fonts.MEDIUM, "\u0412\u0430\u0448").size(4.2f).width() - 6.0f, y + 29.0f, GuiDraw.getAccent(), a) + 3.0f;
        } else if (owned) {
            bx -= ui.badge(graphics, "\u041a\u0443\u043f\u043b\u0435\u043d\u043e", bx - Text.of(Fonts.MEDIUM, "\u041a\u0443\u043f\u043b\u0435\u043d\u043e").size(4.2f).width() - 6.0f, y + 29.0f, MarketWidgets.GREEN, a) + 3.0f;
        }
        if (local != null) {
            String label = "\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d v" + local.version();
            ui.badge(graphics, label, bx - Text.of(Fonts.MEDIUM, label).size(4.2f).width() - 6.0f, y + 29.0f, MarketWidgets.SOFT, a);
        }
        y += headH + 5.0f;
        float bw = (w - 8.0f) / 3.0f;
        float bh = 15.0f;
        if (!access) {
            boolean armed;
            boolean bl = armed = now - this.buyArmedAt <= 3000L;
            String label = this.busy ? "\u041f\u043e\u043a\u0443\u043f\u043a\u0430..." : (armed ? "\u041f\u043e\u0434\u0442\u0432\u0435\u0440\u0434\u0438\u0442\u044c: " + MarketState.format(card.price()) + " BC" : "\u041a\u0443\u043f\u0438\u0442\u044c \u0437\u0430 " + MarketState.format(card.price()) + " BC");
            ui.button(graphics, "market-buy", label, x, y, bw, bh, a, armed ? MarketWidgets.GOLD : GuiDraw.getAccent(), true, top, bottom, this.busy ? null : () -> this.buy(card));
        } else {
            boolean update;
            boolean bl = update = local != null && MarketInstaller.hasUpdate(card.id(), card.version());
            String label = this.busy ? "\u041f\u043e\u0434\u043e\u0436\u0434\u0438\u0442\u0435..." : (local == null ? "\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c" : (update ? "\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c \u0434\u043e v" + card.version() : "\u041f\u0435\u0440\u0435\u0443\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c"));
            ui.button(graphics, "market-install", label, x, y, bw, bh, a, update ? MarketWidgets.GOLD : GuiDraw.getAccent(), true, top, bottom, this.busy ? null : () -> this.install(card));
        }
        if (access) {
            String codeLabel = this.codeLoading ? "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430 \u043a\u043e\u0434\u0430..." : (this.codeOpen ? "\u0421\u043a\u0440\u044b\u0442\u044c \u043a\u043e\u0434" : "\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043a\u043e\u0434\u0430");
            ui.button(graphics, "market-code", codeLabel, x + bw + 4.0f, y, bw, bh, a, GuiDraw.getAccent(), false, top, bottom, () -> this.toggleCode(card.id()));
        }
        if (card.mine()) {
            ui.button(graphics, "market-edit", "\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u0442\u044c", x + (bw + 4.0f) * 2.0f, y, bw, bh, a, GuiDraw.getAccent(), false, top, bottom, () -> {
                this.tab.publish.edit(card, script);
                this.tab.openPage(2);
            });
        } else {
            ui.button(graphics, "market-refresh", "\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c \u0434\u0430\u043d\u043d\u044b\u0435", x + (bw + 4.0f) * 2.0f, y, bw, bh, a, GuiDraw.getAccent(), false, top, bottom, this.loading ? null : this::load);
        }
        y += bh + 5.0f;
        if (MarketState.admin()) {
            y = this.renderAdmin(graphics, card, x, y, w, a, top, bottom, now);
        }
        float warnH = 6.0f + (float)ui.wrap(WARNING, 4.6f, w - 14.0f).size() * 7.2f;
        RoundedRect.of(x, y, w, warnH).radius(4.0f).color(MarketWidgets.GOLD.withAlpha(0.1f * a)).render(graphics);
        ui.paragraph(graphics, WARNING, x + 7.0f, y + 3.0f, w - 14.0f, 4.6f, MarketWidgets.GOLD, a, top, bottom);
        y += warnH + 6.0f;
        if (this.codeOpen && this.code != null) {
            y = ui.label(graphics, "\u041a\u043e\u0434 (\u0442\u043e\u043b\u044c\u043a\u043e \u0447\u0442\u0435\u043d\u0438\u0435)", x, y, a);
            y = this.tab.code.render(graphics, this.code, x, y, w, a, top, bottom) + 6.0f;
        }
        if (!card.tags().isEmpty()) {
            float tx = x;
            for (String t : card.tags()) {
                tx += ui.badge(graphics, "#" + t, tx, y + 4.0f, GuiDraw.getAccent(), a) + 3.0f;
            }
            y += 11.0f;
        }
        y = ui.label(graphics, "\u041e\u043f\u0438\u0441\u0430\u043d\u0438\u0435", x, y, a);
        String description = script.longDescription().isBlank() ? card.description() : script.longDescription();
        y = ui.paragraph(graphics, description.isBlank() ? "\u041e\u043f\u0438\u0441\u0430\u043d\u0438\u0435 \u043e\u0442\u0441\u0443\u0442\u0441\u0442\u0432\u0443\u0435\u0442" : description, x + 1.0f, y, w - 2.0f, 5.0f, MarketWidgets.SOFT, a, top, bottom) + 6.0f;
        if (!script.versions().isEmpty()) {
            y = ui.label(graphics, "\u0412\u0435\u0440\u0441\u0438\u0438", x, y, a);
            int shown = 0;
            for (int i = script.versions().size() - 1; i >= 0 && shown < 8; --i, ++shown) {
                MarketScript.Version v = script.versions().get(i);
                Text.of(Fonts.MEDIUM, "v" + v.version(), x + 2.0f, y + 4.0f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.TEXT.withAlpha(a)).render(graphics);
                Text.of(Fonts.MEDIUM, MarketWidgets.date(v.at()), x + w - 2.0f, y + 4.0f, 4.4f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
                y += 8.0f;
                if (!v.changelog().isBlank()) {
                    y = ui.paragraph(graphics, v.changelog(), x + 8.0f, y, w - 10.0f, 4.6f, MarketWidgets.SOFT, a, top, bottom);
                }
                y += 3.0f;
            }
            y += 3.0f;
        }
        y = this.renderReviews(graphics, card, owned, x, y, w, a, top, bottom, now);
        return y;
    }

    private float renderAdmin(GuiGraphicsExtractor graphics, MarketCard card, float x, float y, float w, float a, float top, float bottom, long now) {
        MarketWidgets ui = this.tab.ui;
        float bw = (w - 8.0f) / 3.0f;
        float bh = 13.0f;
        boolean removeArmed = now - this.removeArmedAt <= 3000L;
        ui.button(graphics, "market-admin-remove", removeArmed ? "\u0422\u043e\u0447\u043d\u043e \u0441\u043d\u044f\u0442\u044c?" : "\u0421\u043d\u044f\u0442\u044c (\u0430\u0434\u043c\u0438\u043d)", x, y, bw, bh, a, MarketWidgets.RED, removeArmed, top, bottom, this.busy ? null : () -> this.adminRemove(card));
        boolean banArmed = now - this.banArmedAt <= 3000L;
        ui.button(graphics, "market-admin-ban", banArmed ? "\u0422\u043e\u0447\u043d\u043e \u0437\u0430\u0431\u0430\u043d\u0438\u0442\u044c?" : "\u0411\u0430\u043d \u0430\u0432\u0442\u043e\u0440\u0430", x + bw + 4.0f, y, bw, bh, a, MarketWidgets.RED, banArmed, top, bottom, this.busy || card.mine() ? null : () -> this.adminBan(card));
        ui.button(graphics, "market-admin-user", "\u041f\u0440\u043e\u0444\u0438\u043b\u044c \u0430\u0432\u0442\u043e\u0440\u0430", x + (bw + 4.0f) * 2.0f, y, bw, bh, a, GuiDraw.getAccent(), false, top, bottom, () -> MarketClient.adminUser(card.author(), r -> {
            if (!r.ok()) {
                this.tab.toast(r.message(), false);
                return;
            }
            JsonObject u = ((JsonObject)r.value()).getAsJsonObject("user");
            this.tab.toast(card.author() + ": " + MarketState.format(u.get("balance").getAsInt()) + " BC, \u0441\u043a\u0440\u0438\u043f\u0442\u043e\u0432 " + u.get("scripts").getAsInt() + (u.get("banned").getAsBoolean() ? ", \u0437\u0430\u0431\u0430\u043d\u0435\u043d" : ""), true);
        }));
        return y + bh + 5.0f;
    }

    private void adminRemove(MarketCard card) {
        long now = System.currentTimeMillis();
        if (now - this.removeArmedAt > 3000L) {
            this.removeArmedAt = now;
            return;
        }
        this.removeArmedAt = 0L;
        this.busy = true;
        MarketClient.adminRemove(card.id(), "admin", r -> {
            this.busy = false;
            if (!r.ok()) {
                this.tab.toast(r.message(), false);
                return;
            }
            this.tab.toast("\u0421\u043a\u0440\u0438\u043f\u0442 \u00ab" + card.name() + "\u00bb \u0441\u043d\u044f\u0442 \u0441 \u043c\u0430\u0440\u043a\u0435\u0442\u0430", true);
            this.tab.catalog.reload();
            this.tab.openPage(0);
        });
    }

    private void adminBan(MarketCard card) {
        long now = System.currentTimeMillis();
        if (now - this.banArmedAt > 3000L) {
            this.banArmedAt = now;
            return;
        }
        this.banArmedAt = 0L;
        this.busy = true;
        MarketClient.adminBan(card.author(), "admin", true, r -> {
            this.busy = false;
            if (!r.ok()) {
                this.tab.toast(r.message(), false);
                return;
            }
            this.tab.toast(card.author() + " \u0437\u0430\u0431\u043b\u043e\u043a\u0438\u0440\u043e\u0432\u0430\u043d, \u0435\u0433\u043e \u0441\u043a\u0440\u0438\u043f\u0442\u044b \u0441\u043d\u044f\u0442\u044b", true);
            this.tab.catalog.reload();
            this.tab.openPage(0);
        });
    }

    private void adminDeleteReview(MarketCard card, String nick) {
        MarketClient.adminDeleteReview(card.id(), nick, r -> {
            if (!r.ok()) {
                this.tab.toast(r.message(), false);
                return;
            }
            this.tab.toast("\u041e\u0442\u0437\u044b\u0432 " + nick + " \u0443\u0434\u0430\u043b\u0451\u043d", true);
            this.load();
        });
    }

    private float renderReviews(GuiGraphicsExtractor graphics, MarketCard card, boolean owned, float x, float y, float w, float a, float top, float bottom, long now) {
        boolean canReview;
        MarketWidgets ui = this.tab.ui;
        y = ui.label(graphics, "\u041e\u0442\u0437\u044b\u0432\u044b (" + this.details.reviews().size() + ")", x, y, a);
        boolean bl = canReview = !card.mine() && (owned || card.free() && MarketInstaller.installed(card.id()) != null || this.details.myReview() != null);
        if (canReview) {
            float boxH = 40.0f;
            RoundedRect.of(x, y, w, boxH).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round(6.0f * a))).render(graphics);
            Text.of(Fonts.MEDIUM, this.details.myReview() != null ? "\u0412\u0430\u0448 \u043e\u0442\u0437\u044b\u0432" : "\u041e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u043e\u0442\u0437\u044b\u0432", x + 6.0f, y + 7.0f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.TEXT.withAlpha(a)).render(graphics);
            ui.starsInput(graphics, "market-stars-", x + 60.0f, y + 7.0f, this.reviewStars, a, top, bottom, v -> {
                this.reviewStars = v;
            });
            ui.field(graphics, "market:review", "\u0422\u0435\u043a\u0441\u0442 \u043e\u0442\u0437\u044b\u0432\u0430 (\u043d\u0435\u043e\u0431\u044f\u0437\u0430\u0442\u0435\u043b\u044c\u043d\u043e)", x + 6.0f, y + 16.0f, w - 70.0f, 15.0f, a, top, bottom, now);
            ui.button(graphics, "market-review-send", "\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c", x + w - 60.0f, y + 16.0f, 54.0f, 15.0f, a, GuiDraw.getAccent(), true, top, bottom, this.busy ? null : () -> this.sendReview(card));
            y += boxH + 5.0f;
        } else if (!card.mine() && card.free()) {
            Text.of(Fonts.MEDIUM, "\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u0435 \u0441\u043a\u0440\u0438\u043f\u0442, \u0447\u0442\u043e\u0431\u044b \u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u043e\u0442\u0437\u044b\u0432", x + 2.0f, y + 4.0f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
            y += 10.0f;
        }
        if (this.details.reviews().isEmpty()) {
            Text.of(Fonts.MEDIUM, "\u041e\u0442\u0437\u044b\u0432\u043e\u0432 \u043f\u043e\u043a\u0430 \u043d\u0435\u0442", x + 2.0f, y + 4.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
            return y + 12.0f;
        }
        for (MarketDetails.Review review : this.details.reviews()) {
            float textH = review.text().isBlank() ? 0.0f : (float)ui.wrap(review.text(), 4.8f, w - 14.0f).size() * 7.4f;
            float h = 14.0f + textH;
            if (y + h >= top - 10.0f && y <= bottom + 10.0f) {
                RoundedRect.of(x, y, w, h).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round(5.0f * a))).render(graphics);
                Text.of(Fonts.MEDIUM, review.nick(), x + 6.0f, y + 6.5f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.TEXT.withAlpha(a)).render(graphics);
                ui.rating(graphics, x + 10.0f + Text.of(Fonts.MEDIUM, review.nick()).size(5.0f).width(), y + 6.5f, review.stars(), -1, a);
                Text.of(Fonts.MEDIUM, MarketWidgets.date(review.at()), x + w - 6.0f, y + 6.5f, 4.4f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
                if (textH > 0.0f) {
                    ui.paragraph(graphics, review.text(), x + 6.0f, y + 11.0f, w - 14.0f, 4.8f, MarketWidgets.SOFT, a, top, bottom);
                }
                if (MarketState.admin()) {
                    String nick = review.nick();
                    ui.button(graphics, "market-admin-review-" + nick, "\u0423\u0434\u0430\u043b\u0438\u0442\u044c", x + w - 70.0f, y + 2.0f, 28.0f, 9.0f, a, MarketWidgets.RED, false, top, bottom, () -> this.adminDeleteReview(card, nick));
                }
            }
            y += h + 3.0f;
        }
        return y;
    }
}

