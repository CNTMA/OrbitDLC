/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.input.CharacterEvent
 *  net.minecraft.client.input.KeyEvent
 *  net.minecraft.world.item.ItemStack
 *  org.joml.Matrix3x2fStack
 */
package brokke.cc.ui.clickgui.autobuy;

import brokke.cc.core.module.ModuleManager;
import brokke.cc.core.module.impl.misc.AutoBuy;
import brokke.cc.core.module.impl.render.GuiSettings;
import brokke.cc.system.autobuy.AutoBuyCatalog;
import brokke.cc.system.autobuy.AutoBuyConfig;
import brokke.cc.system.autobuy.AutoBuyContext;
import brokke.cc.system.autobuy.AutoBuyEngine;
import brokke.cc.system.autobuy.AutoBuyIcons;
import brokke.cc.system.autobuy.HolyWorldStaffImporter;
import brokke.cc.system.autobuy.ItemEntry;
import brokke.cc.system.autobuy.PurchaseRecord;
import brokke.cc.system.bot.modules.BotAutoBuy;
import brokke.cc.ui.clickgui.TextEditor;
import brokke.cc.util.Log;
import brokke.cc.util.animation.Animation;
import brokke.cc.util.animation.Easing;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.DisplayUtil;
import brokke.cc.util.render.display.batch.DisplayBatcher;
import brokke.cc.util.render.display.outline.Outline;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.scissor.Scissor;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

public class AutoBuyPanel {
    private static final float TABS_H = 13.0f;
    private static final String[] TAB_LABELS = new String[]{"\u041f\u0440\u0435\u0434\u043c\u0435\u0442\u044b", "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438", "\u0418\u0441\u0442\u043e\u0440\u0438\u044f"};
    private final Animation scrollAnim = new Animation();
    private float scrollTarget = 0.0f;
    private float maxScroll = 0.0f;
    private int currentTab = 0;
    private final List<float[]> clickBoxes = new ArrayList<float[]>();
    private final List<Runnable> clickActions = new ArrayList<Runnable>();
    private PurchaseRecord copiedRecord;
    private long copiedAt;
    private String searchQuery = "";
    private boolean searchActive = false;
    private boolean searchSelectAll = false;
    private final StringBuilder searchBuffer = new StringBuilder();
    private ItemEntry editingEntry = null;
    private final StringBuilder editPriceBuffer = new StringBuilder();
    private boolean editingNick = false;
    private final StringBuilder editNickBuffer = new StringBuilder();
    private boolean editingMinPrice = false;
    private final StringBuilder editMinPriceBuffer = new StringBuilder();
    private ItemEntry editingDurEntry = null;
    private final StringBuilder editDurBuffer = new StringBuilder();
    private boolean editingBanNick = false;
    private final StringBuilder editBanNickBuffer = new StringBuilder();
    private final TextEditor banNickEditor = new TextEditor();
    private final Map<ItemEntry, Animation> abAnims = new HashMap<ItemEntry, Animation>();
    private final Map<ItemEntry, Animation> parseAnims = new HashMap<ItemEntry, Animation>();
    private final Map<ItemEntry, Animation> cardHoverAnims = new HashMap<ItemEntry, Animation>();
    private final Animation allSwitchAnim = new Animation();

    public AutoBuyPanel() {
        this.scrollAnim.setEasing(Easing.CUBIC_OUT);
        this.allSwitchAnim.setEasing(Easing.SPRING_SOFT);
    }

    private Color getAccent() {
        GuiSettings settings = GuiSettings.getInstance();
        return settings != null ? settings.getThemePrimary() : Color.rgb(40, 140, 255);
    }

