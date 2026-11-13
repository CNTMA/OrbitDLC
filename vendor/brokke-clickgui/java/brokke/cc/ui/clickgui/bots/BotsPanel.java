/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.input.CharacterEvent
 *  net.minecraft.client.input.KeyEvent
 *  net.minecraft.client.multiplayer.ServerData
 */
package brokke.cc.ui.clickgui.bots;

import brokke.cc.core.module.impl.render.GuiSettings;
import brokke.cc.core.setting.BooleanSetting;
import brokke.cc.core.setting.ButtonSetting;
import brokke.cc.core.setting.FloatSetting;
import brokke.cc.core.setting.ModeSetting;
import brokke.cc.core.setting.NumberSetting;
import brokke.cc.core.setting.Setting;
import brokke.cc.system.bot.client.BotClient;
import brokke.cc.system.bot.client.BotPhase;
import brokke.cc.system.bot.client.ChatMessage;
import brokke.cc.system.bot.client.HeadlessBots;
import brokke.cc.system.bot.client.HeadlessBots_ProxyPingSnapshot;
import brokke.cc.system.bot.client.HeadlessBots_ProxyPingStatus;
import brokke.cc.system.bot.client.HeadlessBots_SavedBot;
import brokke.cc.system.bot.modules.BotAutoBuy;
import brokke.cc.system.bot.modules.BotModuleManager;
import brokke.cc.system.bot.modules.api.BotModule;
import brokke.cc.system.bot.net.BotAttach;
import brokke.cc.system.bot.via.BotProtocolVersions_Entry;
import brokke.cc.system.bot.via.BotVia;
import brokke.cc.util.Log;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.DisplayUtil;
import brokke.cc.util.render.display.outline.Outline;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.scissor.Scissor;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.TextAlign;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ServerData;

public class BotsPanel {
    private static final float TABS_H = 13.0f;
    private static final String[] TAB_LABELS = new String[]{"\u0411\u043e\u0442\u044b", "\u0421\u043e\u0437\u0434\u0430\u0442\u044c"};
    private BotModuleManager previewModules;
    private float scrollOffset = 0.0f;
    private float scrollTarget = 0.0f;
    private float maxScroll = 0.0f;
    private int currentTab = 0;
    private String nameInput = "";
    private String serverInput = "";
    private String proxyInput = "";
    private int focusedInput = -1;
    private String batchPrefix = "";
    private String batchCountInput = "3";
    private String chatInput = "";
    private String feedbackText = "";
    private Color feedbackColor = Color.rgb(46, 204, 113);
    private long feedbackTime = 0L;
    private final List<float[]> clickBoxes = new ArrayList<float[]>();
    private final List<Runnable> clickActions = new ArrayList<Runnable>();
    private final List<float[]> middleBoxes = new ArrayList<float[]>();
    private final List<Runnable> middleActions = new ArrayList<Runnable>();
    private Setting<?> draggingSlider = null;
    private float dragSliderTrackX = 0.0f;
    private float dragSliderTrackW = 0.0f;
    private final Map<String, Float> moduleExpandProgress = new HashMap<String, Float>();
    private String versionPickerBot;
    private float versionPickerY;
    private String modulePickerBot;
    private float modulePickerY;
    private String chatViewBot;
    private float chatViewY;
    private String proxyPickerBot;
    private float proxyPickerY;
    private static final String[] NICK_ROOTS = new String[]{"Dark", "Shadow", "Frost", "Storm", "Night", "Wolf", "Fox", "Raven", "Ghost", "Blaze", "Venom", "Toxic", "Lucky", "Silent", "Crazy", "Sweet", "Pixel", "Neon", "Cyber", "Iron", "Steel", "Fire", "Ice", "Moon", "Sun", "Star", "Sky", "Rain", "Snow", "Wild", "Kira", "Max", "Alex", "Nik", "Dima", "Vlad", "Artem", "Ilya", "Egor", "Misha", "Sasha", "Pasha", "Kostya", "Danya", "Roma", "Lesha", "Vanya", "Tema", "Kirill", "Denis"};
    private static final String[] NICK_TAILS = new String[]{"Hunter", "Killer", "Master", "King", "Lord", "Boy", "Pro", "Gamer", "Craft", "Miner", "Rider", "Slayer", "Wolf", "Cat", "Dragon", "Knight", "Ninja", "Sniper", "Rage", "Zone"};
    private static final String NICK_VOWELS = "aeiouy";
    private static final String NICK_CONSONANTS = "bcdfgklmnprstvxz";

    public BotsPanel() {
        this.initDefaults();
    }

    private void initDefaults() {
        String lastName;
        ServerData current;
        String lastAddr = HeadlessBots.getLastAddress();
        this.serverInput = lastAddr != null && !lastAddr.isEmpty() ? lastAddr : ((current = Minecraft.getInstance().getCurrentServer()) != null && current.ip != null && !current.ip.isEmpty() ? current.ip : "mc.holyworld.ru");
        String lastPrx = HeadlessBots.getLastProxy();
        if (lastPrx != null) {
            this.proxyInput = lastPrx;
        }
        this.nameInput = (lastName = HeadlessBots.getLastNameInput()) != null && !lastName.isEmpty() ? lastName : BotsPanel.generateNick();
    }

    private Color getAccent() {
        GuiSettings settings = GuiSettings.getInstance();
        return settings != null ? settings.getThemePrimary() : Color.rgb(40, 140, 255);
    }

    public void resetScroll() {
        this.scrollTarget = 0.0f;
        this.scrollOffset = 0.0f;
        this.maxScroll = 0.0f;
    }

    public void setFeedback(String text, boolean success) {
        this.feedbackText = text;
        this.feedbackColor = success ? Color.rgb(46, 204, 113) : Color.rgb(235, 87, 87);
        this.feedbackTime = System.currentTimeMillis();
    }

    public void render(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float mouseX, float mouseY, float delta) {
        float contentY;
        this.clickBoxes.clear();
        this.clickActions.clear();
        this.middleBoxes.clear();
        this.middleActions.clear();
        float tabsY = y;
        float tabGap = 3.5f;
        float curTabX = x;
        int i = 0;
        while (i < TAB_LABELS.length) {
            String label = TAB_LABELS[i];
            float tw = Text.of(Fonts.MEDIUM, label).size(5.8f).width();
            float tabW = tw + 14.0f;
            boolean active = this.currentTab == i;
            boolean hov = mouseX >= curTabX && mouseX <= curTabX + tabW && mouseY >= tabsY && mouseY <= tabsY + 13.0f;
            RoundedRect.of(curTabX, tabsY, tabW, 13.0f).radius(3.0f).color(active ? this.getAccent().withAlpha(28) : (hov ? Color.rgba(255, 255, 255, 12) : Color.rgba(255, 255, 255, 5))).render(graphics);
            if (active) {
                Outline.of(curTabX, tabsY, tabW, 13.0f).radius(3.0f).thickness(0.6f).color(this.getAccent().withAlpha(140)).render(graphics);
            }
            Text.of(Fonts.MEDIUM, label, curTabX + tabW * 0.5f, tabsY + 6.5f, 5.8f).centered().verticalAlign(VerticalAlign.MIDDLE).color(active ? Color.WHITE : (hov ? Color.rgb(205, 210, 225) : Color.rgb(130, 135, 150))).render(graphics);
            int tabIdx = i++;
            this.addClickBox(curTabX, tabsY, tabW, 13.0f, () -> {
                if (this.currentTab != tabIdx) {
                    this.currentTab = tabIdx;
                    this.resetScroll();
                    this.focusedInput = -1;
                }
            });
            curTabX += tabW + tabGap;
        }
        if (System.currentTimeMillis() - this.feedbackTime < 3500L && !this.feedbackText.isEmpty()) {
            float fade = 1.0f - Math.max(0.0f, (float)(System.currentTimeMillis() - this.feedbackTime - 2500L) / 1000.0f);
            int alpha = (int)(255.0f * fade);
            float fbW = Text.of(Fonts.MEDIUM, this.feedbackText).size(5.6f).width() + 10.0f;
            float fbX = x + w - fbW;
            RoundedRect.of(fbX, tabsY, fbW, 13.0f).radius(3.0f).color(this.feedbackColor.withAlpha((int)(35.0f * fade))).render(graphics);
            Outline.of(fbX, tabsY, fbW, 13.0f).radius(3.0f).thickness(0.6f).color(this.feedbackColor.withAlpha((int)(120.0f * fade))).render(graphics);
            Text.of(Fonts.MEDIUM, this.feedbackText, fbX + fbW * 0.5f, tabsY + 6.5f, 5.6f).centered().verticalAlign(VerticalAlign.MIDDLE).color(this.feedbackColor.withAlpha(alpha)).render(graphics);
        }
        float viewportY = tabsY + 13.0f + 4.0f;
        float viewportH = h - 17.0f;
        if (this.scrollTarget < 0.0f) {
            this.scrollTarget += (0.0f - this.scrollTarget) * 0.2f;
        } else if (this.scrollTarget > this.maxScroll && this.maxScroll > 0.0f) {
            this.scrollTarget += (this.maxScroll - this.scrollTarget) * 0.2f;
        }
        this.scrollOffset += (this.scrollTarget - this.scrollOffset) * 0.25f;
        float scroll = this.scrollOffset;
        Scissor.push(graphics, x, viewportY, w, viewportH);
        float finalContentBottom = contentY = viewportY - scroll;
        if (this.currentTab >= TAB_LABELS.length) {
            this.currentTab = 0;
        }
        switch (this.currentTab) {
            case 0: {
                finalContentBottom = this.renderBotsTab(graphics, x, contentY, w, viewportH, mouseX, mouseY);
                break;
            }
            case 1: {
                finalContentBottom = this.renderCreateTab(graphics, x, contentY, w, viewportH, mouseX, mouseY);
                break;
            }
            case 2: {
                finalContentBottom = this.renderModulesTab(graphics, x, contentY, w, viewportH, mouseX, mouseY);
                break;
            }
            case 3: {
                finalContentBottom = this.renderChatTab(graphics, x, contentY, w, viewportH, mouseX, mouseY);
            }
        }
        Scissor.pop(graphics);
        for (float[] box : this.middleBoxes) {
            float boxBottom = Math.min(box[1] + box[3], viewportY + viewportH);
            box[1] = Math.max(box[1], viewportY);
            box[3] = Math.max(0.0f, boxBottom - box[1]);
        }
        float totalHeight = finalContentBottom - contentY;
        this.maxScroll = Math.max(0.0f, totalHeight - viewportH);
        if (this.maxScroll > 1.0f) {
            float trackX = x + w - 3.0f;
            float trackY = viewportY + 2.0f;
            float trackH = viewportH - 4.0f;
            float thumbH = Math.max(14.0f, viewportH / totalHeight * trackH);
            float thumbProgress = Math.clamp(scroll / this.maxScroll, 0.0f, 1.0f);
            float thumbY = trackY + thumbProgress * (trackH - thumbH);
            RoundedRect.of(trackX, trackY, 2.0f, trackH).radius(1.0f).color(Color.rgba(255, 255, 255, 10)).render(graphics);
            RoundedRect.of(trackX, thumbY, 2.0f, thumbH).radius(1.0f).color(this.getAccent().withAlpha(160)).render(graphics);
        }
    }

