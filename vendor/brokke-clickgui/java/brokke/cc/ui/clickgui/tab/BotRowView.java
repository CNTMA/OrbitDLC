/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.tab;

import brokke.cc.core.module.impl.render.NameProtect;
import brokke.cc.core.setting.StringSetting;
import brokke.cc.system.bot.client.BotClient;
import brokke.cc.system.bot.client.BotPhase;
import brokke.cc.system.bot.client.HeadlessBots;
import brokke.cc.system.bot.client.HeadlessBots_ProxyPingSnapshot;
import brokke.cc.system.bot.client.HeadlessBots_SavedBot;
import brokke.cc.system.bot.modules.api.BotModule;
import brokke.cc.system.bot.net.BotAttach;
import brokke.cc.system.bot.net.BotProxy;
import brokke.cc.system.bot.via.BotProtocolVersions_Entry;
import brokke.cc.system.bot.via.BotVia;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.scissor.Scissor;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class BotRowView
extends GuiComponent {
    public final Map<String, StringSetting> textSettings = new HashMap<String, StringSetting>();
    public String botExpanded = null;

    public BotRowView(ClickGuiContext ctx) {
        super(ctx);
    }

    public float renderBotRow(GuiGraphicsExtractor graphics, HeadlessBots_SavedBot bot, float x, float y, float w, float a, Color accent, float top, float bottom, long now) {
        String anarchy;
        String coins;
        boolean hasProxy;
        String name = bot.name();
        String displayName = NameProtect.protect(name);
        BotClient client = HeadlessBots.get(name);
        boolean connected = client != null && client.getPhase() != BotPhase.DISCONNECTED;
        boolean inGame = client != null && client.getPhase() == BotPhase.PLAY;
        String key = "bot-row-" + name;
        float headH = 26.0f;
        List<BotProtocolVersions_Entry> versions = BotVia.availableVersions();
        List<String> proxies = HeadlessBots.getProxyPool();
        float drawerH = this.botDrawerHeight(name, versions, proxies, w);
        float ex = this.ctx.anim(String.valueOf(key) + "-exp", name.equals(this.botExpanded), 6.0f);
        float h = headH + drawerH * ex;
        if (y > bottom || y + h < top) {
            return h;
        }
        boolean hover = GuiDraw.canHit(y, headH, top, bottom) && this.ctx.hovered(x, y, w, headH);
        float hv = this.ctx.anim(key, this.ctx.hov(key, hover), 7.0f);
        float play = this.ctx.anim(String.valueOf(key) + "-play", inGame, 6.0f);
        RoundedRect.of(x, y, w, h).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round((6.0f + 4.0f * hv) * a))).render(graphics);
        if (GuiDraw.canHit(y, headH, top, bottom)) {
            this.hit(x, y, w, headH, null, () -> {
                this.botExpanded = name.equals(this.botExpanded) ? null : name;
                ClientSound.ui(this.botExpanded != null ? ClientSound.Ui.EXPAND : ClientSound.Ui.COLLAPSE);
            });
        }
        float av = 16.0f;
        float avX = x + 6.0f;
        float avY = y + (headH - av) * 0.5f;
        RoundedRect.of(avX, avY, av, av).radius(av * 0.5f).color(Color.rgb(50, 53, 62).withAlpha(a)).render(graphics);
        if (play > 0.01f) {
            RoundedRect.of(avX, avY, av, av).radius(av * 0.5f).horizontalGradient(accent.withAlpha(play * a), GuiDraw.getAccent2().withAlpha(play * a)).render(graphics);
        }
        Text.of(Fonts.MEDIUM, displayName.isEmpty() ? "B" : displayName.substring(0, 1).toUpperCase(Locale.ROOT), avX + av * 0.5f, avY + av * 0.5f, 6.2f).centered().color(Color.rgb(255, 255, 255).withAlpha(a)).render(graphics);
        Color dot = inGame ? Color.rgb(90, 210, 140) : (connected ? Color.rgb(230, 200, 100) : Color.rgb(110, 115, 126));
        RoundedRect.of(avX + av - 4.5f, avY + av - 4.5f, 5.0f, 5.0f).radius(2.5f).color(Color.rgb(28, 30, 36).withAlpha(a)).render(graphics);
        RoundedRect.of(avX + av - 3.5f, avY + av - 3.5f, 3.0f, 3.0f).radius(1.5f).color(dot.withAlpha(a)).render(graphics);
        float bh = 11.0f;
        float by = y + 5.5f;
        float bx = x + w - 6.0f;
        float delW = 11.0f;
        float delX = bx -= delW;
        boolean delHover = GuiDraw.canHit(by, bh, top, bottom) && this.ctx.hovered(delX, by, delW, bh);
        float dhv = this.ctx.anim(String.valueOf(key) + "-del", this.ctx.hov(String.valueOf(key) + "-del", delHover), 8.0f);
        RoundedRect.of(delX, by, delW, bh).radius(3.0f).color(Color.rgb(255, 80, 80).withAlpha((0.04f + 0.2f * dhv) * a)).render(graphics);
        Color xc = GuiDraw.mix(Color.rgb(130, 135, 146), Color.rgb(255, 110, 110), dhv).withAlpha(a);
        float xcx = delX + delW * 0.5f;
        float xcy = by + bh * 0.5f;
        GuiDraw.stroke(graphics, xcx - 1.8f, xcy - 1.8f, xcx + 1.8f, xcy + 1.8f, 0.8f, xc);
        GuiDraw.stroke(graphics, xcx - 1.8f, xcy + 1.8f, xcx + 1.8f, xcy - 1.8f, 0.8f, xc);
        String connText = connected ? "\u0412\u044b\u0439\u0442\u0438" : "\u0412\u043e\u0439\u0442\u0438";
        float cw = Text.of(Fonts.MEDIUM, connText).size(4.6f).width() + 12.0f;
        float connX = bx -= cw + 3.0f;
        this.ctx.widgets.renderMcpButton(graphics, String.valueOf(key) + "-conn", connText, connX, by, cw, bh, a, connected ? Color.rgb(235, 90, 90) : accent, top, bottom, () -> {
            if (connected) {
                HeadlessBots.disconnect(name);
                this.ctx.bots.botToast("\u0411\u043e\u0442 " + name + " \u043e\u0442\u043a\u043b\u044e\u0447\u0451\u043d", true);
            } else {
                ClickGuiContext clickGuiContext = this.ctx;
                ClickGuiContext clickGuiContext2 = this.ctx;
                HeadlessBots.connect(name, clickGuiContext.bots.botHost()[0], clickGuiContext2.bots.botPort());
                this.ctx.bots.botToast("\u0411\u043e\u0442 " + name + " \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f...", true);
            }
        });
        if (client != null) {
            float jw = Text.of(Fonts.MEDIUM, "\u0423\u043f\u0440\u0430\u0432\u043b\u044f\u0442\u044c").size(4.6f).width() + 12.0f;
            this.ctx.widgets.renderMcpButton(graphics, String.valueOf(key) + "-attach", "\u0423\u043f\u0440\u0430\u0432\u043b\u044f\u0442\u044c", bx -= jw + 3.0f, by, jw, bh, a, accent, top, bottom, () -> {
                this.ctx.screen.onClose();
                BotAttach.attach(client);
            });
        }
        Runnable toggleDrawer = () -> {
            this.botExpanded = name.equals(this.botExpanded) ? null : name;
            ClientSound.ui(this.botExpanded != null ? ClientSound.Ui.EXPAND : ClientSound.Ui.COLLAPSE);
        };
        if (client != null) {
            int enabledCount = BotAttach.enabledFunctions(client).size();
            Object modText = enabledCount > 0 ? "\u0424\u0443\u043d\u043a\u0446\u0438\u0438 " + enabledCount : "\u0424\u0443\u043d\u043a\u0446\u0438\u0438";
            float mw = Text.of(Fonts.MEDIUM, (String)modText).size(4.6f).width() + 10.0f;
            this.renderBotChip(graphics, String.valueOf(key) + "-mod", (String)modText, bx -= mw + 3.0f, by, mw, bh, a, accent, enabledCount > 0, top, bottom, toggleDrawer);
        }
        String prxText = (hasProxy = HeadlessBots.hasProxy(name)) ? "\u041f\u0440\u043e\u043a\u0441\u0438" : "\u0411\u0435\u0437 \u043f\u0440\u043e\u043a\u0441\u0438";
        float pw = Text.of(Fonts.MEDIUM, prxText).size(4.6f).width() + 10.0f;
        this.renderBotChip(graphics, String.valueOf(key) + "-prx", prxText, bx -= pw + 3.0f, by, pw, bh, a, accent, hasProxy, top, bottom, toggleDrawer);
        String verText = BotVia.label(HeadlessBots.getProtocolVersion(name));
        float vw = Text.of(Fonts.MEDIUM, verText).size(4.6f).width() + 10.0f;
        this.renderBotChip(graphics, String.valueOf(key) + "-ver", verText, bx -= vw + 3.0f, by, vw, bh, a, accent, HeadlessBots.getProtocolVersion(name) > 0, top, bottom, toggleDrawer);
        float tx = avX + av + 6.0f;
        float maxW = bx - tx - 6.0f;
        Text.of(Fonts.MEDIUM, GuiDraw.fit(displayName, 5.8f, maxW), tx, y + 9.0f, 5.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(240, 242, 248).withAlpha(a)).render(graphics);
        String string = coins = inGame ? client.getCoinsString() : null;
        Object status = inGame ? ((anarchy = client.getAnarchyString()) == null ? "\u0412 \u0438\u0433\u0440\u0435" : "\u0412 \u0438\u0433\u0440\u0435 - " + anarchy) : (connected ? (client.getPhase() == null ? "\u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435..." : "\u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 (" + client.getPhase().name().toLowerCase(Locale.ROOT) + ")") : "\u041d\u0435 \u0432 \u0441\u0435\u0442\u0438");
        float statusWidth = x + w - tx - 6.0f;
        float coinsWidth = coins == null ? 0.0f : Text.of(Fonts.MEDIUM, coins).size(4.6f).width() + 5.0f;
        String shownStatus = GuiDraw.fit((String)status, 4.6f, Math.max(0.0f, statusWidth - coinsWidth));
        Text.of(Fonts.MEDIUM, shownStatus, tx, y + 18.0f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(dot.withAlpha(a)).render(graphics);
        if (coins != null) {
            float coinsX = tx + Text.of(Fonts.MEDIUM, shownStatus).size(4.6f).width() + 5.0f;
            Text.of(Fonts.MEDIUM, GuiDraw.fit(coins, 4.6f, x + w - coinsX - 6.0f), coinsX, y + 18.0f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(255, 213, 79).withAlpha(a)).render(graphics);
        }
        if (GuiDraw.canHit(y, headH, top, bottom)) {
            this.hit(delX, by, delW, bh, () -> {
                HeadlessBots.remove(name);
                if (name.equals(this.botExpanded)) {
                    this.botExpanded = null;
                }
                this.ctx.bots.botToast("\u0411\u043e\u0442 " + name + " \u0443\u0434\u0430\u043b\u0451\u043d", true);
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, null);
        }
        if (ex > 0.01f) {
            float dTop = Math.max(y + headH, top);
            float dBottom = Math.min(y + h, bottom);
            if (dBottom - dTop >= 1.0f) {
                Scissor.push(graphics, x, dTop, w, dBottom - dTop);
                this.renderBotDrawer(graphics, name, versions, proxies, x + 7.0f, y + headH, w - 14.0f, a * ex, accent, ex > 0.95f ? dTop : Float.MAX_VALUE, dBottom);
                Scissor.pop(graphics);
            }
        }
        return h;
    }

    private float chipsHeight(List<String> labels, float w) {
        float chipH = 11.0f;
        float cx = 0.0f;
        float rows = 1.0f;
        for (String l : labels) {
            float cw = Text.of(Fonts.MEDIUM, l).size(4.6f).width() + 10.0f;
            if (cx + cw > w && cx > 0.0f) {
                cx = 0.0f;
                rows += 1.0f;
            }
            cx += cw + 3.0f;
        }
        return rows * (chipH + 3.0f) - 3.0f;
    }

    private List<String> proxyLabels(List<String> proxies) {
        ArrayList<String> out = new ArrayList<String>();
        out.add("\u0411\u0435\u0437 \u043f\u0440\u043e\u043a\u0441\u0438");
        Iterator<String> iterator = proxies.iterator();
        while (iterator.hasNext()) {
            String p;
            HeadlessBots_ProxyPingSnapshot ping = HeadlessBots.getProxyPing(p = iterator.next());
            out.add(p + (String)(ping != null ? "  " + ping.label() : ""));
        }
        return out;
    }

    private List<BotModule> botModules(String name) {
        BotClient client = HeadlessBots.get(name);
        if (client == null || client.getModules() == null) {
            return List.of();
        }
        ArrayList<BotModule> modules = new ArrayList<BotModule>(client.getModules().getModules());
        modules.sort(Comparator.comparing(BotModule::getName, String.CASE_INSENSITIVE_ORDER));
        return modules;
    }

    private float botModulesHeight(String name, float w) {
        return 10.0f + this.chipsHeight(this.botFunctionLabels(name), w - 14.0f) + 6.0f;
    }

    private float botDrawerHeight(String name, List<BotProtocolVersions_Entry> versions, List<String> proxies, float w) {
        ArrayList<String> vl = new ArrayList<String>();
        for (BotProtocolVersions_Entry e : versions) {
            vl.add(e.label());
        }
        return 54.0f + this.botModulesHeight(name, w) + 2.0f + 10.0f + this.chipsHeight(vl, w - 14.0f) + 6.0f + 10.0f + this.chipsHeight(this.proxyLabels(proxies), w - 14.0f) + 8.0f;
    }

    private void renderBotDrawer(GuiGraphicsExtractor graphics, String name, List<BotProtocolVersions_Entry> versions, List<String> proxies, float x, float y, float w, float a, Color accent, float top, float bottom) {
        y += 2.0f;
        List<BotModule> modules = this.botModules(name);
        y = this.renderBotConnectionField(graphics, name, false, x, y, w, a, accent, top, bottom);
        y = this.renderBotConnectionField(graphics, name, true, x, y, w, a, accent, top, bottom);
        Text.of(Fonts.MEDIUM, "\u0412\u043a\u043b\u044e\u0447\u0451\u043d\u043d\u044b\u0435 \u0444\u0443\u043d\u043a\u0446\u0438\u0438 - \u0443\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0447\u0435\u0440\u0435\u0437 GUI \u0431\u043e\u0442\u0430", x, y + 4.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(140, 145, 156).withAlpha(a)).render(graphics);
        y += 10.0f;
        float statusX = x;
        List<String> enabledFunctions = BotAttach.enabledFunctions(HeadlessBots.get(name));
        for (String label : this.botFunctionLabels(name)) {
            float cw = Text.of(Fonts.MEDIUM, label).size(4.6f).width() + 10.0f;
            if (statusX + cw > x + w && statusX > x) {
                statusX = x;
                y += 14.0f;
            }
            this.renderBotChip(graphics, "bot-status-" + name + "-" + label, label, statusX, y, cw, 11.0f, a, accent, enabledFunctions.contains(label), top, bottom, null);
            statusX += cw + 3.0f;
        }
        Text.of(Fonts.MEDIUM, "\u0412\u0435\u0440\u0441\u0438\u044f", x, (y += 17.0f) + 4.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(140, 145, 156).withAlpha(a)).render(graphics);
        y += 10.0f;
        int current = HeadlessBots.getProtocolVersion(name);
        float chipH = 11.0f;
        float cx = x;
        for (BotProtocolVersions_Entry e : versions) {
            float cw = Text.of(Fonts.MEDIUM, e.label()).size(4.6f).width() + 10.0f;
            if (cx + cw > x + w && cx > x) {
                cx = x;
                y += chipH + 3.0f;
            }
            boolean sel = e.protocol() == current;
            this.renderBotChip(graphics, "bot-ver-" + name + "-" + e.protocol(), e.label(), cx, y, cw, chipH, a, accent, sel, top, bottom, () -> {
                HeadlessBots.setProtocolVersion(name, e.protocol());
                HeadlessBots.savePersistentState();
                BotClient online = HeadlessBots.get(name);
                this.ctx.bots.botToast("\u0412\u0435\u0440\u0441\u0438\u044f " + name + ": " + e.label() + (online != null ? " (\u043f\u043e\u0441\u043b\u0435 \u043f\u0435\u0440\u0435\u0437\u0430\u0445\u043e\u0434\u0430)" : ""), true);
            });
            cx += cw + 3.0f;
        }
        Text.of(Fonts.MEDIUM, "\u041f\u0440\u043e\u043a\u0441\u0438", x, (y += chipH + 6.0f) + 4.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(140, 145, 156).withAlpha(a)).render(graphics);
        y += 10.0f;
        String currentProxy = HeadlessBots.getProxy(name);
        List<String> labels = this.proxyLabels(proxies);
        cx = x;
        for (int i = 0; i < labels.size(); ++i) {
            String label = labels.get(i);
            String proxy = i == 0 ? null : proxies.get(i - 1);
            float cw = Text.of(Fonts.MEDIUM, label).size(4.6f).width() + 10.0f;
            if (cx + cw > x + w && cx > x) {
                cx = x;
                y += chipH + 3.0f;
            }
            boolean sel = proxy == null ? currentProxy == null || currentProxy.isBlank() : proxy.equals(currentProxy);
            this.renderBotChip(graphics, "bot-prx-" + name + "-" + i, label, cx, y, cw, chipH, a, accent, sel, top, bottom, () -> {
                HeadlessBots.setProxy(name, proxy);
                this.ctx.bots.botToast(proxy == null ? name + ": \u0431\u0435\u0437 \u043f\u0440\u043e\u043a\u0441\u0438" : name + ": \u043f\u0440\u043e\u043a\u0441\u0438 " + proxy, true);
            });
            cx += cw + 3.0f;
        }
    }

    private void renderBotChip(GuiGraphicsExtractor graphics, Object key, String text, float x, float y, float w, float h, float a, Color accent, boolean selected, float top, float bottom, Runnable action) {
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.ctx.hovered(x, y, w, h);
        float hv = this.ctx.anim(key, this.ctx.hov(key, hover), 8.0f);
        float sel = this.ctx.anim(String.valueOf(key) + "-sel", selected, 8.0f);
        RoundedRect.of(x, y, w, h).radius(3.0f).color(Color.rgba(255, 255, 255, Math.round((9.0f + 6.0f * hv) * (1.0f - sel) * a))).render(graphics);
        if (sel > 0.01f) {
            RoundedRect.of(x, y, w, h).radius(3.0f).horizontalGradient(accent.withAlpha(0.75f * sel * a), GuiDraw.getAccent2().withAlpha(0.75f * sel * a)).render(graphics);
        }
        Text.of(Fonts.MEDIUM, text, x + w * 0.5f, y + h * 0.5f, 4.6f).centered().color(GuiDraw.mix(Color.rgb(165, 170, 180), Color.rgb(255, 255, 255), Math.max(sel, hv * 0.6f)).withAlpha(a)).render(graphics);
        if (action != null && GuiDraw.canHit(y, h, top, bottom)) {
            Runnable click = () -> {
                action.run();
                ClientSound.ui(ClientSound.Ui.CLICK);
            };
            boolean drawer = key.toString().endsWith("-mod") || key.toString().endsWith("-prx") || key.toString().endsWith("-ver");
            this.hit(x, y, w, h, drawer ? null : click, drawer ? click : null);
        }
    }

    private List<String> botFunctionLabels(String name) {
        List<String> labels = BotAttach.enabledFunctions(HeadlessBots.get(name));
        return labels.isEmpty() ? List.of("\u041d\u0438\u0447\u0435\u0433\u043e \u043d\u0435 \u0432\u043a\u043b\u044e\u0447\u0435\u043d\u043e") : labels;
    }

    private float renderBotConnectionField(GuiGraphicsExtractor graphics, String name, boolean proxy, float x, float y, float w, float alpha, Color accent, float top, float bottom) {
        String id = "bot-setting:" + name + (proxy ? ":proxy" : ":address");
        StringSetting setting = this.textSettings.get(id);
        if (setting == null) {
            setting = new StringSetting(proxy ? "\u041f\u0440\u043e\u043a\u0441\u0438" : "\u0410\u0434\u0440\u0435\u0441 \u0441\u0435\u0440\u0432\u0435\u0440\u0430", "", proxy ? Objects.toString(HeadlessBots.getProxy(name), "") : HeadlessBots.getServerAddress(name), 256);
            setting.onChange(value -> {
                try {
                    if (proxy) {
                        BotProxy.parse(value);
                        HeadlessBots.setProxy(name, value);
                    } else {
                        HeadlessBots.setServerAddress(name, value);
                    }
                }
                catch (IllegalArgumentException error) {
                    this.ctx.bots.botToast("\u041d\u0435\u0432\u0435\u0440\u043d\u044b\u0439 \u0430\u0434\u0440\u0435\u0441 \u043f\u0440\u043e\u043a\u0441\u0438", false);
                }
            });
            this.textSettings.put(id, setting);
        }
        Text.of(Fonts.MEDIUM, proxy ? "\u0421\u0432\u043e\u0439 \u043f\u0440\u043e\u043a\u0441\u0438 (host:port \u0438\u043b\u0438 user:pass@host:port)" : "\u0421\u0435\u0440\u0432\u0435\u0440 \u044d\u0442\u043e\u0433\u043e \u0431\u043e\u0442\u0430 (\u043f\u0443\u0441\u0442\u043e - \u043e\u0431\u0449\u0438\u0439 \u0430\u0434\u0440\u0435\u0441)", x, y + 4.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(140, 145, 156).withAlpha(alpha)).render(graphics);
        if (!id.equals(this.ctx.focus.abField)) {
            String stored;
            String string = stored = proxy ? Objects.toString(HeadlessBots.getProxy(name), "") : HeadlessBots.getServerAddress(name);
            if (!stored.equals(setting.get())) {
                setting.setValue(stored);
            }
        }
        this.ctx.widgets.renderAbField(graphics, id, proxy ? "\u0411\u0435\u0437 \u043f\u0440\u043e\u043a\u0441\u0438" : "\u0410\u0434\u0440\u0435\u0441 \u0441\u0435\u0440\u0432\u0435\u0440\u0430...", null, x, y + 9.0f, w, 13.0f, alpha, accent, top, bottom, System.currentTimeMillis(), setting.get());
        return y + 27.0f;
    }
}

