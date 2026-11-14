/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.multiplayer.ServerData
 */
package brokke.cc.ui.clickgui.tab;

import brokke.cc.system.bot.client.BotPhase;
import brokke.cc.system.bot.client.HeadlessBots;
import brokke.cc.system.bot.client.HeadlessBots_SavedBot;
import brokke.cc.ui.clickgui.NewClickGuiScreen;
import brokke.cc.ui.clickgui.bots.BotsPanel;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
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
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ServerData;

public final class BotsTab
extends GuiComponent {
    public static final Map<String, String> BOT_FIELDS = new HashMap<String, String>();
    private static int botTab = 0;
    private float botTabAnim = botTab;
    public long botSwitchTime = 0L;
    private float botOnlineBar = 0.0f;
    private static int botBatch = 3;

    public BotsTab(ClickGuiContext ctx) {
        super(ctx);
    }

    private static String botValue(String id) {
        if (BOT_FIELDS.isEmpty()) {
            String addr = HeadlessBots.getLastAddress();
            if (addr == null || addr.isEmpty()) {
                ServerData current = Minecraft.getInstance().getCurrentServer();
                addr = current != null && current.ip != null && !current.ip.isEmpty() ? current.ip : "mc.holyworld.ru";
            }
            BOT_FIELDS.put("bot:server", addr);
            String prx = HeadlessBots.getLastProxy();
            BOT_FIELDS.put("bot:proxy", prx == null ? "" : prx);
            String last = HeadlessBots.getLastNameInput();
            BOT_FIELDS.put("bot:name", last != null && !last.isEmpty() ? last : BotsPanel.generateNick());
            BOT_FIELDS.put("bot:prefix", "");
        }
        return BOT_FIELDS.getOrDefault(id, "");
    }

    public static String[] botHost() {
        String server = BotsTab.botValue("bot:server").trim();
        String host = "mc.holyworld.ru";
        String port = "25565";
        if (!server.isEmpty()) {
            String[] parts = server.split(":");
            host = parts[0];
            if (parts.length > 1) {
                port = parts[1];
            }
        }
        return new String[]{host, port};
    }

    public static int botPort() {
        try {
            return Integer.parseInt(BotsTab.botHost()[1]);
        }
        catch (NumberFormatException e) {
            return 25565;
        }
    }

    public void botToast(String text, boolean ok) {
        this.ctx.configToast(text, ok ? Color.rgb(90, 210, 140) : Color.rgb(235, 110, 110));
    }

    private void addBot(String name) {
        HeadlessBots.add(name);
        String proxy = BotsTab.botValue("bot:proxy").trim();
        if (!proxy.isEmpty()) {
            HeadlessBots.setProxy(name, proxy);
        }
    }

    public float renderBots(GuiGraphicsExtractor graphics, float x, float y, float w, float prog, float top, float bottom, long now) {
        float startY = y;
        float appear = GuiDraw.easeOut((float)(now - this.ctx.switchTime) / 380.0f);
        float a = prog * appear;
        y += (1.0f - appear) * 8.0f;
        Color accent = GuiDraw.getAccent();
        String[] tabs = new String[]{"\u0411\u043e\u0442\u044b", "\u0421\u043e\u0437\u0434\u0430\u0442\u044c"};
        int[] counts = new int[]{HeadlessBots.savedBots().size(), -1};
        float tabH = 14.0f;
        float tabsWidth = w - 62.0f;
        float tabW = (tabsWidth - 4.0f) / (float)tabs.length;
        float previewToggleX = x + tabsWidth + 4.0f;
        boolean previewHover = GuiDraw.canHit(y, tabH + 4.0f, top, bottom) && this.ctx.hovered(previewToggleX, y, 58.0f, tabH + 4.0f);
        NewClickGuiScreen cfr_ignored_0 = this.ctx.screen;
        RoundedRect.of(previewToggleX, y, 58.0f, tabH + 4.0f).radius(4.0f).color((NewClickGuiScreen.botPreviewEnabled ? accent : Color.WHITE).withAlpha(a * (previewHover ? 0.22f : 0.12f))).render(graphics);
        NewClickGuiScreen cfr_ignored_1 = this.ctx.screen;
        NewClickGuiScreen cfr_ignored_2 = this.ctx.screen;
        Text.of(Fonts.MEDIUM, NewClickGuiScreen.botPreviewEnabled ? "\u041f\u0440\u0435\u0432\u044c\u044e: \u0432\u043a\u043b" : "\u041f\u0440\u0435\u0432\u044c\u044e: \u0432\u044b\u043a\u043b", previewToggleX + 29.0f, y + (tabH + 4.0f) * 0.5f, 5.0f).centered().verticalAlign(VerticalAlign.MIDDLE).color((NewClickGuiScreen.botPreviewEnabled ? accent : Color.rgb(150, 155, 165)).withAlpha(a)).render(graphics);
        if (GuiDraw.canHit(y, tabH + 4.0f, top, bottom)) {
            this.hit(previewToggleX, y, 58.0f, tabH + 4.0f, () -> {
                NewClickGuiScreen cfr_ignored_3 = this.ctx.screen;
                NewClickGuiScreen cfr_ignored_4 = this.ctx.screen;
                NewClickGuiScreen.botPreviewEnabled = !NewClickGuiScreen.botPreviewEnabled;
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, null);
        }
        RoundedRect.of(x, y, tabsWidth, tabH + 4.0f).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round(6.0f * a))).render(graphics);
        float tabPos = this.botTabAnim;
        this.botTabAnim = this.ctx.smooth(this.botTabAnim, botTab, 14.0f);
        RoundedRect.of(x + 2.0f + tabPos * tabW, y + 2.0f, tabW, tabH).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round(14.0f * a))).render(graphics);
        for (int i = 0; i < tabs.length; ++i) {
            int idx = i;
            float tx = x + 2.0f + (float)i * tabW;
            boolean hover = GuiDraw.canHit(y, tabH, top, bottom) && this.ctx.hovered(tx, y + 2.0f, tabW, tabH);
            String key = "bot-tab-" + i;
            float hv = this.ctx.anim(key, this.ctx.hov(key, hover), 7.0f);
            float act = Math.clamp(1.0f - Math.abs(tabPos - (float)i), 0.0f, 1.0f);
            float lw = Text.of(Fonts.MEDIUM, tabs[i]).size(5.4f).width();
            String count = counts[i] < 0 ? "" : String.valueOf(counts[i]);
            float cw = count.isEmpty() ? 0.0f : Text.of(Fonts.MEDIUM, count).size(4.6f).width() + 4.0f;
            float lx = tx + (tabW - lw - cw) * 0.5f;
            Text.of(Fonts.MEDIUM, tabs[i], lx, y + 2.0f + tabH * 0.5f, 5.4f).verticalAlign(VerticalAlign.MIDDLE).color(GuiDraw.mix(Color.rgb(135, 140, 150), Color.rgb(240, 242, 248), Math.max(act, hv * 0.5f)).withAlpha(a)).render(graphics);
            if (!count.isEmpty()) {
                Text.of(Fonts.MEDIUM, count, lx + lw + 4.0f, y + 2.0f + tabH * 0.5f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(GuiDraw.mix(Color.rgb(105, 110, 120), accent, act).withAlpha(a)).render(graphics);
            }
            if (!GuiDraw.canHit(y, tabH + 4.0f, top, bottom)) continue;
            this.hit(tx, y + 2.0f, tabW, tabH, () -> {
                if (botTab != idx) {
                    this.ctx.focus.setAbField(null, "");
                    botTab = idx;
                    this.botSwitchTime = System.currentTimeMillis();
                    this.ctx.scrollTarget = 0.0f;
                    ClientSound.ui(ClientSound.Ui.CLICK);
                }
            }, null);
        }
        y = botTab == 0 ? this.renderBotList(graphics, x, y, w, a, accent, top, bottom, now) : this.renderBotCreate(graphics, x, y += tabH + 4.0f + 6.0f, w, a, accent, top, bottom, now);
        return y - startY;
    }

    private float renderBotToast(GuiGraphicsExtractor graphics, float x, float y, float w, float a, long now) {
        float toast = 1.0f - GuiDraw.easeOut(Math.max(0.0f, (float)(now - this.ctx.configToastTime - 2200L) / 500.0f));
        float toastIn = GuiDraw.easeOut((float)(now - this.ctx.configToastTime) / 250.0f);
        if (this.ctx.configToastText != null && toast > 0.01f) {
            Text.of(Fonts.MEDIUM, GuiDraw.fit(this.ctx.configToastText, 4.8f, w - 4.0f), x + 2.0f + (1.0f - toastIn) * 4.0f, y + 3.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(this.ctx.configToastColor.withAlpha(a * toast * toastIn)).render(graphics);
        }
        return y + 9.0f;
    }

    private float renderBotList(GuiGraphicsExtractor graphics, float x, float y, float w, float a, Color accent, float top, float bottom, long now) {
        float appear = GuiDraw.easeOut((float)(now - this.botSwitchTime) / 320.0f);
        List<HeadlessBots_SavedBot> saved = HeadlessBots.savedBots();
        float h = 15.0f;
        float bw = 60.0f;
        this.ctx.widgets.renderAbField(graphics, "bot:server", "\u0410\u0434\u0440\u0435\u0441 \u0441\u0435\u0440\u0432\u0435\u0440\u0430...", "C", x, y += (1.0f - appear) * 6.0f, w - bw * 2.0f - 8.0f, h, a *= appear, accent, top, bottom, now, BotsTab.botValue("bot:server"));
        this.ctx.widgets.renderConfigButton(graphics, "bot-join-all", "\u0412\u043e\u0439\u0442\u0438 \u0432\u0441\u0435\u043c", "q", x + w - bw * 2.0f - 4.0f, y, bw, h, a, false, top, bottom, () -> {
            if (saved.isEmpty()) {
                this.botToast("\u041d\u0435\u0442 \u0431\u043e\u0442\u043e\u0432 \u0434\u043b\u044f \u0432\u0445\u043e\u0434\u0430", false);
                return;
            }
            List<String> names = saved.stream().map(HeadlessBots_SavedBot::name).toList();
            HeadlessBots.connectAll(names, BotsTab.botHost()[0], BotsTab.botPort());
            this.botToast("\u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 " + names.size() + " \u0431\u043e\u0442\u043e\u0432...", true);
        });
        this.ctx.widgets.renderConfigButton(graphics, "bot-leave-all", "\u0412\u044b\u0439\u0442\u0438 \u0432\u0441\u0435\u043c", "x", x + w - bw, y, bw, h, a, true, top, bottom, () -> {
            HeadlessBots.disconnectAll();
            this.botToast("\u0412\u0441\u0435 \u0431\u043e\u0442\u044b \u043e\u0442\u043a\u043b\u044e\u0447\u0435\u043d\u044b", true);
        });
        long online = HeadlessBots.all().stream().filter(b -> b.getPhase() == BotPhase.PLAY).count();
        Text.of(Fonts.MEDIUM, "\u0412 \u0438\u0433\u0440\u0435 " + online + " \u0438\u0437 " + saved.size(), x + 1.0f, (y += h + 5.0f) + 4.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(150, 155, 166).withAlpha(a)).render(graphics);
        float k = saved.isEmpty() ? 0.0f : (float)online / (float)saved.size();
        this.botOnlineBar = this.smooth(this.botOnlineBar, k, 8.0f);
        RoundedRect.of(x, y += 10.0f, w, 3.0f).radius(1.5f).color(Color.rgba(255, 255, 255, Math.round(8.0f * a))).render(graphics);
        if (this.botOnlineBar > 0.005f) {
            RoundedRect.of(x, y, Math.max(3.0f, w * this.botOnlineBar), 3.0f).radius(1.5f).horizontalGradient(accent.withAlpha(a), GuiDraw.getAccent2().withAlpha(a)).render(graphics);
        }
        y += 6.0f;
        y = this.renderBotToast(graphics, x, y, w, a, now);
        if (saved.isEmpty()) {
            Text.of(Fonts.MEDIUM, "\u0411\u043e\u0442\u043e\u0432 \u043f\u043e\u043a\u0430 \u043d\u0435\u0442", x + w * 0.5f, y + 12.0f, 5.6f).centered().color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
            float cw = 80.0f;
            this.ctx.widgets.renderConfigButton(graphics, "bot-go-create", "\u0421\u043e\u0437\u0434\u0430\u0442\u044c \u0431\u043e\u0442\u0430", "D", x + (w - cw) * 0.5f, y += 22.0f, cw, 15.0f, a, false, top, bottom, () -> {
                botTab = 1;
                this.botSwitchTime = System.currentTimeMillis();
            });
            return y + 15.0f;
        }
        float gap = 3.0f;
        for (int i = 0; i < saved.size(); ++i) {
            float t = GuiDraw.easeOut((float)(now - this.botSwitchTime - (long)Math.min(i, 20) * 22L) / 320.0f);
            float rh = this.ctx.botRows.renderBotRow(graphics, saved.get(i), x, y + (1.0f - t) * 6.0f, w, a * t, accent, top, bottom, now);
            y += rh + gap;
        }
        return y - gap;
    }

    private float renderBotCreate(GuiGraphicsExtractor graphics, float x, float y, float w, float a, Color accent, float top, float bottom, long now) {
        float appear = GuiDraw.easeOut((float)(now - this.botSwitchTime) / 320.0f);
        y += (1.0f - appear) * 6.0f;
        float h = 15.0f;
        y = GuiDraw.sectionTitle(graphics, "\u041d\u043e\u0432\u044b\u0439 \u0431\u043e\u0442", x, y, a *= appear);
        float rw = 58.0f;
        this.ctx.widgets.renderAbField(graphics, "bot:name", "\u041d\u0438\u043a \u0431\u043e\u0442\u0430...", "D", x, y, w - rw - 4.0f, h, a, accent, top, bottom, now, BotsTab.botValue("bot:name"));
        this.ctx.widgets.renderConfigButton(graphics, "bot-random", "\u0421\u043b\u0443\u0447\u0430\u0439\u043d\u044b\u0439", "T", x + w - rw, y, rw, h, a, false, top, bottom, () -> {
            this.ctx.focus.setAbField(null, "");
            BOT_FIELDS.put("bot:name", BotsPanel.generateNick());
        });
        float colW = (w - 5.0f) * 0.5f;
        this.ctx.widgets.renderAbField(graphics, "bot:server", "\u0410\u0434\u0440\u0435\u0441 \u0441\u0435\u0440\u0432\u0435\u0440\u0430...", "C", x, y += h + 4.0f, colW, h, a, accent, top, bottom, now, BotsTab.botValue("bot:server"));
        this.ctx.widgets.renderAbField(graphics, "bot:proxy", "\u041f\u0440\u043e\u043a\u0441\u0438 (\u043d\u0435\u043e\u0431\u044f\u0437\u0430\u0442\u0435\u043b\u044c\u043d\u043e)", "G", x + colW + 5.0f, y, colW, h, a, accent, top, bottom, now, BotsTab.botValue("bot:proxy"));
        this.ctx.widgets.renderConfigButton(graphics, "bot-add", "\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c", "D", x, y += h + 5.0f, colW, h, a, false, top, bottom, () -> {
            this.ctx.focus.setAbField(null, "");
            String n = BotsTab.botValue("bot:name").trim();
            if (n.isEmpty()) {
                this.botToast("\u0412\u0432\u0435\u0434\u0438 \u043d\u0438\u043a \u0431\u043e\u0442\u0430", false);
                return;
            }
            this.addBot(n);
            HeadlessBots.setLastNameInput(n);
            BOT_FIELDS.put("bot:name", BotsPanel.generateNick());
            this.botToast("\u0411\u043e\u0442 " + n + " \u0441\u043e\u0445\u0440\u0430\u043d\u0451\u043d", true);
        });
        float bx = x + colW + 5.0f;
        boolean bHover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(bx, y, colW, h);
        float bhv = this.anim("bot-add-join", this.hov("bot-add-join", bHover), 7.0f);
        RoundedRect.of(bx, y, colW, h).radius(4.0f).horizontalGradient(accent.withAlpha((0.55f + 0.35f * bhv) * a), GuiDraw.getAccent2().withAlpha((0.55f + 0.35f * bhv) * a)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0438 \u0432\u043e\u0439\u0442\u0438", bx + colW * 0.5f, y + h * 0.5f, 5.0f).centered().color(Color.rgb(255, 255, 255).withAlpha(a)).render(graphics);
        if (GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(bx, y, colW, h, () -> {
                this.ctx.focus.setAbField(null, "");
                String n = BotsTab.botValue("bot:name").trim();
                if (n.isEmpty()) {
                    this.botToast("\u0412\u0432\u0435\u0434\u0438 \u043d\u0438\u043a \u0431\u043e\u0442\u0430", false);
                    return;
                }
                this.addBot(n);
                HeadlessBots.setLastNameInput(n);
                HeadlessBots.setLastAddress(BotsTab.botValue("bot:server").trim());
                HeadlessBots.setLastProxy(BotsTab.botValue("bot:proxy").trim());
                HeadlessBots.connect(n, BotsTab.botHost()[0], BotsTab.botPort());
                BOT_FIELDS.put("bot:name", BotsPanel.generateNick());
                this.botToast("\u0411\u043e\u0442 " + n + " \u0434\u043e\u0431\u0430\u0432\u043b\u0435\u043d \u0438 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f", true);
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, null);
        }
        y += h + 3.0f;
        y = this.renderBotToast(graphics, x, y, w, a, now);
        y += 2.0f;
        y = GuiDraw.sectionTitle(graphics, "\u041f\u0430\u0447\u043a\u0430 \u0431\u043e\u0442\u043e\u0432", x, y, a);
        this.ctx.widgets.renderAbField(graphics, "bot:prefix", "\u041f\u0440\u0435\u0444\u0438\u043a\u0441 (\u043f\u0443\u0441\u0442\u043e - \u0441\u043b\u0443\u0447\u0430\u0439\u043d\u044b\u0435 \u043d\u0438\u043a\u0438)", "J", x, y, w, h, a, accent, top, bottom, now, BotsTab.botValue("bot:prefix"));
        float sh = 17.0f;
        this.ctx.widgets.renderAbStepper(graphics, "bot-count", "\u041a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e", String.valueOf(botBatch), x, y += h + 4.0f, colW, a, accent, top, bottom, () -> {
            botBatch = Math.max(1, botBatch - 1);
        }, () -> {
            botBatch = Math.min(25, botBatch + 1);
        });
        float gx = x + colW + 5.0f;
        boolean gHover = GuiDraw.canHit(y, sh, top, bottom) && this.hovered(gx, y, colW, sh);
        float ghv = this.anim("bot-batch", this.hov("bot-batch", gHover), 7.0f);
        RoundedRect.of(gx, y, colW, sh).radius(4.5f).horizontalGradient(accent.withAlpha((0.55f + 0.35f * ghv) * a), GuiDraw.getAccent2().withAlpha((0.55f + 0.35f * ghv) * a)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u0421\u043e\u0437\u0434\u0430\u0442\u044c \u0438 \u0432\u043e\u0439\u0442\u0438 (" + botBatch + ")", gx + colW * 0.5f, y + sh * 0.5f, 5.0f).centered().color(Color.rgb(255, 255, 255).withAlpha(a)).render(graphics);
        if (GuiDraw.canHit(y, sh, top, bottom)) {
            this.hit(gx, y, colW, sh, () -> {
                this.ctx.focus.setAbField(null, "");
                String pfx = BotsTab.botValue("bot:prefix").trim();
                Random r = new Random();
                ArrayList<String> created = new ArrayList<String>();
                for (int i = 0; i < botBatch; ++i) {
                    String n = pfx.isEmpty() ? BotsPanel.generateNick() : pfx + (10 + r.nextInt(990));
                    this.addBot(n);
                    created.add(n);
                }
                HeadlessBots.connectAll(created, BotsTab.botHost()[0], BotsTab.botPort());
                this.botToast("\u0421\u043e\u0437\u0434\u0430\u043d\u043e \u0438 \u0437\u0430\u043f\u0443\u0449\u0435\u043d\u043e " + created.size() + " \u0431\u043e\u0442\u043e\u0432", true);
                botTab = 0;
                this.botSwitchTime = System.currentTimeMillis();
                this.ctx.scrollTarget = 0.0f;
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, null);
        }
        Text.of(Fonts.MEDIUM, "\u041e\u0441\u0442\u0430\u0432\u044c \u043f\u0440\u043e\u043a\u0441\u0438 \u043f\u0443\u0441\u0442\u044b\u043c \u0434\u043b\u044f \u043f\u0440\u044f\u043c\u043e\u0433\u043e \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u044f", x + w * 0.5f, (y += sh + 6.0f) + 3.0f, 4.4f).centered().color(Color.rgb(95, 100, 111).withAlpha(a)).render(graphics);
        return y + 8.0f;
    }
}

