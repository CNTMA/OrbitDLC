/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.tab;

import brokke.cc.system.scripting.mcp.McpClients;
import brokke.cc.system.scripting.mcp.McpServer;
import brokke.cc.system.scripting.mcp.McpSetup;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.ui.clickgui.tab.ScriptsTab;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class McpPage
extends GuiComponent {
    private final Map<McpSetup.Kind, Integer> mcpStates = new EnumMap<McpSetup.Kind, Integer>(McpSetup.Kind.class);
    private long mcpScanTime = 0L;

    public McpPage(ClickGuiContext ctx) {
        super(ctx);
    }

    public void scanMcp(boolean force) {
        long t = System.currentTimeMillis();
        if (!force && t - this.mcpScanTime < 2000L) {
            return;
        }
        this.mcpScanTime = t;
        String url = McpServer.get().url();
        for (McpClients.Client c : McpClients.ALL) {
            if (c.kind() == McpSetup.Kind.MANUAL) continue;
            this.mcpStates.put(c.kind(), McpSetup.configured(c.kind(), url) ? 2 : (McpSetup.detected(c.kind()) ? 1 : 0));
        }
    }

    private boolean ensureMcpRunning() {
        if (!McpServer.get().isRunning()) {
            McpServer.get().start(false);
        }
        if (!McpServer.get().isRunning()) {
            this.ctx.configToast("\u0421\u0435\u0440\u0432\u0435\u0440 \u043d\u0435 \u0437\u0430\u043f\u0443\u0441\u0442\u0438\u043b\u0441\u044f, \u0441\u043c\u0435\u043d\u0438 \u043f\u043e\u0440\u0442", Color.rgb(235, 110, 110));
            return false;
        }
        return true;
    }

    public float renderMcp(GuiGraphicsExtractor graphics, float x, float y, float w, float a, Color accent, float top, float bottom, long now) {
        float startY = y;
        this.scanMcp(false);
        McpServer server = McpServer.get();
        boolean running = server.isRunning();
        float barH = 14.0f;
        float backW = Text.of(Fonts.MEDIUM, "\u041d\u0430\u0437\u0430\u0434").size(5.0f).width() + 18.0f;
        this.ctx.widgets.renderEventPill(graphics, "mcp-back", "\u041d\u0430\u0437\u0430\u0434", "<", x, y, backW, barH, a, false, top, bottom, () -> {
            ScriptsTab.scriptsMcp = false;
            this.ctx.switchTime = System.currentTimeMillis();
            this.ctx.scrollTarget = 0.0f;
            ClientSound.ui(ClientSound.Ui.CLICK);
        });
        Text.of(Fonts.MEDIUM, "\u0418\u0418 \u043f\u0438\u0448\u0435\u0442 \u0441\u043a\u0440\u0438\u043f\u0442\u044b \u0437\u0430 \u0442\u0435\u0431\u044f", x + backW + 6.0f, y + barH * 0.5f, 5.4f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(150, 155, 166).withAlpha(a)).render(graphics);
        float cardH = 25.0f;
        RoundedRect.of(x, y += barH + 7.0f, w, cardH).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round(6.0f * a))).render(graphics);
        float on = this.anim("mcp-running", running, 7.0f);
        Color dot = GuiDraw.mix(Color.rgb(110, 115, 126), Color.rgb(90, 210, 140), on);
        RoundedRect.of(x + 7.0f, y + 6.0f, 4.0f, 4.0f).radius(2.0f).color(dot.withAlpha(a)).render(graphics);
        Text.of(Fonts.MEDIUM, running ? "\u0421\u0435\u0440\u0432\u0435\u0440 \u0440\u0430\u0431\u043e\u0442\u0430\u0435\u0442" : "\u0421\u0435\u0440\u0432\u0435\u0440 \u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d", x + 14.0f, y + 8.0f, 5.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 238, 245).withAlpha(a)).render(graphics);
        Object info = running ? server.url() : "\u041f\u043e\u0440\u0442 " + server.port();
        float swW = 16.0f;
        float swH = 8.5f;
        float copyW = Text.of(Fonts.MEDIUM, "\u041a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u0442\u044c").size(4.6f).width() + 9.0f;
        Text.of(Fonts.MEDIUM, GuiDraw.fit((String)info, 4.5f, w - copyW - swW - 30.0f), x + 7.0f, y + cardH - 7.0f, 4.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(120, 125, 136).withAlpha(a)).render(graphics);
        float swX = x + w - swW - 7.0f;
        float swY = y + (cardH - swH) * 0.5f;
        GuiDraw.renderSwitch(graphics, swX, swY, swW, swH, on, accent, a);
        if (GuiDraw.canHit(swY, swH, top, bottom)) {
            this.hit(swX - 2.0f, swY - 2.0f, swW + 4.0f, swH + 4.0f, () -> {
                server.toggle();
                this.scanMcp(true);
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, null);
        }
        float cbx = swX - copyW - 6.0f;
        float cby = y + (cardH - 10.0f) * 0.5f;
        this.ctx.widgets.renderMcpButton(graphics, "mcp-copy", "\u041a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u0442\u044c", cbx, cby, copyW, 10.0f, a, accent, top, bottom, () -> {
            if (!this.ensureMcpRunning()) {
                return;
            }
            this.ctx.minecraft().keyboardHandler.setClipboard(server.url());
            this.ctx.configToast("\u0421\u0441\u044b\u043b\u043a\u0430 \u0441\u043a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u043d\u0430", Color.rgb(200, 205, 215));
        });
        float bw = (w - 5.0f) * 0.5f;
        float bh = 15.0f;
        this.ctx.widgets.renderConfigButton(graphics, "mcp-all", "\u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0438\u0442\u044c \u0432\u0441\u0435", "q", x, y += cardH + 5.0f, bw, bh, a, false, top, bottom, () -> {
            if (!this.ensureMcpRunning()) {
                return;
            }
            this.scanMcp(true);
            int n = 0;
            for (McpClients.Client c : McpClients.ALL) {
                if (c.kind() == McpSetup.Kind.MANUAL || this.mcpStates.getOrDefault((Object)c.kind(), 0) == 0 || !McpSetup.configure(c.kind(), server.url()).ok()) continue;
                ++n;
            }
            this.scanMcp(true);
            this.ctx.configToast((String)(n > 0 ? "\u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u043e \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c: " + n + ". \u041f\u0435\u0440\u0435\u0437\u0430\u043f\u0443\u0441\u0442\u0438 \u0438\u0445" : "\u041d\u0435 \u043d\u0430\u0448\u0451\u043b \u043d\u0438 \u043e\u0434\u043d\u043e\u0439 \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u044b"), n > 0 ? Color.rgb(90, 210, 140) : Color.rgb(235, 110, 110));
        });
        this.ctx.widgets.renderConfigButton(graphics, "mcp-none", "\u041e\u0442\u043a\u043b\u044e\u0447\u0438\u0442\u044c \u0432\u0441\u0435", "x", x + bw + 5.0f, y, bw, bh, a, true, top, bottom, () -> {
            int n = McpSetup.removeAll();
            this.scanMcp(true);
            this.ctx.configToast((String)(n > 0 ? "\u041e\u0442\u043a\u043b\u044e\u0447\u0435\u043d\u043e \u043e\u0442 " + n + " \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c" : "\u041d\u0438\u0447\u0435\u0433\u043e \u043d\u0435 \u0431\u044b\u043b\u043e \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u043e"), Color.rgb(235, 140, 140));
        });
        y += bh + 3.0f;
        float toast = 1.0f - GuiDraw.easeOut(Math.max(0.0f, (float)(now - this.ctx.configToastTime - 2500L) / 500.0f));
        float toastIn = GuiDraw.easeOut((float)(now - this.ctx.configToastTime) / 250.0f);
        if (this.ctx.configToastText != null && toast > 0.01f) {
            Text.of(Fonts.MEDIUM, GuiDraw.fit(this.ctx.configToastText, 4.8f, w - 4.0f), x + 2.0f + (1.0f - toastIn) * 4.0f, y + 3.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(this.ctx.configToastColor.withAlpha(a * toast * toastIn)).render(graphics);
        }
        y += 9.0f;
        y = GuiDraw.sectionTitle(graphics, "\u041f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u044b", x, y, a);
        float rowH = 20.0f;
        float gap = 3.0f;
        int i = 0;
        for (McpClients.Client c : McpClients.ALL) {
            float t = GuiDraw.easeOut((float)(now - this.ctx.switchTime - (long)i * 25L) / 320.0f);
            this.renderMcpRow(graphics, c, x, y + (1.0f - t) * 6.0f, w, rowH, a * t, accent, top, bottom);
            y += rowH + gap;
            ++i;
        }
        return y - gap - startY;
    }

    private void renderMcpRow(GuiGraphicsExtractor graphics, McpClients.Client c, float x, float y, float w, float h, float a, Color accent, float top, float bottom) {
        String status;
        int state;
        String key = "mcp-row-" + String.valueOf((Object)c.kind());
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        float hv = this.anim(key, this.hov(key, hover), 7.0f);
        RoundedRect.of(x, y, w, h).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round((6.0f + 4.0f * hv) * a))).render(graphics);
        boolean manual = c.kind() == McpSetup.Kind.MANUAL;
        int n = state = manual ? 1 : this.mcpStates.getOrDefault((Object)c.kind(), 0);
        String label = manual ? "\u0421\u0441\u044b\u043b\u043a\u0430" : (state == 2 ? "\u041e\u0442\u043a\u043b\u044e\u0447\u0438\u0442\u044c" : "\u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0438\u0442\u044c");
        float bw = Text.of(Fonts.MEDIUM, label).size(4.6f).width() + 9.0f;
        float bx = x + w - bw - 6.0f;
        float by = y + (h - 10.0f) * 0.5f;
        Text.of(Fonts.MEDIUM, c.name(), x + 7.0f, y + 7.0f, 5.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 238, 245).withAlpha(a * (state == 0 ? 0.6f : 1.0f))).render(graphics);
        status = manual ? "\u041b\u044e\u0431\u0430\u044f \u0434\u0440\u0443\u0433\u0430\u044f \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u0430" : (state == 2 ? "\u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u043e" : state == 1 ? "\u041d\u0430\u0439\u0434\u0435\u043d\u043e" : "\u041d\u0435 \u0443\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u043e");
        Color sc = state == 2 && !manual ? Color.rgb(90, 210, 140) : (state == 1 ? accent : Color.rgb(105, 110, 122));
        float sw = Text.of(Fonts.MEDIUM, status).size(4.4f).width();
        Text.of(Fonts.MEDIUM, status, x + 7.0f, y + h - 6.0f, 4.4f).verticalAlign(VerticalAlign.MIDDLE).color(sc.withAlpha(a)).render(graphics);
        Text.of(Fonts.MEDIUM, GuiDraw.fit(c.models(), 4.4f, bx - x - sw - 18.0f), x + 11.0f + sw, y + h - 6.0f, 4.4f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(100, 105, 116).withAlpha(a)).render(graphics);
        if (state == 0) {
            return;
        }
        this.ctx.widgets.renderMcpButton(graphics, String.valueOf(key) + "-btn", label, bx, by, bw, 10.0f, a, state == 2 && !manual ? Color.rgb(235, 90, 90) : accent, top, bottom, () -> {
            if (state == 2 && !manual) {
                McpSetup.Result r = McpSetup.remove(c.kind());
                this.ctx.configToast((String)(r.ok() ? c.name() + " \u043e\u0442\u043a\u043b\u044e\u0447\u0451\u043d" : r.message()), Color.rgb(235, 140, 140));
            } else {
                if (!this.ensureMcpRunning()) {
                    return;
                }
                if (manual) {
                    this.ctx.minecraft().keyboardHandler.setClipboard(McpServer.get().url());
                    this.ctx.configToast(McpSetup.afterHint(c.kind()) + " (\u0441\u043a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u043d\u043e)", Color.rgb(200, 205, 215));
                } else {
                    McpSetup.Result r = McpSetup.configure(c.kind(), McpServer.get().url());
                    this.ctx.configToast(r.message(), r.ok() ? Color.rgb(90, 210, 140) : Color.rgb(235, 110, 110));
                }
            }
            this.scanMcp(true);
        });
    }
}

