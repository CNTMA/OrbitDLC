/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.tab.market;

import brokke.cc.system.market.MarketClient;
import brokke.cc.system.market.MarketState;
import brokke.cc.system.market.MarketWallet;
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
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class MarketWalletView
extends GuiComponent {
    private final MarketTab tab;
    private MarketWallet wallet;
    private boolean loading;
    private String error;
    private boolean sending;
    private long transferArmedAt;
    private String armedKey;

    public MarketWalletView(ClickGuiContext ctx, MarketTab tab) {
        super(ctx);
        this.tab = tab;
    }

    public void load() {
        if (this.loading) {
            return;
        }
        this.loading = true;
        this.error = null;
        MarketClient.wallet(r -> {
            this.loading = false;
            if (!r.ok()) {
                this.error = r.message();
                return;
            }
            this.wallet = (MarketWallet)r.value();
            MarketState.setBalance(this.wallet.balance());
        });
    }

    private void transfer() {
        int amount;
        this.ctx.focus.setAbField(null, "");
        String target = MarketTab.field("market:target").trim();
        String note = MarketTab.field("market:note").trim();
        try {
            amount = Integer.parseInt(MarketTab.field("market:amount").trim());
        }
        catch (NumberFormatException e) {
            amount = 0;
        }
        if (target.length() < 3) {
            this.tab.toast(MarketClient.message("bad_target"), false);
            return;
        }
        if (amount <= 0) {
            this.tab.toast(MarketClient.message("bad_amount"), false);
            return;
        }
        if (MarketState.hasBalance() && amount > MarketState.balance()) {
            this.tab.toast(MarketClient.message("insufficient_funds"), false);
            return;
        }
        String key = target + "|" + amount + "|" + note;
        long now = System.currentTimeMillis();
        if (!key.equals(this.armedKey) || now - this.transferArmedAt > 3000L) {
            this.armedKey = key;
            this.transferArmedAt = now;
            return;
        }
        this.armedKey = null;
        this.sending = true;
        int sent = amount;
        MarketClient.transfer(target, amount, note, r -> {
            this.sending = false;
            if (!r.ok()) {
                this.tab.toast(r.message(), false);
                return;
            }
            MarketState.setBalance((Integer)r.value());
            this.tab.toast("\u041e\u0442\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u043e " + MarketState.format(sent) + " BC \u0438\u0433\u0440\u043e\u043a\u0443 " + target, true);
            MarketTab.setField("market:amount", "");
            MarketTab.setField("market:note", "");
            this.load();
        });
    }

    private static String typeName(String type) {
        return switch (type) {
            case "buy" -> "\u041f\u043e\u043a\u0443\u043f\u043a\u0430";
            case "sale" -> "\u041f\u0440\u043e\u0434\u0430\u0436\u0430";
            case "fee" -> "\u041a\u043e\u043c\u0438\u0441\u0441\u0438\u044f";
            case "transfer_in" -> "\u041f\u0435\u0440\u0435\u0432\u043e\u0434 \u043e\u0442 \u0438\u0433\u0440\u043e\u043a\u0430";
            case "transfer_out" -> "\u041f\u0435\u0440\u0435\u0432\u043e\u0434 \u0438\u0433\u0440\u043e\u043a\u0443";
            case "grant" -> "\u041d\u0430\u0447\u0438\u0441\u043b\u0435\u043d\u0438\u0435";
            case "bonus" -> "\u0411\u043e\u043d\u0443\u0441";
            default -> type;
        };
    }

    public float render(GuiGraphicsExtractor graphics, float x, float y, float w, float a, float top, float bottom, long now) {
        boolean armed;
        MarketWidgets ui = this.tab.ui;
        float boxH = 46.0f;
        RoundedRect.of(x, y, w, boxH).radius(6.0f).horizontalGradient(MarketWidgets.GOLD.withAlpha(0.14f * a), GuiDraw.getAccent().withAlpha(0.06f * a)).render(graphics);
        ui.coin(graphics, x + 8.0f, y + 7.0f, 32.0f, a);
        Text.of(Fonts.MEDIUM, "\u0411\u0440\u043e\u043a\u043a\u043e\u0438\u043d\u044b", x + 46.0f, y + 13.0f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.SOFT.withAlpha(a)).render(graphics);
        String balance = MarketState.hasBalance() ? MarketState.format(MarketState.balance()) : (this.loading ? "..." : "-");
        Text.of(Fonts.MEDIUM, balance + " BC", x + 46.0f, y + 28.0f, 11.0f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.GOLD.withAlpha(a)).render(graphics);
        ui.button(graphics, "market-wallet-refresh", this.loading ? "..." : "\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c", x + w - 56.0f, y + 6.0f, 50.0f, 13.0f, a, GuiDraw.getAccent(), false, top, bottom, this.loading ? null : this::load);
        if (!MarketState.nick().isEmpty()) {
            Text.of(Fonts.MEDIUM, MarketState.nick(), x + w - 8.0f, y + 36.0f, 4.6f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
        }
        y += boxH + 6.0f;
        y = GuiDraw.sectionTitle(graphics, "\u041f\u0435\u0440\u0435\u0432\u043e\u0434", x, y, a);
        float nickW = (w - 8.0f) * 0.3f;
        float amountW = (w - 8.0f) * 0.2f;
        float noteW = w - 8.0f - nickW - amountW;
        ui.field(graphics, "market:target", "\u041d\u0438\u043a \u043f\u043e\u043b\u0443\u0447\u0430\u0442\u0435\u043b\u044f", x, y, nickW, 14.0f, a, top, bottom, now);
        ui.field(graphics, "market:amount", "\u0421\u0443\u043c\u043c\u0430", x + nickW + 4.0f, y, amountW, 14.0f, a, top, bottom, now);
        ui.field(graphics, "market:note", "\u041a\u043e\u043c\u043c\u0435\u043d\u0442\u0430\u0440\u0438\u0439", x + nickW + amountW + 8.0f, y, noteW, 14.0f, a, top, bottom, now);
        boolean bl = armed = this.armedKey != null && now - this.transferArmedAt <= 3000L;
        String label = this.sending ? "\u041e\u0442\u043f\u0440\u0430\u0432\u043a\u0430..." : (armed ? "\u041f\u043e\u0434\u0442\u0432\u0435\u0440\u0434\u0438\u0442\u044c \u043f\u0435\u0440\u0435\u0432\u043e\u0434 " + MarketTab.field("market:amount") + " BC -> " + MarketTab.field("market:target") : "\u041f\u0435\u0440\u0435\u0432\u0435\u0441\u0442\u0438");
        ui.button(graphics, "market-transfer", label, x, y += 18.0f, w, 15.0f, a, armed ? MarketWidgets.GOLD : GuiDraw.getAccent(), true, top, bottom, this.sending ? null : this::transfer);
        y += 22.0f;
        y = GuiDraw.sectionTitle(graphics, "\u0418\u0441\u0442\u043e\u0440\u0438\u044f \u043e\u043f\u0435\u0440\u0430\u0446\u0438\u0439", x, y, a);
        if (this.error != null) {
            Text.of(Fonts.MEDIUM, this.error, x + 2.0f, y + 5.0f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.RED.withAlpha(a)).render(graphics);
            y += 12.0f;
        }
        if (this.wallet == null || this.wallet.transactions().isEmpty()) {
            Text.of(Fonts.MEDIUM, this.loading ? "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430..." : "\u041e\u043f\u0435\u0440\u0430\u0446\u0438\u0439 \u043f\u043e\u043a\u0430 \u043d\u0435\u0442", x + 2.0f, y + 5.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
            return y + 14.0f;
        }
        float rowH = 18.0f;
        for (MarketWallet.Transaction t : this.wallet.transactions()) {
            if (y + rowH >= top - 4.0f && y <= bottom + 4.0f) {
                RoundedRect.of(x, y, w, rowH).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round(5.0f * a))).render(graphics);
                Text.of(Fonts.MEDIUM, MarketWalletView.typeName(t.type()), x + 6.0f, y + 6.0f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.TEXT.withAlpha(a)).render(graphics);
                Text.of(Fonts.MEDIUM, GuiDraw.fit(MarketWidgets.date(t.at()) + (String)(t.ref().isEmpty() ? "" : "   " + t.ref()), 4.2f, w - 110.0f), x + 6.0f, y + 13.0f, 4.2f).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
                String sum = (t.amount() > 0 ? "+" : "") + MarketState.format(t.amount());
                Text.of(Fonts.MEDIUM, sum, x + w - 6.0f, y + 6.0f, 5.4f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color((t.amount() >= 0 ? MarketWidgets.GREEN : MarketWidgets.RED).withAlpha(a)).render(graphics);
                Text.of(Fonts.MEDIUM, "\u0431\u0430\u043b\u0430\u043d\u0441 " + MarketState.format(t.balance()), x + w - 6.0f, y + 13.0f, 4.2f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
            }
            y += rowH + 3.0f;
        }
        return y;
    }
}

