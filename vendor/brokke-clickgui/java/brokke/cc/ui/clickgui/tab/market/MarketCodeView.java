/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.tab.market;

import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.scripts.KotlinHighlighter;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.scissor.Scissor;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.TextAlign;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import java.util.Arrays;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class MarketCodeView
extends GuiComponent {
    private static final float LINE_H = 6.4f;
    private static final float CODE_SIZE = 4.5f;
    private static final int MAX_LINES = 4000;
    private String source;
    private String[] lines = new String[0];

    public MarketCodeView(ClickGuiContext ctx) {
        super(ctx);
    }

    private void prepare(String code) {
        if (code == this.source) {
            return;
        }
        this.source = code;
        String[] split = (code == null ? "" : code.replace("\r\n", "\n").replace("\t", "    ")).split("\n", -1);
        this.lines = split.length > 4000 ? Arrays.copyOf(split, 4000) : split;
    }

    public float render(GuiGraphicsExtractor graphics, String code, float x, float y, float w, float a, float top, float bottom) {
        this.prepare(code);
        float pad = 4.0f;
        float gutter = 16.0f;
        float h = (float)this.lines.length * 6.4f + pad * 2.0f;
        RoundedRect.of(x, y, w, h).radius(4.0f).color(Color.rgba(0, 0, 0, Math.round(70.0f * a))).render(graphics);
        float clipTop = Math.max(y, top == Float.MAX_VALUE ? y : top);
        float clipBottom = Math.min(y + h, bottom);
        if (clipBottom - clipTop < 1.0f) {
            return y + h;
        }
        Scissor.push(graphics, x, clipTop, w, clipBottom - clipTop);
        int first = Math.max(0, (int)Math.floor((clipTop - y - pad) / 6.4f) - 1);
        int last = Math.min(this.lines.length - 1, (int)Math.ceil((clipBottom - y - pad) / 6.4f) + 1);
        float textX = x + gutter + 3.0f;
        block0: for (int i = first; i <= last; ++i) {
            float mid = y + pad + (float)i * 6.4f + 3.2f;
            Text.of(Fonts.MEDIUM, String.valueOf(i + 1), x + gutter - 2.0f, mid, 4.1f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(90, 95, 106).withAlpha(a)).render(graphics);
            String text = this.lines[i];
            if (text.isEmpty()) continue;
            float cursor = textX;
            int consumed = 0;
            for (KotlinHighlighter.Span span : KotlinHighlighter.tokenize(text)) {
                if (span.start() > consumed) {
                    cursor += Text.of(Fonts.MEDIUM, text.substring(consumed, span.start())).size(4.5f).width();
                }
                if (cursor > x + w) continue block0;
                String part = text.substring(span.start(), span.end());
                Text.of(Fonts.MEDIUM, part, cursor, mid, 4.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.of(span.color()).withAlpha(a)).render(graphics);
                cursor += Text.of(Fonts.MEDIUM, part).size(4.5f).width();
                consumed = span.end();
            }
        }
        Scissor.pop(graphics);
        return y + h;
    }
}

