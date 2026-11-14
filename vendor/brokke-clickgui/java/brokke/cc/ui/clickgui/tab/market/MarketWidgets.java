/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.resources.Identifier
 */
package brokke.cc.ui.clickgui.tab.market;

import brokke.cc.system.market.MarketCard;
import brokke.cc.system.market.MarketState;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.ui.clickgui.tab.market.MarketTab;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.image.Image;
import brokke.cc.util.render.display.outline.Outline;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.scissor.Scissor;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.TextAlign;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class MarketWidgets
extends GuiComponent {
    public static final Identifier COIN = Identifier.fromNamespaceAndPath((String)"brokke", (String)"textures/market/brokkecoin.png");
    public static final Identifier COIN_SMALL = Identifier.fromNamespaceAndPath((String)"brokke", (String)"textures/market/brokkecoin_32.png");
    public static final Color GOLD = Color.rgb(245, 196, 70);
    public static final Color GREEN = Color.rgb(90, 210, 140);
    public static final Color RED = Color.rgb(235, 110, 110);
    public static final Color MUTED = Color.rgb(120, 125, 136);
    public static final Color TEXT = Color.rgb(235, 238, 245);
    public static final Color SOFT = Color.rgb(170, 175, 186);
    private final Map<String, List<String>> wrapCache = new HashMap<String, List<String>>();

    public MarketWidgets(ClickGuiContext ctx) {
        super(ctx);
    }

    public void coin(GuiGraphicsExtractor graphics, float x, float y, float size, float a) {
        Image.of(size <= 16.0f ? COIN_SMALL : COIN, x, y, size, size).alpha(a).render(graphics);
    }

    public float amount(GuiGraphicsExtractor graphics, int value, float x, float cy, float size, float a, Color color, boolean alignRight) {
        String text = MarketState.format(value);
        float coin = size * 1.35f;
        float tw = Text.of(Fonts.MEDIUM, text).size(size).width();
        float total = coin + 2.0f + tw;
        float sx = alignRight ? x - total : x;
        this.coin(graphics, sx, cy - coin * 0.5f, coin, a);
        Text.of(Fonts.MEDIUM, text, sx + coin + 2.0f, cy, size).verticalAlign(VerticalAlign.MIDDLE).color(color.withAlpha(a)).render(graphics);
        return total;
    }

    public float price(GuiGraphicsExtractor graphics, MarketCard card, float rightX, float cy, float size, float a) {
        if (card.free()) {
            float tw = Text.of(Fonts.MEDIUM, "\u0411\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u043e").size(size).width();
            Text.of(Fonts.MEDIUM, "\u0411\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u043e", rightX, cy, size).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(GREEN.withAlpha(a)).render(graphics);
            return tw;
        }
        return this.amount(graphics, card.price(), rightX, cy, size, a, GOLD, true);
    }

    public float rating(GuiGraphicsExtractor graphics, float x, float cy, float rating, int count, float a) {
        float dot = 3.2f;
        float gap = 1.4f;
        for (int i = 0; i < 5; ++i) {
            float fill = Math.clamp(rating - (float)i, 0.0f, 1.0f);
            float dx = x + (float)i * (dot + gap);
            RoundedRect.of(dx, cy - dot * 0.5f, dot, dot).radius(dot * 0.5f).color(Color.rgba(255, 255, 255, Math.round(22.0f * a))).render(graphics);
            if (!(fill > 0.05f)) continue;
            RoundedRect.of(dx, cy - dot * 0.5f, dot, dot).radius(dot * 0.5f).color(GOLD.withAlpha(a * (0.35f + 0.65f * fill))).render(graphics);
        }
        float end = x + 5.0f * (dot + gap);
        if (count >= 0) {
            String text = count == 0 ? "\u043d\u0435\u0442 \u043e\u0446\u0435\u043d\u043e\u043a" : String.format(Locale.ROOT, "%.1f (%d)", Float.valueOf(rating), count);
            Text.of(Fonts.MEDIUM, text, end + 1.5f, cy, 4.4f).verticalAlign(VerticalAlign.MIDDLE).color(MUTED.withAlpha(a)).render(graphics);
            end += 1.5f + Text.of(Fonts.MEDIUM, text).size(4.4f).width();
        }
        return end - x;
    }

    public float starsInput(GuiGraphicsExtractor graphics, String key, float x, float cy, int value, float a, float top, float bottom, IntConsumer pick) {
        float s = 7.0f;
        float gap = 2.0f;
        for (int i = 1; i <= 5; ++i) {
            int stars = i;
            float dx = x + (float)(i - 1) * (s + gap);
            boolean hover = GuiDraw.canHit(cy - s * 0.5f, s, top, bottom) && this.hovered(dx, cy - s * 0.5f, s, s);
            float hv = this.anim(key + i, this.hov(key + i, hover), 9.0f);
            Color c = i <= value ? GOLD : GuiDraw.mix(Color.rgb(70, 74, 84), GOLD, hv * 0.5f);
            RoundedRect.of(dx, cy - s * 0.5f, s, s).radius(s * 0.5f).color(c.withAlpha(a)).render(graphics);
            if (!GuiDraw.canHit(cy - s * 0.5f, s, top, bottom)) continue;
            this.hit(dx - 1.0f, cy - s * 0.5f - 1.0f, s + 2.0f, s + 2.0f, () -> {
                pick.accept(stars);
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, null);
        }
        return 5.0f * (s + gap);
    }

    public float badge(GuiGraphicsExtractor graphics, String text, float x, float cy, Color color, float a) {
        float tw = Text.of(Fonts.MEDIUM, text).size(4.2f).width();
        float w = tw + 6.0f;
        float h = 7.0f;
        RoundedRect.of(x, cy - h * 0.5f, w, h).radius(2.5f).color(color.withAlpha(0.2f * a)).render(graphics);
        Text.of(Fonts.MEDIUM, text, x + w * 0.5f, cy, 4.2f).centered().color(color.withAlpha(a)).render(graphics);
        return w;
    }

    public void button(GuiGraphicsExtractor graphics, Object key, String label, float x, float y, float w, float h, float a, Color tint, boolean primary, float top, float bottom, Runnable action) {
        boolean enabled = action != null;
        boolean hover = enabled && GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        float hv = this.anim(key, this.hov(key, hover), 8.0f);
        float base = primary ? 0.55f : 0.0f;
        RoundedRect.of(x, y, w, h).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round(9.0f * a))).render(graphics);
        float fill = Math.max(base, hv * (primary ? 0.85f : 0.3f));
        if (fill > 0.01f) {
            RoundedRect.of(x, y, w, h).radius(4.0f).color(tint.withAlpha(fill * a * (enabled ? 1.0f : 0.4f))).render(graphics);
        }
        Text.of(Fonts.MEDIUM, GuiDraw.fit(label, 5.0f, w - 6.0f), x + w * 0.5f, y + h * 0.5f, 5.0f).centered().color(GuiDraw.mix(Color.rgb(215, 219, 228), Color.rgb(255, 255, 255), hv).withAlpha(a * (enabled ? 1.0f : 0.5f))).render(graphics);
        if (enabled && GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(x, y, w, h, () -> {
                ClientSound.ui(ClientSound.Ui.CLICK);
                action.run();
            }, null);
        }
    }

    public float chip(GuiGraphicsExtractor graphics, Object key, String label, float x, float y, float h, float a, boolean selected, float top, float bottom, Runnable action) {
        float w = Text.of(Fonts.MEDIUM, label).size(4.8f).width() + 10.0f;
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        float hv = this.anim(key, this.hov(key, hover), 8.0f);
        float sel = this.anim(String.valueOf(key) + "-sel", selected, 8.0f);
        RoundedRect.of(x, y, w, h).radius(h * 0.5f).color(Color.rgba(255, 255, 255, Math.round((8.0f + 6.0f * hv) * a))).render(graphics);
        if (sel > 0.01f) {
            RoundedRect.of(x, y, w, h).radius(h * 0.5f).color(GuiDraw.getAccent().withAlpha(0.3f * sel * a)).render(graphics);
            Outline.of(x, y, w, h).radius(h * 0.5f).thickness(0.6f).color(GuiDraw.getAccent().withAlpha(0.7f * sel * a)).render(graphics);
        }
        Text.of(Fonts.MEDIUM, label, x + w * 0.5f, y + h * 0.5f, 4.8f).centered().color(GuiDraw.mix(SOFT, TEXT, Math.max(sel, hv * 0.6f)).withAlpha(a)).render(graphics);
        if (GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(x, y, w, h, () -> {
                ClientSound.ui(ClientSound.Ui.CLICK);
                action.run();
            }, null);
        }
        return w;
    }

    public void field(GuiGraphicsExtractor graphics, String id, String placeholder, float x, float y, float w, float h, float a, float top, float bottom, long now) {
        boolean focused = id.equals(this.ctx.focus.abField);
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        String key = "market-field-" + id;
        float hv = this.anim(key, this.hov(key, hover), 7.0f);
        float fc = this.anim(String.valueOf(key) + "-focus", focused, 6.0f);
        RoundedRect.of(x, y, w, h).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round((8.0f + 4.0f * hv + 6.0f * fc) * a))).render(graphics);
        if (fc > 0.01f) {
            Outline.of(x, y, w, h).radius(4.0f).thickness(0.7f).color(GuiDraw.getAccent().withAlpha(0.6f * fc * a)).render(graphics);
        }
        String value = MarketTab.field(id);
        String text = focused ? this.ctx.focus.abInput.toString() : value;
        float size = 5.0f;
        float tx = x + 5.0f;
        float maxW = w - 10.0f;
        float cy = y + h * 0.5f;
        if (text.isEmpty()) {
            Text.of(Fonts.MEDIUM, GuiDraw.fit(placeholder, size, maxW), tx, cy, size).verticalAlign(VerticalAlign.MIDDLE).color(MUTED.withAlpha(a)).render(graphics);
        } else {
            String shown = text;
            if (focused) {
                while (shown.length() > 1 && Text.of(Fonts.MEDIUM, shown).size(size).width() > maxW) {
                    shown = shown.substring(1);
                }
            } else {
                shown = GuiDraw.fit(text, size, maxW);
            }
            Scissor.push(graphics, tx - 1.0f, y, maxW + 2.0f, h);
            Text.of(Fonts.MEDIUM, shown, tx, cy, size).verticalAlign(VerticalAlign.MIDDLE).color(TEXT.withAlpha(a)).render(graphics);
            Scissor.pop(graphics);
            text = shown;
        }
        if (focused) {
            float blink = 0.5f + 0.5f * (float)Math.cos((double)now / 170.0);
            float caretX = tx + (text.isEmpty() ? 0.0f : Math.min(maxW, Text.of(Fonts.MEDIUM, text).size(size).width() + 0.5f));
            RoundedRect.of(caretX, y + 3.0f, 0.6f, h - 6.0f).color(TEXT.withAlpha(a * blink)).render(graphics);
        }
        if (GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(x, y, w, h, () -> {
                this.ctx.focus.setAbField(id, MarketTab.field(id));
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, () -> {
                if (id.equals(this.ctx.focus.abField)) {
                    this.ctx.focus.abInput.setLength(0);
                    this.ctx.focus.abCharTimes.clear();
                } else {
                    MarketTab.setField(id, "");
                }
            });
        }
    }

    public float label(GuiGraphicsExtractor graphics, String text, float x, float y, float a) {
        Text.of(Fonts.MEDIUM, text, x + 1.0f, y + 4.0f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(MUTED.withAlpha(a)).render(graphics);
        return y + 8.0f;
    }

    public float paragraph(GuiGraphicsExtractor graphics, String text, float x, float y, float w, float size, Color color, float a, float top, float bottom) {
        float lineH = size + 2.6f;
        for (String line : this.wrap(text, size, w)) {
            if (y + lineH >= top - 20.0f && y <= bottom + 20.0f) {
                Text.of(Fonts.MEDIUM, line, x, y + lineH * 0.5f, size).verticalAlign(VerticalAlign.MIDDLE).color(color.withAlpha(a)).render(graphics);
            }
            y += lineH;
        }
        return y;
    }

    public List<String> wrap(String text, float size, float maxW) {
        String key = size + "|" + Math.round(maxW) + "|" + text;
        List<String> cached = this.wrapCache.get(key);
        if (cached != null) {
            return cached;
        }
        ArrayList<String> out = new ArrayList<String>();
        for (String para : (text == null ? "" : text).split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (String word : para.split(" ")) {
                String candidate;
                String string = candidate = line.isEmpty() ? word : String.valueOf(line) + " " + word;
                if (Text.of(Fonts.MEDIUM, candidate).size(size).width() <= maxW || line.isEmpty()) {
                    if (line.isEmpty() && Text.of(Fonts.MEDIUM, word).size(size).width() > maxW) {
                        String rest = word;
                        while (!rest.isEmpty()) {
                            int n;
                            for (n = rest.length(); n > 1 && Text.of(Fonts.MEDIUM, rest.substring(0, n)).size(size).width() > maxW; --n) {
                            }
                            out.add(rest.substring(0, n));
                            rest = rest.substring(n);
                        }
                        continue;
                    }
                    line.setLength(0);
                    line.append(candidate);
                    continue;
                }
                out.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
            out.add(line.toString());
        }
        if (this.wrapCache.size() > 256) {
            this.wrapCache.clear();
        }
        this.wrapCache.put(key, out);
        return out;
    }

    public static String date(long at) {
        if (at <= 0L) {
            return "";
        }
        return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(at));
    }
}

