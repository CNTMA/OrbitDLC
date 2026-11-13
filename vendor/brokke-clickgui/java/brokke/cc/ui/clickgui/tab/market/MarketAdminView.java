/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.tab.market;

import brokke.cc.system.market.MarketClient;
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
import brokke.cc.util.sound.ClientSound;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class MarketAdminView
extends GuiComponent {
    private final MarketTab tab;
    private JsonObject stats;
    private final List<JsonObject> users = new ArrayList<JsonObject>();
    private int usersTotal;
    private final List<JsonObject> removed = new ArrayList<JsonObject>();
    private JsonObject selected;
    private final List<JsonObject> selectedScripts = new ArrayList<JsonObject>();
    private final List<JsonObject> selectedTx = new ArrayList<JsonObject>();
    private String selectedNick;
    private boolean loading;
    private boolean busy;
    private long banArmedAt;
    private long grantArmedAt;
    private String grantArmedKey;

    public MarketAdminView(ClickGuiContext ctx, MarketTab tab) {
        super(ctx);
        this.tab = tab;
    }

    public void load() {
        this.loading = true;
        MarketClient.admin("stats", new JsonObject(), r -> {
            if (r.ok()) {
                this.stats = (JsonObject)r.value();
            } else {
                this.tab.toast(r.message(), false);
            }
        });
        MarketClient.admin("removed", new JsonObject(), r -> {
            if (!r.ok()) {
                return;
            }
            this.removed.clear();
            this.removed.addAll(MarketAdminView.objects((JsonObject)r.value(), "items"));
        });
        this.searchUsers();
        if (this.selectedNick != null) {
            this.selectUser(this.selectedNick);
        }
    }

    private void searchUsers() {
        this.ctx.focus.setAbField(null, "");
        JsonObject body = new JsonObject();
        body.addProperty("query", MarketTab.field("market:admin-q").trim());
        body.addProperty("size", (Number)50);
        this.loading = true;
        MarketClient.admin("users", body, r -> {
            this.loading = false;
            if (!r.ok()) {
                this.tab.toast(r.message(), false);
                return;
            }
            this.users.clear();
            this.users.addAll(MarketAdminView.objects((JsonObject)r.value(), "users"));
            this.usersTotal = MarketAdminView.num((JsonObject)r.value(), "total");
        });
    }

    private void selectUser(String target) {
        this.selectedNick = target;
        MarketClient.adminUser(target, r -> {
            if (!r.ok()) {
                this.tab.toast(r.message(), false);
                return;
            }
            this.selected = ((JsonObject)r.value()).getAsJsonObject("user");
            this.selectedScripts.clear();
            this.selectedScripts.addAll(MarketAdminView.objects((JsonObject)r.value(), "scripts"));
            this.selectedTx.clear();
            this.selectedTx.addAll(MarketAdminView.objects((JsonObject)r.value(), "transactions"));
        });
    }

    private String selectedTarget() {
        return this.selected == null ? null : MarketAdminView.str(this.selected, "id");
    }

    private void grant(int sign) {
        int amount;
        if (this.selected == null) {
            return;
        }
        try {
            amount = Integer.parseInt(MarketTab.field("market:admin-amount").trim());
        }
        catch (NumberFormatException e) {
            amount = 0;
        }
        if (amount <= 0) {
            this.tab.toast(MarketClient.message("bad_amount"), false);
            return;
        }
        int value = amount * sign;
        String key = this.selectedTarget() + "|" + value;
        long now = System.currentTimeMillis();
        if (!key.equals(this.grantArmedKey) || now - this.grantArmedAt > 3000L) {
            this.grantArmedKey = key;
            this.grantArmedAt = now;
            return;
        }
        this.grantArmedKey = null;
        this.busy = true;
        String nick = MarketAdminView.str(this.selected, "nick");
        MarketClient.adminGrant(this.selectedTarget(), value, MarketTab.field("market:admin-note").trim(), r -> {
            this.busy = false;
            if (!r.ok()) {
                this.tab.toast(r.message(), false);
                return;
            }
            this.tab.toast((value > 0 ? "\u041d\u0430\u0447\u0438\u0441\u043b\u0435\u043d\u043e " : "\u0421\u043f\u0438\u0441\u0430\u043d\u043e ") + MarketState.format(Math.abs(value)) + " BC: " + nick, true);
            MarketTab.setField("market:admin-amount", "");
            if (nick.equalsIgnoreCase(MarketState.nick())) {
                MarketState.refresh(null);
            }
            this.load();
        });
    }

    private void toggleBan() {
        if (this.selected == null) {
            return;
        }
        boolean banned = this.selected.get("banned").getAsBoolean();
        String nick = MarketAdminView.str(this.selected, "nick");
        if (!banned) {
            long now = System.currentTimeMillis();
            if (now - this.banArmedAt > 3000L) {
                this.banArmedAt = now;
                return;
            }
            this.banArmedAt = 0L;
        }
        this.busy = true;
        if (banned) {
            MarketClient.adminUnban(this.selectedTarget(), r -> this.afterAction(r.ok(), r.message(), nick + " \u0440\u0430\u0437\u0431\u043b\u043e\u043a\u0438\u0440\u043e\u0432\u0430\u043d"));
        } else {
            MarketClient.adminBan(this.selectedTarget(), MarketTab.field("market:admin-note").trim(), true, r -> this.afterAction(r.ok(), r.message(), nick + " \u0437\u0430\u0431\u043b\u043e\u043a\u0438\u0440\u043e\u0432\u0430\u043d, \u0441\u043a\u0440\u0438\u043f\u0442\u044b \u0441\u043d\u044f\u0442\u044b"));
        }
    }

    private void afterAction(boolean ok, String error, String success) {
        this.busy = false;
        this.tab.toast(ok ? success : error, ok);
        if (ok) {
            this.load();
            this.tab.catalog.reload();
        }
    }

    private void removeScript(String id, String name) {
        this.busy = true;
        MarketClient.adminRemove(id, "admin", r -> this.afterAction(r.ok(), r.message(), "\u0421\u043a\u0440\u0438\u043f\u0442 \u00ab" + name + "\u00bb \u0441\u043d\u044f\u0442"));
    }

    private void restoreScript(String id, String name) {
        this.busy = true;
        MarketClient.adminRestore(id, r -> this.afterAction(r.ok(), r.message(), "\u0421\u043a\u0440\u0438\u043f\u0442 \u00ab" + name + "\u00bb \u0432\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d"));
    }

    public float render(GuiGraphicsExtractor graphics, float x, float y, float w, float a, float top, float bottom, long now) {
        MarketWidgets ui = this.tab.ui;
        if (!MarketState.admin()) {
            Text.of(Fonts.MEDIUM, "\u0414\u043e\u0441\u0442\u0443\u043f \u0442\u043e\u043b\u044c\u043a\u043e \u0434\u043b\u044f \u0430\u0434\u043c\u0438\u043d\u043e\u0432 \u043c\u0430\u0440\u043a\u0435\u0442\u0430", x + w * 0.5f, y + 20.0f, 5.6f).centered().color(MarketWidgets.RED.withAlpha(a)).render(graphics);
            return y + 40.0f;
        }
        y = this.renderStats(graphics, x, y, w, a, top, bottom);
        y = GuiDraw.sectionTitle(graphics, "\u041f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u0435\u043b\u0438" + (String)(this.usersTotal > 0 ? " (" + this.usersTotal + ")" : ""), x, y, a);
        ui.field(graphics, "market:admin-q", "\u041d\u0438\u043a \u0438\u043b\u0438 \u043d\u0430\u0447\u0430\u043b\u043e ID", x, y, w - 64.0f, 14.0f, a, top, bottom, now);
        ui.button(graphics, "market-admin-search", this.loading ? "..." : "\u041d\u0430\u0439\u0442\u0438", x + w - 60.0f, y, 60.0f, 14.0f, a, GuiDraw.getAccent(), true, top, bottom, this.loading ? null : this::searchUsers);
        y += 19.0f;
        if (this.selected != null) {
            y = this.renderSelected(graphics, x, y, w, a, top, bottom, now);
        }
        float rowH = 15.0f;
        for (JsonObject u : this.users) {
            boolean active;
            String nick = MarketAdminView.str(u, "nick");
            String id = MarketAdminView.str(u, "id");
            boolean bl = active = this.selected != null && id.equals(this.selectedTarget());
            if (y + rowH >= top - 4.0f && y <= bottom + 4.0f) {
                boolean hover = GuiDraw.canHit(y, rowH, top, bottom) && this.hovered(x, y, w, rowH);
                String key = "market-admin-user-" + id;
                float hv = this.anim(key, this.hov(key, hover), 8.0f);
                RoundedRect.of(x, y, w, rowH).radius(4.0f).color(active ? GuiDraw.getAccent().withAlpha(0.18f * a) : Color.rgba(255, 255, 255, Math.round((5.0f + 6.0f * hv) * a))).render(graphics);
                Text.of(Fonts.MEDIUM, GuiDraw.fit(nick, 5.2f, w * 0.35f), x + 6.0f, y + rowH * 0.5f, 5.2f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.TEXT.withAlpha(a)).render(graphics);
                float bx = x + 10.0f + Math.min(w * 0.35f, Text.of(Fonts.MEDIUM, nick).size(5.2f).width());
                if (u.get("admin").getAsBoolean()) {
                    bx += ui.badge(graphics, "\u0410\u0434\u043c\u0438\u043d", bx, y + rowH * 0.5f, MarketWidgets.RED, a) + 3.0f;
                }
                if (u.get("banned").getAsBoolean()) {
                    ui.badge(graphics, "\u0411\u0430\u043d", bx, y + rowH * 0.5f, MarketWidgets.RED, a);
                }
                Text.of(Fonts.MEDIUM, "\u0441\u043a\u0440\u0438\u043f\u0442\u043e\u0432 " + MarketAdminView.num(u, "scripts") + "   \u0431\u044b\u043b " + MarketWidgets.date(u.get("lastSeen").getAsLong()), x + w * 0.5f, y + rowH * 0.5f, 4.4f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
                ui.amount(graphics, MarketAdminView.num(u, "balance"), x + w - 6.0f, y + rowH * 0.5f, 4.8f, a, MarketWidgets.GOLD, true);
                if (GuiDraw.canHit(y, rowH, top, bottom)) {
                    this.hit(x, y, w, rowH, () -> {
                        this.selectUser(id);
                        ClientSound.ui(ClientSound.Ui.CLICK);
                    }, null);
                }
            }
            y += rowH + 2.0f;
        }
        if (this.users.isEmpty()) {
            Text.of(Fonts.MEDIUM, this.loading ? "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430..." : "\u041d\u0438\u043a\u043e\u0433\u043e \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e", x + 2.0f, y + 5.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
            y += 14.0f;
        }
        y += 6.0f;
        y = GuiDraw.sectionTitle(graphics, "\u0421\u043d\u044f\u0442\u044b\u0435 \u0441\u043a\u0440\u0438\u043f\u0442\u044b (" + this.removed.size() + ")", x, y, a);
        if (this.removed.isEmpty()) {
            Text.of(Fonts.MEDIUM, "\u041f\u0443\u0441\u0442\u043e", x + 2.0f, y + 5.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
            return y + 14.0f;
        }
        for (JsonObject s : this.removed) {
            y = this.scriptRow(graphics, s, true, x, y, w, a, top, bottom);
        }
        return y;
    }

    private float renderStats(GuiGraphicsExtractor graphics, float x, float y, float w, float a, float top, float bottom) {
        MarketWidgets ui = this.tab.ui;
        float boxH = 40.0f;
        RoundedRect.of(x, y, w, boxH).radius(6.0f).horizontalGradient(MarketWidgets.RED.withAlpha(0.12f * a), GuiDraw.getAccent().withAlpha(0.06f * a)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u0410\u0434\u043c\u0438\u043d-\u043f\u0430\u043d\u0435\u043b\u044c \u043c\u0430\u0440\u043a\u0435\u0442\u0430", x + 8.0f, y + 9.0f, 6.4f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.TEXT.withAlpha(a)).render(graphics);
        ui.button(graphics, "market-admin-refresh", "\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c", x + w - 56.0f, y + 4.0f, 50.0f, 12.0f, a, GuiDraw.getAccent(), false, top, bottom, this::load);
        if (this.stats != null) {
            String[][] cells = new String[][]{{"\u041f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u0435\u043b\u0438", String.valueOf(MarketAdminView.num(this.stats, "users"))}, {"\u0421\u0435\u0433\u043e\u0434\u043d\u044f", String.valueOf(MarketAdminView.num(this.stats, "activeToday"))}, {"\u0421\u043a\u0440\u0438\u043f\u0442\u044b", MarketAdminView.num(this.stats, "scripts") + " / " + MarketAdminView.num(this.stats, "removed")}, {"\u041f\u0440\u043e\u0434\u0430\u0436\u0438", String.valueOf(MarketAdminView.num(this.stats, "sales"))}, {"\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0438", String.valueOf(MarketAdminView.num(this.stats, "downloads"))}, {"\u0411\u0430\u043d\u044b", String.valueOf(MarketAdminView.num(this.stats, "banned"))}};
            float cw = (w - 16.0f) / (float)(cells.length + 1);
            for (int i = 0; i < cells.length; ++i) {
                float cx = x + 8.0f + (float)i * cw;
                Text.of(Fonts.MEDIUM, cells[i][1], cx, y + 23.0f, 6.0f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.TEXT.withAlpha(a)).render(graphics);
                Text.of(Fonts.MEDIUM, cells[i][0], cx, y + 32.0f, 4.0f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
            }
            float cx = x + 8.0f + (float)cells.length * cw;
            ui.amount(graphics, MarketAdminView.num(this.stats, "coins"), cx, y + 23.0f, 5.4f, a, MarketWidgets.GOLD, false);
            Text.of(Fonts.MEDIUM, "\u0432 \u043e\u0431\u043e\u0440\u043e\u0442\u0435", cx, y + 32.0f, 4.0f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
        }
        return y + boxH + 6.0f;
    }

    private float renderSelected(GuiGraphicsExtractor graphics, float x, float y, float w, float a, float top, float bottom, long now) {
        MarketWidgets ui = this.tab.ui;
        String nick = MarketAdminView.str(this.selected, "nick");
        boolean banned = this.selected.get("banned").getAsBoolean();
        float boxTop = y;
        float pad = 6.0f;
        float inner = w - pad * 2.0f;
        float cy = y + pad;
        float headH = 50.0f;
        RoundedRect.of(x, y, w, headH).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round(8.0f * a))).render(graphics);
        Text.of(Fonts.MEDIUM, nick, x + pad, cy + 3.0f, 6.6f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.TEXT.withAlpha(a)).render(graphics);
        Text.of(Fonts.MEDIUM, "ID " + MarketAdminView.str(this.selected, "id").substring(0, 12) + "...   \u043a\u0443\u043f\u043b\u0435\u043d\u043e " + MarketAdminView.num(this.selected, "owned") + "   \u0441 " + MarketWidgets.date(this.selected.get("createdAt").getAsLong()) + (String)(banned && !MarketAdminView.str(this.selected, "banReason").isEmpty() ? "   \u043f\u0440\u0438\u0447\u0438\u043d\u0430: " + MarketAdminView.str(this.selected, "banReason") : ""), x + pad, cy + 11.0f, 4.2f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
        ui.amount(graphics, MarketAdminView.num(this.selected, "balance"), x + w - pad, cy + 4.0f, 6.0f, a, MarketWidgets.GOLD, true);
        float amountW = inner * 0.2f;
        float noteW = inner * 0.3f;
        float bw = (inner - amountW - noteW - 12.0f) / 3.0f;
        ui.field(graphics, "market:admin-amount", "\u0421\u0443\u043c\u043c\u0430", x + pad, cy += 18.0f, amountW, 14.0f, a, top, bottom, now);
        ui.field(graphics, "market:admin-note", "\u0417\u0430\u043c\u0435\u0442\u043a\u0430 / \u043f\u0440\u0438\u0447\u0438\u043d\u0430", x + pad + amountW + 4.0f, cy, noteW, 14.0f, a, top, bottom, now);
        float bx = x + pad + amountW + noteW + 8.0f;
        String amountText = MarketTab.field("market:admin-amount");
        boolean plusArmed = this.grantArmedKey != null && this.grantArmedKey.endsWith("|" + amountText) && now - this.grantArmedAt <= 3000L;
        boolean minusArmed = this.grantArmedKey != null && this.grantArmedKey.endsWith("|-" + amountText) && now - this.grantArmedAt <= 3000L;
        ui.button(graphics, "market-admin-plus", (String)(plusArmed ? "\u0422\u043e\u0447\u043d\u043e +" + amountText + "?" : "\u041d\u0430\u0447\u0438\u0441\u043b\u0438\u0442\u044c"), bx, cy, bw, 14.0f, a, MarketWidgets.GREEN, plusArmed, top, bottom, this.busy ? null : () -> this.grant(1));
        ui.button(graphics, "market-admin-minus", (String)(minusArmed ? "\u0422\u043e\u0447\u043d\u043e -" + amountText + "?" : "\u0421\u043f\u0438\u0441\u0430\u0442\u044c"), bx + bw + 2.0f, cy, bw, 14.0f, a, MarketWidgets.GOLD, minusArmed, top, bottom, this.busy ? null : () -> this.grant(-1));
        boolean banArmed = now - this.banArmedAt <= 3000L;
        boolean isAdminUser = this.selected.get("admin").getAsBoolean();
        ui.button(graphics, "market-admin-banuser", banned ? "\u0420\u0430\u0437\u0431\u0430\u043d\u0438\u0442\u044c" : (banArmed ? "\u0422\u043e\u0447\u043d\u043e \u0431\u0430\u043d?" : "\u0417\u0430\u0431\u0430\u043d\u0438\u0442\u044c"), bx + (bw + 2.0f) * 2.0f, cy, bw, 14.0f, a, banned ? MarketWidgets.GREEN : MarketWidgets.RED, banArmed, top, bottom, this.busy || isAdminUser ? null : this::toggleBan);
        y += headH + 4.0f;
        if (!this.selectedScripts.isEmpty()) {
            y = ui.label(graphics, "\u0421\u043a\u0440\u0438\u043f\u0442\u044b " + nick, x, y, a);
            Iterator<JsonObject> iterator = this.selectedScripts.iterator();
            while (iterator.hasNext()) {
                JsonObject s;
                s = iterator.next();
                y = this.scriptRow(graphics, s, s.has("hidden") && s.get("hidden").getAsBoolean(), x, y, w, a, top, bottom);
            }
            y += 3.0f;
        }
        if (!this.selectedTx.isEmpty()) {
            y = ui.label(graphics, "\u041f\u043e\u0441\u043b\u0435\u0434\u043d\u0438\u0435 \u043e\u043f\u0435\u0440\u0430\u0446\u0438\u0438", x, y, a);
            int shown = 0;
            for (JsonObject t : this.selectedTx) {
                if (shown++ >= 8) break;
                int amount = MarketAdminView.num(t, "amount");
                Text.of(Fonts.MEDIUM, GuiDraw.fit(MarketWidgets.date(t.get("at").getAsLong()) + "   " + MarketAdminView.str(t, "type") + "   " + MarketAdminView.str(t, "ref"), 4.4f, w - 70.0f), x + 4.0f, y + 4.0f, 4.4f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.SOFT.withAlpha(a)).render(graphics);
                Text.of(Fonts.MEDIUM, (amount > 0 ? "+" : "") + MarketState.format(amount), x + w - 4.0f, y + 4.0f, 4.6f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color((amount >= 0 ? MarketWidgets.GREEN : MarketWidgets.RED).withAlpha(a)).render(graphics);
                y += 8.0f;
            }
            y += 4.0f;
        }
        RoundedRect.of(x, y, w, 1.0f).color(Color.rgba(255, 255, 255, Math.round(14.0f * a))).render(graphics);
        return Math.max(y, boxTop) + 6.0f;
    }

    private float scriptRow(GuiGraphicsExtractor graphics, JsonObject s, boolean hidden, float x, float y, float w, float a, float top, float bottom) {
        MarketWidgets ui = this.tab.ui;
        float rowH = 15.0f;
        String id = MarketAdminView.str(s, "id");
        String name = MarketAdminView.str(s, "name");
        if (y + rowH >= top - 4.0f && y <= bottom + 4.0f) {
            RoundedRect.of(x, y, w, rowH).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round(5.0f * a))).render(graphics);
            String info = name + "   " + MarketAdminView.str(s, "author") + "   v" + MarketAdminView.str(s, "version") + "   " + MarketAdminView.num(s, "sales") + " \u043f\u0440\u043e\u0434\u0430\u0436" + (String)(s.has("removeReason") && !MarketAdminView.str(s, "removeReason").isEmpty() ? "   (" + MarketAdminView.str(s, "removeReason") + ")" : "");
            Text.of(Fonts.MEDIUM, GuiDraw.fit(info, 4.8f, w - 130.0f), x + 6.0f, y + rowH * 0.5f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color((hidden ? MarketWidgets.MUTED : MarketWidgets.TEXT).withAlpha(a)).render(graphics);
            if (!hidden) {
                ui.button(graphics, "market-admin-open-" + id, "\u041e\u0442\u043a\u0440\u044b\u0442\u044c", x + w - 118.0f, y + 2.0f, 54.0f, rowH - 4.0f, a, GuiDraw.getAccent(), false, top, bottom, () -> this.tab.openScript(id));
            }
            ui.button(graphics, "market-admin-sr-" + id, hidden ? "\u0412\u0435\u0440\u043d\u0443\u0442\u044c" : "\u0421\u043d\u044f\u0442\u044c", x + w - 60.0f, y + 2.0f, 54.0f, rowH - 4.0f, a, hidden ? MarketWidgets.GREEN : MarketWidgets.RED, false, top, bottom, this.busy ? null : (hidden ? () -> this.restoreScript(id, name) : () -> this.removeScript(id, name)));
        }
        return y + rowH + 2.0f;
    }

    private static List<JsonObject> objects(JsonObject o, String key) {
        ArrayList<JsonObject> out = new ArrayList<JsonObject>();
        if (o == null || !o.has(key) || !o.get(key).isJsonArray()) {
            return out;
        }
        JsonArray arr = o.getAsJsonArray(key);
        for (JsonElement e : arr) {
            if (!e.isJsonObject()) continue;
            out.add(e.getAsJsonObject());
        }
        return out;
    }

    private static String str(JsonObject o, String key) {
        return o != null && o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private static int num(JsonObject o, String key) {
        try {
            return o != null && o.has(key) && !o.get(key).isJsonNull() ? (int)Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, o.get(key).getAsLong())) : 0;
        }
        catch (Exception e) {
            return 0;
        }
    }
}