    public void resetScroll() {
        this.scrollTarget = 0.0f;
        this.scrollAnim.set(0.0f);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void render(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float mouseX, float mouseY, float delta) {
        this.clickBoxes.clear();
        this.clickActions.clear();
        float tabsY = y;
        float tabGap = 4.0f;
        float totalTabsW = 0.0f;
        for (int i = 0; i < TAB_LABELS.length; ++i) {
            totalTabsW += Text.of(Fonts.SEMIBOLD, TAB_LABELS[i]).size(5.5f).width() + 14.0f;
            if (i >= TAB_LABELS.length - 1) continue;
            totalTabsW += tabGap;
        }
        float tabX = x + (w - totalTabsW) * 0.5f;
        int i = 0;
        while (i < TAB_LABELS.length) {
            Color tabBg;
            boolean hover;
            String label = TAB_LABELS[i];
            float tabW = Text.of(Fonts.SEMIBOLD, label).size(5.5f).width() + 14.0f;
            float tabH = 12.0f;
            boolean active = i == this.currentTab;
            boolean bl = hover = mouseX >= tabX && mouseX <= tabX + tabW && mouseY >= tabsY && mouseY <= tabsY + tabH;
            tabBg = active ? this.getAccent() : hover ? Color.rgba(255, 255, 255, 26) : Color.rgba(255, 255, 255, 12);
            Color textCol = active ? Color.WHITE : (hover ? Color.WHITE : Color.rgb(180, 185, 195));
            RoundedRect.of(tabX, tabsY, tabW, tabH).radius(3.0f).color(tabBg).render(graphics);
            Text.of(Fonts.SEMIBOLD, label, tabX + tabW * 0.5f, tabsY + tabH * 0.5f, 5.5f).centered().verticalAlign(VerticalAlign.MIDDLE).color(textCol).render(graphics);
            int tabIdx = i++;
            this.clickBoxes.add(new float[]{tabX, tabsY, tabW, tabH});
            this.clickActions.add(() -> {
                if (this.currentTab != tabIdx) {
                    this.commitEdits();
                    this.currentTab = tabIdx;
                    this.scrollTarget = 0.0f;
                    this.searchSelectAll = false;
                }
            });
            tabX += tabW + tabGap;
        }
        float contentY = tabsY + 13.0f + 4.0f;
        float contentH = h - (contentY - y);
        this.scrollAnim.animate(this.scrollTarget, delta, 9.5f);
        float currentScroll = this.scrollAnim.value();
        Scissor.push(graphics, x, contentY, w, contentH);
        float totalContentH = 0.0f;
        try {
            if (this.currentTab == 0) {
                totalContentH = this.renderItemsTab(graphics, x, contentY + currentScroll, w, mouseX, mouseY, delta, contentY, contentH);
            } else if (this.currentTab == 1) {
                totalContentH = this.renderSettingsTab(graphics, x, contentY + currentScroll, w, mouseX, mouseY, contentY, contentH);
            } else if (this.currentTab == 2) {
                totalContentH = this.renderHistoryTab(graphics, x, contentY + currentScroll, w, mouseX, mouseY, contentY, contentH);
            }
        }
        finally {
            Scissor.pop(graphics);
        }
        this.maxScroll = Math.max(0.0f, totalContentH - contentH);
        if (this.scrollTarget < -this.maxScroll) {
            this.scrollTarget = -this.maxScroll;
        }
        if (this.scrollTarget > 0.0f) {
            this.scrollTarget = 0.0f;
        }
    }

    private float renderItemsTab(GuiGraphicsExtractor graphics, float x, float y, float w, float mouseX, float mouseY, float delta, float clipTop, float clipH) {
        float startY = y;
        float curY = y;
        List<ItemEntry> allEntries = AutoBuyEngine.activeEngine().getConfig().entries;
        boolean allEnabled = !allEntries.isEmpty() && allEntries.stream().allMatch(e -> e.enabled);
        this.allSwitchAnim.animate(allEnabled ? 1.0f : 0.0f, delta, 7.0f);
        float allVal = Math.clamp(this.allSwitchAnim.value(), 0.0f, 1.0f);
        float btnW = 82.0f;
        float btnH = 13.0f;
        float btnX = x + w - btnW;
        float searchH = 13.0f;
        float searchW = w - btnW - 4.0f;
        boolean sHover = mouseX >= x && mouseX <= x + searchW && mouseY >= curY && mouseY <= curY + searchH;
        RoundedRect.of(x, curY, searchW, searchH).radius(3.0f).color(Color.rgba(255, 255, 255, 14)).render(graphics);
        Color searchOutline = this.searchActive ? this.getAccent() : (sHover ? Color.rgba(255, 255, 255, 42) : Color.rgba(255, 255, 255, 20));
        Outline.of(x, curY, searchW, searchH).radius(3.0f).thickness(0.5f).color(searchOutline).render(graphics);
        if (this.searchActive && this.searchSelectAll && !this.searchBuffer.isEmpty()) {
            float tw = Text.of(Fonts.SEMIBOLD, this.searchBuffer.toString()).size(5.2f).width();
            RoundedRect.of(x + 4.5f, curY + 2.0f, tw + 2.0f, searchH - 4.0f).radius(2.0f).color(this.getAccent().withAlpha(90)).render(graphics);
        }
        String sText = this.searchBuffer.isEmpty() ? (this.searchActive ? "_" : "\u041f\u043e\u0438\u0441\u043a \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432...") : String.valueOf(this.searchBuffer) + (this.searchActive ? "_" : "");
        Color sCol = this.searchBuffer.isEmpty() && !this.searchActive ? Color.rgba(130, 135, 145, 200) : Color.rgba(240, 240, 245, 255);
        Text.of(Fonts.SEMIBOLD, sText, x + 5.0f, curY + searchH * 0.5f, 5.2f).verticalAlign(VerticalAlign.MIDDLE).color(sCol).render(graphics);
        float sx = x;
        float sy = curY;
        this.clickBoxes.add(new float[]{sx, sy, searchW, searchH});
        this.clickActions.add(() -> {
            this.searchActive = true;
            this.searchSelectAll = false;
            this.editingEntry = null;
            this.editingNick = false;
            this.editingMinPrice = false;
            this.editingDurEntry = null;
            this.editingBanNick = false;
        });
        boolean bHover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= curY && mouseY <= curY + btnH;
        RoundedRect.of(btnX, curY, btnW, btnH).radius(3.0f).color(Color.rgba(255, 255, 255, bHover ? 20 : 14)).render(graphics);
        Color btnBorder = Color.rgba(255, 255, 255, bHover ? 38 : 20).lerp(this.getAccent(), allVal);
        Outline.of(btnX, curY, btnW, btnH).radius(3.0f).thickness(0.5f).color(btnBorder).render(graphics);
        String btnText = allEnabled ? "\u0412\u044b\u043a\u043b\u044e\u0447\u0438\u0442\u044c \u0432\u0441\u0451" : "\u0412\u043a\u043b\u044e\u0447\u0438\u0442\u044c \u0432\u0441\u0451";
        float btnTextW = Text.of(Fonts.SEMIBOLD, btnText).size(4.5f).width();
        float swW = 14.0f;
        float swH = 8.0f;
        float innerTotalW = btnTextW + 3.0f + swW;
        float innerStartX = btnX + (btnW - innerTotalW) * 0.5f;
        Color labelCol = Color.rgb(160, 165, 178).lerp(Color.WHITE, allVal);
        Text.of(Fonts.SEMIBOLD, btnText, innerStartX, curY + btnH * 0.5f, 4.5f).verticalAlign(VerticalAlign.MIDDLE).color(labelCol).render(graphics);
        float swX = innerStartX + btnTextW + 3.0f;
        float swY = curY + (btnH - swH) * 0.5f;
        Color trackBg = Color.rgba(255, 255, 255, 16).lerp(this.getAccent(), allVal);
        RoundedRect.of(swX, swY, swW, swH).radius(4.0f).color(trackBg).render(graphics);
        float knobSize = 5.5f;
        float knobX = swX + 1.25f + (swW - 2.5f - knobSize) * allVal;
        float knobY = swY + (swH - knobSize) * 0.5f;
        RoundedRect.of(knobX, knobY, knobSize, knobSize).radius(2.75f).color(Color.WHITE).render(graphics);
        float bx = btnX;
        float by = curY;
        this.clickBoxes.add(new float[]{bx, by, btnW, btnH});
        this.clickActions.add(() -> {
            boolean anyOff = allEntries.stream().anyMatch(e -> !e.enabled);
            for (ItemEntry e2 : allEntries) {
                e2.enabled = anyOff;
            }
            AutoBuyEngine.activeEngine().getConfig().saveConfig();
        });
        curY += searchH + 5.0f;
        ArrayList<ItemEntry> filtered = new ArrayList<ItemEntry>();
        for (ItemEntry e2 : allEntries) {
            String matchStr;
            String name;
            String string = name = e2.displayName != null && !e2.displayName.isBlank() ? e2.displayName : e2.itemId;
            if (!this.searchQuery.isEmpty() && !(matchStr = (name + " " + (e2.itemId != null ? e2.itemId : "") + " " + (e2.loreKeyword != null ? e2.loreKeyword : "")).toLowerCase(Locale.ROOT)).contains(this.searchQuery)) continue;
            filtered.add(e2);
        }
        filtered.sort(AutoBuyCatalog.ORDER);
        if (filtered.isEmpty()) {
            String emptyText = allEntries.isEmpty() ? "\u0421\u043f\u0438\u0441\u043e\u043a \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432 \u043f\u0443\u0441\u0442" : "\u041d\u0438\u0447\u0435\u0433\u043e \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e";
            Text.of(Fonts.SEMIBOLD, emptyText, x + w * 0.5f, curY + 25.0f, 5.8f).centered().color(Color.rgba(130, 135, 145, 180)).render(graphics);
            return curY + 50.0f - startY;
        }
        float gapX = 4.0f;
        float cardW = (w - gapX) * 0.5f;
        float cardH = 34.0f;
        float cardGap = 3.5f;
        for (int i = 0; i < filtered.size(); ++i) {
            ItemEntry e3 = (ItemEntry)filtered.get(i);
            int col = i % 2;
            int row = i / 2;
            float cardX = x + (float)col * (cardW + gapX);
            float cardY = curY + (float)row * (cardH + cardGap);
            if (!(cardY + cardH >= clipTop) || !(cardY <= clipTop + clipH)) continue;
            this.drawItemCard(graphics, e3, cardX, cardY, cardW, cardH, mouseX, mouseY, delta, clipTop, clipH);
        }
        int rows = (filtered.size() + 1) / 2;
        return (curY += (float)rows * (cardH + cardGap)) - startY;
    }

    private void drawItemCard(GuiGraphicsExtractor graphics, ItemEntry entry, float x, float y, float w, float h, float mouseX, float mouseY, float delta, float clipTop, float clipH) {
        boolean lHover;
        boolean isEditing;
        Color accent = this.getAccent();
        Animation abA = this.abAnims.computeIfAbsent(entry, k -> {
            Animation a = new Animation();
            a.setEasing(Easing.SPRING_SOFT);
            a.set(entry.enabled ? 1.0f : 0.0f);
            return a;
        });
        abA.animate(entry.enabled ? 1.0f : 0.0f, delta, 7.0f);
        float abVal = Math.clamp(abA.value(), 0.0f, 1.0f);
        Animation parseA = this.parseAnims.computeIfAbsent(entry, k -> {
            Animation a = new Animation();
            a.setEasing(Easing.SPRING_SOFT);
            a.set(entry.parseEnabled ? 1.0f : 0.0f);
            return a;
        });
        parseA.animate(entry.parseEnabled ? 1.0f : 0.0f, delta, 7.0f);
        float parseVal = Math.clamp(parseA.value(), 0.0f, 1.0f);
        boolean cardHover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        Animation hoverA = this.cardHoverAnims.computeIfAbsent(entry, k -> {
            Animation a = new Animation();
            a.setEasing(Easing.SPRING_SOFT);
            return a;
        });
        hoverA.animate(cardHover ? 1.0f : 0.0f, delta, 7.0f);
        float hVal = Math.clamp(hoverA.value(), 0.0f, 1.0f);
        RoundedRect.of(x, y, w, h).radius(5.0f).color(cardHover ? Color.rgba(18, 20, 28, 115) : Color.rgba(12, 12, 18, 90)).render(graphics);
        if (abVal > 0.01f) {
            Color activeOutline = accent.withAlpha(Math.round((140.0f + 50.0f * hVal) * abVal));
            Outline.of(x, y, w, h).radius(5.0f).thickness(0.6f).color(activeOutline).render(graphics);
        }
        float iconBoxSize = 18.0f;
        float iconBoxX = x + 4.0f;
        float iconBoxY = y + (h - iconBoxSize) * 0.5f;
        RoundedRect.of(iconBoxX, iconBoxY, iconBoxSize, iconBoxSize).radius(3.0f).color(Color.rgba(255, 255, 255, 8)).render(graphics);
        Outline.of(iconBoxX, iconBoxY, iconBoxSize, iconBoxSize).radius(3.0f).thickness(0.5f).color(Color.rgba(255, 255, 255, cardHover ? 28 : 16)).render(graphics);
        ItemStack stack = AutoBuyPanel.iconStack(entry);
        if (!stack.isEmpty() && iconBoxY + iconBoxSize > clipTop && iconBoxY < clipTop + clipH) {
            AutoBuyPanel.drawItemAt(graphics, stack, iconBoxX + 1.0f, iconBoxY + 1.0f, 16.0f);
        }
        float textX = x + 25.0f;
        float abLabelW = Text.of(Fonts.SEMIBOLD, "AB").size(4.5f).width();
        float swW = 16.0f;
        float swH = 9.0f;
        float abGroupW = abLabelW + 2.5f + swW;
        float abX = x + w - 4.5f - abGroupW;
        float abY = y + 3.5f;
        float parseLabelW = Text.of(Fonts.SEMIBOLD, "\u041f\u0430\u0440\u0441").size(4.5f).width();
        float boxSize = 9.5f;
        float parseGroupW = parseLabelW + 2.5f + boxSize;
        float parseX = abX - 5.0f - parseGroupW;
        float parseY = y + 3.5f;
        String name = entry.displayName != null && !entry.displayName.isBlank() ? entry.displayName : entry.itemId;
        float maxTitleW = parseX - textX - 4.0f;
        Object displayName = name;
        if (Text.of(Fonts.SEMIBOLD, (String)displayName).size(5.2f).width() > maxTitleW) {
            while (((String)displayName).length() > 3 && Text.of(Fonts.SEMIBOLD, (String)displayName + "...").size(5.2f).width() > maxTitleW) {
                displayName = ((String)displayName).substring(0, ((String)displayName).length() - 1);
            }
            displayName = (String)displayName + "...";
        }
        Text.of(Fonts.SEMIBOLD, (String)displayName, textX, y + 4.5f, 5.2f).color(Color.rgb(245, 245, 250)).render(graphics);
        Color parseLabelCol = Color.rgb(160, 165, 178).lerp(Color.WHITE, parseVal);
        Text.of(Fonts.SEMIBOLD, "\u041f\u0430\u0440\u0441", parseX, parseY + boxSize * 0.5f, 4.5f).verticalAlign(VerticalAlign.MIDDLE).color(parseLabelCol).render(graphics);
        float pBoxX = parseX + parseLabelW + 2.5f;
        float pBoxY = parseY;
        Color boxFill = Color.rgba(255, 255, 255, 14).lerp(accent, parseVal);
        RoundedRect.of(pBoxX, pBoxY, boxSize, boxSize).radius(2.5f).color(boxFill).render(graphics);
        Outline.of(pBoxX, pBoxY, boxSize, boxSize).radius(2.5f).thickness(0.5f).color(Color.rgba(255, 255, 255, 28).lerp(accent, parseVal)).render(graphics);
        if (parseVal > 0.05f) {
            DisplayUtil.renderCross(graphics, pBoxX + boxSize * 0.5f, pBoxY + boxSize * 0.5f, 2.3f, 1.15f, Color.WHITE.withAlpha(parseVal));
        }
        this.clickBoxes.add(new float[]{parseX - 1.0f, parseY - 1.0f, parseGroupW + 2.0f, boxSize + 2.0f});
        this.clickActions.add(() -> {
            entry.parseEnabled = !entry.parseEnabled;
            AutoBuyEngine.activeEngine().getConfig().saveConfig();
        });
        Color abLabelCol = Color.rgb(160, 165, 178).lerp(Color.WHITE, abVal);
        Text.of(Fonts.SEMIBOLD, "AB", abX, abY + swH * 0.5f, 4.5f).verticalAlign(VerticalAlign.MIDDLE).color(abLabelCol).render(graphics);
        float swTrackX = abX + abLabelW + 2.5f;
        float swTrackY = abY;
        Color abTrackBg = Color.rgba(255, 255, 255, 16).lerp(accent, abVal);
        RoundedRect.of(swTrackX, swTrackY, swW, swH).radius(4.5f).color(abTrackBg).render(graphics);
        Outline.of(swTrackX, swTrackY, swW, swH).radius(4.5f).thickness(0.5f).color(Color.rgba(255, 255, 255, 30).lerp(accent, abVal)).render(graphics);
        float knobSize = 6.0f;
        float knobX = swTrackX + 1.5f + (swW - 3.0f - knobSize) * abVal;
        float knobY = swTrackY + (swH - knobSize) * 0.5f;
        RoundedRect.of(knobX, knobY, knobSize, knobSize).radius(3.0f).color(Color.WHITE).render(graphics);
        this.clickBoxes.add(new float[]{abX - 1.0f, abY - 1.0f, abGroupW + 2.0f, swH + 2.0f});
        this.clickActions.add(() -> {
            entry.enabled = !entry.enabled;
            AutoBuyEngine.activeEngine().getConfig().saveConfig();
        });
        boolean bl = isEditing = this.editingEntry == entry;
        Object limitStr = isEditing ? (this.editPriceBuffer.isEmpty() ? "0" : this.editPriceBuffer.toString()) + "_" : (entry.maxUnitPrice > 0L ? "\u041b\u0438\u043c\u0438\u0442: " + AutoBuyPanel.fmt(entry.maxUnitPrice) + "$" : "+ \u041b\u0438\u043c\u0438\u0442");
        float limitTw = Text.of(Fonts.SEMIBOLD, (String)limitStr).size(4.5f).width();
        float limitW = Math.max(34.0f, limitTw + 8.0f);
        float limitH = 10.5f;
        float limitX = x + w - 4.5f - limitW;
        float limitY = y + 18.5f;
        boolean bl2 = lHover = mouseX >= limitX && mouseX <= limitX + limitW && mouseY >= limitY && mouseY <= limitY + limitH;
        if (isEditing) {
            RoundedRect.of(limitX, limitY, limitW, limitH).radius(2.5f).color(accent.withAlpha(50)).render(graphics);
            Outline.of(limitX, limitY, limitW, limitH).radius(2.5f).thickness(0.5f).color(accent.withAlpha(220)).render(graphics);
        } else {
            RoundedRect.of(limitX, limitY, limitW, limitH).radius(2.5f).color(Color.rgba(255, 255, 255, lHover ? 26 : 12)).render(graphics);
            Outline.of(limitX, limitY, limitW, limitH).radius(2.5f).thickness(0.5f).color(Color.rgba(255, 255, 255, lHover ? 45 : 20)).render(graphics);
        }
        Color limitCol = isEditing ? accent : (entry.maxUnitPrice > 0L ? Color.rgb(220, 225, 235) : Color.rgba(130, 135, 145, 200));
        Text.of(Fonts.SEMIBOLD, (String)limitStr, limitX + limitW * 0.5f, limitY + limitH * 0.5f, 4.5f).centered().verticalAlign(VerticalAlign.MIDDLE).color(limitCol).render(graphics);
        this.clickBoxes.add(new float[]{limitX, limitY, limitW, limitH});
        this.clickActions.add(() -> {
            this.editingEntry = entry;
            this.editPriceBuffer.setLength(0);
            if (entry.maxUnitPrice > 0L) {
                this.editPriceBuffer.append(entry.maxUnitPrice);
            }
            this.searchActive = false;
            this.editingNick = false;
            this.editingMinPrice = false;
            this.editingDurEntry = null;
            this.editingBanNick = false;
        });
        boolean hasDurability = entry.isDamageableEquipment();
        float durX = limitX;
        if (hasDurability) {
            boolean dHover;
            String durStr;
            boolean isEditingDur;
            boolean bl3 = isEditingDur = this.editingDurEntry == entry;
            if (isEditingDur) {
                durStr = (this.editDurBuffer.isEmpty() ? "0" : this.editDurBuffer.toString()) + "%_";
            } else {
                int dVal = entry.minDurabilityPercent > 0 ? entry.minDurabilityPercent : 50;
                durStr = "\u041f\u0440\u043e\u0447\u043d: " + dVal + "%";
            }
            float durTw = Text.of(Fonts.SEMIBOLD, durStr).size(4.5f).width();
            float durW = Math.max(30.0f, durTw + 7.0f);
            float durH = 10.5f;
            durX = limitX - durW - 3.0f;
            float durY = y + 18.5f;
            boolean bl4 = dHover = mouseX >= durX && mouseX <= durX + durW && mouseY >= durY && mouseY <= durY + durH;
            if (isEditingDur) {
                RoundedRect.of(durX, durY, durW, durH).radius(2.5f).color(accent.withAlpha(50)).render(graphics);
                Outline.of(durX, durY, durW, durH).radius(2.5f).thickness(0.5f).color(accent.withAlpha(220)).render(graphics);
                Text.of(Fonts.SEMIBOLD, durStr, durX + durW * 0.5f, durY + durH * 0.5f, 4.5f).centered().verticalAlign(VerticalAlign.MIDDLE).color(accent).render(graphics);
            } else {
                RoundedRect.of(durX, durY, durW, durH).radius(2.5f).color(Color.rgba(255, 255, 255, dHover ? 26 : 12)).render(graphics);
                Outline.of(durX, durY, durW, durH).radius(2.5f).thickness(0.5f).color(Color.rgba(255, 255, 255, dHover ? 45 : 20)).render(graphics);
                Color durCol = entry.minDurabilityPercent > 0 ? Color.rgb(220, 225, 235) : Color.rgba(150, 155, 165, 200);
                Text.of(Fonts.SEMIBOLD, durStr, durX + durW * 0.5f, durY + durH * 0.5f, 4.5f).centered().verticalAlign(VerticalAlign.MIDDLE).color(durCol).render(graphics);
            }
            this.clickBoxes.add(new float[]{durX, durY, durW, durH});
            this.clickActions.add(() -> {
                this.editingDurEntry = entry;
                this.editDurBuffer.setLength(0);
                int dVal = entry.minDurabilityPercent > 0 ? entry.minDurabilityPercent : 50;
                this.editDurBuffer.append(dVal);
                this.searchActive = false;
                this.editingEntry = null;
                this.editingNick = false;
                this.editingMinPrice = false;
                this.editingBanNick = false;
            });
        }
        float maxMTextW = (hasDurability ? durX : limitX) - textX - 4.0f;
        String mText = "\u0420\u044b\u043d\u043e\u043a: " + (String)(entry.marketPrice > 0L ? AutoBuyPanel.fmt(entry.marketPrice) + "$" : "\u2014");
        Object displayMText = mText;
        if (Text.of(Fonts.SEMIBOLD, (String)displayMText).size(4.5f).width() > maxMTextW) {
            while (((String)displayMText).length() > 3 && Text.of(Fonts.SEMIBOLD, (String)displayMText + "...").size(4.5f).width() > maxMTextW) {
                displayMText = ((String)displayMText).substring(0, ((String)displayMText).length() - 1);
            }
            displayMText = (String)displayMText + "...";
        }
        Text.of(Fonts.SEMIBOLD, (String)displayMText, textX, y + 20.0f, 4.5f).color(Color.rgba(145, 150, 160, 220)).render(graphics);
    }

    private float renderSettingsTab(GuiGraphicsExtractor graphics, float x, float y, float w, float mouseX, float mouseY, float clipTop, float clipH) {
        float startY = y;
        float curY = y;
        AutoBuyConfig cfg = AutoBuyEngine.activeEngine().getConfig();
        float rowH = 17.0f;
        float rowGap = 3.0f;
        curY = this.drawSettingToggle(graphics, x, curY, w, rowH, "\u0412\u043a\u043b\u044e\u0447\u0438\u0442\u044c AutoBuy", cfg.enabled, mouseX, mouseY, () -> {
            cfg.enabled = !cfg.enabled;
            AutoBuy autoBuy = ModuleManager.getModule(AutoBuy.class);
            if (autoBuy != null && autoBuy.isEnabled() != cfg.enabled) {
                autoBuy.toggle();
            }
            AutoBuyEngine.activeEngine().getConfig().saveConfig();
        });
        curY += rowGap;
        curY = this.drawSettingToggle(graphics, x, curY, w, rowH, "\u0410\u0432\u0442\u043e-\u043f\u0440\u043e\u0434\u0430\u0436\u0430 (AutoSell)", cfg.autoSell, mouseX, mouseY, () -> {
            cfg.autoSell = !cfg.autoSell;
            AutoBuyEngine.activeEngine().getConfig().saveConfig();
        });
        curY += rowGap;
        curY = this.drawSettingToggle(graphics, x, curY, w, rowH, "\u0410\u0432\u0442\u043e-\u0440\u0435\u043b\u0438\u0441\u0442", cfg.autoRelist, mouseX, mouseY, () -> {
            cfg.autoRelist = !cfg.autoRelist;
            AutoBuyEngine.activeEngine().getConfig().saveConfig();
        });
        curY += rowGap;
        curY = this.drawSettingToggle(graphics, x, curY, w, rowH, "\u0410\u0432\u0442\u043e-\u043f\u0430\u0440\u0441 \u0446\u0435\u043d", cfg.autoParse, mouseX, mouseY, () -> {
            cfg.autoParse = !cfg.autoParse;
            AutoBuyEngine.activeEngine().getConfig().saveConfig();
        });
        curY += rowGap;
        curY = this.drawSettingToggle(graphics, x, curY, w, rowH, "\u0410\u0432\u0442\u043e-\u043e\u043f\u043b\u0430\u0442\u0430 (AutoPay)", cfg.autoPay, mouseX, mouseY, () -> {
            cfg.autoPay = !cfg.autoPay;
            AutoBuyEngine.activeEngine().getConfig().saveConfig();
        });
        curY += rowGap;
        curY = this.drawSettingToggle(graphics, x, curY, w, rowH, "\u0410\u0432\u0442\u043e-\u043e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u0435 (AutoRefresh)", cfg.autoRefresh, mouseX, mouseY, () -> {
            cfg.autoRefresh = !cfg.autoRefresh;
            AutoBuyEngine.activeEngine().getConfig().saveConfig();
        });
        curY += rowGap;
        curY = this.drawSettingToggle(graphics, x, curY, w, rowH, "\u0411\u0430\u043d-\u043b\u0438\u0441\u0442 \u043f\u0440\u043e\u0434\u0430\u0432\u0446\u043e\u0432", cfg.banListEnabled, mouseX, mouseY, () -> {
            cfg.banListEnabled = !cfg.banListEnabled;
            AutoBuy autoBuy = ModuleManager.getModule(AutoBuy.class);
            if (autoBuy != null && AutoBuyContext.currentBotModule() == null) {
                autoBuy.getBanList().setState(cfg.banListEnabled);
            }
            AutoBuyEngine.activeEngine().getConfig().saveConfig();
        });
        curY += rowGap;
        curY = this.drawMinPriceRow(graphics, x, curY, w, rowH, cfg, mouseX, mouseY);
        curY += rowGap;
        curY = this.drawStepper(graphics, x, curY, w, rowH, "\u041f\u0440\u043e\u0446\u0435\u043d\u0442 \u043e\u0442 \u0440\u044b\u043d\u043a\u0430 (\u043f\u0430\u0440\u0441\u0438\u043d\u0433)", cfg.parsePercent + "%", mouseX, mouseY, () -> {
            cfg.parsePercent = Math.max(5, cfg.parsePercent - 5);
            AutoBuyEngine.activeEngine().getConfig().saveConfig();
        }, () -> {
            cfg.parsePercent = Math.min(100, cfg.parsePercent + 5);
            AutoBuyEngine.activeEngine().getConfig().saveConfig();
        });
        curY += rowGap;
        curY = this.drawStepper(graphics, x, curY, w, rowH, "\u0418\u043d\u0442\u0435\u0440\u0432\u0430\u043b \u0440\u0435\u043b\u0438\u0441\u0442\u0430", cfg.relistIntervalSeconds + " \u0441", mouseX, mouseY, () -> {
            cfg.relistIntervalSeconds = Math.max(5, cfg.relistIntervalSeconds - 5);
            AutoBuyEngine.activeEngine().getConfig().saveConfig();
        }, () -> {
            cfg.relistIntervalSeconds = Math.min(300, cfg.relistIntervalSeconds + 5);
            AutoBuyEngine.activeEngine().getConfig().saveConfig();
        });
        float nickW = 80.0f;
        float nickX = x + w - nickW - 4.0f;
        boolean nickHover = mouseX >= nickX && mouseX <= nickX + nickW && mouseY >= (curY += rowGap) + 2.0f && mouseY <= curY + rowH - 2.0f;
        RoundedRect.of(x, curY, w, rowH).radius(3.5f).color(Color.rgba(255, 255, 255, 8)).render(graphics);
        Text.of(Fonts.SEMIBOLD, "\u041d\u0438\u043a \u0434\u043b\u044f AutoPay", x + 6.0f, curY + rowH * 0.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(240, 240, 245)).render(graphics);
        Color nickBg = this.editingNick ? this.getAccent() : (nickHover ? Color.rgba(255, 255, 255, 35) : Color.rgba(255, 255, 255, 14));
        RoundedRect.of(nickX, curY + 2.0f, nickW, rowH - 4.0f).radius(2.5f).color(nickBg).render(graphics);
        String nickStr = this.editingNick ? (this.editNickBuffer.isEmpty() ? "_" : String.valueOf(this.editNickBuffer) + "_") : (cfg.autoPayNick.isEmpty() ? "(\u043d\u0435 \u0437\u0430\u0434\u0430\u043d)" : cfg.autoPayNick);
        Color nickCol = this.editingNick ? Color.WHITE : Color.rgb(230, 230, 235);
        Text.of(Fonts.SEMIBOLD, nickStr, nickX + nickW * 0.5f, curY + rowH * 0.5f, 4.5f).centered().verticalAlign(VerticalAlign.MIDDLE).color(nickCol).render(graphics);
        this.clickBoxes.add(new float[]{nickX, curY + 2.0f, nickW, rowH - 4.0f});
        this.clickActions.add(() -> {
            this.editingNick = true;
            this.editNickBuffer.setLength(0);
            this.editNickBuffer.append(cfg.autoPayNick);
            this.searchActive = false;
            this.editingEntry = null;
            this.editingMinPrice = false;
            this.editingDurEntry = null;
            this.editingBanNick = false;
        });
        curY += rowH + rowGap + 4.0f;
        curY = this.drawBanListSection(graphics, x, curY, w, mouseX, mouseY, clipTop, clipH);
        float rstW = w;
        boolean rstHover = mouseX >= x && mouseX <= x + rstW && mouseY >= (curY += rowGap + 4.0f) && mouseY <= curY + rowH;
        RoundedRect.of(x, curY, rstW, rowH).radius(3.5f).color(rstHover ? Color.rgba(220, 60, 60, 200) : Color.rgba(255, 255, 255, 12)).render(graphics);
        Outline.of(x, curY, rstW, rowH).radius(3.5f).thickness(0.5f).color(Color.rgba(255, 255, 255, 25)).render(graphics);
        Text.of(Fonts.SEMIBOLD, "\u0421\u0431\u0440\u043e\u0441\u0438\u0442\u044c \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u043a \u0441\u0442\u0430\u043d\u0434\u0430\u0440\u0442\u043d\u044b\u043c", x + rstW * 0.5f, curY + rowH * 0.5f, 5.0f).centered().verticalAlign(VerticalAlign.MIDDLE).color(rstHover ? Color.WHITE : Color.rgb(210, 210, 215)).render(graphics);
        this.clickBoxes.add(new float[]{x, curY, rstW, rowH});
        this.clickActions.add(() -> {
            cfg.resetToDefaults();
            BotAutoBuy module = AutoBuyContext.currentBotModule();
            if (module != null) {
                module.execute(() -> module.setToggled(false));
            }
        });
        return (curY += rowH + rowGap) - startY;
    }

    private float drawBanListSection(GuiGraphicsExtractor graphics, float x, float y, float w, float mouseX, float mouseY, float clipTop, float clipH) {
        AutoBuyConfig cfg = AutoBuyEngine.activeEngine().getConfig();
        float curY = y;
        RoundedRect.of(x, curY, w, 15.0f).radius(3.5f).color(Color.rgba(255, 255, 255, 18)).render(graphics);
        Text.of(Fonts.SEMIBOLD, HolyWorldStaffImporter.buttonLabel(cfg), x + w * 0.5f, curY + 7.5f, 5.2f).centered().verticalAlign(VerticalAlign.MIDDLE).color(this.getAccent()).render(graphics);
        if (!cfg.staffDownloading) {
            this.clickBoxes.add(new float[]{x, curY, w, 15.0f});
            this.clickActions.add(() -> HolyWorldStaffImporter.download(cfg));
        }
        curY += 19.0f;
        if (cfg.staffImportStatus != null && !cfg.staffImportStatus.isEmpty()) {
            Text.of(Fonts.SEMIBOLD, cfg.staffImportStatus, x + 4.0f, curY + 5.0f, 4.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(150, 160, 175)).render(graphics);
            curY += 13.0f;
        }
        float headerH = 15.0f;
        RoundedRect.of(x, curY, w, headerH).radius(3.5f).color(Color.rgba(255, 255, 255, 10)).render(graphics);
        Text.of(Fonts.SEMIBOLD, "\u0411\u0430\u043d-\u043b\u0438\u0441\u0442 \u043f\u0440\u043e\u0434\u0430\u0432\u0446\u043e\u0432", x + 6.0f, curY + headerH * 0.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(240, 240, 245)).render(graphics);
        String countText = "\u0417\u0430\u0431\u043b\u043e\u043a\u0438\u0440\u043e\u0432\u0430\u043d\u043e: " + cfg.banList.size();
        float ctw = Text.of(Fonts.SEMIBOLD, countText).size(4.5f).width();
        if (!cfg.banList.isEmpty()) {
            float clrW = 48.0f;
            float clrH = 11.0f;
            float clrX = x + w - clrW - 4.0f;
            float clrY = curY + 2.0f;
            boolean clrHover = mouseX >= clrX && mouseX <= clrX + clrW && mouseY >= clrY && mouseY <= clrY + clrH;
            RoundedRect.of(clrX, clrY, clrW, clrH).radius(2.0f).color(clrHover ? Color.rgba(220, 60, 60, 200) : Color.rgba(255, 255, 255, 18)).render(graphics);
            Text.of(Fonts.SEMIBOLD, "\u041e\u0447\u0438\u0441\u0442\u0438\u0442\u044c", clrX + clrW * 0.5f, clrY + clrH * 0.5f, 4.5f).centered().verticalAlign(VerticalAlign.MIDDLE).color(clrHover ? Color.WHITE : Color.rgb(210, 215, 225)).render(graphics);
            this.clickBoxes.add(new float[]{clrX, clrY, clrW, clrH});
            this.clickActions.add(cfg::clearBans);
            Text.of(Fonts.SEMIBOLD, countText, clrX - ctw - 8.0f, curY + headerH * 0.5f, 4.5f).verticalAlign(VerticalAlign.MIDDLE).color(this.getAccent()).render(graphics);
        } else {
            Text.of(Fonts.SEMIBOLD, countText, x + w - ctw - 6.0f, curY + headerH * 0.5f, 4.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgba(150, 155, 165, 220)).render(graphics);
        }
        float inputRowH = 14.0f;
        float addBtnW = 54.0f;
        float inputW = w - addBtnW - 3.0f;
        float inputX = x;
        float inputY = curY += headerH + 3.0f;
        boolean inHover = mouseX >= inputX && mouseX <= inputX + inputW && mouseY >= inputY && mouseY <= inputY + inputRowH;
        RoundedRect.of(inputX, inputY, inputW, inputRowH).radius(3.0f).color(Color.rgba(255, 255, 255, 12)).render(graphics);
        Color inBorder = this.editingBanNick ? this.getAccent() : (inHover ? Color.rgba(255, 255, 255, 40) : Color.rgba(255, 255, 255, 18));
        Outline.of(inputX, inputY, inputW, inputRowH).radius(3.0f).thickness(0.5f).color(inBorder).render(graphics);
        if (this.editingBanNick) {
            String banText = this.editBanNickBuffer.toString();
            float tx = inputX + 5.0f;
            float cy = inputY + inputRowH * 0.5f;
            if (this.banNickEditor.hasSelection(banText)) {
                float sx = Text.of(Fonts.SEMIBOLD, banText.substring(0, this.banNickEditor.selectionStart(banText))).size(4.75f).width();
                float ex = Text.of(Fonts.SEMIBOLD, banText.substring(0, this.banNickEditor.selectionEnd(banText))).size(4.75f).width();
                RoundedRect.of(tx + sx, inputY + 2.0f, ex - sx, inputRowH - 4.0f).radius(1.0f).color(this.getAccent().withAlpha(110)).render(graphics);
            }
            Text.of(Fonts.SEMIBOLD, banText, tx, cy, 4.75f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 238, 245)).render(graphics);
            float curX = tx + Text.of(Fonts.SEMIBOLD, banText.substring(0, this.banNickEditor.cursor(banText))).size(4.75f).width();
            float pulse = 0.5f + 0.5f * (float)Math.sin((double)((float)(System.currentTimeMillis() % 1000L) / 1000.0f) * Math.PI * 2.0);
            RoundedRect.of(curX + 0.4f, inputY + 2.5f, 0.8f, inputRowH - 5.0f).radius(0.4f).color(Color.rgba(245, 248, 255, (int)(40.0f + 215.0f * pulse))).render(graphics);
        } else {
            Text.of(Fonts.SEMIBOLD, "\u041d\u0438\u043a \u043f\u0440\u043e\u0434\u0430\u0432\u0446\u0430 \u0434\u043b\u044f \u0431\u0430\u043d\u0430...", inputX + 5.0f, inputY + inputRowH * 0.5f, 4.75f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgba(130, 135, 145, 200)).render(graphics);
        }
        this.clickBoxes.add(new float[]{inputX, inputY, inputW, inputRowH});
        this.clickActions.add(() -> {
            if (this.editingBanNick) {
                return;
            }
            this.editingBanNick = true;
            this.editBanNickBuffer.setLength(0);
            this.banNickEditor.reset("");
            this.searchActive = false;
            this.editingEntry = null;
            this.editingNick = false;
            this.editingMinPrice = false;
            this.editingDurEntry = null;
        });
        float addBtnX = inputX + inputW + 3.0f;
        float addBtnY = curY;
        boolean addHover = mouseX >= addBtnX && mouseX <= addBtnX + addBtnW && mouseY >= addBtnY && mouseY <= addBtnY + inputRowH;
        RoundedRect.of(addBtnX, addBtnY, addBtnW, inputRowH).radius(3.0f).color(addHover ? this.getAccent() : Color.rgba(255, 255, 255, 20)).render(graphics);
        Text.of(Fonts.SEMIBOLD, "+ \u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c", addBtnX + addBtnW * 0.5f, addBtnY + inputRowH * 0.5f, 4.75f).centered().verticalAlign(VerticalAlign.MIDDLE).color(addHover ? Color.WHITE : Color.rgb(240, 240, 245)).render(graphics);
        this.clickBoxes.add(new float[]{addBtnX, addBtnY, addBtnW, inputRowH});
        this.clickActions.add(() -> {
            String nick = this.editBanNickBuffer.toString().trim();
            if (!nick.isEmpty()) {
                cfg.addBan(nick);
                this.editBanNickBuffer.setLength(0);
                this.editingBanNick = false;
            } else {
                this.editingBanNick = true;
                this.searchActive = false;
                this.editingEntry = null;
                this.editingNick = false;
                this.editingMinPrice = false;
                this.editingDurEntry = null;
            }
        });
        curY += inputRowH + 4.0f;
        if (cfg.banList.isEmpty()) {
            RoundedRect.of(x, curY, w, 14.0f).radius(3.0f).color(Color.rgba(255, 255, 255, 6)).render(graphics);
            Text.of(Fonts.SEMIBOLD, "\u0421\u043f\u0438\u0441\u043e\u043a \u043f\u0443\u0441\u0442 \u2014 AutoBuy \u043f\u043e\u043a\u0443\u043f\u0430\u0435\u0442 \u0443 \u0432\u0441\u0435\u0445 \u043f\u0440\u043e\u0434\u0430\u0432\u0446\u043e\u0432", x + 6.0f, curY + 7.0f, 4.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgba(130, 135, 145, 200)).render(graphics);
            curY += 14.0f;
        } else {
            float chipH = 12.0f;
            float chipGapX = 3.0f;
            float chipGapY = 3.0f;
            float curChipX = x;
            float curChipY = curY;
            float maxChipRight = x + w;
            for (String banned : cfg.banList) {
                float closeW;
                float textW = Text.of(Fonts.SEMIBOLD, banned).size(4.5f).width();
                float chipW = textW + (closeW = 8.0f) + 10.0f;
                if (curChipX + chipW > maxChipRight && curChipX > x) {
                    curChipX = x;
                    curChipY += chipH + chipGapY;
                }
                if (curChipY + chipH <= clipTop || curChipY >= clipTop + clipH) {
                    curChipX += chipW + chipGapX;
                    continue;
                }
                boolean chipHover = mouseX >= curChipX && mouseX <= curChipX + chipW && mouseY >= curChipY && mouseY <= curChipY + chipH;
                RoundedRect.of(curChipX, curChipY, chipW, chipH).radius(2.5f).color(chipHover ? Color.rgba(255, 255, 255, 24) : Color.rgba(255, 255, 255, 12)).render(graphics);
                Outline.of(curChipX, curChipY, chipW, chipH).radius(2.5f).thickness(0.5f).color(chipHover ? Color.rgba(220, 70, 70, 160) : Color.rgba(255, 255, 255, 20)).render(graphics);
                Text.of(Fonts.SEMIBOLD, banned, curChipX + 4.0f, curChipY + chipH * 0.5f, 4.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(225, 230, 240)).render(graphics);
                float closeX = curChipX + chipW - closeW - 2.0f;
                Text.of(Fonts.SEMIBOLD, "x", closeX + closeW * 0.5f, curChipY + chipH * 0.5f, 6.0f).centered().color(chipHover ? Color.rgb(240, 80, 80) : Color.rgba(150, 155, 165, 200)).render(graphics);
                String toRemove = banned;
                this.clickBoxes.add(new float[]{curChipX, curChipY, chipW, chipH});
                this.clickActions.add(() -> cfg.removeBan(toRemove));
                curChipX += chipW + chipGapX;
            }
            curY = curChipY + chipH;
        }
        return curY;
    }

