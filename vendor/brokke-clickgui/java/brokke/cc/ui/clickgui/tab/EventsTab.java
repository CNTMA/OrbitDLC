/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.tab;

import brokke.cc.core.command.RCTCommand;
import brokke.cc.ui.clickgui.events.EventsPanel;
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
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class EventsTab
extends GuiComponent {
    public static String eventSearch = "";
    public static final EventsPanel EVENTS = new EventsPanel();

    public EventsTab(ClickGuiContext ctx) {
        super(ctx);
    }

    public float renderEvents(GuiGraphicsExtractor graphics, float x, float y, float w, float prog, float top, float bottom, long now) {
        EVENTS.update(true);
        float startY = y;
        float appear = GuiDraw.easeOut((float)(now - this.ctx.switchTime) / 380.0f);
        float a = prog * appear;
        Color accent = GuiDraw.getAccent();
        float barH = 14.0f;
        String sortText = "\u0421\u043e\u0440\u0442\u0438\u0440\u043e\u0432\u043a\u0430: " + EVENTS.getSortMode().getLabel();
        float sortW = Text.of(Fonts.MEDIUM, sortText).size(5.0f).width() + 18.0f;
        this.ctx.widgets.renderEventPill(graphics, "sort", sortText, "J", x, y += (1.0f - appear) * 8.0f, sortW, barH, a, false, top, bottom, () -> {
            EVENTS.cycleSortMode();
            ClientSound.ui(ClientSound.Ui.MODE);
        });
        boolean fetching = EVENTS.isFetching();
        String refText = fetching ? "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430..." : "\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c";
        float refW = Text.of(Fonts.MEDIUM, refText).size(5.0f).width() + 18.0f;
        this.ctx.widgets.renderEventPill(graphics, "refresh", refText, "T", x + w - refW, y, refW, barH, a, true, top, bottom, fetching ? null : () -> {
            EVENTS.triggerFetch();
            ClientSound.ui(ClientSound.Ui.CLICK);
        });
        this.ctx.widgets.renderAbField(graphics, "events:search", eventSearch.isEmpty() ? "\u041f\u043e\u0438\u0441\u043a \u0441\u043e\u0431\u044b\u0442\u0438\u0439 \u043f\u043e \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u044e..." : eventSearch, "M", x, y += barH + 7.0f, w, barH, a, accent, top, bottom, now, eventSearch);
        y += barH + 7.0f;
        float colGap = 5.0f;
        float colW = (w - colGap) * 0.5f;
        long elapsed = EVENTS.getFetchTimestamp() == 0L ? 0L : (System.currentTimeMillis() - EVENTS.getFetchTimestamp()) / 1000L;
        String query = eventSearch.trim().toLowerCase(Locale.ROOT);
        List<EventsPanel.EventCard> ftCards = EVENTS.getFtCards().stream().filter(card -> query.isEmpty() || card.displayName().toLowerCase(Locale.ROOT).contains(query)).toList();
        List<EventsPanel.EventCard> hwCards = EVENTS.getHwCards().stream().filter(card -> query.isEmpty() || card.displayName().toLowerCase(Locale.ROOT).contains(query)).toList();
        float ftH = this.renderEventColumn(graphics, "FunTime", ftCards, query.isEmpty() ? EVENTS.getFtStatus() : "\u041f\u043e\u0438\u0441\u043a \u043f\u043e \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u044e", true, x, y, colW, a, accent, elapsed, top, bottom, now);
        float hwH = this.renderEventColumn(graphics, "HolyWorld", hwCards, query.isEmpty() ? EVENTS.getHwStatus() : "\u041f\u043e\u0438\u0441\u043a \u043f\u043e \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u044e", false, x + colW + colGap, y, colW, a, accent, elapsed, top, bottom, now);
        return (y += Math.max(ftH, hwH)) - startY;
    }

    private float renderEventColumn(GuiGraphicsExtractor graphics, String title, List<EventsPanel.EventCard> cards, String status, boolean funtime, float x, float y, float w, float a, Color accent, long elapsed, float top, float bottom, long now) {
        float startY = y;
        Text.of(Fonts.MEDIUM, title, x + 1.0f, y + 6.0f, 6.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 238, 245).withAlpha(a)).render(graphics);
        String count = String.valueOf(cards.size());
        float bw = Text.of(Fonts.MEDIUM, count).size(4.6f).width() + 7.0f;
        RoundedRect.of(x + w - bw, y + 1.5f, bw, 9.0f).radius(3.0f).horizontalGradient(accent.withAlpha(0.35f * a), GuiDraw.getAccent2().withAlpha(0.35f * a)).render(graphics);
        Text.of(Fonts.MEDIUM, count, x + w - bw * 0.5f, y + 6.0f, 4.6f).centered().color(Color.rgb(240, 242, 248).withAlpha(a)).render(graphics);
        y += 15.0f;
        if (cards.isEmpty()) {
            RoundedRect.of(x, y, w, 34.0f).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round(5.0f * a))).render(graphics);
            Text.of(Fonts.MEDIUM, status == null ? "\u041d\u0435\u0442 \u0438\u0432\u0435\u043d\u0442\u043e\u0432" : status, x + w * 0.5f, y + 17.0f, 5.0f).centered().color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
            return y + 34.0f - startY;
        }
        float cardH = 25.0f;
        float gap = 3.0f;
        for (int i = 0; i < cards.size(); ++i) {
            EventsPanel.EventCard card = cards.get(i);
            float t = GuiDraw.easeOut((float)(now - this.ctx.switchTime - (long)i * 22L) / 320.0f);
            float cy = y + (1.0f - t) * 6.0f;
            if (cy + cardH >= top && cy <= bottom) {
                this.renderEventCard(graphics, card, funtime, x, cy, w, cardH, a * t, accent, elapsed, top, bottom);
            }
            y += cardH + gap;
        }
        return y - gap - startY;
    }

    private void renderEventCard(GuiGraphicsExtractor graphics, EventsPanel.EventCard card, boolean funtime, float x, float y, float w, float h, float a, Color accent, long elapsed, float top, float bottom) {
        Color rightColor;
        String right;
        Object info;
        String key = "event-" + (funtime ? "ft-" : "hw-") + card.serverNum() + "-" + card.eventId() + "-" + card.server();
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        float hv = this.anim(key, this.hov(key, hover), 7.0f);
        RoundedRect.of(x, y, w, h).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round((6.0f + 4.0f * hv) * a))).render(graphics);
        float pad = 6.0f;
        Object cmd = funtime ? "/an" + card.serverNum() : (card.serverNum() > 0 ? ".rct " + card.serverNum() : ".rct");
        float bw = Text.of(Fonts.MEDIUM, (String)cmd).size(4.6f).width() + 9.0f;
        float bh = 10.0f;
        float bx = x + w - bw - pad + 1.0f;
        float by = y + 3.5f;
        boolean bHover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(bx, by, bw, bh);
        float bhv = this.anim(String.valueOf(key) + "-btn", bHover, 8.0f);
        RoundedRect.of(bx, by, bw, bh).radius(3.0f).color(Color.rgba(255, 255, 255, Math.round(12.0f * (1.0f - bhv) * a))).render(graphics);
        if (bhv > 0.01f) {
            RoundedRect.of(bx, by, bw, bh).radius(3.0f).horizontalGradient(accent.withAlpha(0.8f * bhv * a), GuiDraw.getAccent2().withAlpha(0.8f * bhv * a)).render(graphics);
        }
        Text.of(Fonts.MEDIUM, (String)cmd, bx + bw * 0.5f, by + bh * 0.5f, 4.6f).centered().color(GuiDraw.mix(Color.rgb(205, 209, 218), Color.rgb(255, 255, 255), bhv).withAlpha(a)).render(graphics);
        Text.of(Fonts.MEDIUM, GuiDraw.fit(card.displayName(), 5.6f, w - bw - pad * 2.0f - 2.0f), x + pad, y + 8.5f, 5.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 238, 245).withAlpha(a)).render(graphics);
        if (funtime) {
            info = "\u0410\u043d\u0430\u0440\u0445\u0438\u044f " + card.serverNum() + " - " + card.version();
            right = card.statusStr(elapsed);
            rightColor = accent;
        } else {
            info = card.server();
            if (card.phase() != null && card.phase().startsWith("HOLYWORLD_ACTIVE:")) {
                right = EventsPanel.translateRarity(card.phase().substring("HOLYWORLD_ACTIVE:".length()));
                rightColor = EventsPanel.getRarityColor(right);
            } else {
                right = "\u0410\u043a\u0442\u0438\u0432\u0435\u043d";
                rightColor = Color.rgb(90, 210, 140);
            }
        }
        float rw = Text.of(Fonts.MEDIUM, right).size(4.5f).width();
        Text.of(Fonts.MEDIUM, GuiDraw.fit((String)info, 4.5f, w - rw - pad * 2.0f - 4.0f), x + pad, y + h - 7.0f, 4.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(120, 125, 136).withAlpha(a)).render(graphics);
        Text.of(Fonts.MEDIUM, right, x + w - pad, y + h - 7.0f, 4.5f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(rightColor.withAlpha(a)).render(graphics);
        if (GuiDraw.canHit(by, bh, top, bottom)) {
            this.hit(bx, by, bw, bh, () -> {
                ClientSound.ui(ClientSound.Ui.CLICK);
                if (funtime) {
                    if (this.ctx.minecraft() != null && this.ctx.minecraft().player != null && this.ctx.minecraft().player.connection != null) {
                        this.ctx.minecraft().player.connection.sendCommand("an" + card.serverNum());
                    }
                } else if (card.serverNum() > 0) {
                    RCTCommand.execute(card.serverNum());
                } else {
                    RCTCommand.executeCurrentOrLast();
                }
                this.ctx.screen.onClose();
            }, null);
        }
    }
}

