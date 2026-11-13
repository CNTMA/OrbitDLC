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
import brokke.cc.ui.clickgui.scripts.ScriptDocs;
import brokke.cc.ui.clickgui.tab.ScriptsTab;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class DocsPage
extends GuiComponent {
    private static int docSection = 0;
    private static final List<Object[]> DOC_SECTIONS = new ArrayList<Object[]>();
    private final Map<String, List<String>> docWrapCache = new HashMap<String, List<String>>();

    public DocsPage(ClickGuiContext ctx) {
        super(ctx);
    }

    private static List<Object[]> docSections() {
        if (DOC_SECTIONS.isEmpty()) {
            String title = "\u0412\u0432\u0435\u0434\u0435\u043d\u0438\u0435";
            ArrayList<String> lines = new ArrayList<String>();
            for (String line : ScriptDocs.DOCS.split("\n")) {
                if (line.startsWith("## ")) {
                    if (!lines.isEmpty()) {
                        DOC_SECTIONS.add(new Object[]{title, lines});
                    }
                    title = line.substring(3).trim();
                    lines = new ArrayList();
                    continue;
                }
                if (line.startsWith("# ")) continue;
                lines.add(line);
            }
            if (!lines.isEmpty()) {
                DOC_SECTIONS.add(new Object[]{title, lines});
            }
        }
        return DOC_SECTIONS;
    }

    private List<String> wrapDoc(String text, float size, float maxW) {
        return this.docWrapCache.computeIfAbsent(size + "|" + maxW + "|" + text, k -> {
            ArrayList<String> out = new ArrayList<String>();
            StringBuilder line = new StringBuilder();
            for (String word : text.split(" ")) {
                String candidate;
                String string = candidate = line.isEmpty() ? word : String.valueOf(line) + " " + word;
                if (!line.isEmpty() && Text.of(Fonts.MEDIUM, candidate).size(size).width() > maxW) {
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                    continue;
                }
                line.setLength(0);
                line.append(candidate);
            }
            if (!line.isEmpty()) {
                out.add(line.toString());
            }
            if (out.isEmpty()) {
                out.add("");
            }
            return out;
        });
    }

    private void openDocSection(int index) {
        if (docSection == index) {
            return;
        }
        docSection = index;
        this.ctx.switchTime = System.currentTimeMillis();
        this.ctx.scrollTarget = 0.0f;
        ClientSound.ui(ClientSound.Ui.CLICK);
    }

    public float renderDocs(GuiGraphicsExtractor graphics, float x, float y, float w, float a, Color accent, float top, float bottom, long now) {
        float startY = y;
        List<Object[]> sections = DocsPage.docSections();
        docSection = Math.clamp((long)docSection, 0, sections.size() - 1);
        float barH = 14.0f;
        float backW = Text.of(Fonts.MEDIUM, "\u041d\u0430\u0437\u0430\u0434").size(5.0f).width() + 18.0f;
        this.ctx.widgets.renderEventPill(graphics, "docs-back", "\u041d\u0430\u0437\u0430\u0434", "<", x, y, backW, barH, a, false, top, bottom, () -> {
            ScriptsTab.scriptsDocs = false;
            this.ctx.switchTime = System.currentTimeMillis();
            this.ctx.scrollTarget = 0.0f;
            ClientSound.ui(ClientSound.Ui.CLICK);
        });
        Text.of(Fonts.MEDIUM, "\u0414\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u0430\u0446\u0438\u044f API", x + backW + 6.0f, y + barH * 0.5f, 5.4f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(150, 155, 166).withAlpha(a)).render(graphics);
        y += barH + 6.0f;
        float chipH = 12.0f;
        float cx = x;
        for (int i = 0; i < sections.size(); ++i) {
            String name = (String)sections.get(i)[0];
            float cw = Text.of(Fonts.MEDIUM, name).size(4.8f).width() + 10.0f;
            if (cx + cw > x + w) {
                cx = x;
                y += chipH + 3.0f;
            }
            int idx = i;
            String key = "doc-chip-" + i;
            boolean hover = GuiDraw.canHit(y, chipH, top, bottom) && this.hovered(cx, y, cw, chipH);
            float hv = this.anim(key, this.hov(key, hover), 8.0f);
            float sel = this.anim(String.valueOf(key) + "-sel", docSection == i, 8.0f);
            RoundedRect.of(cx, y, cw, chipH).radius(3.5f).color(Color.rgba(255, 255, 255, Math.round((7.0f + 5.0f * hv) * (1.0f - sel) * a))).render(graphics);
            if (sel > 0.01f) {
                RoundedRect.of(cx, y, cw, chipH).radius(3.5f).horizontalGradient(accent.withAlpha(0.75f * sel * a), GuiDraw.getAccent2().withAlpha(0.75f * sel * a)).render(graphics);
            }
            Text.of(Fonts.MEDIUM, name, cx + cw * 0.5f, y + chipH * 0.5f, 4.8f).centered().color(GuiDraw.mix(Color.rgb(175, 180, 190), Color.rgb(255, 255, 255), Math.max(hv, sel)).withAlpha(a)).render(graphics);
            if (GuiDraw.canHit(y, chipH, top, bottom)) {
                this.hit(cx, y, cw, chipH, () -> this.openDocSection(idx), null);
            }
            cx += cw + 3.0f;
        }
        y += chipH + 8.0f;
        float appear = GuiDraw.easeOut((float)(now - this.ctx.switchTime) / 320.0f);
        float ca = a * appear;
        y += (1.0f - appear) * 6.0f;
        List<String> lines = (List<String>)sections.get(docSection)[1];
        Color textC = Color.rgb(190, 194, 204);
        Color dimC = Color.rgb(120, 125, 136);
        Color codeC = Color.rgb(170, 215, 160);
        float textSize = 5.0f;
        float lineH = 7.5f;
        boolean code = false;
        ArrayList<String> codeLines = new ArrayList<String>();
        boolean tableHead = false;
        for (String raw : lines) {
            String line = raw.stripTrailing();
            if (line.trim().startsWith("```")) {
                if (code) {
                    float boxH = (float)codeLines.size() * 6.8f + 8.0f;
                    if (y + boxH >= top && y <= bottom) {
                        RoundedRect.of(x, y, w, boxH).radius(4.0f).color(Color.rgba(0, 0, 0, Math.round(70.0f * ca))).render(graphics);
                        float ly = y + 4.0f;
                        for (String cl : codeLines) {
                            if (ly + 6.8f >= top && ly <= bottom) {
                                boolean comment = cl.trim().startsWith("//");
                                Text.of(Fonts.MEDIUM, GuiDraw.fit(cl.replace("\t", "    "), 4.6f, w - 12.0f), x + 6.0f, ly + 3.4f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color((comment ? dimC : codeC).withAlpha(ca)).render(graphics);
                            }
                            ly += 6.8f;
                        }
                    }
                    y += boxH + 4.0f;
                    codeLines.clear();
                }
                code = !code;
                continue;
            }
            if (code) {
                codeLines.add(line);
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                y += 3.0f;
                tableHead = false;
                continue;
            }
            if (trimmed.startsWith("### ")) {
                if ((y += 3.0f) + 10.0f >= top && y <= bottom) {
                    RoundedRect.of(x, y + 1.5f, 2.0f, 7.0f).radius(1.0f).verticalGradient(accent.withAlpha(ca), GuiDraw.getAccent2().withAlpha(ca)).render(graphics);
                    Text.of(Fonts.MEDIUM, trimmed.substring(4), x + 6.0f, y + 5.0f, 6.0f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(240, 242, 248).withAlpha(ca)).render(graphics);
                }
                y += 12.0f;
                tableHead = true;
                continue;
            }
            if (trimmed.startsWith("|")) {
                if (trimmed.replace("|", "").replace("-", "").replace(":", "").isBlank()) continue;
                String[] cells = trimmed.substring(1, trimmed.endsWith("|") ? trimmed.length() - 1 : trimmed.length()).split("\\|");
                String left = cells[0].trim();
                StringBuilder rest = new StringBuilder();
                for (int i = 1; i < cells.length; ++i) {
                    if (rest.length() > 0) {
                        rest.append(" - ");
                    }
                    rest.append(cells[i].trim());
                }
                boolean head = tableHead;
                tableHead = false;
                float leftW = w * 0.42f;
                List<String> rl = this.wrapDoc(rest.toString(), 4.7f, w - leftW - 12.0f);
                List<String> ll = this.wrapDoc(left, 4.7f, leftW - 8.0f);
                float rowH = (float)Math.max(rl.size(), ll.size()) * 6.8f + 4.0f;
                if (y + rowH >= top && y <= bottom) {
                    int i;
                    RoundedRect.of(x, y, w, rowH).radius(3.0f).color(Color.rgba(255, 255, 255, Math.round((head ? 10.0f : 5.0f) * ca))).render(graphics);
                    for (i = 0; i < ll.size(); ++i) {
                        Text.of(Fonts.MEDIUM, ll.get(i), x + 5.0f, y + 5.4f + (float)i * 6.8f, 4.7f).verticalAlign(VerticalAlign.MIDDLE).color((head ? Color.rgb(235, 238, 245) : codeC).withAlpha(ca)).render(graphics);
                    }
                    for (i = 0; i < rl.size(); ++i) {
                        Text.of(Fonts.MEDIUM, rl.get(i), x + leftW + 4.0f, y + 5.4f + (float)i * 6.8f, 4.7f).verticalAlign(VerticalAlign.MIDDLE).color((head ? Color.rgb(235, 238, 245) : textC).withAlpha(ca)).render(graphics);
                    }
                }
                y += rowH + 1.5f;
                continue;
            }
            tableHead = false;
            boolean bullet = trimmed.startsWith("- ");
            String body = bullet ? trimmed.substring(2) : trimmed;
            float indent = bullet ? 7.0f : 0.0f;
            List<String> wrapped = this.wrapDoc(body, textSize, w - indent - 2.0f);
            for (int i = 0; i < wrapped.size(); ++i) {
                if (y + lineH >= top && y <= bottom) {
                    if (bullet && i == 0) {
                        RoundedRect.of(x + 2.0f, y + lineH * 0.5f - 1.0f, 2.0f, 2.0f).radius(1.0f).color(accent.withAlpha(ca)).render(graphics);
                    }
                    Text.of(Fonts.MEDIUM, wrapped.get(i), x + indent, y + lineH * 0.5f, textSize).verticalAlign(VerticalAlign.MIDDLE).color(textC.withAlpha(ca)).render(graphics);
                }
                y += lineH;
            }
            y += 1.0f;
        }
        float navH = 14.0f;
        y += 6.0f;
        if (docSection > 0) {
            String prev = (String)sections.get(docSection - 1)[0];
            float pw = Text.of(Fonts.MEDIUM, prev).size(5.0f).width() + 18.0f;
            this.ctx.widgets.renderEventPill(graphics, "docs-prev", prev, "<", x, y, pw, navH, ca, false, top, bottom, () -> this.openDocSection(docSection - 1));
        }
        if (docSection < sections.size() - 1) {
            String next = (String)sections.get(docSection + 1)[0];
            float nw = Text.of(Fonts.MEDIUM, next).size(5.0f).width() + 18.0f;
            this.ctx.widgets.renderEventPill(graphics, "docs-next", next, ">", x + w - nw, y, nw, navH, ca, true, top, bottom, () -> this.openDocSection(docSection + 1));
        }
        return (y += navH) - startY;
    }
}