    private float drawMinPriceRow(GuiGraphicsExtractor graphics, float x, float y, float w, float h, AutoBuyConfig cfg, float mouseX, float mouseY) {
        boolean pHover;
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        RoundedRect.of(x, y, w, h).radius(5.0f).color(hover ? Color.rgba(18, 20, 28, 115) : Color.rgba(12, 12, 18, 90)).render(graphics);
        Text.of(Fonts.SEMIBOLD, "\u041c\u0438\u043d. \u043f\u043e\u0440\u043e\u0433 \u0446\u0435\u043d\u044b", x + 6.0f, y + h * 0.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(240, 240, 245)).render(graphics);
        float switchW = 18.0f;
        float switchH = 10.0f;
        float switchRadius = 5.0f;
        float switchX = x + w - switchW - 5.0f;
        float switchY = y + (h - switchH) * 0.5f;
        float knobSize = 7.6f;
        float knobRadius = 3.8f;
        float knobY = switchY + 1.2f;
        Color accent = this.getAccent();
        if (cfg.minPriceEnabled) {
            RoundedRect.of(switchX, switchY, switchW, switchH).radius(switchRadius).color(accent.withAlpha(220)).render(graphics);
            float knobX = switchX + switchW - 1.2f - knobSize;
            RoundedRect.of(knobX, knobY, knobSize, knobSize).radius(knobRadius).color(Color.WHITE).render(graphics);
        } else {
            RoundedRect.of(switchX, switchY, switchW, switchH).radius(switchRadius).color(Color.rgba(255, 255, 255, 18)).render(graphics);
            Outline.of(switchX, switchY, switchW, switchH).radius(switchRadius).thickness(0.5f).color(Color.rgba(255, 255, 255, 30)).render(graphics);
            float knobX = switchX + 1.2f;
            RoundedRect.of(knobX, knobY, knobSize, knobSize).radius(knobRadius).color(Color.rgba(212, 216, 226, 220)).render(graphics);
        }
        String priceStr = this.editingMinPrice ? (this.editMinPriceBuffer.isEmpty() ? "0" : this.editMinPriceBuffer.toString()) + "_" : AutoBuyPanel.fmt(cfg.minPrice) + "$";
        float ptw = Text.of(Fonts.SEMIBOLD, priceStr).size(4.5f).width();
        float pBoxW = Math.max(52.0f, ptw + 12.0f);
        float pBoxH = 11.0f;
        float pBoxX = switchX - pBoxW - 5.0f;
        float pBoxY = y + (h - pBoxH) * 0.5f;
        boolean bl = pHover = mouseX >= pBoxX && mouseX <= pBoxX + pBoxW && mouseY >= pBoxY && mouseY <= pBoxY + pBoxH;
        if (this.editingMinPrice) {
            RoundedRect.of(pBoxX, pBoxY, pBoxW, pBoxH).radius(2.5f).color(accent.withAlpha(50)).render(graphics);
            Outline.of(pBoxX, pBoxY, pBoxW, pBoxH).radius(2.5f).thickness(0.5f).color(accent.withAlpha(220)).render(graphics);
            Text.of(Fonts.SEMIBOLD, priceStr, pBoxX + pBoxW * 0.5f, pBoxY + pBoxH * 0.5f, 4.5f).centered().verticalAlign(VerticalAlign.MIDDLE).color(accent).render(graphics);
        } else {
            RoundedRect.of(pBoxX, pBoxY, pBoxW, pBoxH).radius(2.5f).color(Color.rgba(255, 255, 255, pHover ? 26 : 14)).render(graphics);
            Outline.of(pBoxX, pBoxY, pBoxW, pBoxH).radius(2.5f).thickness(0.5f).color(Color.rgba(255, 255, 255, pHover ? 45 : 20)).render(graphics);
            Color pTextCol = cfg.minPriceEnabled ? Color.rgb(235, 235, 240) : Color.rgba(150, 155, 165, 200);
            Text.of(Fonts.SEMIBOLD, priceStr, pBoxX + pBoxW * 0.5f, pBoxY + pBoxH * 0.5f, 4.5f).centered().verticalAlign(VerticalAlign.MIDDLE).color(pTextCol).render(graphics);
        }
        this.clickBoxes.add(new float[]{pBoxX, pBoxY, pBoxW, pBoxH});
        this.clickActions.add(() -> {
            this.editingMinPrice = true;
            this.editMinPriceBuffer.setLength(0);
            if (cfg.minPrice > 0L) {
                this.editMinPriceBuffer.append(cfg.minPrice);
            }
            this.editingNick = false;
            this.editingEntry = null;
            this.searchActive = false;
            this.editingDurEntry = null;
            this.editingBanNick = false;
        });
        this.clickBoxes.add(new float[]{switchX - 2.0f, switchY - 2.0f, switchW + 4.0f, switchH + 4.0f});
        this.clickActions.add(() -> {
            cfg.minPriceEnabled = !cfg.minPriceEnabled;
            AutoBuyEngine.activeEngine().getConfig().saveConfig();
        });
        this.clickBoxes.add(new float[]{x, y, pBoxX - x, h});
        this.clickActions.add(() -> {
            cfg.minPriceEnabled = !cfg.minPriceEnabled;
            AutoBuyEngine.activeEngine().getConfig().saveConfig();
        });
        return y + h;
    }