    private float renderBotsTab(GuiGraphicsExtractor graphics, float x, float y, float w, float vpH, float mouseX, float mouseY) {
        float curY = y;
        float barH = 17.0f;
        RoundedRect.of(x, curY, w, barH).radius(4.0f).color(Color.rgba(14, 16, 22, 120)).render(graphics);
        Outline.of(x, curY, w, barH).radius(4.0f).thickness(0.6f).color(Color.rgba(255, 255, 255, 14)).render(graphics);
        float btnW1 = 70.0f;
        float btnH = 12.0f;
        float btnY = curY + (barH - btnH) * 0.5f;
        float btnX1 = x + 3.5f;
        boolean hov1 = mouseX >= btnX1 && mouseX <= btnX1 + btnW1 && mouseY >= btnY && mouseY <= btnY + btnH;
        RoundedRect.of(btnX1, btnY, btnW1, btnH).radius(2.5f).color(hov1 ? Color.rgba(46, 204, 113, 45) : Color.rgba(46, 204, 113, 25)).render(graphics);
        Outline.of(btnX1, btnY, btnW1, btnH).radius(2.5f).thickness(0.6f).color(Color.rgba(46, 204, 113, 110)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u0412\u043e\u0439\u0442\u0438 \u0432\u0441\u0435\u043c", btnX1 + btnW1 * 0.5f, btnY + btnH * 0.5f, 5.5f).centered().verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(46, 204, 113)).render(graphics);
        this.addClickBox(btnX1, btnY, btnW1, btnH, () -> {
            List<HeadlessBots_SavedBot> saved = HeadlessBots.savedBots();
            if (saved.isEmpty()) {
                this.setFeedback("\u041d\u0435\u0442 \u0431\u043e\u0442\u043e\u0432 \u0434\u043b\u044f \u0432\u0445\u043e\u0434\u0430", false);
                return;
            }
            String host = "mc.holyworld.ru";
            int port = 25565;
            if (this.serverInput != null && !this.serverInput.isBlank()) {
                String[] parts = this.serverInput.trim().split(":");
                host = parts[0];
                if (parts.length > 1) {
                    try {
                        port = Integer.parseInt(parts[1]);
                    }
                    catch (Exception ignored) {
                        Log.swallow(ignored);
                    }
                }
            }
            List<String> names = saved.stream().map(HeadlessBots_SavedBot::name).toList();
            HeadlessBots.connectAll(names, host, port);
            this.setFeedback("\u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 " + names.size() + " \u0431\u043e\u0442\u043e\u0432...", true);
        });
        float btnW2 = 72.0f;
        float btnX2 = btnX1 + btnW1 + 4.0f;
        boolean hov2 = mouseX >= btnX2 && mouseX <= btnX2 + btnW2 && mouseY >= btnY && mouseY <= btnY + btnH;
        RoundedRect.of(btnX2, btnY, btnW2, btnH).radius(2.5f).color(hov2 ? Color.rgba(235, 87, 87, 45) : Color.rgba(235, 87, 87, 25)).render(graphics);
        Outline.of(btnX2, btnY, btnW2, btnH).radius(2.5f).thickness(0.6f).color(Color.rgba(235, 87, 87, 110)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u0412\u044b\u0439\u0442\u0438 \u0432\u0441\u0435\u043c", btnX2 + btnW2 * 0.5f, btnY + btnH * 0.5f, 5.5f).centered().verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 87, 87)).render(graphics);
        this.addClickBox(btnX2, btnY, btnW2, btnH, () -> {
            HeadlessBots.disconnectAll();
            this.setFeedback("\u0412\u0441\u0435 \u0431\u043e\u0442\u044b \u043e\u0442\u043a\u043b\u044e\u0447\u0435\u043d\u044b", true);
        });
        int totalSaved = HeadlessBots.savedBots().size();
        long onlineCount = HeadlessBots.all().stream().filter(b -> b.getPhase() != BotPhase.DISCONNECTED).count();
        String statsStr = "\u0411\u043e\u0442\u043e\u0432: " + totalSaved + " \u2022 \u0412 \u0441\u0435\u0442\u0438: " + onlineCount;
        Text.of(Fonts.MEDIUM, statsStr, x + w - 6.0f, curY + barH * 0.5f, 5.8f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(130, 135, 150)).render(graphics);
        curY += barH + 4.5f;
        List<HeadlessBots_SavedBot> savedBots = HeadlessBots.savedBots();
        if (savedBots.isEmpty()) {
            float emptyH = 65.0f;
            RoundedRect.of(x, curY, w, emptyH).radius(4.0f).color(Color.rgba(14, 16, 22, 90)).render(graphics);
            Outline.of(x, curY, w, emptyH).radius(4.0f).thickness(0.6f).color(Color.rgba(255, 255, 255, 12)).render(graphics);
            Text.of(Fonts.MEDIUM, "\u0421\u043f\u0438\u0441\u043e\u043a \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u043d\u044b\u0445 \u0431\u043e\u0442\u043e\u0432 \u043f\u0443\u0441\u0442", x + w * 0.5f, curY + 22.0f, 7.0f).centered().verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(220, 224, 235)).render(graphics);
            Text.of(Fonts.MEDIUM, "\u041f\u0435\u0440\u0435\u0439\u0434\u0438\u0442\u0435 \u0432\u043e \u0432\u043a\u043b\u0430\u0434\u043a\u0443 '\u0421\u043e\u0437\u0434\u0430\u0442\u044c', \u0447\u0442\u043e\u0431\u044b \u0434\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0431\u043e\u0442\u0430", x + w * 0.5f, curY + 36.0f, 5.5f).centered().verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(115, 120, 135)).render(graphics);
            float goBtnW = 68.0f;
            float goBtnH = 12.0f;
            float goBtnX = x + (w - goBtnW) * 0.5f;
            float goBtnY = curY + 48.0f;
            boolean goHov = mouseX >= goBtnX && mouseX <= goBtnX + goBtnW && mouseY >= goBtnY && mouseY <= goBtnY + goBtnH;
            RoundedRect.of(goBtnX, goBtnY, goBtnW, goBtnH).radius(2.5f).color(this.getAccent().withAlpha(goHov ? 40 : 25)).render(graphics);
            Outline.of(goBtnX, goBtnY, goBtnW, goBtnH).radius(2.5f).thickness(0.6f).color(this.getAccent().withAlpha(120)).render(graphics);
            Text.of(Fonts.MEDIUM, "\u0421\u043e\u0437\u0434\u0430\u0442\u044c \u0431\u043e\u0442\u0430", goBtnX + goBtnW * 0.5f, goBtnY + goBtnH * 0.5f, 5.5f).centered().verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
            this.addClickBox(goBtnX, goBtnY, goBtnW, goBtnH, () -> {
                this.currentTab = 1;
                this.resetScroll();
            });
            return curY + emptyH;
        }
        float cardH = 26.0f;
        float cardGap = 3.0f;
        for (HeadlessBots_SavedBot bot : savedBots) {
            boolean connHov;
            boolean ctlHov;
            String an;
            String name = bot.name();
            BotClient client = HeadlessBots.get(name);
            boolean isOnline = client != null && client.getPhase() != BotPhase.DISCONNECTED;
            boolean inGame = client != null && client.getPhase() == BotPhase.PLAY;
            boolean cardHov = mouseX >= x && mouseX <= x + w && mouseY >= curY && mouseY <= curY + cardH;
            RoundedRect.of(x, curY, w, cardH).radius(3.5f).color(cardHov ? Color.rgba(18, 20, 28, 125) : Color.rgba(13, 14, 20, 95)).render(graphics);
            Outline.of(x, curY, w, cardH).radius(3.5f).thickness(0.6f).color(inGame ? this.getAccent().withAlpha(55) : Color.rgba(255, 255, 255, 14)).render(graphics);
            float avSize = 16.0f;
            float avX = x + 5.0f;
            float avY = curY + (cardH - avSize) * 0.5f;
            RoundedRect.of(avX, avY, avSize, avSize).radius(avSize * 0.5f).color(inGame ? this.getAccent() : Color.rgba(35, 38, 50, 200)).render(graphics);
            String initial = !name.isEmpty() ? name.substring(0, 1).toUpperCase(Locale.ROOT) : "B";
            Text.of(Fonts.MEDIUM, initial, avX + avSize * 0.5f, avY + avSize * 0.5f, 7.0f).centered().verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
            float dotSize = 4.0f;
            float dotX = avX + avSize - 2.5f;
            float dotY = avY + avSize - 2.5f;
            Color dotColor = inGame ? Color.rgb(35, 165, 90) : (isOnline ? Color.rgb(230, 175, 40) : Color.rgb(100, 105, 120));
            RoundedRect.of(dotX, dotY, dotSize, dotSize).radius(dotSize * 0.5f).color(dotColor).render(graphics);
            float textX = avX + avSize + 6.0f;
            Text.of(Fonts.MEDIUM, name, textX, curY + 8.0f, 6.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
            float chatBtnX = textX + Text.of(Fonts.MEDIUM, name).size(6.8f).width() + 4.0f;
            float chatBtnY = curY + 4.0f;
            float chatBtnW = 16.0f;
            float chatBtnH = 8.0f;
            boolean chatHov = mouseX >= chatBtnX && mouseX <= chatBtnX + chatBtnW && mouseY >= chatBtnY && mouseY <= chatBtnY + chatBtnH;
            boolean chatOpenNow = name.equals(this.chatViewBot);
            RoundedRect.of(chatBtnX, chatBtnY, chatBtnW, chatBtnH).radius(2.0f).color(chatOpenNow || chatHov ? this.getAccent().withAlpha(70) : Color.rgba(255, 255, 255, 12)).render(graphics);
            Text.of(Fonts.MEDIUM, "\u0427\u0430\u0442", chatBtnX + chatBtnW * 0.5f, chatBtnY + chatBtnH * 0.5f, 4.6f).centered().verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
            float cardBottom = curY + cardH;
            this.addClickBox(chatBtnX, chatBtnY, chatBtnW, chatBtnH, () -> {
                this.chatViewBot = name.equals(this.chatViewBot) ? null : name;
                this.chatViewY = cardBottom + 2.0f;
                this.versionPickerBot = null;
                this.proxyPickerBot = null;
            });
            Object statusLabel = inGame ? ((an = client.getAnarchyString()) != null ? "\u0412 \u0438\u0433\u0440\u0435 (" + an + ")" : "\u0412 \u0438\u0433\u0440\u0435") : (isOnline ? (client.getPhase() != null ? client.getPhase().name() : "\u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435...") : "\u041d\u0435 \u0432 \u0441\u0435\u0442\u0438");
            if (bot.proxy() != null && !bot.proxy().isBlank()) {
                statusLabel = (String)statusLabel + " \u2022 Proxy: " + bot.proxy();
            }
            Text.of(Fonts.MEDIUM, (String)statusLabel, textX, curY + 18.0f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(inGame ? Color.rgb(46, 204, 113) : Color.rgb(120, 125, 140)).render(graphics);
            float delSize = 12.0f;
            float delX = x + w - delSize - 4.0f;
            float delY = curY + (cardH - delSize) * 0.5f;
            boolean delHov = mouseX >= delX && mouseX <= delX + delSize && mouseY >= delY && mouseY <= delY + delSize;
            RoundedRect.of(delX, delY, delSize, delSize).radius(2.0f).color(delHov ? Color.rgba(235, 87, 87, 50) : Color.rgba(255, 255, 255, 8)).render(graphics);
            DisplayUtil.renderCross(graphics, delX + delSize * 0.5f, delY + delSize * 0.5f, 2.6f, 1.25f, delHov ? Color.rgb(255, 100, 100) : Color.rgb(130, 135, 150));
            this.addClickBox(delX, delY, delSize, delSize, () -> {
                HeadlessBots.remove(name);
                this.setFeedback("\u0411\u043e\u0442 '" + name + "' \u0443\u0434\u0430\u043b\u0435\u043d", true);
            });
            float ctlW = 46.0f;
            float ctlX = delX - ctlW - 3.5f;
            float ctlY = delY;
            boolean bl = ctlHov = mouseX >= ctlX && mouseX <= ctlX + ctlW && mouseY >= ctlY && mouseY <= ctlY + delSize;
            if (client != null) {
                RoundedRect.of(ctlX, ctlY, ctlW, delSize).radius(2.0f).color(ctlHov ? this.getAccent().withAlpha(50) : this.getAccent().withAlpha(25)).render(graphics);
                Outline.of(ctlX, ctlY, ctlW, delSize).radius(2.0f).thickness(0.5f).color(this.getAccent().withAlpha(120)).render(graphics);
                Text.of(Fonts.MEDIUM, "\u0417\u0430\u0439\u0442\u0438", ctlX + ctlW * 0.5f, ctlY + delSize * 0.5f, 5.0f).centered().verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
                this.addClickBox(ctlX, ctlY, ctlW, delSize, () -> {
                    Minecraft.getInstance().gui.setScreen(null);
                    BotAttach.attach(client);
                });
            }
            float connW = 38.0f;
            float connX = (client != null ? ctlX : delX) - connW - 3.5f;
            float connY = delY;
            boolean bl2 = connHov = mouseX >= connX && mouseX <= connX + connW && mouseY >= connY && mouseY <= connY + delSize;
            if (isOnline) {
                RoundedRect.of(connX, connY, connW, delSize).radius(2.0f).color(connHov ? Color.rgba(235, 87, 87, 40) : Color.rgba(235, 87, 87, 20)).render(graphics);
                Outline.of(connX, connY, connW, delSize).radius(2.0f).thickness(0.5f).color(Color.rgba(235, 87, 87, 100)).render(graphics);
                Text.of(Fonts.MEDIUM, "\u0412\u044b\u0439\u0442\u0438", connX + connW * 0.5f, connY + delSize * 0.5f, 5.0f).centered().verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 87, 87)).render(graphics);
                this.addClickBox(connX, connY, connW, delSize, () -> {
                    HeadlessBots.disconnect(name);
                    this.setFeedback("\u0411\u043e\u0442 '" + name + "' \u043e\u0442\u043a\u043b\u044e\u0447\u0435\u043d", true);
                });
            } else {
                RoundedRect.of(connX, connY, connW, delSize).radius(2.0f).color(connHov ? Color.rgba(46, 204, 113, 40) : Color.rgba(46, 204, 113, 20)).render(graphics);
                Outline.of(connX, connY, connW, delSize).radius(2.0f).thickness(0.5f).color(Color.rgba(46, 204, 113, 100)).render(graphics);
                Text.of(Fonts.MEDIUM, "\u0412\u043e\u0439\u0442\u0438", connX + connW * 0.5f, connY + delSize * 0.5f, 5.0f).centered().verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(46, 204, 113)).render(graphics);
                this.addClickBox(connX, connY, connW, delSize, () -> {
                    String host = "mc.holyworld.ru";
                    int port = 25565;
                    if (this.serverInput != null && !this.serverInput.isBlank()) {
                        String[] parts = this.serverInput.trim().split(":");
                        host = parts[0];
                        if (parts.length > 1) {
                            try {
                                port = Integer.parseInt(parts[1]);
                            }
                            catch (Exception ignored) {
                                Log.swallow(ignored);
                            }
                        }
                    }
                    HeadlessBots.connect(name, host, port);
                    this.setFeedback("\u0411\u043e\u0442 '" + name + "' \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f...", true);
                });
            }
            float verW = 40.0f;
            float verX = connX - verW - 3.5f;
            boolean verHov = mouseX >= verX && mouseX <= verX + verW && mouseY >= connY && mouseY <= connY + delSize;
            int botVersion = HeadlessBots.getProtocolVersion(name);
            RoundedRect.of(verX, connY, verW, delSize).radius(2.0f).color(verHov || name.equals(this.versionPickerBot) ? this.getAccent().withAlpha(50) : Color.rgba(255, 255, 255, 10)).render(graphics);
            Outline.of(verX, connY, verW, delSize).radius(2.0f).thickness(0.5f).color(botVersion > 0 ? this.getAccent().withAlpha(140) : Color.rgba(255, 255, 255, 30)).render(graphics);
            Text.of(Fonts.MEDIUM, BotVia.label(botVersion), verX + verW * 0.5f, connY + delSize * 0.5f, 5.0f).centered().verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
            this.addClickBox(verX, connY, verW, delSize, () -> {
                this.versionPickerBot = name.equals(this.versionPickerBot) ? null : name;
                this.proxyPickerBot = null;
                this.versionPickerY = connY + delSize + 2.0f;
            });
            float prxW = 30.0f;
            float prxX = verX - prxW - 3.5f;
            boolean prxHov = mouseX >= prxX && mouseX <= prxX + prxW && mouseY >= connY && mouseY <= connY + delSize;
            boolean hasPrx = HeadlessBots.hasProxy(name);
            RoundedRect.of(prxX, connY, prxW, delSize).radius(2.0f).color(prxHov || name.equals(this.proxyPickerBot) ? this.getAccent().withAlpha(50) : Color.rgba(255, 255, 255, 10)).render(graphics);
            Outline.of(prxX, connY, prxW, delSize).radius(2.0f).thickness(0.5f).color(hasPrx ? this.getAccent().withAlpha(140) : Color.rgba(255, 255, 255, 30)).render(graphics);
            Text.of(Fonts.MEDIUM, "\u041f\u0440\u043e\u043a\u0441\u0438", prxX + prxW * 0.5f, connY + delSize * 0.5f, 5.0f).centered().verticalAlign(VerticalAlign.MIDDLE).color(hasPrx ? Color.WHITE : Color.rgb(150, 155, 170)).render(graphics);
            this.addClickBox(prxX, connY, prxW, delSize, () -> {
                this.proxyPickerBot = name.equals(this.proxyPickerBot) ? null : name;
                this.versionPickerBot = null;
                this.proxyPickerY = connY + delSize + 2.0f;
            });
            if (client != null) {
                float modW = 34.0f;
                float modX = prxX - modW - 3.5f;
                boolean modHov = mouseX >= modX && mouseX <= modX + modW && mouseY >= connY && mouseY <= connY + delSize;
                int enabledCount = 0;
                for (BotModule m : client.getModules().getModules()) {
                    if (!m.isEnabled()) continue;
                    ++enabledCount;
                }
                RoundedRect.of(modX, connY, modW, delSize).radius(2.0f).color(modHov || name.equals(this.modulePickerBot) ? this.getAccent().withAlpha(50) : Color.rgba(255, 255, 255, 10)).render(graphics);
                Outline.of(modX, connY, modW, delSize).radius(2.0f).thickness(0.5f).color(enabledCount > 0 ? this.getAccent().withAlpha(140) : Color.rgba(255, 255, 255, 30)).render(graphics);
                Text.of(Fonts.MEDIUM, (String)(enabledCount > 0 ? "\u041c\u043e\u0434\u0443\u043b\u0438 " + enabledCount : "\u041c\u043e\u0434\u0443\u043b\u0438"), modX + modW * 0.5f, connY + delSize * 0.5f, 5.0f).centered().verticalAlign(VerticalAlign.MIDDLE).color(enabledCount > 0 ? Color.WHITE : Color.rgb(150, 155, 170)).render(graphics);
                this.addClickBox(modX, connY, modW, delSize, () -> {
                    this.modulePickerBot = name.equals(this.modulePickerBot) ? null : name;
                    this.versionPickerBot = null;
                    this.proxyPickerBot = null;
                    this.chatViewBot = null;
                    this.modulePickerY = connY + delSize + 2.0f;
                });
            }
            curY += cardH + cardGap;
        }
        if (this.versionPickerBot != null) {
            this.renderVersionPicker(graphics, x, w, mouseX, mouseY);
        }
        if (this.proxyPickerBot != null) {
            this.renderProxyPicker(graphics, x, w, mouseX, mouseY);
        }
        if (this.chatViewBot != null) {
            this.renderChatViewer(graphics, x, w, mouseX, mouseY);
        }
        if (this.modulePickerBot != null) {
            this.renderModulePicker(graphics, x, w, mouseX, mouseY);
        }
        return curY;
    }

    private void renderModulePicker(GuiGraphicsExtractor graphics, float x, float w, float mouseX, float mouseY) {
        String botName = this.modulePickerBot;
        BotClient bot = HeadlessBots.get(botName);
        if (bot == null) {
            this.modulePickerBot = null;
            return;
        }
        ArrayList<BotModule> modules = new ArrayList<BotModule>(bot.getModules().getModules());
        float rowH = 13.0f;
        float panelH = (float)modules.size() * rowH + 20.0f;
        float px = x;
        float py = this.modulePickerY;
        this.addClickBox(-10000.0f, -10000.0f, 20000.0f, 20000.0f, () -> {
            this.modulePickerBot = null;
        });
        RoundedRect.of(px, py, w, panelH).radius(3.5f).color(Color.rgba(10, 11, 16, 245)).render(graphics);
        Outline.of(px, py, w, panelH).radius(3.5f).thickness(0.6f).color(this.getAccent().withAlpha(120)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u041c\u043e\u0434\u0443\u043b\u0438 " + botName + " (\u0440\u0430\u0431\u043e\u0442\u0430\u044e\u0442 \u0432 \u0444\u043e\u043d\u0435)", px + 6.0f, py + 7.0f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
        this.addClickBox(px, py, w, panelH, () -> {});
        float ry = py + 15.0f;
        for (BotModule module : modules) {
            String desc;
            boolean on = module.isEnabled();
            float rx = px + 6.0f;
            float rw = w - 12.0f;
            boolean hov = mouseX >= rx && mouseX <= rx + rw && mouseY >= ry && mouseY <= ry + rowH - 2.0f;
            RoundedRect.of(rx, ry, rw, rowH - 2.0f).radius(2.0f).color(on ? this.getAccent().withAlpha(hov ? 130 : 95) : (hov ? Color.rgba(255, 255, 255, 25) : Color.rgba(255, 255, 255, 8))).render(graphics);
            Text.of(Fonts.MEDIUM, module.getName(), rx + 5.0f, ry + (rowH - 2.0f) * 0.5f, 5.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
            String string = desc = module instanceof BotAutoBuy ? "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438" : module.getDescription();
            if (desc != null && !desc.isEmpty()) {
                Text.of(Fonts.MEDIUM, desc, rx + 60.0f, ry + (rowH - 2.0f) * 0.5f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(150, 155, 170)).render(graphics);
            }
            Text.of(Fonts.MEDIUM, on ? "\u0412\u041a\u041b" : "\u0412\u042b\u041a\u041b", rx + rw - 14.0f, ry + (rowH - 2.0f) * 0.5f, 5.2f).centered().verticalAlign(VerticalAlign.MIDDLE).color(on ? Color.rgb(46, 204, 113) : Color.rgb(150, 155, 170)).render(graphics);
            this.addClickBox(rx, ry, rw, rowH - 2.0f, () -> {
                boolean target = !module.isEnabled();
                bot.execute(() -> module.setToggled(target));
                this.setFeedback(module.getName() + " \u0443 " + botName + ": " + (target ? "\u0432\u043a\u043b\u044e\u0447\u0451\u043d" : "\u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d"), true);
            });
            if (module instanceof BotAutoBuy) {
                BotAutoBuy autoBuy = (BotAutoBuy)module;
                this.addClickBox(rx + 60.0f, ry, rw - 85.0f, rowH - 2.0f, autoBuy::openSettings);
            }
            ry += rowH;
        }
    }

    private void renderChatViewer(GuiGraphicsExtractor graphics, float x, float w, float mouseX, float mouseY) {
        String botName = this.chatViewBot;
        BotClient bot = HeadlessBots.get(botName);
        List<ChatMessage> logs = bot != null ? bot.getChatLog() : List.of();
        int maxLines = 16;
        float lineH = 9.0f;
        float panelH = 16.0f + (float)maxLines * lineH + 4.0f;
        float px = x;
        float py = this.chatViewY;
        this.addClickBox(-10000.0f, -10000.0f, 20000.0f, 20000.0f, () -> {
            this.chatViewBot = null;
        });
        RoundedRect.of(px, py, w, panelH).radius(3.5f).color(Color.rgba(10, 11, 16, 245)).render(graphics);
        Outline.of(px, py, w, panelH).radius(3.5f).thickness(0.6f).color(this.getAccent().withAlpha(120)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u0427\u0430\u0442 " + botName + " (\u0442\u043e\u043b\u044c\u043a\u043e \u043f\u0440\u043e\u0441\u043c\u043e\u0442\u0440)", px + 6.0f, py + 7.0f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
        this.addClickBox(px, py, w, panelH, () -> {});
        float closeX = px + w - 12.0f;
        boolean closeHov = mouseX >= closeX && mouseX <= closeX + 8.0f && mouseY >= py + 2.0f && mouseY <= py + 12.0f;
        Text.of(Fonts.MEDIUM, "\u00d7", closeX + 4.0f, py + 7.0f, 7.0f).centered().verticalAlign(VerticalAlign.MIDDLE).color(closeHov ? Color.rgb(255, 100, 100) : Color.rgb(150, 155, 170)).render(graphics);
        this.addClickBox(closeX, py + 2.0f, 8.0f, 10.0f, () -> {
            this.chatViewBot = null;
        });
        float maxTextW = w - 12.0f;
        float ly = py + 16.0f;
        if (logs.isEmpty()) {
            Text.of(Fonts.MEDIUM, bot == null ? "\u0411\u043e\u0442 \u043d\u0435 \u0437\u0430\u043f\u0443\u0449\u0435\u043d" : "\u0421\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0439 \u043f\u043e\u043a\u0430 \u043d\u0435\u0442", px + 6.0f, ly + lineH * 0.5f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(130, 135, 150)).render(graphics);
            return;
        }
        List<ChatMessage> recent = logs.subList(Math.max(0, logs.size() - maxLines), logs.size());
        for (ChatMessage chatMessage : recent) {
            Object text = (String)(chatMessage.time() != null ? "[" + chatMessage.time() + "] " : "") + chatMessage.text().getString().replace('\n', ' ');
            if (Text.of(Fonts.MEDIUM, (String)text).size(5.0f).width() > maxTextW) {
                while (((String)text).length() > 4 && Text.of(Fonts.MEDIUM, (String)text + "...").size(5.0f).width() > maxTextW) {
                    text = ((String)text).substring(0, ((String)text).length() - 1);
                }
                text = (String)text + "...";
            }
            Text.of(Fonts.MEDIUM, (String)text, px + 6.0f, ly + lineH * 0.5f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(220, 225, 235)).render(graphics);
            ly += lineH;
        }
    }

    private void renderProxyPicker(GuiGraphicsExtractor graphics, float x, float w, float mouseX, float mouseY) {
        String botName = this.proxyPickerBot;
        List<String> pool = HeadlessBots.getProxyPool();
        String current = HeadlessBots.getProxy(botName);
        String fieldProxy = this.proxyInput != null ? this.proxyInput.trim() : "";
        boolean canAdd = !fieldProxy.isEmpty() && !pool.contains(fieldProxy);
        float rowH = 11.0f;
        int rows = 1 + pool.size() + (canAdd ? 1 : 0);
        float panelH = (float)rows * rowH + 18.0f;
        float px = x;
        float py = this.proxyPickerY;
        this.addClickBox(-10000.0f, -10000.0f, 20000.0f, 20000.0f, () -> {
            this.proxyPickerBot = null;
        });
        RoundedRect.of(px, py, w, panelH).radius(3.5f).color(Color.rgba(10, 11, 16, 245)).render(graphics);
        Outline.of(px, py, w, panelH).radius(3.5f).thickness(0.6f).color(this.getAccent().withAlpha(120)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u041f\u0440\u043e\u043a\u0441\u0438 \u0434\u043b\u044f " + botName + " (\u043f\u0440\u0438\u043c\u0435\u043d\u0438\u0442\u0441\u044f \u043f\u0440\u0438 \u0441\u043b\u0435\u0434\u0443\u044e\u0449\u0435\u043c \u0432\u0445\u043e\u0434\u0435)", px + 6.0f, py + 7.0f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
        this.addClickBox(px, py, w, panelH, () -> {});
        float ry = py + 14.0f;
        this.renderProxyRow(graphics, px, ry, w, rowH, "\u0411\u0435\u0437 \u043f\u0440\u043e\u043a\u0441\u0438 (\u043d\u0430\u043f\u0440\u044f\u043c\u0443\u044e)", null, current == null || current.isBlank(), mouseX, mouseY, () -> {
            HeadlessBots.setProxy(botName, null);
            this.proxyPickerBot = null;
            this.setFeedback("\u0411\u043e\u0442 " + botName + ": \u0431\u0435\u0437 \u043f\u0440\u043e\u043a\u0441\u0438", true);
        });
        ry += rowH;
        for (String proxy : pool) {
            HeadlessBots_ProxyPingSnapshot ping = HeadlessBots.getProxyPing(proxy);
            int uses = HeadlessBots.getProxyUseCount(proxy);
            String label = proxy + "   " + ping.label() + "   \u0431\u043e\u0442\u043e\u0432: " + uses;
            this.renderProxyRow(graphics, px, ry, w, rowH, label, ping, proxy.equals(current), mouseX, mouseY, () -> {
                HeadlessBots.setProxy(botName, proxy);
                this.proxyPickerBot = null;
                this.setFeedback("\u0411\u043e\u0442 " + botName + ": \u043f\u0440\u043e\u043a\u0441\u0438 " + proxy, true);
            });
            float delX = px + w - 14.0f;
            boolean delHov = mouseX >= delX && mouseX <= delX + 8.0f && mouseY >= ry && mouseY <= ry + rowH - 1.0f;
            Text.of(Fonts.MEDIUM, "\u00d7", delX + 4.0f, ry + (rowH - 1.0f) * 0.5f, 6.0f).centered().verticalAlign(VerticalAlign.MIDDLE).color(delHov ? Color.rgb(255, 100, 100) : Color.rgb(130, 135, 150)).render(graphics);
            this.addClickBox(delX, ry, 8.0f, rowH - 1.0f, () -> {
                HeadlessBots.removeProxy(proxy);
                this.setFeedback("\u041f\u0440\u043e\u043a\u0441\u0438 " + proxy + " \u0443\u0434\u0430\u043b\u0451\u043d \u0438\u0437 \u0441\u043f\u0438\u0441\u043a\u0430", true);
            });
            ry += rowH;
        }
        if (canAdd) {
            this.renderProxyRow(graphics, px, ry, w, rowH, "+ \u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0438 \u0432\u044b\u0431\u0440\u0430\u0442\u044c: " + fieldProxy, null, false, mouseX, mouseY, () -> {
                HeadlessBots.setProxy(botName, fieldProxy);
                this.proxyPickerBot = null;
                this.setFeedback("\u0411\u043e\u0442 " + botName + ": \u043f\u0440\u043e\u043a\u0441\u0438 " + fieldProxy, true);
            });
        }
    }

    private void renderProxyRow(GuiGraphicsExtractor graphics, float px, float ry, float w, float rowH, String label, HeadlessBots_ProxyPingSnapshot ping, boolean selected, float mouseX, float mouseY, Runnable action) {
        float rx = px + 6.0f;
        float rw = w - 24.0f;
        boolean hov = mouseX >= rx && mouseX <= rx + rw && mouseY >= ry && mouseY <= ry + rowH - 1.0f;
        RoundedRect.of(rx, ry, rw, rowH - 1.0f).radius(1.5f).color(selected ? this.getAccent().withAlpha(110) : (hov ? Color.rgba(255, 255, 255, 25) : Color.rgba(255, 255, 255, 6))).render(graphics);
        Color textColor = Color.rgb(200, 205, 215);
        if (ping != null) {
            textColor = switch (ping.status()) {
                case HeadlessBots_ProxyPingStatus.OK -> Color.rgb(46, 204, 113);
                case HeadlessBots_ProxyPingStatus.FAILED -> Color.rgb(235, 87, 87);
                default -> Color.rgb(200, 205, 215);
            };
        }
        Text.of(Fonts.MEDIUM, label, rx + 4.0f, ry + (rowH - 1.0f) * 0.5f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(selected ? Color.WHITE : textColor).render(graphics);
        this.addClickBox(rx, ry, rw, rowH - 1.0f, action);
    }

    private void renderVersionPicker(GuiGraphicsExtractor graphics, float x, float w, float mouseX, float mouseY) {
        String botName = this.versionPickerBot;
        List<BotProtocolVersions_Entry> versions = BotVia.availableVersions();
        int columns = 5;
        float cellW = (w - 12.0f) / (float)columns;
        float cellH = 10.0f;
        int rows = (versions.size() + columns - 1) / columns;
        float panelH = (float)rows * cellH + 18.0f;
        float px = x;
        float py = this.versionPickerY;
        this.addClickBox(-10000.0f, -10000.0f, 20000.0f, 20000.0f, () -> {
            this.versionPickerBot = null;
        });
        RoundedRect.of(px, py, w, panelH).radius(3.5f).color(Color.rgba(10, 11, 16, 245)).render(graphics);
        Outline.of(px, py, w, panelH).radius(3.5f).thickness(0.6f).color(this.getAccent().withAlpha(120)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u0412\u0435\u0440\u0441\u0438\u044f \u0434\u043b\u044f " + botName + " (ViaFabricPlus)", px + 6.0f, py + 7.0f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
        this.addClickBox(px, py, w, panelH, () -> {});
        int current = HeadlessBots.getProtocolVersion(botName);
        for (int i = 0; i < versions.size(); ++i) {
            BotProtocolVersions_Entry entry = versions.get(i);
            float cx = px + 6.0f + (float)(i % columns) * cellW;
            float cy = py + 14.0f + (float)(i / columns) * cellH;
            boolean selected = entry.protocol() == current || entry.protocol() <= 0 && current <= 0;
            boolean hov = mouseX >= cx && mouseX <= cx + cellW - 2.0f && mouseY >= cy && mouseY <= cy + cellH - 1.0f;
            RoundedRect.of(cx, cy, cellW - 2.0f, cellH - 1.0f).radius(1.5f).color(selected ? this.getAccent().withAlpha(110) : (hov ? Color.rgba(255, 255, 255, 25) : Color.rgba(255, 255, 255, 6))).render(graphics);
            Text.of(Fonts.MEDIUM, entry.label(), cx + (cellW - 2.0f) * 0.5f, cy + (cellH - 1.0f) * 0.5f, 4.6f).centered().verticalAlign(VerticalAlign.MIDDLE).color(selected ? Color.WHITE : Color.rgb(190, 195, 210)).render(graphics);
            this.addClickBox(cx, cy, cellW - 2.0f, cellH - 1.0f, () -> {
                HeadlessBots.setProtocolVersion(botName, entry.protocol());
                HeadlessBots.savePersistentState();
                this.versionPickerBot = null;
                this.proxyPickerBot = null;
                BotClient online = HeadlessBots.get(botName);
                this.setFeedback("\u0412\u0435\u0440\u0441\u0438\u044f " + botName + ": " + entry.label() + (online != null ? " \u2014 \u043f\u0435\u0440\u0435\u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0438\u0442\u0435 \u0431\u043e\u0442\u0430" : ""), true);
            });
        }
    }

    private float renderCreateTab(GuiGraphicsExtractor graphics, float x, float y, float w, float vpH, float mouseX, float mouseY) {
        float curY = y;
        float boxW = w;
        float boxH = 100.0f;
        RoundedRect.of(x, curY, boxW, boxH).radius(4.0f).color(Color.rgba(14, 16, 22, 110)).render(graphics);
        Outline.of(x, curY, boxW, boxH).radius(4.0f).thickness(0.6f).color(Color.rgba(255, 255, 255, 14)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u043d\u043e\u0432\u043e\u0433\u043e \u0431\u043e\u0442\u0430", x + 8.0f, curY + 9.0f, 7.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
        float inY = curY + 18.0f;
        float inH = 13.0f;
        float inGap = 16.0f;
        Text.of(Fonts.MEDIUM, "\u041d\u0438\u043a\u043d\u0435\u0439\u043c:", x + 8.0f, inY + 6.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(140, 145, 160)).render(graphics);
        float fieldX = x + 44.0f;
        float randBtnW = 48.0f;
        float fieldW = boxW - 54.0f - randBtnW;
        this.renderInputField(graphics, fieldX, inY, fieldW, inH, this.nameInput, "\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u043d\u0438\u043a \u0431\u043e\u0442\u0430...", this.focusedInput == 0, mouseX, mouseY);
        this.addClickBox(fieldX, inY, fieldW, inH, () -> {
            this.focusedInput = 0;
            BotsPanel.setTextInputFocus(true);
        });
        float randBtnX = fieldX + fieldW + 3.0f;
        boolean randHov = mouseX >= randBtnX && mouseX <= randBtnX + randBtnW && mouseY >= inY && mouseY <= inY + inH;
        RoundedRect.of(randBtnX, inY, randBtnW, inH).radius(2.5f).color(randHov ? Color.rgba(255, 255, 255, 18) : Color.rgba(255, 255, 255, 8)).render(graphics);
        Outline.of(randBtnX, inY, randBtnW, inH).radius(2.5f).thickness(0.5f).color(Color.rgba(255, 255, 255, 16)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u0421\u043b\u0443\u0447\u0430\u0439\u043d\u044b\u0439", randBtnX + randBtnW * 0.5f, inY + inH * 0.5f, 5.0f).centered().verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(200, 205, 220)).render(graphics);
        this.addClickBox(randBtnX, inY, randBtnW, inH, () -> {
            this.nameInput = BotsPanel.generateNick();
        });
        Text.of(Fonts.MEDIUM, "\u0421\u0435\u0440\u0432\u0435\u0440:", x + 8.0f, (inY += inGap) + 6.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(140, 145, 160)).render(graphics);
        float fieldW2 = boxW - 52.0f;
        this.renderInputField(graphics, fieldX, inY, fieldW2, inH, this.serverInput, "ip:port (\u043d\u0430\u043f\u0440\u0438\u043c\u0435\u0440 mc.holyworld.ru)", this.focusedInput == 1, mouseX, mouseY);
        this.addClickBox(fieldX, inY, fieldW2, inH, () -> {
            this.focusedInput = 1;
            BotsPanel.setTextInputFocus(true);
        });
        Text.of(Fonts.MEDIUM, "\u041f\u0440\u043e\u043a\u0441\u0438:", x + 8.0f, (inY += inGap) + 6.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(140, 145, 160)).render(graphics);
        this.renderInputField(graphics, fieldX, inY, fieldW2, inH, this.proxyInput, "host:port \u0438\u043b\u0438 user:pass@host:port (\u043d\u0435\u043e\u0431\u044f\u0437\u0430\u0442\u0435\u043b\u044c\u043d\u043e)", this.focusedInput == 2, mouseX, mouseY);
        this.addClickBox(fieldX, inY, fieldW2, inH, () -> {
            this.focusedInput = 2;
            BotsPanel.setTextInputFocus(true);
        });
        float actW1 = (boxW - 20.0f) * 0.5f;
        float actH = 13.0f;
        float actX1 = x + 8.0f;
        boolean actHov1 = mouseX >= actX1 && mouseX <= actX1 + actW1 && mouseY >= (inY += inGap + 2.0f) && mouseY <= inY + actH;
        RoundedRect.of(actX1, inY, actW1, actH).radius(3.0f).color(actHov1 ? Color.rgba(255, 255, 255, 22) : Color.rgba(255, 255, 255, 10)).render(graphics);
        Outline.of(actX1, inY, actW1, actH).radius(3.0f).thickness(0.6f).color(Color.rgba(255, 255, 255, 25)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0432 \u0441\u043f\u0438\u0441\u043e\u043a", actX1 + actW1 * 0.5f, inY + actH * 0.5f, 5.5f).centered().verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
        this.addClickBox(actX1, inY, actW1, actH, () -> {
            if (this.nameInput.trim().isEmpty()) {
                this.setFeedback("\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u043d\u0438\u043a\u043d\u0435\u0439\u043c \u0431\u043e\u0442\u0430!", false);
                return;
            }
            String n = this.nameInput.trim();
            HeadlessBots.add(n);
            if (this.proxyInput != null && !this.proxyInput.isBlank()) {
                HeadlessBots.setProxy(n, this.proxyInput.trim());
            }
            HeadlessBots.setLastNameInput(n);
            this.setFeedback("\u0411\u043e\u0442 '" + n + "' \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d", true);
            this.nameInput = BotsPanel.generateNick();
        });
        float actX2 = actX1 + actW1 + 4.0f;
        boolean actHov2 = mouseX >= actX2 && mouseX <= actX2 + actW1 && mouseY >= inY && mouseY <= inY + actH;
        RoundedRect.of(actX2, inY, actW1, actH).radius(3.0f).color(this.getAccent().withAlpha(actHov2 ? 55 : 30)).render(graphics);
        Outline.of(actX2, inY, actW1, actH).radius(3.0f).thickness(0.6f).color(this.getAccent().withAlpha(140)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0438 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0438\u0442\u044c", actX2 + actW1 * 0.5f, inY + actH * 0.5f, 5.5f).centered().verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
        this.addClickBox(actX2, inY, actW1, actH, () -> {
            if (this.nameInput.trim().isEmpty()) {
                this.setFeedback("\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u043d\u0438\u043a\u043d\u0435\u0439\u043c \u0431\u043e\u0442\u0430!", false);
                return;
            }
            String n = this.nameInput.trim();
            HeadlessBots.add(n);
            if (this.proxyInput != null && !this.proxyInput.isBlank()) {
                HeadlessBots.setProxy(n, this.proxyInput.trim());
            }
            HeadlessBots.setLastNameInput(n);
            String host = "mc.holyworld.ru";
            int port = 25565;
            if (this.serverInput != null && !this.serverInput.isBlank()) {
                String[] parts = this.serverInput.trim().split(":");
                host = parts[0];
                if (parts.length > 1) {
                    try {
                        port = Integer.parseInt(parts[1]);
                    }
                    catch (Exception ignored) {
                        Log.swallow(ignored);
                    }
                }
            }
            HeadlessBots.connect(n, host, port);
            this.setFeedback("\u0411\u043e\u0442 '" + n + "' \u0434\u043e\u0431\u0430\u0432\u043b\u0435\u043d \u0438 \u0437\u0430\u043f\u0443\u0441\u043a\u0430\u0435\u0442\u0441\u044f", true);
            this.nameInput = BotsPanel.generateNick();
            this.currentTab = 0;
            this.resetScroll();
        });
        float batchH = 58.0f;
        RoundedRect.of(x, curY += boxH + 6.0f, boxW, batchH).radius(4.0f).color(Color.rgba(14, 16, 22, 110)).render(graphics);
        Outline.of(x, curY, boxW, batchH).radius(4.0f).thickness(0.6f).color(Color.rgba(255, 255, 255, 14)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u0413\u0435\u043d\u0435\u0440\u0430\u0446\u0438\u044f \u043f\u0430\u0447\u043a\u0438 \u0431\u043e\u0442\u043e\u0432", x + 8.0f, curY + 9.0f, 7.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
        float binY = curY + 18.0f;
        Text.of(Fonts.MEDIUM, "\u041f\u0440\u0435\u0444\u0438\u043a\u0441:", x + 8.0f, binY + 6.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(140, 145, 160)).render(graphics);
        float pfxW = 60.0f;
        this.renderInputField(graphics, x + 44.0f, binY, pfxW, inH, this.batchPrefix, "\u0410\u0432\u0442\u043e", this.focusedInput == 4, mouseX, mouseY);
        this.addClickBox(x + 44.0f, binY, pfxW, inH, () -> {
            this.focusedInput = 4;
            BotsPanel.setTextInputFocus(true);
        });
        float cntLblX = x + 44.0f + pfxW + 8.0f;
        Text.of(Fonts.MEDIUM, "\u041a\u043e\u043b-\u0432\u043e:", cntLblX, binY + 6.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(140, 145, 160)).render(graphics);
        float cntFldX = cntLblX + 28.0f;
        float cntW = 30.0f;
        this.renderInputField(graphics, cntFldX, binY, cntW, inH, this.batchCountInput, "3", this.focusedInput == 5, mouseX, mouseY);
        this.addClickBox(cntFldX, binY, cntW, inH, () -> {
            this.focusedInput = 5;
            BotsPanel.setTextInputFocus(true);
        });
        float bBtnX = cntFldX + cntW + 8.0f;
        float bBtnW = boxW - (bBtnX - x) - 8.0f;
        boolean bBtnHov = mouseX >= bBtnX && mouseX <= bBtnX + bBtnW && mouseY >= binY && mouseY <= binY + inH;
        RoundedRect.of(bBtnX, binY, bBtnW, inH).radius(2.5f).color(this.getAccent().withAlpha(bBtnHov ? 55 : 30)).render(graphics);
        Outline.of(bBtnX, binY, bBtnW, inH).radius(2.5f).thickness(0.6f).color(this.getAccent().withAlpha(130)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u0421\u043e\u0437\u0434\u0430\u0442\u044c \u0438 \u0432\u043e\u0439\u0442\u0438", bBtnX + bBtnW * 0.5f, binY + inH * 0.5f, 5.5f).centered().verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
        this.addClickBox(bBtnX, binY, bBtnW, inH, () -> {
            int count = 3;
            try {
                count = Math.clamp((long)Integer.parseInt(this.batchCountInput.trim()), 1, 25);
            }
            catch (Exception ignored) {
                Log.swallow(ignored);
            }
            String pfx = this.batchPrefix.trim();
            String host = "mc.holyworld.ru";
            int port = 25565;
            if (this.serverInput != null && !this.serverInput.isBlank()) {
                String[] parts = this.serverInput.trim().split(":");
                host = parts[0];
                if (parts.length > 1) {
                    try {
                        port = Integer.parseInt(parts[1]);
                    }
                    catch (Exception ignored) {
                        Log.swallow(ignored);
                    }
                }
            }
            ArrayList<String> created = new ArrayList<String>();
            for (int i = 0; i < count; ++i) {
                String botName = pfx.isEmpty() ? BotsPanel.generateNick() : pfx + (10 + new Random().nextInt(990));
                HeadlessBots.add(botName);
                if (this.proxyInput != null && !this.proxyInput.isBlank()) {
                    HeadlessBots.setProxy(botName, this.proxyInput.trim());
                }
                created.add(botName);
            }
            HeadlessBots.connectAll(created, host, port);
            this.setFeedback("\u0421\u043e\u0437\u0434\u0430\u043d\u043e \u0438 \u0437\u0430\u043f\u0443\u0449\u0435\u043d\u043e " + count + " \u0431\u043e\u0442\u043e\u0432", true);
            this.currentTab = 0;
            this.resetScroll();
        });
        Text.of(Fonts.MEDIUM, "\u0421\u043e\u0432\u0435\u0442: \u0447\u0442\u043e\u0431\u044b \u0431\u043e\u0442\u044b \u043d\u0435 \u0437\u0430\u0441\u0442\u0440\u044f\u043b\u0438 \u043d\u0430 \u043f\u0440\u043e\u043a\u0441\u0438, \u043c\u043e\u0436\u043d\u043e \u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u043f\u043e\u043b\u0435 '\u041f\u0440\u043e\u043a\u0441\u0438' \u043f\u0443\u0441\u0442\u044b\u043c \u0434\u043b\u044f \u043f\u0440\u044f\u043c\u043e\u0433\u043e \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u044f.", x + 8.0f, curY + 44.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(105, 110, 125)).render(graphics);
        return curY + batchH;
    }

    private float renderModulesTab(GuiGraphicsExtractor graphics, float x, float y, float w, float vpH, float mouseX, float mouseY) {
        float curY = y;
        Text.of(Fonts.MEDIUM, "\u041c\u043e\u0434\u0443\u043b\u0438 \u0431\u043e\u0442\u043e\u0432 \u043f\u0440\u0438\u043c\u0435\u043d\u044f\u044e\u0442\u0441\u044f \u043a\u043e \u0432\u0441\u0435\u043c \u0430\u043a\u0442\u0438\u0432\u043d\u044b\u043c \u0431\u043e\u0442\u0430\u043c", x + 2.0f, curY + 4.0f, 5.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(130, 135, 150)).render(graphics);
        curY += 12.0f;
        Collection<BotClient> clients = HeadlessBots.all();
        BotClient sampleClient = clients.isEmpty() ? null : clients.iterator().next();
        BotModuleManager moduleMgr = sampleClient != null ? sampleClient.getModules() : null;
        ArrayList<BotModule> modulesToRender = new ArrayList<BotModule>();
        if (moduleMgr != null) {
            modulesToRender.addAll(moduleMgr.getModules());
        } else {
            if (this.previewModules == null) {
                this.previewModules = new BotModuleManager(null);
            }
            modulesToRender.addAll(this.previewModules.getModules());
        }
        modulesToRender.sort(Comparator.comparing(BotModule::getName, String.CASE_INSENSITIVE_ORDER));
        float cardW = w;
        float headerH = 22.0f;
        for (BotModule module : modulesToRender) {
            String name = module.getName();
            boolean enabled = module.isEnabled();
            List<Setting<?>> settings = module.getSettings();
            float expandProgress = this.moduleExpandProgress.getOrDefault(name, Float.valueOf(0.0f)).floatValue();
            float settingsH = 0.0f;
            if (!settings.isEmpty() && expandProgress > 0.001f) {
                for (Setting<?> s : settings) {
                    if (!s.isVisible()) continue;
                    settingsH += this.getSettingHeight(s);
                }
                settingsH += 6.0f;
            }
            float cardH = headerH + settingsH * expandProgress;
            boolean cardHov = mouseX >= x && mouseX <= x + cardW && mouseY >= curY && mouseY <= curY + headerH;
            RoundedRect.of(x, curY, cardW, cardH).radius(4.0f).color(cardHov ? Color.rgba(18, 20, 28, 115) : Color.rgba(13, 14, 20, 95)).render(graphics);
            Outline.of(x, curY, cardW, cardH).radius(4.0f).thickness(0.6f).color(enabled ? this.getAccent().withAlpha(60) : Color.rgba(255, 255, 255, 14)).render(graphics);
            Text.of(Fonts.MEDIUM, name, x + 8.0f, curY + 8.0f, 7.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
            String desc = this.getModuleDesc(name);
            Text.of(Fonts.MEDIUM, desc, x + 8.0f, curY + 16.0f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(115, 120, 135)).render(graphics);
            float swW = 20.0f;
            float swH = 10.0f;
            float swX = x + cardW - swW - 8.0f;
            float swY = curY + (headerH - swH) * 0.5f;
            boolean swHov = mouseX >= swX - 2.0f && mouseX <= swX + swW + 2.0f && mouseY >= swY - 2.0f && mouseY <= swY + swH + 2.0f;
            RoundedRect.of(swX, swY, swW, swH).radius(swH * 0.5f).color(enabled ? this.getAccent() : Color.rgba(45, 48, 60, 200)).render(graphics);
            float knobR = swH * 0.5f - 1.0f;
            float knobX = enabled ? swX + swW - knobR * 2.0f - 1.0f : swX + 1.0f;
            RoundedRect.of(knobX, swY + 1.0f, knobR * 2.0f, knobR * 2.0f).radius(knobR).color(Color.WHITE).render(graphics);
            float clickModuleY = curY;
            this.addClickBox(x, clickModuleY, cardW - (settings.isEmpty() ? swW + 12.0f : swW + 28.0f), headerH, () -> {
                boolean newToggled = !enabled;
                for (BotClient bc : HeadlessBots.all()) {
                    bc.getModules().setEnabled(name, newToggled);
                }
                module.setToggled(newToggled);
            });
            this.addClickBox(swX - 2.0f, swY - 2.0f, swW + 4.0f, swH + 4.0f, () -> {
                boolean newToggled = !enabled;
                for (BotClient bc : HeadlessBots.all()) {
                    bc.getModules().setEnabled(name, newToggled);
                }
                module.setToggled(newToggled);
            });
            if (!settings.isEmpty()) {
                float arrSize = 10.0f;
                float arrX = swX - arrSize - 4.0f;
                float arrY = curY + (headerH - arrSize) * 0.5f;
                boolean arrHov = mouseX >= arrX && mouseX <= arrX + arrSize && mouseY >= arrY && mouseY <= arrY + arrSize;
                RoundedRect.of(arrX, arrY, arrSize, arrSize).radius(2.0f).color(arrHov ? Color.rgba(255, 255, 255, 14) : Color.rgba(255, 255, 255, 5)).render(graphics);
                String arrowGlyph = expandProgress > 0.5f ? "P" : "O";
                Text.of(Fonts.ICONS, arrowGlyph, arrX + arrSize * 0.5f, arrY + arrSize * 0.5f, 5.5f).centered().color(Color.rgb(160, 165, 180)).render(graphics);
                this.addClickBox(arrX, arrY, arrSize, arrSize, () -> this.moduleExpandProgress.put(name, Float.valueOf(expandProgress > 0.5f ? 0.0f : 1.0f)));
            }
            if (!settings.isEmpty() && expandProgress > 0.01f) {
                Scissor.push(graphics, x, curY + headerH, cardW, settingsH * expandProgress);
                float sY = curY + headerH + 3.0f;
                for (Setting<?> s : settings) {
                    if (!s.isVisible()) continue;
                    sY = this.renderSetting(graphics, s, x + 8.0f, sY, cardW - 16.0f, mouseX, mouseY);
                }
                Scissor.pop(graphics);
            }
            curY += cardH + 4.0f;
        }
        return curY;
    }

    private String getModuleDesc(String moduleName) {
        return switch (moduleName.toLowerCase(Locale.ROOT)) {
            case "autosprint" -> "\u0410\u0432\u0442\u043e\u043c\u0430\u0442\u0438\u0447\u0435\u0441\u043a\u0438 \u0443\u0434\u0435\u0440\u0436\u0438\u0432\u0430\u0435\u0442 \u0431\u0435\u0433 \u043f\u0440\u0438 \u043f\u0435\u0440\u0435\u0434\u0432\u0438\u0436\u0435\u043d\u0438\u0438 \u0431\u043e\u0442\u0430";
            case "jumpcircle" -> "\u041e\u0442\u043e\u0431\u0440\u0430\u0436\u0430\u0435\u0442 \u043a\u0440\u0430\u0441\u0438\u0432\u044b\u0435 \u043d\u0435\u043e\u043d\u043e\u0432\u044b\u0435 \u043a\u0440\u0443\u0433\u0438 \u043f\u0440\u0438 \u043f\u0440\u044b\u0436\u043a\u0435 \u0431\u043e\u0442\u0430";
            case "autocase" -> "\u0410\u0432\u0442\u043e\u043c\u0430\u0442\u0438\u0447\u0435\u0441\u043a\u0438 \u043e\u0442\u043a\u0440\u044b\u0432\u0430\u0435\u0442 \u0441\u0443\u043d\u0434\u0443\u043a\u0438 \u0438 \u043c\u0438\u0441\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0435 \u043a\u0435\u0439\u0441\u044b";
            case "pvpsell" -> "\u0410\u0432\u0442\u043e\u043c\u0430\u0442\u0438\u0447\u0435\u0441\u043a\u0430\u044f \u043f\u0440\u043e\u0434\u0430\u0436\u0430 \u0440\u0435\u0441\u0443\u0440\u0441\u043e\u0432 \u043d\u0430 PvP \u0441\u0435\u0440\u0432\u0435\u0440\u0430\u0445";
            default -> "\u041c\u043e\u0434\u0443\u043b\u044c \u0434\u043b\u044f \u0430\u0432\u0442\u043e\u043d\u043e\u043c\u043d\u043e\u0439 \u0440\u0430\u0431\u043e\u0442\u044b \u0431\u043e\u0442\u0430";
        };
    }

    private float getSettingHeight(Setting<?> s) {
        if (s instanceof BooleanSetting) {
            return 14.0f;
        }
        if (s instanceof NumberSetting || s instanceof FloatSetting) {
            return 18.0f;
        }
        if (s instanceof ModeSetting) {
            return 22.0f;
        }
        return 14.0f;
    }

    private float renderSetting(GuiGraphicsExtractor graphics, Setting<?> s, float sx, float sy, float sw, float mouseX, float mouseY) {
        if (s instanceof ButtonSetting) {
            ButtonSetting button = (ButtonSetting)s;
            float sh = 13.0f;
            boolean hover = mouseX >= sx && mouseX <= sx + sw && mouseY >= sy && mouseY <= sy + sh;
            RoundedRect.of(sx, sy, sw, sh).radius(3.0f).color(hover ? this.getAccent().withAlpha(100) : Color.rgba(255, 255, 255, 15)).render(graphics);
            Text.of(Fonts.MEDIUM, button.name(), sx + 5.0f, sy + sh * 0.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
            this.addClickBox(sx, sy, sw, sh, button::runAction);
            return sy + sh + 1.0f;
        }
        if (s instanceof BooleanSetting) {
            BooleanSetting bs = (BooleanSetting)s;
            float sh = 13.0f;
            boolean hov = mouseX >= sx && mouseX <= sx + sw && mouseY >= sy && mouseY <= sy + sh;
            Text.of(Fonts.MEDIUM, bs.name(), sx, sy + sh * 0.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(200, 205, 220)).render(graphics);
            float chkW = 14.0f;
            float chkH = 8.0f;
            float chkX = sx + sw - chkW;
            float chkY = sy + (sh - chkH) * 0.5f;
            RoundedRect.of(chkX, chkY, chkW, chkH).radius(chkH * 0.5f).color(bs.getValue() ? this.getAccent() : Color.rgba(45, 48, 60, 200)).render(graphics);
            float kR = chkH * 0.5f - 1.0f;
            float kX = bs.getValue() ? chkX + chkW - kR * 2.0f - 1.0f : chkX + 1.0f;
            RoundedRect.of(kX, chkY + 1.0f, kR * 2.0f, kR * 2.0f).radius(kR).color(Color.WHITE).render(graphics);
            this.addClickBox(sx, sy, sw, sh, bs::toggle);
            return sy + sh;
        }
        if (s instanceof NumberSetting) {
            NumberSetting ns = (NumberSetting)s;
            float sh = 17.0f;
            this.middleBoxes.add(new float[]{sx, sy, sw, sh});
            this.middleActions.add(() -> {
                this.draggingSlider = null;
                ns.reset();
            });
            Text.of(Fonts.MEDIUM, ns.name(), sx, sy + 4.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(200, 205, 220)).render(graphics);
            String valStr = String.valueOf(ns.getValue());
            Text.of(Fonts.MEDIUM, valStr, sx + sw, sy + 4.5f, 5.5f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(this.getAccent()).render(graphics);
            float trkH = 3.0f;
            float trkY = sy + 11.0f;
            RoundedRect.of(sx, trkY, sw, trkH).radius(1.5f).color(Color.rgba(255, 255, 255, 15)).render(graphics);
            double pct = Math.clamp((ns.getValue() - ns.min()) / (ns.max() - ns.min()), 0.0, 1.0);
            float fillW = (float)((double)sw * pct);
            if (fillW > 0.0f) {
                RoundedRect.of(sx, trkY, fillW, trkH).radius(1.5f).color(this.getAccent()).render(graphics);
            }
            this.addClickBox(sx, sy + 8.0f, sw, 8.0f, () -> {
                this.draggingSlider = ns;
                this.dragSliderTrackX = sx;
                this.dragSliderTrackW = sw;
                double p = Math.clamp((double)((mouseX - sx) / sw), 0.0, 1.0);
                double v = ns.min() + p * (ns.max() - ns.min());
                if (ns.step() > 0.0) {
                    v = (double)Math.round(v / ns.step()) * ns.step();
                }
                ns.setValue(v);
            });
            return sy + sh;
        }
        if (s instanceof FloatSetting) {
            FloatSetting fs = (FloatSetting)s;
            float sh = 17.0f;
            this.middleBoxes.add(new float[]{sx, sy, sw, sh});
            this.middleActions.add(() -> {
                this.draggingSlider = null;
                fs.reset();
            });
            Text.of(Fonts.MEDIUM, fs.getName(), sx, sy + 4.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(200, 205, 220)).render(graphics);
            String valStr = String.format(Locale.ROOT, "%.1f", fs.get());
            Text.of(Fonts.MEDIUM, valStr, sx + sw, sy + 4.5f, 5.5f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(this.getAccent()).render(graphics);
            float trkH = 3.0f;
            float trkY = sy + 11.0f;
            RoundedRect.of(sx, trkY, sw, trkH).radius(1.5f).color(Color.rgba(255, 255, 255, 15)).render(graphics);
            double pct = Math.clamp((((Float)fs.get()).floatValue() - fs.getMin()) / (fs.getMax() - fs.getMin()), 0.0f, 1.0f);
            float fillW = (float)((double)sw * pct);
            if (fillW > 0.0f) {
                RoundedRect.of(sx, trkY, fillW, trkH).radius(1.5f).color(this.getAccent()).render(graphics);
            }
            this.addClickBox(sx, sy + 8.0f, sw, 8.0f, () -> {
                this.draggingSlider = fs;
                this.dragSliderTrackX = sx;
                this.dragSliderTrackW = sw;
                float p = Math.clamp((mouseX - sx) / sw, 0.0f, 1.0f);
                float v = fs.getMin() + p * (fs.getMax() - fs.getMin());
                if (fs.getStep() > 0.0f) {
                    v = (float)Math.round(v / fs.getStep()) * fs.getStep();
                }
                fs.set(v);
            });
            return sy + sh;
        }
        if (s instanceof ModeSetting) {
            ModeSetting ms = (ModeSetting)s;
            float sh = 20.0f;
            Text.of(Fonts.MEDIUM, ms.getName(), sx, sy + 4.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(200, 205, 220)).render(graphics);
            float chipX = sx;
            float chipY = sy + 9.5f;
            float chipH = 9.5f;
            for (String mode : ms.getModes()) {
                float tw = Text.of(Fonts.MEDIUM, mode).size(5.0f).width();
                float chipW = tw + 8.0f;
                boolean sel = mode.equalsIgnoreCase((String)ms.get());
                boolean hov = mouseX >= chipX && mouseX <= chipX + chipW && mouseY >= chipY && mouseY <= chipY + chipH;
                RoundedRect.of(chipX, chipY, chipW, chipH).radius(2.0f).color(sel ? this.getAccent() : (hov ? Color.rgba(255, 255, 255, 14) : Color.rgba(255, 255, 255, 6))).render(graphics);
                Text.of(Fonts.MEDIUM, mode, chipX + chipW * 0.5f, chipY + chipH * 0.5f, 5.0f).centered().verticalAlign(VerticalAlign.MIDDLE).color(sel ? Color.WHITE : Color.rgb(160, 165, 180)).render(graphics);
                String finalMode = mode;
                this.addClickBox(chipX, chipY, chipW, chipH, () -> ms.set(finalMode));
                chipX += chipW + 3.0f;
            }
            return sy + sh;
        }
        return sy + 14.0f;
    }

    private float renderChatTab(GuiGraphicsExtractor graphics, float x, float y, float w, float vpH, float mouseX, float mouseY) {
        float curY = y;
        float inCardH = 42.0f;
        RoundedRect.of(x, curY, w, inCardH).radius(4.0f).color(Color.rgba(14, 16, 22, 110)).render(graphics);
        Outline.of(x, curY, w, inCardH).radius(4.0f).thickness(0.6f).color(Color.rgba(255, 255, 255, 14)).render(graphics);
        float inY = curY + 6.0f;
        float inH = 13.0f;
        float sendBtnW = 60.0f;
        float inW = w - 16.0f - sendBtnW - 4.0f;
        this.renderInputField(graphics, x + 8.0f, inY, inW, inH, this.chatInput, "\u0421\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435 \u0438\u043b\u0438 \u043a\u043e\u043c\u0430\u043d\u0434\u0430 (/spawn, /pay...)", this.focusedInput == 3, mouseX, mouseY);
        this.addClickBox(x + 8.0f, inY, inW, inH, () -> {
            this.focusedInput = 3;
            BotsPanel.setTextInputFocus(true);
        });
        float sendBtnX = x + 8.0f + inW + 4.0f;
        boolean sendHov = mouseX >= sendBtnX && mouseX <= sendBtnX + sendBtnW && mouseY >= inY && mouseY <= inY + inH;
        RoundedRect.of(sendBtnX, inY, sendBtnW, inH).radius(2.5f).color(this.getAccent().withAlpha(sendHov ? 55 : 30)).render(graphics);
        Outline.of(sendBtnX, inY, sendBtnW, inH).radius(2.5f).thickness(0.6f).color(this.getAccent().withAlpha(140)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c", sendBtnX + sendBtnW * 0.5f, inY + inH * 0.5f, 5.5f).centered().verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
        this.addClickBox(sendBtnX, inY, sendBtnW, inH, this::sendChatToBots);
        float chipY = inY + inH + 4.0f;
        float chipH = 10.0f;
        String[] quickCmds = new String[]{"/spawn", "/hub", "/rtp", "/clan home", "/anarchy-101", "/suicide"};
        float chipX = x + 8.0f;
        for (String cmd : quickCmds) {
            float tw = Text.of(Fonts.MEDIUM, cmd).size(5.0f).width();
            float cW = tw + 8.0f;
            boolean cHov = mouseX >= chipX && mouseX <= chipX + cW && mouseY >= chipY && mouseY <= chipY + chipH;
            RoundedRect.of(chipX, chipY, cW, chipH).radius(2.0f).color(cHov ? Color.rgba(255, 255, 255, 18) : Color.rgba(255, 255, 255, 8)).render(graphics);
            Outline.of(chipX, chipY, cW, chipH).radius(2.0f).thickness(0.5f).color(Color.rgba(255, 255, 255, 18)).render(graphics);
            Text.of(Fonts.MEDIUM, cmd, chipX + cW * 0.5f, chipY + chipH * 0.5f, 5.0f).centered().verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(205, 210, 225)).render(graphics);
            this.addClickBox(chipX, chipY, cW, chipH, () -> {
                this.chatInput = cmd;
                this.sendChatToBots();
            });
            chipX += cW + 3.0f;
        }
        Text.of(Fonts.MEDIUM, "\u0418\u0441\u0442\u043e\u0440\u0438\u044f \u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0439 \u0431\u043e\u0442\u043e\u0432:", x + 2.0f, (curY += inCardH + 6.0f) + 4.0f, 5.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(130, 135, 150)).render(graphics);
        curY += 12.0f;
        ArrayList<ChatMessage> logs = new ArrayList<ChatMessage>();
        for (BotClient bc : HeadlessBots.all()) {
            logs.addAll(bc.getChatLog());
        }
        if (logs.isEmpty()) {
            float emptyH = 40.0f;
            RoundedRect.of(x, curY, w, emptyH).radius(3.5f).color(Color.rgba(14, 16, 22, 90)).render(graphics);
            Outline.of(x, curY, w, emptyH).radius(3.5f).thickness(0.6f).color(Color.rgba(255, 255, 255, 12)).render(graphics);
            Text.of(Fonts.MEDIUM, "\u0421\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0439 \u043f\u043e\u043a\u0430 \u043d\u0435\u0442", x + w * 0.5f, curY + emptyH * 0.5f, 6.0f).centered().verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(115, 120, 135)).render(graphics);
            return curY + emptyH;
        }
        int maxLogs = Math.min(30, logs.size());
        List<ChatMessage> recent = logs.subList(logs.size() - maxLogs, logs.size());
        float logRowH = 11.5f;
        for (ChatMessage msg : recent) {
            String timeStr = msg.time() != null ? "[" + msg.time() + "] " : "";
            String text = timeStr + msg.text().getString();
            Text.of(Fonts.MEDIUM, text, x + 4.0f, curY + logRowH * 0.5f, 5.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(220, 225, 235)).render(graphics);
            curY += logRowH;
        }
        return curY;
    }

    private void sendChatToBots() {
        if (this.chatInput.trim().isEmpty()) {
            return;
        }
        String msg = this.chatInput.trim();
        int sent = 0;
        for (BotClient bc : HeadlessBots.all()) {
            if (bc.getPhase() != BotPhase.PLAY) continue;
            bc.sendChat(msg);
            ++sent;
        }
        if (sent > 0) {
            this.setFeedback("\u041e\u0442\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u043e \u043e\u0442 " + sent + " \u0431\u043e\u0442\u043e\u0432", true);
            this.chatInput = "";
        } else {
            this.setFeedback("\u041d\u0435\u0442 \u0430\u043a\u0442\u0438\u0432\u043d\u044b\u0445 \u0431\u043e\u0442\u043e\u0432 \u0432 \u0438\u0433\u0440\u0435!", false);
        }
    }

    private void renderInputField(GuiGraphicsExtractor graphics, float ix, float iy, float iw, float ih, String text, String placeholder, boolean focused, float mouseX, float mouseY) {
        boolean hov = mouseX >= ix && mouseX <= ix + iw && mouseY >= iy && mouseY <= iy + ih;
        RoundedRect.of(ix, iy, iw, ih).radius(2.5f).color(focused ? Color.rgba(20, 24, 34, 210) : (hov ? Color.rgba(18, 20, 28, 170) : Color.rgba(14, 16, 22, 140))).render(graphics);
        Outline.of(ix, iy, iw, ih).radius(2.5f).thickness(focused ? 0.9f : 0.5f).color(focused ? this.getAccent().withAlpha(160) : Color.rgba(255, 255, 255, 18)).render(graphics);
        String display = text == null || text.isEmpty() ? placeholder : text;
        Color col = text == null || text.isEmpty() ? Color.rgb(100, 105, 120) : Color.rgb(238, 241, 248);
        float maxW = iw - 10.0f;
        while (Text.of(Fonts.MEDIUM, display).size(5.4f).width() > maxW && display.length() > 2) {
            display = display.substring(1);
        }
        Text.of(Fonts.MEDIUM, display, ix + 5.0f, iy + ih * 0.5f, 5.4f).verticalAlign(VerticalAlign.MIDDLE).color(col).render(graphics);
        if (focused) {
            float textW = text == null || text.isEmpty() ? 0.0f : Text.of(Fonts.MEDIUM, display).size(5.4f).width();
            float curX = ix + 5.0f + textW + 1.0f;
            float pulse = 0.5f + 0.5f * (float)Math.sin((double)((float)(System.currentTimeMillis() % 1000L) / 1000.0f) * Math.PI * 2.0);
            int cursorAlpha = (int)(40.0f + 215.0f * pulse);
            RoundedRect.of(curX, iy + (ih - 7.0f) * 0.5f, 0.8f, 7.0f).radius(0.4f).color(this.getAccent().withAlpha(cursorAlpha)).render(graphics);
        }
    }

    private void addClickBox(float cx, float cy, float cw, float ch, Runnable action) {
        this.clickBoxes.add(new float[]{cx, cy, cw, ch});
        this.clickActions.add(action);
    }

    private static void setTextInputFocus(boolean focused) {
        Screen screen;
        Minecraft mc = Minecraft.getInstance();
        Screen screen2 = screen = mc.gui != null ? mc.gui.screen() : null;
        if (screen != null) {
            mc.onTextInputFocusChange((GuiEventListener)screen, focused);
        }
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (button == 2) {
            for (int i = this.middleBoxes.size() - 1; i >= 0; --i) {
                float[] box = this.middleBoxes.get(i);
                if (!(box[3] > 0.0f) || !(mouseX >= box[0]) || !(mouseX <= box[0] + box[2]) || !(mouseY >= box[1]) || !(mouseY <= box[1] + box[3])) continue;
                this.middleActions.get(i).run();
                return true;
            }
            return false;
        }
        if (button == 1) {
            for (int i = this.clickBoxes.size() - 1; i >= 0; --i) {
                float[] box = this.clickBoxes.get(i);
                if (!(mouseX >= box[0]) || !(mouseX <= box[0] + box[2]) || !(mouseY >= box[1]) || !(mouseY <= box[1] + box[3])) continue;
                this.clickActions.get(i).run();
                return true;
            }
            this.focusedInput = -1;
            BotsPanel.setTextInputFocus(false);
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.draggingSlider != null && this.dragSliderTrackW > 0.0f) {
            float mx = (float)mouseX;
            Setting<?> setting = this.draggingSlider;
            if (setting instanceof NumberSetting) {
                NumberSetting ns = (NumberSetting)setting;
                double pct = Math.clamp((double)((mx - this.dragSliderTrackX) / this.dragSliderTrackW), 0.0, 1.0);
                double v = ns.min() + pct * (ns.max() - ns.min());
                if (ns.step() > 0.0) {
                    v = (double)Math.round(v / ns.step()) * ns.step();
                }
                ns.setValue(v);
                return true;
            }
            Setting<?> pct = this.draggingSlider;
            if (pct instanceof FloatSetting) {
                FloatSetting fs = (FloatSetting)pct;
                float pct2 = Math.clamp((mx - this.dragSliderTrackX) / this.dragSliderTrackW, 0.0f, 1.0f);
                float v = fs.getMin() + pct2 * (fs.getMax() - fs.getMin());
                if (fs.getStep() > 0.0f) {
                    v = (float)Math.round(v / fs.getStep()) * fs.getStep();
                }
                fs.set(v);
                return true;
            }
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingSlider = null;
    }

    public void mouseScrolled(double verticalAmount) {
        if (this.maxScroll <= 0.0f) {
            this.scrollTarget = Math.clamp(this.scrollTarget - (float)verticalAmount * 8.0f, -6.0f, 6.0f);
            return;
        }
        this.scrollTarget = Math.clamp(this.scrollTarget - (float)verticalAmount * 24.0f, -10.0f, this.maxScroll + 10.0f);
    }

    public boolean keyPressed(KeyEvent event) {
        if (this.focusedInput >= 0) {
            boolean ctrlDown;
            if (event.key() == 41) {
                this.focusedInput = -1;
                BotsPanel.setTextInputFocus(false);
                return true;
            }
            if (event.key() == 40 || event.key() == 88) {
                if (this.focusedInput == 3) {
                    this.sendChatToBots();
                }
                this.focusedInput = -1;
                BotsPanel.setTextInputFocus(false);
                return true;
            }
            if (event.key() == 42) {
                switch (this.focusedInput) {
                    case 0: {
                        if (this.nameInput.isEmpty()) break;
                        this.nameInput = this.nameInput.substring(0, this.nameInput.length() - 1);
                        break;
                    }
                    case 1: {
                        if (this.serverInput.isEmpty()) break;
                        this.serverInput = this.serverInput.substring(0, this.serverInput.length() - 1);
                        break;
                    }
                    case 2: {
                        if (this.proxyInput.isEmpty()) break;
                        this.proxyInput = this.proxyInput.substring(0, this.proxyInput.length() - 1);
                        break;
                    }
                    case 3: {
                        if (this.chatInput.isEmpty()) break;
                        this.chatInput = this.chatInput.substring(0, this.chatInput.length() - 1);
                        break;
                    }
                    case 4: {
                        if (this.batchPrefix.isEmpty()) break;
                        this.batchPrefix = this.batchPrefix.substring(0, this.batchPrefix.length() - 1);
                        break;
                    }
                    case 5: {
                        if (this.batchCountInput.isEmpty()) break;
                        this.batchCountInput = this.batchCountInput.substring(0, this.batchCountInput.length() - 1);
                    }
                }
                return true;
            }
            boolean bl = ctrlDown = event.hasControlDown() || event.hasControlDownWithQuirk() || (event.modifiers() & 2) != 0;
            if (event.isPaste() || ctrlDown && event.key() == 25) {
                String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
                if (clip != null && !clip.isEmpty()) {
                    String clean = clip.replaceAll("[\\r\\n]", "").trim();
                    switch (this.focusedInput) {
                        case 0: {
                            this.nameInput = this.nameInput + clean;
                            break;
                        }
                        case 1: {
                            this.serverInput = this.serverInput + clean;
                            break;
                        }
                        case 2: {
                            this.proxyInput = this.proxyInput + clean;
                            break;
                        }
                        case 3: {
                            this.chatInput = this.chatInput + clean;
                            break;
                        }
                        case 4: {
                            this.batchPrefix = this.batchPrefix + clean;
                            break;
                        }
                        case 5: {
                            this.batchCountInput = this.batchCountInput + clean;
                        }
                    }
                }
                return true;
            }
            if (event.isSelectAll() || ctrlDown && event.key() == 4) {
                switch (this.focusedInput) {
                    case 0: {
                        this.nameInput = "";
                        break;
                    }
                    case 1: {
                        this.serverInput = "";
                        break;
                    }
                    case 2: {
                        this.proxyInput = "";
                        break;
                    }
                    case 3: {
                        this.chatInput = "";
                        break;
                    }
                    case 4: {
                        this.batchPrefix = "";
                        break;
                    }
                    case 5: {
                        this.batchCountInput = "";
                    }
                }
                return true;
            }
            return true;
        }
        return false;
    }

    public boolean charTyped(CharacterEvent event) {
        if (this.focusedInput >= 0 && event.isAllowedChatCharacter()) {
            String c = event.codepointAsString();
            switch (this.focusedInput) {
                case 0: {
                    this.nameInput = this.nameInput + c;
                    break;
                }
                case 1: {
                    this.serverInput = this.serverInput + c;
                    break;
                }
                case 2: {
                    this.proxyInput = this.proxyInput + c;
                    break;
                }
                case 3: {
                    this.chatInput = this.chatInput + c;
                    break;
                }
                case 4: {
                    this.batchPrefix = this.batchPrefix + c;
                    break;
                }
                case 5: {
                    if (!Character.isDigit(c.charAt(0))) break;
                    this.batchCountInput = this.batchCountInput + c;
                }
            }
            return true;
        }
        return false;
    }

    public static String generateNick() {
        StringBuilder sb;
        String nick;
        Random r = new Random();
        do {
            sb = new StringBuilder();
            switch (r.nextInt(4)) {
                case 0: {
                    sb.append(NICK_ROOTS[r.nextInt(NICK_ROOTS.length)]).append(NICK_TAILS[r.nextInt(NICK_TAILS.length)]);
                    break;
                }
                case 1: {
                    sb.append(NICK_ROOTS[r.nextInt(NICK_ROOTS.length)]).append('_').append(NICK_TAILS[r.nextInt(NICK_TAILS.length)].toLowerCase(Locale.ROOT));
                    break;
                }
                case 2: {
                    int syl = 2 + r.nextInt(2);
                    for (int i = 0; i < syl; ++i) {
                        sb.append(NICK_CONSONANTS.charAt(r.nextInt(NICK_CONSONANTS.length())));
                        sb.append(NICK_VOWELS.charAt(r.nextInt(NICK_VOWELS.length())));
                    }
                    if (r.nextBoolean()) {
                        sb.append(NICK_CONSONANTS.charAt(r.nextInt(NICK_CONSONANTS.length())));
                    }
                    sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
                    break;
                }
                default: {
                    sb.append(NICK_ROOTS[r.nextInt(NICK_ROOTS.length)].toLowerCase(Locale.ROOT));
                }
            }
            int suffix = r.nextInt(3);
            if (suffix == 0) {
                sb.append(r.nextInt(100));
            } else if (suffix == 1) {
                sb.append(2000 + r.nextInt(15));
            }
            if (r.nextInt(6) != 0) continue;
            sb.insert(0, "x").append("x");
        } while ((nick = sb.toString()).length() < 3 || nick.length() > 16);
        return nick;
    }
}

