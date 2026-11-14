/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.scripts;

import brokke.cc.ui.clickgui.scripts.KotlinHighlighter;
import brokke.cc.ui.clickgui.scripts.ScriptDocs;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.outline.Outline;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.scissor.Scissor;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.TextAlign;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class DocsView {
    private static final float NAV_W = 92.0f;
    private static final float NAV_ITEM_H = 13.0f;
    private static final float TEXT_SIZE = 5.4f;
    private static final float CODE_SIZE = 5.2f;
    private static final float LINE_H = 7.2f;
    private final List<Section> sections = new ArrayList<Section>();
    private final Runnable onBack;
    private final List<float[]> boxes = new ArrayList<float[]>();
    private final List<Runnable> actions = new ArrayList<Runnable>();
    private int selected;
    private float scroll;
    private float scrollTarget;
    private float maxScroll;
    private float navScroll;
    private float navMaxScroll;
    private float contentX;
    private float contentY;
    private float contentW;
    private float contentH;
    private float navX;
    private float navY;
    private float navW;
    private float navH;
    private float mouseX;
    private float mouseY;

    public DocsView(Runnable onBack) {
        this.onBack = onBack;
        this.parse(ScriptDocs.DOCS);
    }

    private static String clean(String s) {
        return s.replace("\u2014", "-").replace("\u2013", "-").replace("\u00ab", "\"").replace("\u00bb", "\"").replace("\u00b7", "|").replace("\u2192", "->").replace("\u2026", "...").replace("`", "");
    }

    private void parse(String docs) {
        Section current = new Section("\u041e\u0431\u0437\u043e\u0440", "\u041e\u0431\u0437\u043e\u0440", new ArrayList<Block>());
        this.sections.add(current);
        ArrayList<String> code = null;
        ArrayList<String> table = null;
        for (String raw : docs.split("\n")) {
            String line = DocsView.clean(raw);
            if (raw.startsWith("```")) {
                if (code == null) {
                    code = new ArrayList<String>();
                    continue;
                }
                current.blocks.add(new Block(Kind.CODE, code));
                code = null;
                continue;
            }
            if (code != null) {
                code.add(line);
                continue;
            }
            if (line.startsWith("|")) {
                if (line.startsWith("|---")) continue;
                if (table == null) {
                    table = new ArrayList<String>();
                }
                table.add(line);
                continue;
            }
            if (table != null) {
                current.blocks.add(new Block(Kind.TABLE, (List<String>)table));
                table = null;
            }
            if (line.startsWith("# ")) {
                current.blocks.add(new Block(Kind.PARA, List.of("@" + line.substring(2))));
                continue;
            }
            if (line.startsWith("### ")) {
                current.blocks.add(new Block(Kind.PARA, List.of("^" + line.substring(4).trim())));
                continue;
            }
            if (line.startsWith("## ")) {
                int dash;
                String title = line.substring(3).trim();
                String nav = title;
                int paren = nav.indexOf(" (");
                if (paren > 0) {
                    nav = nav.substring(0, paren);
                }
                if ((dash = nav.indexOf(" - ")) > 0) {
                    nav = nav.substring(0, dash);
                }
                current = new Section(nav, title, new ArrayList<Block>());
                this.sections.add(current);
                continue;
            }
            if (line.isBlank()) continue;
            current.blocks.add(new Block(Kind.PARA, List.of(line)));
        }
        if (table != null) {
            current.blocks.add(new Block(Kind.TABLE, table));
        }
    }

    public void reset() {
        this.scrollTarget = 0.0f;
        this.scroll = 0.0f;
    }

    private boolean hover(float x, float y, float w, float h) {
        return this.mouseX >= x && this.mouseX <= x + w && this.mouseY >= y && this.mouseY <= y + h;
    }

    private void box(float x, float y, float w, float h, Runnable r) {
        this.boxes.add(new float[]{x, y, w, h});
        this.actions.add(r);
    }

    private static float width(String s, float size) {
        return Text.of(Fonts.MEDIUM, s).size(size).width();
    }

    private static List<String> wrap(String text, float size, float maxW) {
        ArrayList<String> out = new ArrayList<String>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate;
            String string = candidate = line.isEmpty() ? word : String.valueOf(line) + " " + word;
            if (DocsView.width(candidate, size) > maxW && !line.isEmpty()) {
                out.add(line.toString());
                line = new StringBuilder(word);
                continue;
            }
            line = new StringBuilder(candidate);
        }
        if (!line.isEmpty()) {
            out.add(line.toString());
        }
        return out;
    }

    public void render(GuiGraphicsExtractor g, float x, float y, float w, float h, float mx, float my, Color accent, float delta) {
        this.mouseX = mx;
        this.mouseY = my;
        this.boxes.clear();
        this.actions.clear();
        float headH = 20.0f;
        RoundedRect.of(x, y, w, headH).radius(5.0f).color(Color.rgba(12, 12, 18, 90)).render(g);
        Outline.of(x, y, w, headH).radius(5.0f).thickness(0.7f).color(Color.rgba(255, 255, 255, 10)).render(g);
        RoundedRect.of(x + 6.0f, y + 6.0f, 2.0f, 8.0f).radius(1.0f).color(accent).render(g);
        Text.of(Fonts.MEDIUM, "\u0414\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u0430\u0446\u0438\u044f API", x + 12.0f, y + headH * 0.5f, 7.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(g);
        float titleW = DocsView.width("\u0414\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u0430\u0446\u0438\u044f API", 7.2f);
        Text.of(Fonts.MEDIUM, "Kotlin  .kts", x + 18.0f + titleW, y + headH * 0.5f, 5.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(110, 115, 130)).render(g);
        float backW = 42.0f;
        float backX = x + w - backW - 4.0f;
        float backY = y + 4.5f;
        boolean backHov = this.hover(backX, backY, backW, 11.0f);
        RoundedRect.of(backX, backY, backW, 11.0f).radius(2.5f).color(backHov ? accent.brighter(0.1f) : accent).render(g);
        Text.of(Fonts.MEDIUM, "<- \u041d\u0430\u0437\u0430\u0434", backX + backW * 0.5f, backY + 5.5f, 5.4f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(g);
        this.box(backX, backY, backW, 11.0f, this.onBack);
        float bodyY = y + headH + 5.0f;
        float bodyH = h - headH - 5.0f;
        this.renderNav(g, x, bodyY, 92.0f, bodyH, accent);
        this.renderContent(g, x + 92.0f + 5.0f, bodyY, w - 92.0f - 5.0f, bodyH, accent, delta);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderNav(GuiGraphicsExtractor g, float x, float y, float w, float h, Color accent) {
        RoundedRect.of(x, y, w, h).radius(5.0f).color(Color.rgba(12, 12, 18, 90)).render(g);
        Outline.of(x, y, w, h).radius(5.0f).thickness(0.7f).color(Color.rgba(255, 255, 255, 10)).render(g);
        Text.of(Fonts.MEDIUM, "\u0420\u0410\u0417\u0414\u0415\u041b\u042b", x + 7.0f, y + 7.5f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(100, 105, 120)).render(g);
        this.navX = x;
        this.navY = y + 14.0f;
        this.navW = w;
        this.navH = h - 16.0f;
        this.navMaxScroll = Math.max(0.0f, (float)this.sections.size() * 13.0f - this.navH + 2.0f);
        this.navScroll = Math.clamp(this.navScroll, 0.0f, this.navMaxScroll);
        Scissor.push(g, this.navX, this.navY, this.navW, this.navH);
        try {
            for (int i = 0; i < this.sections.size(); ++i) {
                boolean hov;
                float iy = this.navY + (float)i * 13.0f - this.navScroll;
                if (iy + 13.0f < this.navY || iy > this.navY + this.navH) continue;
                boolean sel = i == this.selected;
                boolean bl = hov = this.hover(x + 4.0f, iy, w - 8.0f, 12.0f) && this.hover(this.navX, this.navY, this.navW, this.navH);
                if (sel) {
                    RoundedRect.of(x + 4.0f, iy, w - 8.0f, 12.0f).radius(3.0f).color(accent.withAlpha(35)).render(g);
                    RoundedRect.of(x + 4.0f, iy + 3.0f, 1.5f, 6.0f).radius(0.75f).color(accent).render(g);
                } else if (hov) {
                    RoundedRect.of(x + 4.0f, iy, w - 8.0f, 12.0f).radius(3.0f).color(Color.rgba(255, 255, 255, 7)).render(g);
                }
                Object label = this.sections.get(i).nav();
                while (((String)label).length() > 3 && DocsView.width((String)label, 5.5f) > w - 18.0f) {
                    label = ((String)label).substring(0, ((String)label).length() - 3) + "..";
                }
                Text.of(Fonts.MEDIUM, (String)label, x + 9.0f, iy + 6.0f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(sel ? Color.WHITE : (hov ? Color.rgb(210, 215, 228) : Color.rgb(145, 150, 165))).render(g);
                int index = i;
                float by = Math.max(iy, this.navY);
                float bh = Math.min(iy + 13.0f - 1.0f, this.navY + this.navH) - by;
                if (!(bh > 0.0f)) continue;
                this.box(x + 4.0f, by, w - 8.0f, bh, () -> {
                    if (this.selected != index) {
                        this.selected = index;
                        this.scrollTarget = 0.0f;
                        this.scroll = 0.0f;
                    }
                });
            }
        }
        finally {
            Scissor.pop(g);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderContent(GuiGraphicsExtractor g, float x, float y, float w, float h, Color accent, float delta) {
        RoundedRect.of(x, y, w, h).radius(5.0f).color(Color.rgba(12, 12, 18, 90)).render(g);
        Outline.of(x, y, w, h).radius(5.0f).thickness(0.7f).color(Color.rgba(255, 255, 255, 10)).render(g);
        this.contentX = x;
        this.contentY = y + 2.0f;
        this.contentW = w;
        this.contentH = h - 4.0f;
        this.scrollTarget = Math.clamp(this.scrollTarget, 0.0f, this.maxScroll);
        this.scroll += (this.scrollTarget - this.scroll) * Math.min(1.0f, delta * 16.0f);
        Section section = this.sections.get(Math.clamp((long)this.selected, 0, this.sections.size() - 1));
        float px = x + 9.0f;
        float pw = w - 20.0f;
        float cy = y + 8.0f - this.scroll;
        Scissor.push(g, this.contentX, this.contentY, this.contentW, this.contentH);
        try {
            Text.of(Fonts.MEDIUM, section.title(), px, cy + 5.0f, 7.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(g);
            RoundedRect.of(px, cy += 12.0f, 24.0f, 1.5f).radius(0.75f).color(accent).render(g);
            RoundedRect.of(px + 26.0f, cy + 0.25f, pw - 26.0f, 1.0f).color(Color.rgba(255, 255, 255, 8)).render(g);
            cy += 7.0f;
            for (Block block : section.blocks()) {
                cy = switch (block.kind().ordinal()) {
                    default -> throw new MatchException(null, null);
                    case 0 -> this.renderPara(g, block.lines().get(0), px, cy, pw, accent);
                    case 1 -> this.renderCode(g, block.lines(), px, cy, pw, accent);
                    case 2 -> this.renderTable(g, block.lines(), px, cy, pw, accent);
                };
                cy += 5.0f;
            }
        }
        finally {
            Scissor.pop(g);
        }
        float total = cy + this.scroll - (y + 8.0f);
        this.maxScroll = Math.max(0.0f, total - h + 10.0f);
        if (this.maxScroll > 1.0f) {
            float thumbH = Math.max(14.0f, this.contentH / (this.contentH + this.maxScroll) * this.contentH);
            float thumbY = this.contentY + Math.clamp(this.scroll / this.maxScroll, 0.0f, 1.0f) * (this.contentH - thumbH);
            RoundedRect.of(x + w - 3.5f, this.contentY, 2.0f, this.contentH).radius(1.0f).color(Color.rgba(255, 255, 255, 8)).render(g);
            RoundedRect.of(x + w - 3.5f, thumbY, 2.0f, thumbH).radius(1.0f).color(accent.withAlpha(130)).render(g);
        }
    }

    private float renderPara(GuiGraphicsExtractor g, String text, float x, float y, float w, Color accent) {
        if (text.startsWith("^")) {
            String title = text.substring(1);
            float ty = y + 4.0f;
            Text.of(Fonts.MEDIUM, title, x, ty + 4.0f, 6.4f).verticalAlign(VerticalAlign.MIDDLE).color(accent.brighter(0.35f)).render(g);
            RoundedRect.of(x, ty + 9.5f, DocsView.width(title, 6.4f), 0.8f).color(accent.withAlpha(90)).render(g);
            return ty + 12.0f;
        }
        if (text.startsWith("@")) {
            String title = text.substring(1);
            RoundedRect.of(x, y, w, 22.0f).radius(4.0f).horizontalGradient(accent.withAlpha(60), accent.withAlpha(10)).render(g);
            Text.of(Fonts.MEDIUM, title, x + 7.0f, y + 11.0f, 7.0f).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(g);
            return y + 22.0f;
        }
        String key = null;
        String rest = text;
        int colon = text.indexOf(": ");
        if (colon > 0 && colon <= 14 && !text.substring(0, colon).contains(" ")) {
            key = text.substring(0, colon);
            rest = text.substring(colon + 2);
        }
        float textX = x + 6.0f;
        float textW = w - 6.0f;
        RoundedRect.of(x + 1.0f, y + 2.5f, 2.0f, 2.0f).radius(1.0f).color(accent.withAlpha(180)).render(g);
        float cy = y;
        if (key != null) {
            float kw = DocsView.width(key, 5.2f) + 7.0f;
            RoundedRect.of(textX, cy, kw, 7.5f).radius(2.0f).color(accent.withAlpha(40)).render(g);
            Text.of(Fonts.MEDIUM, key, textX + kw * 0.5f, cy + 3.75f, 5.2f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(accent.brighter(0.25f)).render(g);
            cy += 9.0f;
        }
        for (String line : DocsView.wrap(rest, 5.4f, textW)) {
            Text.of(Fonts.MEDIUM, line, textX, cy + 3.6f, 5.4f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(185, 190, 205)).render(g);
            cy += 7.2f;
        }
        return cy;
    }

    private float renderCode(GuiGraphicsExtractor g, List<String> lines, float x, float y, float w, Color accent) {
        float pad = 4.0f;
        float h = (float)lines.size() * 7.2f + pad * 2.0f;
        RoundedRect.of(x, y, w, h).radius(3.5f).color(Color.rgba(10, 11, 16, 200)).render(g);
        Outline.of(x, y, w, h).radius(3.5f).thickness(0.6f).color(Color.rgba(255, 255, 255, 12)).render(g);
        RoundedRect.of(x, y + 3.0f, 1.5f, h - 6.0f).radius(0.75f).color(accent.withAlpha(160)).render(g);
        Text.of(Fonts.MEDIUM, "kotlin", x + w - 5.0f, y + 5.0f, 4.4f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(90, 95, 110)).render(g);
        float cy = y + pad;
        for (String line : lines) {
            KotlinHighlighter.Span span;
            float sx;
            float mid = cy + 3.6f;
            Iterator<KotlinHighlighter.Span> iterator = KotlinHighlighter.tokenize(line).iterator();
            while (iterator.hasNext() && !((sx = x + 7.0f + DocsView.width(line.substring(0, (span = iterator.next()).start()).replace(' ', '_'), 5.2f)) > x + w - 4.0f)) {
                Text.of(Fonts.MEDIUM, line.substring(span.start(), span.end()), sx, mid, 5.2f).verticalAlign(VerticalAlign.MIDDLE).color(span.color()).render(g);
            }
            cy += 7.2f;
        }
        return y + h;
    }

    private float renderTable(GuiGraphicsExtractor g, List<String> rows, float x, float y, float w, Color accent) {
        ArrayList<String[]> cells = new ArrayList<String[]>();
        int cols = 0;
        for (String row : rows) {
            String trimmed = row.trim();
            if (trimmed.startsWith("|")) {
                trimmed = trimmed.substring(1);
            }
            if (trimmed.endsWith("|")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            String[] parts = trimmed.split("\\|");
            for (int i = 0; i < parts.length; ++i) {
                parts[i] = parts[i].trim();
            }
            cells.add(parts);
            cols = Math.max(cols, parts.length);
        }
        if (cols == 0) {
            return y;
        }
        float[] colW = new float[cols];
        if (cols == 3) {
            colW[0] = w * 0.36f;
            colW[1] = w * 0.32f;
            colW[2] = w * 0.32f;
        } else {
            for (int i = 0; i < cols; ++i) {
                colW[i] = w / (float)cols;
            }
        }
        float cy = y;
        float cell = 4.5f;
        for (int r = 0; r < cells.size(); ++r) {
            Color bg;
            String[] row = (String[])cells.get(r);
            boolean header = r == 0;
            ArrayList<List<String>> wrapped = new ArrayList<List<String>>();
            int maxLines = 1;
            for (int c = 0; c < cols; ++c) {
                String value = c < row.length ? row[c] : "";
                List<String> lines = DocsView.wrap(value, header ? 5.2f : 5.0f, colW[c] - cell * 2.0f);
                if (lines.isEmpty()) {
                    lines = List.of("");
                }
                wrapped.add(lines);
                maxLines = Math.max(maxLines, lines.size());
            }
            float rowH = (float)maxLines * 6.6f + 4.0f;
            bg = header ? accent.withAlpha(45) : r % 2 == 0 ? Color.rgba(255, 255, 255, 5) : Color.rgba(255, 255, 255, 2);
            if (header) {
                RoundedRect.of(x, cy, w, rowH).radius(3.0f, 3.0f, 0.0f, 0.0f).color(bg).render(g);
            } else if (r == cells.size() - 1) {
                RoundedRect.of(x, cy, w, rowH).radius(0.0f, 0.0f, 3.0f, 3.0f).color(bg).render(g);
            } else {
                RoundedRect.of(x, cy, w, rowH).color(bg).render(g);
            }
            float cx = x;
            for (int c = 0; c < cols; ++c) {
                float ly = cy + 2.0f;
                for (String line : wrapped.get(c)) {
                    Color col = header ? Color.WHITE : (c == 0 ? accent.brighter(0.3f) : Color.rgb(175, 180, 195));
                    Text.of(Fonts.MEDIUM, line, cx + cell, ly + 3.3f, header ? 5.2f : 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(col).render(g);
                    ly += 6.6f;
                }
                cx += colW[c];
                if (c >= cols - 1) continue;
                RoundedRect.of(cx, cy + 1.5f, 0.5f, rowH - 3.0f).color(Color.rgba(255, 255, 255, 10)).render(g);
            }
            cy += rowH;
        }
        Outline.of(x, y, w, cy - y).radius(3.0f).thickness(0.6f).color(Color.rgba(255, 255, 255, 12)).render(g);
        return cy;
    }

    public boolean mouseClicked(float mx, float my) {
        this.mouseX = mx;
        this.mouseY = my;
        for (int i = this.boxes.size() - 1; i >= 0; --i) {
            float[] b = this.boxes.get(i);
            if (!(mx >= b[0]) || !(mx <= b[0] + b[2]) || !(my >= b[1]) || !(my <= b[1] + b[3])) continue;
            this.actions.get(i).run();
            return true;
        }
        return false;
    }

    public void mouseScrolled(float mx, float my, double amount) {
        if (mx >= this.navX && mx <= this.navX + this.navW && my >= this.navY && my <= this.navY + this.navH) {
            this.navScroll = Math.clamp(this.navScroll - (float)amount * 13.0f, 0.0f, this.navMaxScroll);
        } else {
            this.scrollTarget = Math.clamp(this.scrollTarget - (float)amount * 22.0f, 0.0f, this.maxScroll);
        }
    }

    private record Section(String nav, String title, List<Block> blocks) {
    }

    private record Block(Kind kind, List<String> lines) {
    }

    private static enum Kind {
        PARA,
        CODE,
        TABLE;

    }
}