    private float drawSettingToggle(GuiGraphicsExtractor graphics, float x, float y, float w, float h, String label, boolean active, float mouseX, float mouseY, Runnable action) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        RoundedRect.of(x, y, w, h).radius(5.0f).color(hover ? Color.rgba(18, 20, 28, 115) : Color.rgba(12, 12, 18, 90)).render(graphics);
        Text.of(Fonts.SEMIBOLD, label, x + 6.0f, y + h * 0.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(active ? Color.WHITE : Color.rgb(210, 215, 225)).render(graphics);
        float switchW = 18.0f;
        float switchH = 10.0f;
        float switchRadius = 5.0f;
        float switchX = x + w - switchW - 5.0f;
        float switchY = y + (h - switchH) / 2.0f;
        float knobSize = 7.6f;
        float knobRadius = 3.8f;
        float knobY = switchY + 1.2f;
        Color accent = this.getAccent();
        if (active) {
            RoundedRect.of(switchX, switchY, switchW, switchH).radius(switchRadius).color(accent.withAlpha(220)).render(graphics);
            float knobX = switchX + switchW - 1.2f - knobSize;
            RoundedRect.of(knobX, knobY, knobSize, knobSize).radius(knobRadius).color(Color.WHITE).render(graphics);
        } else {
            RoundedRect.of(switchX, switchY, switchW, switchH).radius(switchRadius).color(Color.rgba(255, 255, 255, 18)).render(graphics);
            Outline.of(switchX, switchY, switchW, switchH).radius(switchRadius).thickness(0.5f).color(Color.rgba(255, 255, 255, hover ? 50 : 30)).render(graphics);
            float knobX = switchX + 1.2f;
            RoundedRect.of(knobX, knobY, knobSize, knobSize).radius(knobRadius).color(Color.rgba(212, 216, 226, 220)).render(graphics);
        }
        this.clickBoxes.add(new float[]{x, y, w, h});
        this.clickActions.add(action);
        return y + h;
    }

