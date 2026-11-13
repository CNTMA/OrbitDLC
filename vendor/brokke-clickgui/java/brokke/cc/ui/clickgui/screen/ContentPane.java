/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.screen;

import brokke.cc.core.module.Category;
import brokke.cc.core.module.Module;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.ClickGuiLayout;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.animation.Animation;
import brokke.cc.util.animation.Easing;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.scissor.Scissor;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ContentPane
extends GuiComponent {
    public final Animation titleAnim = new Animation(1.0f);

    public ContentPane(ClickGuiContext ctx) {
        super(ctx);
    }

    public void renderContent(GuiGraphicsExtractor graphics, float px, float py, float prog, float delta) {
        List<Module> modules;
        this.ctx.hits.clear();
        this.ctx.settings.beginFrame();
        Category category = ClickGuiLayout.CATEGORIES.get(this.ctx.selected);
        float cx = px + 80.0f + 7.0f;
        float fullW = 306.0f;
        float cw = fullW - 2.5f - 3.0f;
        String query = this.ctx.searchBar.search.toString().trim().toLowerCase(Locale.ROOT);
        if (!query.equals(this.ctx.searchBar.lastQuery)) {
            this.ctx.searchBar.lastQuery = query;
            this.ctx.scrollTarget = 0.0f;
            this.ctx.scrollValue = 0.0f;
            this.ctx.scrollbar.thumbYValue = Float.NaN;
            this.ctx.switchTime = System.currentTimeMillis();
        }
        boolean searching = !query.isEmpty();
        boolean autoSbor = this.ctx.autoSborOpen && !searching;
        this.titleAnim.animate(1.0f, delta, 4.0f, Easing.EXPO_OUT);
        float title = Math.clamp(this.titleAnim.value(), 0.0f, 1.0f);
        this.ctx.searchBar.renderSearch(graphics, cx + fullW - 85.0f, py + 3.5f + 3.0f, prog);
        if (!searching && category == Category.MARKET) {
            this.ctx.market.renderHeaderBalance(graphics, cx + fullW - 85.0f - 6.0f, py + 10.0f + 3.0f, prog * title);
        }
        Text.of(Fonts.MEDIUM, searching ? "\u041f\u043e\u0438\u0441\u043a" : (autoSbor ? "AutoSbor" : ClickGuiLayout.categoryName(category)), cx + (1.0f - title) * 6.0f, py + 10.0f + 3.0f, 6.4f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 238, 245).withAlpha(prog * title)).render(graphics);
        float areaY = py + 20.0f + 4.0f;
        float areaH = 219.0f;
        float colW = (cw - 5.0f) * 0.5f;
        if (this.ctx.scrollbar.scrollDragging && this.ctx.maxScroll > 0.0f) {
            float thumbH = this.ctx.scrollbar.thumbBox[3];
            float k = Math.clamp((this.getMouseY() - this.ctx.scrollbar.scrollDragOffset - areaY) / Math.max(1.0f, areaH - thumbH), 0.0f, 1.0f);
            this.ctx.scrollTarget = k * this.ctx.maxScroll;
        }
        long nowNanos = System.nanoTime();
        this.ctx.frameDt = this.ctx.lastFrameNanos == 0L ? 0.016666668f : Math.clamp((float)(nowNanos - this.ctx.lastFrameNanos) / 1.0E9f, 5.0E-4f, 0.05f);
        this.ctx.lastFrameNanos = nowNanos;
        this.ctx.scrollValue = this.smooth(this.ctx.scrollValue, this.ctx.scrollTarget, this.ctx.scrollbar.scrollDragging ? 30.0f : 14.0f);
        if (Math.abs(this.ctx.scrollValue - this.ctx.scrollTarget) < 0.05f) {
            this.ctx.scrollValue = this.ctx.scrollTarget;
        }
        float scroll = this.ctx.scrollValue;
        long now = System.currentTimeMillis();
        Scissor.push(graphics, cx - 1.0f, areaY, cw + 2.0f, areaH);
        this.ctx.hits.setClip(cx - 1.0f, areaY, cw + 2.0f, areaH);
        float[] colY = new float[]{areaY - scroll, areaY - scroll};
        int index = 0;
        int placed = 0;
        boolean special = autoSbor || !searching && (category == null || !category.isMainCategory());
        float specialH = 0.0f;
        if (special) {
            modules = List.of();
            if (autoSbor) {
                specialH = this.ctx.autoSbor.renderAutoSbor(graphics, cx, areaY - scroll, cw, prog, areaY, areaY + areaH, now);
            } else if (category == null) {
                specialH = this.ctx.themes.renderThemes(graphics, cx, areaY - scroll, cw, prog, areaY, areaY + areaH, now);
            } else if (category == Category.COSMETICS) {
                specialH = this.ctx.cosmetics.renderCosmetics(graphics, cx, areaY - scroll, cw, prog, areaY, areaY + areaH, now);
            } else if (category == Category.CONFIGS) {
                specialH = this.ctx.configs.renderConfigs(graphics, cx, areaY - scroll, cw, prog, areaY, areaY + areaH, now);
            } else if (category == Category.EVENTS) {
                specialH = this.ctx.events.renderEvents(graphics, cx, areaY - scroll, cw, prog, areaY, areaY + areaH, now);
            } else if (category == Category.SCRIPTS) {
                specialH = this.ctx.scripts.renderScripts(graphics, cx, areaY - scroll, cw, prog, areaY, areaY + areaH, now);
            } else if (category == Category.AUTOBUY) {
                specialH = this.ctx.autoBuy.renderAutoBuy(graphics, cx, areaY - scroll, cw, prog, areaY, areaY + areaH, now);
            } else if (category == Category.BOTS) {
                specialH = this.ctx.bots.renderBots(graphics, cx, areaY - scroll, cw, prog, areaY, areaY + areaH, now);
            } else if (category == Category.FRIENDS) {
                specialH = this.ctx.globals.renderGlobals(graphics, cx, areaY - scroll, cw, prog, areaY, areaY + areaH, now);
            } else if (category == Category.MARKET) {
                specialH = this.ctx.market.renderMarket(graphics, cx, areaY - scroll, cw, prog, areaY, areaY + areaH, now);
            } else {
                this.renderPlaceholder(graphics, category, cx, areaY, cw, areaH, prog, now);
            }
        } else if (searching) {
            modules = new ArrayList();
            for (Module m : this.ctx.guiModules()) {
                if (!m.name().toLowerCase(Locale.ROOT).contains(query)) continue;
                modules.add(m);
            }
        } else {
            modules = new ArrayList<Module>(this.ctx.guiModules().stream().filter(module -> module.category() == category).toList());
        }
        if (!modules.isEmpty()) {
            modules = new ArrayList(modules);
            modules.sort(Comparator.comparing(Module::name, String.CASE_INSENSITIVE_ORDER));
        }
        if (searching && modules.isEmpty()) {
            Text.of(Fonts.MEDIUM, "\u041d\u0438\u0447\u0435\u0433\u043e \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e", cx + cw * 0.5f, areaY + areaH * 0.4f, 5.6f).centered().color(Color.rgb(120, 125, 136).withAlpha(prog * GuiDraw.easeOut((float)(now - this.ctx.switchTime) / 300.0f))).render(graphics);
        }
        for (Module module2 : modules) {
            module2.updateAnimation(delta);
            int col = placed++ % 2;
            float x = cx + (float)col * (colW + 5.0f);
            float appear = GuiDraw.easeOut((float)(now - this.ctx.switchTime - Math.min((long)(index / 2) * 10L, 90L)) / 160.0f);
            float h = this.ctx.moduleCard.renderCard(graphics, module2, x, colY[col] + (1.0f - appear) * 10.0f, colW, prog * appear, areaY, areaY + areaH);
            int n = col;
            colY[n] = colY[n] + (h + 5.0f);
            ++index;
        }
        Scissor.pop(graphics);
        this.ctx.hits.clearClip();
        float contentH = special ? specialH : Math.max(colY[0], colY[1]) + scroll - areaY - 5.0f + 10.0f;
        this.ctx.maxScroll = Math.max(0.0f, contentH - areaH);
        this.ctx.scrollTarget = Math.clamp(this.ctx.scrollTarget, 0.0f, this.ctx.maxScroll);
        this.ctx.scrollbar.renderScrollbar(graphics, cx + fullW - 2.5f, areaY, areaH, contentH, scroll, prog, delta);
    }

    private void renderPlaceholder(GuiGraphicsExtractor graphics, Category category, float x, float y, float w, float h, float prog, long now) {
        float t = GuiDraw.easeOut((float)(now - this.ctx.switchTime) / 400.0f);
        float a = prog * t;
        float cy = y + h * 0.42f + (1.0f - t) * 6.0f;
        Text.of(Fonts.ICONS, category.icon(), x + w * 0.5f, cy - 10.0f, 16.0f).centered().color(Color.rgb(90, 95, 106).withAlpha(a)).render(graphics);
        Text.of(Fonts.MEDIUM, category.displayName(), x + w * 0.5f, cy + 6.0f, 6.4f).centered().color(Color.rgb(200, 204, 214).withAlpha(a)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u0420\u0430\u0437\u0434\u0435\u043b \u0441\u043a\u043e\u0440\u043e \u043f\u043e\u044f\u0432\u0438\u0442\u0441\u044f", x + w * 0.5f, cy + 14.0f, 5.0f).centered().color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
    }
}

