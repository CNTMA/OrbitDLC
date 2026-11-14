/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.tab;

import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.animation.Animation;
import brokke.cc.util.animation.Easing;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.scissor.Scissor;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class SearchBar
extends GuiComponent {
    public final StringBuilder search = new StringBuilder();
    public final List<Long> charTimes = new ArrayList<Long>();
    private final List<Object[]> ghosts = new ArrayList<Object[]>();
    private final Animation caretAnim = new Animation();
    private final Animation placeholderAnim = new Animation(1.0f);
    public String lastQuery = "";

    public SearchBar(ClickGuiContext ctx) {
        super(ctx);
    }

    public void removeLast() {
        int i = this.search.length() - 1;
        this.ghosts.add(new Object[]{String.valueOf(this.search.charAt(i)), this.search.substring(0, i), System.currentTimeMillis()});
        this.search.setLength(i);
        if (!this.charTimes.isEmpty()) {
            this.charTimes.remove(this.charTimes.size() - 1);
        }
    }

    public void clearSearch() {
        this.ctx.focus.searchSelectedAll = false;
        long now = System.currentTimeMillis();
        for (int i = 0; i < this.search.length(); ++i) {
            this.ghosts.add(new Object[]{String.valueOf(this.search.charAt(i)), this.search.substring(0, i), now + (long)i * 12L});
        }
        this.search.setLength(0);
        this.charTimes.clear();
    }

    public void renderSearch(GuiGraphicsExtractor graphics, float x, float y, float prog) {
        int start;
        float w = 85.0f;
        float h = 13.0f;
        boolean hover = this.hovered(x, y, w, h);
        float hv = this.anim("search-hover", hover, 7.0f);
        float fc = this.anim("search-focus", this.ctx.focus.searchFocused, 6.0f);
        this.hov("search", hover);
        RoundedRect.of(x, y, w, h).radius(h * 0.5f * 0.7f).color(Color.rgba(255, 255, 255, Math.round((10.0f + 5.0f * hv + 6.0f * fc) * prog))).render(graphics);
        Color iconColor = GuiDraw.mix(Color.rgb(140, 145, 156), Color.rgb(225, 228, 236), Math.max(hv * 0.5f, fc));
        Text.of(Fonts.ICONS, "M", x + 8.0f, y + h * 0.5f, 6.0f).centered().color(iconColor.withAlpha(prog)).render(graphics);
        String badge = "Ctrl F";
        float bw = Text.of(Fonts.MEDIUM, badge).size(4.6f).width() + 7.0f;
        float bh = h - 4.0f;
        float bx = x + w - bw - 2.0f;
        float by = y + 2.0f;
        float badgeAlpha = prog * (1.0f - fc * 0.6f);
        RoundedRect.of(bx, by, bw, bh).radius(2.5f).color(Color.rgba(255, 255, 255, Math.round(12.0f * badgeAlpha))).render(graphics);
        Text.of(Fonts.MEDIUM, badge, bx + bw * 0.5f, by + bh * 0.5f, 4.6f).centered().color(Color.rgb(160, 165, 175).withAlpha(badgeAlpha)).render(graphics);
        float tx = x + 15.0f;
        float maxW = bx - tx - 3.0f;
        String text = this.search.toString();
        float size = 5.4f;
        float cy = y + h * 0.5f;
        long now = System.currentTimeMillis();
        this.placeholderAnim.animate(text.isEmpty() ? 1.0f : 0.0f, 1.0f, 8.0f, Easing.CUBIC_OUT);
        float ph = Math.clamp(this.placeholderAnim.value(), 0.0f, 1.0f);
        if (ph > 0.01f) {
            Text.of(Fonts.MEDIUM, "\u041f\u043e\u0438\u0441\u043a...", tx + (1.0f - ph) * 4.0f, cy, size).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(125, 130, 141).withAlpha(prog * ph)).render(graphics);
        }
        for (start = 0; start < text.length() - 1 && Text.of(Fonts.MEDIUM, text.substring(start)).size(size).width() > maxW; ++start) {
        }
        float shift = start > 0 ? Text.of(Fonts.MEDIUM, text.substring(0, start)).size(size).width() : 0.0f;
        Scissor.push(graphics, tx - 1.0f, y, maxW + 2.0f, h);
        if (this.ctx.focus.searchSelectedAll && this.ctx.focus.searchFocused) {
            float selectedWidth = Text.of(Fonts.MEDIUM, text.substring(start)).size(size).width();
            RoundedRect.of(tx, cy - 4.0f, selectedWidth, 8.0f).radius(1.0f).color(GuiDraw.getAccent().withAlpha(prog * 0.35f)).render(graphics);
        }
        for (int i = start; i < text.length(); ++i) {
            float cx = tx + Text.of(Fonts.MEDIUM, text.substring(0, i)).size(size).width() - shift;
            long born = i < this.charTimes.size() ? this.charTimes.get(i) : 0L;
            float t = GuiDraw.easeOut((float)(now - born) / 220.0f);
            Text.of(Fonts.MEDIUM, String.valueOf(text.charAt(i)), cx, cy + (1.0f - t) * 4.0f, size).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 238, 245).withAlpha(prog * t)).render(graphics);
        }
        this.ghosts.removeIf(g -> now - (Long)g[2] > 200L);
        for (Object[] g2 : this.ghosts) {
            float t = Math.clamp((float)(now - (Long)g2[2]) / 200.0f, 0.0f, 1.0f);
            if (t <= 0.0f && now < (Long)g2[2]) {
                t = 0.0f;
            }
            float cx = tx + Text.of(Fonts.MEDIUM, (String)g2[1]).size(size).width() - shift;
            Text.of(Fonts.MEDIUM, (String)g2[0], cx, cy - GuiDraw.easeOut(t) * 4.0f, size).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 238, 245).withAlpha(prog * (1.0f - t) * 0.8f)).render(graphics);
        }
        Scissor.pop(graphics);
        float caretTarget = text.isEmpty() ? 0.0f : Text.of(Fonts.MEDIUM, text).size(size).width() - shift + 0.5f;
        this.caretAnim.animate(caretTarget, 1.0f, 14.0f, Easing.CUBIC_OUT);
        if (this.ctx.focus.searchFocused && !this.ctx.focus.searchSelectedAll) {
            float blink = 0.5f + 0.5f * (float)Math.cos((double)now / 170.0);
            RoundedRect.of(tx + this.caretAnim.value(), y + 3.5f, 0.6f, h - 7.0f).color(Color.rgb(235, 238, 245).withAlpha(prog * blink * fc)).render(graphics);
        }
        this.hit(x, y, w, h, () -> {
            this.ctx.focus.setSearchFocus(true);
            ClientSound.ui(ClientSound.Ui.CLICK);
        }, this::clearSearch);
    }
}