    private float drawStepper(GuiGraphicsExtractor graphics, float x, float y, float w, float h, String label, String valueStr, float mouseX, float mouseY, Runnable dec, Runnable inc) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        RoundedRect.of(x, y, w, h).radius(5.0f).color(hover ? Color.rgba(18, 20, 28, 115) : Color.rgba(12, 12, 18, 90)).render(graphics);
        Text.of(Fonts.SEMIBOLD, label, x + 6.0f, y + h * 0.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(240, 240, 245)).render(graphics);
        float btnSize = 11.0f;
        float valW = 36.0f;
        float incX = x + w - btnSize - 4.0f;
        float valX = incX - valW - 2.0f;
        float decX = valX - btnSize - 2.0f;
        float by = y + (h - btnSize) * 0.5f;
        boolean decHover = mouseX >= decX && mouseX <= decX + btnSize && mouseY >= by && mouseY <= by + btnSize;
        RoundedRect.of(decX, by, btnSize, btnSize).radius(2.0f).color(decHover ? this.getAccent() : Color.rgba(255, 255, 255, 20)).render(graphics);
        Text.of(Fonts.SEMIBOLD, "-", decX + btnSize * 0.5f, by + btnSize * 0.5f, 5.0f).centered().verticalAlign(VerticalAlign.MIDDLE).color(decHover ? Color.WHITE : Color.rgb(240, 240, 245)).render(graphics);
        this.clickBoxes.add(new float[]{decX, by, btnSize, btnSize});
        this.clickActions.add(dec);
        Text.of(Fonts.SEMIBOLD, valueStr, valX + valW * 0.5f, by + btnSize * 0.5f, 4.75f).centered().verticalAlign(VerticalAlign.MIDDLE).color(this.getAccent()).render(graphics);
        boolean incHover = mouseX >= incX && mouseX <= incX + btnSize && mouseY >= by && mouseY <= by + btnSize;
        RoundedRect.of(incX, by, btnSize, btnSize).radius(2.0f).color(incHover ? this.getAccent() : Color.rgba(255, 255, 255, 20)).render(graphics);
        Text.of(Fonts.SEMIBOLD, "+", incX + btnSize * 0.5f, by + btnSize * 0.5f, 5.0f).centered().verticalAlign(VerticalAlign.MIDDLE).color(incHover ? Color.WHITE : Color.rgb(240, 240, 245)).render(graphics);
        this.clickBoxes.add(new float[]{incX, by, btnSize, btnSize});
        this.clickActions.add(inc);
        return y + h;
    }

    private float renderHistoryTab(GuiGraphicsExtractor graphics, float x, float y, float w, float mouseX, float mouseY, float clipTop, float clipH) {
        float startY = y;
        float curY = y;
        List<PurchaseRecord> history = AutoBuyEngine.activeEngine().getConfig().history;
        float barH = 14.0f;
        RoundedRect.of(x, curY, w, barH).radius(3.0f).color(Color.rgba(255, 255, 255, 10)).render(graphics);
        String info = "\u0412\u0441\u0435\u0433\u043e \u0437\u0430\u043f\u0438\u0441\u0435\u0439: " + history.size();
        Text.of(Fonts.SEMIBOLD, info, x + 5.0f, curY + barH * 0.5f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(200, 205, 215)).render(graphics);
        float clrW = 54.0f;
        float clrX = x + w - clrW - 3.0f;
        float clrY = curY + 1.5f;
        float clrH = barH - 3.0f;
        boolean clrHover = mouseX >= clrX && mouseX <= clrX + clrW && mouseY >= clrY && mouseY <= clrY + clrH;
        RoundedRect.of(clrX, clrY, clrW, clrH).radius(2.0f).color(clrHover ? Color.rgba(220, 60, 60, 220) : Color.rgba(255, 255, 255, 20)).render(graphics);
        Text.of(Fonts.SEMIBOLD, "\u041e\u0447\u0438\u0441\u0442\u0438\u0442\u044c", clrX + clrW * 0.5f, clrY + clrH * 0.5f, 4.5f).centered().verticalAlign(VerticalAlign.MIDDLE).color(clrHover ? Color.WHITE : Color.rgb(220, 220, 230)).render(graphics);
        this.clickBoxes.add(new float[]{clrX, clrY, clrW, clrH});
        this.clickActions.add(() -> {
            history.clear();
            AutoBuyEngine.activeEngine().getConfig().saveConfig();
        });
        curY += barH + 4.0f;
        if (history.isEmpty()) {
            Text.of(Fonts.SEMIBOLD, "\u0418\u0441\u0442\u043e\u0440\u0438\u044f \u043f\u043e\u043a\u0443\u043f\u043e\u043a \u043f\u0443\u0441\u0442\u0430", x + 6.0f, curY + 6.0f, 5.5f).color(Color.rgba(140, 145, 155, 220)).render(graphics);
            return curY + 20.0f - startY;
        }
        float rowH = 22.0f;
        float rowGap = 3.0f;
        Color accent = this.getAccent();
        for (PurchaseRecord r : history) {
            if (curY + rowH >= clipTop && curY <= clipTop + clipH) {
                String tail;
                RoundedRect.of(x, curY, w, rowH).radius(3.5f).color(Color.rgba(255, 255, 255, 8)).render(graphics);
                Outline.of(x, curY, w, rowH).radius(3.5f).thickness(0.5f).color(Color.rgba(255, 255, 255, 16)).render(graphics);
                ItemStack stack = AutoBuyPanel.iconStack(r.itemId, r.label);
                float itemY = curY + 3.0f;
                if (!stack.isEmpty() && itemY + 16.0f > clipTop && itemY < clipTop + clipH) {
                    AutoBuyPanel.drawItemAt(graphics, stack, x + 3.5f, itemY, 16.0f);
                }
                float textX = x + 23.0f;
                Color markCol = r.success ? accent : Color.rgb(220, 90, 90);
                String mark = r.success ? "+ " : "- ";
                String name = mark + r.getDisplayName();
                Text.of(Fonts.SEMIBOLD, name, textX, curY + 3.0f, 5.25f).color(markCol).render(graphics);
                String prefix = "\u041f\u0440\u043e\u0434\u0430\u0432\u0435\u0446: ";
                boolean hasSeller = r.seller != null && !r.seller.isEmpty();
                boolean copied = hasSeller && this.copiedRecord == r && System.currentTimeMillis() - this.copiedAt < 1500L;
                String nick = hasSeller ? r.seller : "\u2014";
                float subY = curY + 12.0f;
                float prefixW = Text.of(Fonts.SEMIBOLD, prefix).size(4.5f).width();
                float nickW = Text.of(Fonts.SEMIBOLD, nick).size(4.5f).width();
                float nickX = textX + prefixW;
                boolean nickHover = hasSeller && mouseX >= nickX && mouseX <= nickX + nickW && mouseY >= subY - 1.0f && mouseY <= subY + 6.0f && mouseY >= clipTop && mouseY <= clipTop + clipH;
                Text.of(Fonts.SEMIBOLD, prefix, textX, subY, 4.5f).color(Color.rgba(145, 150, 160, 220)).render(graphics);
                Text.of(Fonts.SEMIBOLD, nick, nickX, subY, 4.5f).color(copied || nickHover ? accent : Color.rgba(200, 205, 215, 230)).render(graphics);
                tail = copied ? " \u2022 \u0441\u043a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u043d\u043e" : !r.success && r.failReason != null ? " \u2022 " + r.failReason : "";
                if (!tail.isEmpty()) {
                    Text.of(Fonts.SEMIBOLD, tail, nickX + nickW, subY, 4.5f).color(copied ? accent : Color.rgba(145, 150, 160, 220)).render(graphics);
                }
                if (hasSeller) {
                    float boxTop = Math.max(subY - 1.0f, clipTop);
                    float boxBottom = Math.min(subY + 6.0f, clipTop + clipH);
                    if (boxBottom > boxTop) {
                        this.clickBoxes.add(new float[]{nickX, boxTop, nickW, boxBottom - boxTop});
                        String seller = r.seller;
                        this.clickActions.add(() -> {
                            Minecraft.getInstance().keyboardHandler.setClipboard(seller);
                            this.copiedRecord = r;
                            this.copiedAt = System.currentTimeMillis();
                        });
                    }
                }
                String priceStr = "x" + r.count + "  " + AutoBuyPanel.fmt(r.totalPrice) + "$";
                float ptw = Text.of(Fonts.SEMIBOLD, priceStr).size(5.0f).width();
                Text.of(Fonts.SEMIBOLD, priceStr, x + w - ptw - 6.0f, curY + 7.5f, 5.0f).color(accent).render(graphics);
            }
            curY += rowH + rowGap;
        }
        return curY - startY;
    }

    public void commitEdits() {
        String s;
        if (this.editingEntry != null) {
            try {
                s = this.editPriceBuffer.toString().trim();
                this.editingEntry.maxUnitPrice = s.isEmpty() ? 0L : Long.parseLong(s);
                AutoBuyEngine.activeEngine().getConfig().saveConfig();
            }
            catch (Exception ignored) {
                Log.swallow(ignored);
            }
            this.editingEntry = null;
        }
        if (this.editingNick) {
            AutoBuyEngine.activeEngine().getConfig().autoPayNick = this.editNickBuffer.toString().trim();
            AutoBuyEngine.activeEngine().getConfig().saveConfig();
            this.editingNick = false;
        }
        if (this.editingMinPrice) {
            try {
                s = this.editMinPriceBuffer.toString().trim();
                AutoBuyEngine.activeEngine().getConfig().minPrice = s.isEmpty() ? 0L : Long.parseLong(s);
                AutoBuyEngine.activeEngine().getConfig().saveConfig();
            }
            catch (Exception ignored) {
                Log.swallow(ignored);
            }
            this.editingMinPrice = false;
        }
        if (this.editingDurEntry != null) {
            try {
                s = this.editDurBuffer.toString().trim();
                this.editingDurEntry.minDurabilityPercent = s.isEmpty() ? 0 : Math.clamp((long)Integer.parseInt(s), 0, 100);
                AutoBuyEngine.activeEngine().getConfig().saveConfig();
            }
            catch (Exception ignored) {
                Log.swallow(ignored);
            }
            this.editingDurEntry = null;
        }
        if (this.editingBanNick) {
            String nick = this.editBanNickBuffer.toString().trim();
            if (!nick.isEmpty()) {
                AutoBuyEngine.activeEngine().getConfig().addBan(nick);
            }
            this.editBanNickBuffer.setLength(0);
            this.editingBanNick = false;
        }
    }

    public boolean isTyping() {
        return this.editingBanNick || this.editingNick || this.editingMinPrice || this.editingEntry != null || this.editingDurEntry != null || this.searchActive;
    }

    public boolean mouseClicked(float mx, float my, int button) {
        if (button != 1) {
            return false;
        }
        for (int i = 0; i < this.clickBoxes.size(); ++i) {
            float[] box = this.clickBoxes.get(i);
            if (!(mx >= box[0]) || !(mx <= box[0] + box[2]) || !(my >= box[1]) || !(my <= box[1] + box[3]) || i >= this.clickActions.size()) continue;
            this.commitEdits();
            this.clickActions.get(i).run();
            return true;
        }
        this.searchActive = false;
        this.searchSelectAll = false;
        this.commitEdits();
        return false;
    }

    public boolean mouseScrolled(double verticalAmount) {
        this.scrollTarget += (float)verticalAmount * 24.0f;
        if (this.scrollTarget < -this.maxScroll) {
            this.scrollTarget = -this.maxScroll;
        }
        if (this.scrollTarget > 0.0f) {
            this.scrollTarget = 0.0f;
        }
        return true;
    }

    public boolean keyPressed(KeyEvent event) {
        boolean isCtrl;
        boolean bl = isCtrl = event.hasControlDown() || InputConstants.isKeyDown((int)224) || InputConstants.isKeyDown((int)228);
        if (isCtrl && event.key() == 9) {
            this.commitEdits();
            this.currentTab = 0;
            this.searchSelectAll = this.searchActive = !this.searchActive;
            return true;
        }
        if (this.editingEntry != null) {
            if (event.key() == 40 || event.key() == 88) {
                try {
                    String s = this.editPriceBuffer.toString().trim();
                    this.editingEntry.maxUnitPrice = s.isEmpty() ? 0L : Long.parseLong(s);
                    AutoBuyEngine.activeEngine().getConfig().saveConfig();
                }
                catch (Exception ignored) {
                    Log.swallow(ignored);
                }
                this.editingEntry = null;
                return true;
            }
            if (event.key() == 41) {
                this.editingEntry = null;
                return true;
            }
            if (event.key() == 42) {
                if (!this.editPriceBuffer.isEmpty()) {
                    this.editPriceBuffer.deleteCharAt(this.editPriceBuffer.length() - 1);
                }
                return true;
            }
            return true;
        }
        if (this.editingNick) {
            if (event.key() == 40 || event.key() == 88) {
                AutoBuyEngine.activeEngine().getConfig().autoPayNick = this.editNickBuffer.toString().trim();
                AutoBuyEngine.activeEngine().getConfig().saveConfig();
                this.editingNick = false;
                return true;
            }
            if (event.key() == 41) {
                this.editingNick = false;
                return true;
            }
            if (event.key() == 42) {
                if (!this.editNickBuffer.isEmpty()) {
                    this.editNickBuffer.deleteCharAt(this.editNickBuffer.length() - 1);
                }
                return true;
            }
            return true;
        }
        if (this.editingMinPrice) {
            if (event.key() == 40 || event.key() == 88) {
                try {
                    String s = this.editMinPriceBuffer.toString().trim();
                    AutoBuyEngine.activeEngine().getConfig().minPrice = s.isEmpty() ? 0L : Long.parseLong(s);
                    AutoBuyEngine.activeEngine().getConfig().saveConfig();
                }
                catch (Exception ignored) {
                    Log.swallow(ignored);
                }
                this.editingMinPrice = false;
                return true;
            }
            if (event.key() == 41) {
                this.editingMinPrice = false;
                return true;
            }
            if (event.key() == 42) {
                if (!this.editMinPriceBuffer.isEmpty()) {
                    this.editMinPriceBuffer.deleteCharAt(this.editMinPriceBuffer.length() - 1);
                }
                return true;
            }
            return true;
        }
        if (this.editingDurEntry != null) {
            if (event.key() == 40 || event.key() == 88) {
                try {
                    String s = this.editDurBuffer.toString().trim();
                    this.editingDurEntry.minDurabilityPercent = s.isEmpty() ? 0 : Math.clamp((long)Integer.parseInt(s), 0, 100);
                    AutoBuyEngine.activeEngine().getConfig().saveConfig();
                }
                catch (Exception ignored) {
                    Log.swallow(ignored);
                }
                this.editingDurEntry = null;
                return true;
            }
            if (event.key() == 41) {
                this.editingDurEntry = null;
                return true;
            }
            if (event.key() == 42) {
                if (!this.editDurBuffer.isEmpty()) {
                    this.editDurBuffer.deleteCharAt(this.editDurBuffer.length() - 1);
                }
                return true;
            }
            return true;
        }
        if (this.editingBanNick) {
            if (event.key() == 40 || event.key() == 88) {
                String nick = this.editBanNickBuffer.toString().trim();
                if (!nick.isEmpty()) {
                    AutoBuyEngine.activeEngine().getConfig().addBan(nick);
                }
                this.editBanNickBuffer.setLength(0);
                this.editingBanNick = false;
                return true;
            }
            if (event.key() == 41) {
                this.editingBanNick = false;
                this.editBanNickBuffer.setLength(0);
                return true;
            }
            TextEditor.Result res = this.banNickEditor.key(event, this.editBanNickBuffer.toString(), 32);
            if (res.handled()) {
                this.editBanNickBuffer.setLength(0);
                this.editBanNickBuffer.append(res.text());
            }
            return true;
        }
        if (this.searchActive) {
            if (isCtrl && event.key() == 4) {
                this.searchSelectAll = true;
                return true;
            }
            if (isCtrl && (event.key() == 6 || event.key() == 27)) {
                Minecraft.getInstance().keyboardHandler.setClipboard(this.searchBuffer.toString());
                if (event.key() == 27) {
                    this.searchBuffer.setLength(0);
                    this.searchQuery = "";
                    this.searchSelectAll = false;
                }
                return true;
            }
            if (isCtrl && event.key() == 25) {
                String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
                if (clip != null) {
                    clip = clip.replace("\r", "").replace("\n", "");
                    if (this.searchSelectAll) {
                        this.searchBuffer.setLength(0);
                        this.searchSelectAll = false;
                    }
                    this.searchBuffer.append(clip);
                    this.searchQuery = this.searchBuffer.toString().toLowerCase(Locale.ROOT);
                }
                return true;
            }
            if (event.key() == 40 || event.key() == 88 || event.key() == 41) {
                this.searchActive = false;
                this.searchSelectAll = false;
                return true;
            }
            if (event.key() == 42 || event.key() == 76) {
                if (this.searchSelectAll) {
                    this.searchBuffer.setLength(0);
                    this.searchQuery = "";
                    this.searchSelectAll = false;
                } else if (!this.searchBuffer.isEmpty()) {
                    this.searchBuffer.deleteCharAt(this.searchBuffer.length() - 1);
                    this.searchQuery = this.searchBuffer.toString().toLowerCase(Locale.ROOT);
                }
                return true;
            }
            return true;
        }
        return false;
    }

    public boolean charTyped(CharacterEvent event) {
        if (this.editingEntry != null) {
            if (event.isAllowedChatCharacter()) {
                String s = event.codepointAsString();
                for (char c : s.toCharArray()) {
                    if (c < '0' || c > '9' || this.editPriceBuffer.length() >= 12) continue;
                    this.editPriceBuffer.append(c);
                }
            }
            return true;
        }
        if (this.editingNick) {
            if (event.isAllowedChatCharacter()) {
                this.editNickBuffer.append(event.codepointAsString());
            }
            return true;
        }
        if (this.editingMinPrice) {
            if (event.isAllowedChatCharacter()) {
                String s = event.codepointAsString();
                for (char c : s.toCharArray()) {
                    if (c < '0' || c > '9' || this.editMinPriceBuffer.length() >= 12) continue;
                    this.editMinPriceBuffer.append(c);
                }
            }
            return true;
        }
        if (this.editingDurEntry != null) {
            if (event.isAllowedChatCharacter()) {
                String s = event.codepointAsString();
                for (char c : s.toCharArray()) {
                    if (c < '0' || c > '9' || this.editDurBuffer.length() >= 3) continue;
                    this.editDurBuffer.append(c);
                }
            }
            return true;
        }
        if (this.editingBanNick) {
            if (event.isAllowedChatCharacter()) {
                String s = event.codepointAsString();
                for (char c : s.toCharArray()) {
                    if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c >= '\u0430' && c <= '\u044f') && (c < '\u0410' || c > '\u042f')) continue;
                    String typed = this.banNickEditor.type(this.editBanNickBuffer.toString(), String.valueOf(c), 32);
                    this.editBanNickBuffer.setLength(0);
                    this.editBanNickBuffer.append(typed);
                }
            }
            return true;
        }
        if (this.searchActive) {
            if (event.isAllowedChatCharacter()) {
                if (this.searchSelectAll) {
                    this.searchBuffer.setLength(0);
                    this.searchSelectAll = false;
                }
                this.searchBuffer.append(event.codepointAsString());
                this.searchQuery = this.searchBuffer.toString().toLowerCase(Locale.ROOT);
            }
            return true;
        }
        return false;
    }

    public static void drawItemAt(GuiGraphicsExtractor graphics, ItemStack stack, float fx, float fy, float sizeFixed) {
        DisplayBatcher.flush();
        Matrix3x2fStack mats = graphics.pose();
        mats.pushMatrix();
        mats.translate(fx + sizeFixed * 0.5f, fy + sizeFixed * 0.5f);
        float f = sizeFixed / 16.0f;
        mats.scale(f, f);
        mats.translate(-8.0f, -8.0f);
        try {
            graphics.item(stack, 0, 0);
        }
        catch (Throwable ignored) {
            Log.swallow(ignored);
        }
        mats.popMatrix();
        DisplayBatcher.flush();
    }

    private static ItemStack iconStack(ItemEntry entry) {
        if (entry == null) {
            return ItemStack.EMPTY;
        }
        return AutoBuyPanel.iconStack(entry.itemId, entry.displayName);
    }

    public static ItemStack iconStack(String itemId, String displayName) {
        return AutoBuyIcons.stack(itemId, displayName);
    }

    public static String fmt(long v) {
        if (v <= 0L) {
            return "0";
        }
        StringBuilder b = new StringBuilder(Long.toString(v));
        for (int i = b.length() - 3; i > 0; i -= 3) {
            b.insert(i, ' ');
        }
        return b.toString();
    }
}

