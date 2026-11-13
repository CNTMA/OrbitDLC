/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.world.item.ItemStack
 */
package brokke.cc.ui.clickgui.tab;

import brokke.cc.core.module.ModuleManager;
import brokke.cc.core.module.impl.misc.AutoSbor;
import brokke.cc.core.module.impl.misc.AutoSborConfig;
import brokke.cc.core.module.impl.misc.AutoSborItems;
import brokke.cc.core.setting.NumberSetting;
import brokke.cc.ui.autosbor.AutoSborScreen;
import brokke.cc.ui.clickgui.autobuy.AutoBuyPanel;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.ui.hud.legacy.ConfigBridge;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.TextAlign;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public final class AutoSborTab
extends GuiComponent {
    private static final String SEARCH_FIELD = "sbor:search";
    private static final String CONFIG_FIELD = "sbor:cfg";
    private static final int COLS = 9;
    private static String search = "";
    private static String configName = "";
    private int tab = 0;
    private float tabAnim = 0.0f;
    private int selType = -1;
    private int selIndex = -1;
    private int targetType = -1;
    private int targetIndex = -1;
    private ItemStack countStack = ItemStack.EMPTY;
    private NumberSetting countSetting;

    public AutoSborTab(ClickGuiContext ctx) {
        super(ctx);
    }

    public static boolean commit(String id, String value) {
        if (SEARCH_FIELD.equals(id)) {
            search = value;
            return true;
        }
        if (CONFIG_FIELD.equals(id)) {
            configName = value;
            return true;
        }
        return false;
    }

    public void reset() {
        this.tab = 0;
        this.tabAnim = 0.0f;
    }

    public float renderAutoSbor(GuiGraphicsExtractor graphics, float x, float y, float w, float prog, float top, float bottom, long now) {
        if (this.ctx.minecraft().level == null) {
            float a = prog * GuiDraw.easeOut((float)(now - this.ctx.switchTime) / 380.0f);
            Text.of(Fonts.MEDIUM, "\u0417\u0430\u0439\u0434\u0438\u0442\u0435 \u0432 \u043c\u0438\u0440, \u0447\u0442\u043e\u0431\u044b \u043d\u0430\u0441\u0442\u0440\u043e\u0438\u0442\u044c AutoSbor", x + w * 0.5f, y + 40.0f, 5.6f).centered().color(Color.rgb(140, 145, 156).withAlpha(a)).render(graphics);
            return 80.0f;
        }
        AutoSborScreen.embedded();
        if (SEARCH_FIELD.equals(this.ctx.focus.abField)) {
            search = this.ctx.focus.abInput.toString();
        }
        if (CONFIG_FIELD.equals(this.ctx.focus.abField)) {
            configName = this.ctx.focus.abInput.toString();
        }
        if (this.selType >= 0 && AutoSborTab.stackAt(this.selType, this.selIndex).isEmpty()) {
            this.selType = -1;
            this.selIndex = -1;
        }
        float startY = y;
        float appear = GuiDraw.easeOut((float)(now - this.ctx.switchTime) / 380.0f);
        float a = prog * appear;
        y += (1.0f - appear) * 8.0f;
        Color accent = GuiDraw.getAccent();
        y = this.renderTabs(graphics, x, y, w, a, accent, top, bottom);
        float toast = 1.0f - GuiDraw.easeOut(Math.max(0.0f, (float)(now - this.ctx.configToastTime - 1800L) / 500.0f));
        if (this.ctx.configToastText != null && toast > 0.01f && now - this.ctx.configToastTime < 2400L) {
            float toastIn = GuiDraw.easeOut((float)(now - this.ctx.configToastTime) / 250.0f);
            Text.of(Fonts.MEDIUM, this.ctx.configToastText, x + 2.0f + (1.0f - toastIn) * 4.0f, y + 2.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(this.ctx.configToastColor.withAlpha(a * toast * toastIn)).render(graphics);
            y += 9.0f;
        }
        y = this.tab == 0 ? this.renderBuild(graphics, x, y, w, a, accent, top, bottom, now) : (this.tab == 1 ? this.renderCatalog(graphics, x, y, w, a, accent, top, bottom, now) : this.renderConfigs(graphics, x, y, w, a, accent, top, bottom, now));
        return y - startY;
    }

    private float renderTabs(GuiGraphicsExtractor graphics, float x, float y, float w, float a, Color accent, float top, float bottom) {
        String[] tabs = new String[]{"\u0421\u0431\u043e\u0440\u043a\u0430", "\u041a\u0430\u0442\u0430\u043b\u043e\u0433", "\u041a\u043e\u043d\u0444\u0438\u0433\u0438"};
        int[] counts = new int[]{AutoSborTab.filledCount(), -1, AutoSborConfig.listAutoSborConfigs().size()};
        float tabH = 14.0f;
        float tabW = (w - 4.0f) / (float)tabs.length;
        RoundedRect.of(x, y, w, tabH + 4.0f).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round(6.0f * a))).render(graphics);
        float tabPos = this.tabAnim;
        this.tabAnim = this.smooth(this.tabAnim, this.tab, 14.0f);
        RoundedRect.of(x + 2.0f + tabPos * tabW, y + 2.0f, tabW, tabH).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round(14.0f * a))).render(graphics);
        for (int i = 0; i < tabs.length; ++i) {
            int idx = i;
            float tx = x + 2.0f + (float)i * tabW;
            boolean hover = GuiDraw.canHit(y, tabH, top, bottom) && this.hovered(tx, y + 2.0f, tabW, tabH);
            String key = "sbor-tab-" + i;
            float hv = this.anim(key, this.hov(key, hover), 7.0f);
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
            this.hit(tx, y + 2.0f, tabW, tabH, () -> this.switchTab(idx), null);
        }
        return y + tabH + 4.0f + 6.0f;
    }

    private void switchTab(int idx) {
        if (this.tab == idx) {
            return;
        }
        this.ctx.focus.setAbField(null, "");
        this.tab = idx;
        this.ctx.switchTime = System.currentTimeMillis();
        this.ctx.scrollTarget = 0.0f;
        ClientSound.ui(ClientSound.Ui.CLICK);
    }

    private float renderBuild(GuiGraphicsExtractor graphics, float x, float y, float w, float a, Color accent, float top, float bottom, long now) {
        int i;
        AutoSbor module = ModuleManager.getModule(AutoSbor.class);
        float bh = 16.0f;
        float gap = 5.0f;
        float clearW = 62.0f;
        float bw = (w - clearW - gap * 2.0f) * 0.5f;
        this.ctx.widgets.renderConfigButton(graphics, "sbor-start", "\u0417\u0430\u043f\u0443\u0441\u0442\u0438\u0442\u044c \u0441\u0431\u043e\u0440", ">", x, y, bw, bh, a, false, top, bottom, () -> {
            if (module != null) {
                module.startAutoSbor();
            }
        });
        this.ctx.widgets.renderConfigButton(graphics, "sbor-price", "\u041f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0441\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c", "M", x + bw + gap, y, bw, bh, a, false, top, bottom, () -> {
            if (module != null) {
                module.startPriceCheck();
            }
        });
        this.ctx.widgets.renderConfigButton(graphics, "sbor-clear", "\u041e\u0447\u0438\u0441\u0442\u0438\u0442\u044c", "x", x + w - clearW, y, clearW, bh, a, true, top, bottom, this::clearAll);
        float slotGap = 3.0f;
        float slot = Math.min(24.0f, (w - 8.0f * slotGap) / 9.0f);
        float gridW = 9.0f * slot + 8.0f * slotGap;
        float gx = x + (w - gridW) * 0.5f;
        float titleY = GuiDraw.sectionTitle(graphics, "\u0421\u0435\u0442", gx - 1.0f, y += bh + 8.0f, a);
        Text.of(Fonts.MEDIUM, AutoSborTab.filledCount() + "/" + (AutoSbor.TOP_SLOTS.length + AutoSbor.INVENTORY_SLOTS.length), gx + gridW, y + 6.0f, 4.8f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
        y = titleY;
        for (i = 0; i < AutoSbor.TOP_SLOTS.length; ++i) {
            this.renderSlot(graphics, 0, i, gx + (float)i * (slot + slotGap), y, slot, a, accent, top, bottom);
        }
        y += slot + 7.0f;
        y = GuiDraw.sectionTitle(graphics, "\u0418\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044c", gx - 1.0f, y, a);
        for (i = 0; i < AutoSbor.INVENTORY_SLOTS.length; ++i) {
            int row = i / 9;
            int col = i % 9;
            this.renderSlot(graphics, 1, i, gx + (float)col * (slot + slotGap), y + (float)row * (slot + slotGap), slot, a, accent, top, bottom);
        }
        Text.of(Fonts.MEDIUM, "\u041b\u041a\u041c: \u043d\u0430\u0441\u0442\u0440\u043e\u0438\u0442\u044c     \u0421\u041a\u041c: \u0443\u0431\u0440\u0430\u0442\u044c     \u043f\u0443\u0441\u0442\u043e\u0439 \u0441\u043b\u043e\u0442: \u0432\u044b\u0431\u0440\u0430\u0442\u044c \u0438\u0437 \u043a\u0430\u0442\u0430\u043b\u043e\u0433\u0430", x + w * 0.5f, (y += 3.0f * slot + 2.0f * slotGap + 5.0f) + 3.0f, 4.4f).centered().color(Color.rgb(100, 105, 116).withAlpha(a)).render(graphics);
        y += 12.0f;
        if (this.selType >= 0) {
            y = this.renderItemSettings(graphics, x, y, w, a, accent, top, bottom);
        }
        return y;
    }

    private void renderSlot(GuiGraphicsExtractor graphics, int type, int index, float sx, float sy, float size, float a, Color accent, float top, float bottom) {
        ItemStack stack = AutoSborTab.stackAt(type, index);
        boolean selected = this.selType == type && this.selIndex == index;
        boolean hover = GuiDraw.canHit(sy, size, top, bottom) && this.hovered(sx, sy, size, size);
        String key = "sbor-slot-" + type + "-" + index;
        float hv = this.anim(key, this.hov(key, hover), 8.0f);
        float sel = this.anim(String.valueOf(key) + "-sel", selected, 8.0f);
        RoundedRect.of(sx, sy, size, size).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round((7.0f + 7.0f * hv) * a))).render(graphics);
        if (sel > 0.01f) {
            RoundedRect.of(sx, sy, size, size).radius(4.0f).horizontalGradient(accent.withAlpha(0.28f * sel * a), GuiDraw.getAccent2().withAlpha(0.28f * sel * a)).render(graphics);
        }
        if (!stack.isEmpty()) {
            float icon = size * 0.66f;
            AutoBuyPanel.drawItemAt(graphics, stack, sx + (size - icon) * 0.5f, sy + (size - icon) * 0.5f, icon);
            String badge = this.badge(type, index, stack);
            if (!badge.isEmpty()) {
                Text.of(Fonts.MEDIUM, badge, sx + size - 1.5f, sy + size - 3.5f, 4.0f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 238, 245).withAlpha(a)).render(graphics);
            }
        }
        if (GuiDraw.canHit(sy, size, top, bottom)) {
            this.ctx.hits.hit(sx, sy, size, size, () -> this.clickSlot(type, index), () -> this.clickSlot(type, index));
            if (!stack.isEmpty()) {
                this.ctx.hits.middle(sx, sy, size, size, () -> {
                    this.removeSlot(type, index);
                    ClientSound.ui(ClientSound.Ui.CLICK);
                });
            }
        }
    }

    private String badge(int type, int index, ItemStack stack) {
        if (AutoSborItems.isDurabilityConfigurable(stack)) {
            int dur = AutoSbor.getMinDurability(type, index);
            return dur > 0 ? dur + "%" : "";
        }
        return stack.getCount() > 1 ? String.valueOf(stack.getCount()) : "";
    }

    private void clickSlot(int type, int index) {
        ClientSound.ui(ClientSound.Ui.CLICK);
        if (AutoSborTab.stackAt(type, index).isEmpty()) {
            this.targetType = type;
            this.targetIndex = index;
            this.selType = -1;
            this.selIndex = -1;
            this.switchTab(1);
            return;
        }
        if (this.selType == type && this.selIndex == index) {
            this.selType = -1;
            this.selIndex = -1;
        } else {
            this.selType = type;
            this.selIndex = index;
        }
    }

    private float renderItemSettings(GuiGraphicsExtractor graphics, float x, float y, float w, float a, Color accent, float top, float bottom) {
        int type = this.selType;
        int index = this.selIndex;
        ItemStack stack = AutoSborTab.stackAt(type, index);
        float headH = 26.0f;
        RoundedRect.of(x, y, w, headH).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round(6.0f * a))).render(graphics);
        AutoBuyPanel.drawItemAt(graphics, stack, x + 6.0f, y + 5.0f, 16.0f);
        Text.of(Fonts.MEDIUM, GuiDraw.fit(AutoSborScreen.getItemDisplayName(stack), 5.6f, w - 90.0f), x + 28.0f, y + 9.0f, 5.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(240, 242, 248).withAlpha(a)).render(graphics);
        Text.of(Fonts.MEDIUM, (type == 0 ? "\u0421\u0435\u0442, \u0441\u043b\u043e\u0442 " : "\u0418\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044c, \u0441\u043b\u043e\u0442 ") + (index + 1), x + 28.0f, y + 18.0f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
        float rw = 52.0f;
        this.ctx.widgets.renderConfigButton(graphics, "sbor-remove", "\u0423\u0431\u0440\u0430\u0442\u044c", "x", x + w - rw - 5.0f, y + (headH - 13.0f) * 0.5f, rw, 13.0f, a, true, top, bottom, () -> this.removeSlot(type, index));
        y += headH + 5.0f;
        if (AutoSborItems.isDurabilityConfigurable(stack)) {
            int dur = AutoSbor.getMinDurability(type, index);
            y = this.renderChoiceRow(graphics, "dur", "\u041c\u0438\u043d. \u043f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u044c", new int[]{0, 25, 50, 75, 90, 100}, dur, "%", x, y, w, a, accent, top, bottom, v -> AutoSbor.setMinDurability(type, index, v), false);
            if (AutoSborItems.isNetheriteSword(stack)) {
                y = this.renderChoiceRow(graphics, "sharp", "\u041e\u0441\u0442\u0440\u043e\u0442\u0430", AutoSborTab.range(1, 9), AutoSbor.getSharpness(type, index), "", x, y, w, a, accent, top, bottom, v -> AutoSbor.setSharpness(type, index, v), true);
                boolean crit = AutoSbor.getCrit2(type, index);
                y = this.renderChoiceRow(graphics, "crit", "\u041a\u0440\u0438\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0439 II", new int[]{1}, crit ? 1 : 0, "", x, y, w, a, accent, top, bottom, v -> AutoSbor.setCrit2(type, index, v == 1), true, "\u0412\u043a\u043b");
            } else if (AutoSborItems.isNetheritePickaxe(stack)) {
                y = this.renderChoiceRow(graphics, "eff", "\u042d\u0444\u0444\u0435\u043a\u0442\u0438\u0432\u043d\u043e\u0441\u0442\u044c", AutoSborTab.range(1, 10), AutoSbor.getEfficiency(type, index), "", x, y, w, a, accent, top, bottom, v -> AutoSbor.setEfficiency(type, index, v), true);
            } else if (AutoSborItems.isNetheriteHelmet(stack) || AutoSborItems.isNetheriteChestplate(stack) || AutoSborItems.isNetheriteLeggings(stack) || AutoSborItems.isNetheriteBoots(stack)) {
                y = this.renderChoiceRow(graphics, "prot", "\u0417\u0430\u0449\u0438\u0442\u0430", AutoSborTab.range(1, 5), AutoSbor.getProtection(type, index), "", x, y, w, a, accent, top, bottom, v -> AutoSbor.setProtection(type, index, v), true);
                y = this.renderChoiceRow(graphics, "unb", "\u041f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u044c", AutoSborTab.range(1, 5), AutoSbor.getUnbreaking(type, index), "", x, y, w, a, accent, top, bottom, v -> AutoSbor.setUnbreaking(type, index, v), true);
                if (AutoSborItems.isNetheriteHelmet(stack)) {
                    y = this.renderChoiceRow(graphics, "resp", "\u041f\u043e\u0434\u0432\u043e\u0434\u043d\u043e\u0435 \u0434\u044b\u0445\u0430\u043d\u0438\u0435", AutoSborTab.range(1, 3), AutoSbor.getRespiration(type, index), "", x, y, w, a, accent, top, bottom, v -> AutoSbor.setRespiration(type, index, v), true);
                } else if (AutoSborItems.isNetheriteBoots(stack)) {
                    y = this.renderChoiceRow(graphics, "ds", "\u041f\u043e\u0434\u0432\u043e\u0434\u043d\u0430\u044f \u0445\u043e\u0434\u044c\u0431\u0430", AutoSborTab.range(1, 3), AutoSbor.getDepthStrider(type, index), "", x, y, w, a, accent, top, bottom, v -> AutoSbor.setDepthStrider(type, index, v), true);
                }
            }
        } else {
            int max = AutoSborScreen.getMaxStack(stack);
            int count = Math.clamp((long)stack.getCount(), 1, max);
            if (max > 1) {
                if (this.countStack != stack || this.countSetting == null || this.countSetting.max() != (double)max) {
                    if (this.ctx.dragSetting == this.countSetting) {
                        this.ctx.dragSetting = null;
                    }
                    this.ctx.settings.sliderPos.remove(this.countSetting);
                    this.countStack = stack;
                    this.countSetting = new NumberSetting("\u041a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e", 1.0, 1.0, (double)max, 1.0, " / " + max);
                    this.countSetting.onChange(value -> this.setCount(stack, value.intValue(), max));
                }
                if (this.ctx.dragSetting != this.countSetting && this.countSetting.getInt() != count) {
                    this.countSetting.setValue(Double.valueOf(count));
                }
                y += this.ctx.settings.renderSetting(graphics, this.countSetting, x + 7.0f, y, w - 14.0f, a, accent, top, bottom) + 5.0f;
                count = Math.clamp((long)stack.getCount(), 1, max);
                y = this.renderChoiceRow(graphics, "count", "\u0411\u044b\u0441\u0442\u0440\u044b\u0439 \u0432\u044b\u0431\u043e\u0440", AutoSborScreen.getPresets(max), count, "", x, y, w, a, accent, top, bottom, v -> this.setCount(stack, v, max), false);
            } else {
                Text.of(Fonts.MEDIUM, "\u041f\u0440\u0435\u0434\u043c\u0435\u0442 \u043d\u0435 \u0441\u0442\u0430\u043a\u0430\u0435\u0442\u0441\u044f, \u043d\u0430\u0441\u0442\u0440\u0430\u0438\u0432\u0430\u0442\u044c \u043d\u0435\u0447\u0435\u0433\u043e", x + w * 0.5f, y + 6.0f, 4.8f).centered().color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
                y += 14.0f;
            }
        }
        return y;
    }

    private float renderChoiceRow(GuiGraphicsExtractor graphics, String id, String label, int[] values, int current, String suffix, float x, float y, float w, float a, Color accent, float top, float bottom, IntConsumer apply, boolean toggleOff) {
        return this.renderChoiceRow(graphics, id, label, values, current, suffix, x, y, w, a, accent, top, bottom, apply, toggleOff, null);
    }

    private float renderChoiceRow(GuiGraphicsExtractor graphics, String id, String label, int[] values, int current, String suffix, float x, float y, float w, float a, Color accent, float top, float bottom, IntConsumer apply, boolean toggleOff, String singleLabel) {
        float right;
        float h = 17.0f;
        String key = "sbor-row-" + id;
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        float hv = this.anim(key, this.hov(key, hover), 7.0f);
        RoundedRect.of(x, y, w, h).radius(4.5f).color(Color.rgba(255, 255, 255, Math.round((6.0f + 4.0f * hv) * a))).render(graphics);
        String shown = toggleOff && current == 0 && singleLabel == null ? label + ": \u043b\u044e\u0431\u0430\u044f" : label;
        Text.of(Fonts.MEDIUM, shown, x + 7.0f, y + h * 0.5f, 5.4f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(215, 219, 228).withAlpha(a)).render(graphics);
        float chipH = 11.0f;
        float chipGap = 2.5f;
        float cx = right = x + w - 6.0f;
        for (int i = values.length - 1; i >= 0; --i) {
            int v = values[i];
            String text = singleLabel != null ? singleLabel : v + suffix;
            float cw = Math.max(11.0f, Text.of(Fonts.MEDIUM, text).size(4.6f).width() + 7.0f);
            boolean active = v == current;
            this.ctx.widgets.renderAbChip(graphics, String.valueOf(key) + "-" + v, text, cx -= cw, y + (h - chipH) * 0.5f, cw, chipH, a, accent, active, true, top, bottom, () -> {
                apply.accept(toggleOff && active ? 0 : v);
                ConfigBridge.onSettingChanged();
                ClientSound.ui(ClientSound.Ui.CLICK);
            });
            cx -= chipGap;
        }
        return y + h + 3.0f;
    }

    private float renderCatalog(GuiGraphicsExtractor graphics, float x, float y, float w, float a, Color accent, float top, float bottom, long now) {
        List<AutoSborScreen.ItemEntry> items;
        this.ctx.widgets.renderAbField(graphics, SEARCH_FIELD, "\u041f\u043e\u0438\u0441\u043a \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432...", "M", x, y, w, 15.0f, a, accent, top, bottom, now, search);
        y += 19.0f;
        if (this.targetType >= 0) {
            String text = "\u0414\u043e\u0431\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0432 " + (this.targetType == 0 ? "\u0441\u0435\u0442" : "\u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044c") + ", \u0441\u043b\u043e\u0442 " + (this.targetIndex + 1);
            Text.of(Fonts.MEDIUM, text, x + 1.0f, y + 5.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(accent.withAlpha(a)).render(graphics);
            float cw = 44.0f;
            this.ctx.widgets.renderAbChip(graphics, "sbor-target-cancel", "\u041e\u0442\u043c\u0435\u043d\u0438\u0442\u044c", x + w - cw, y, cw, 10.0f, a, accent, false, true, top, bottom, () -> {
                this.targetType = -1;
                this.targetIndex = -1;
                ClientSound.ui(ClientSound.Ui.CLICK);
            });
            y += 14.0f;
        }
        if ((items = AutoSborTab.catalog(search)).isEmpty()) {
            Text.of(Fonts.MEDIUM, "\u041d\u0438\u0447\u0435\u0433\u043e \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e", x + w * 0.5f, y + 20.0f, 5.4f).centered().color(Color.rgb(120, 125, 136).withAlpha(a)).render(graphics);
            return y + 40.0f;
        }
        float gap = 5.0f;
        float cardW = (w - gap) * 0.5f;
        float cardH = 24.0f;
        for (int i = 0; i < items.size(); ++i) {
            AutoSborScreen.ItemEntry entry = items.get(i);
            float cx = x + (float)(i % 2) * (cardW + gap);
            float cy = y + (float)(i / 2) * (cardH + gap);
            this.renderCatalogCard(graphics, entry, cx, cy, cardW, cardH, a, accent, top, bottom);
        }
        int rows = (items.size() + 1) / 2;
        return y + (float)rows * (cardH + gap);
    }

    private void renderCatalogCard(GuiGraphicsExtractor graphics, AutoSborScreen.ItemEntry entry, float x, float y, float w, float h, float a, Color accent, float top, float bottom) {
        String key = "sbor-cat-" + entry.name();
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        float hv = this.anim(key, this.hov(key, hover), 7.0f);
        RoundedRect.of(x, y, w, h).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round((6.0f + 5.0f * hv) * a))).render(graphics);
        AutoBuyPanel.drawItemAt(graphics, entry.stack(), x + 5.0f, y + (h - 15.0f) * 0.5f, 15.0f);
        float tx = x + 25.0f;
        Text.of(Fonts.MEDIUM, GuiDraw.fit(entry.name(), 5.2f, w - 44.0f), tx, y + 8.5f, 5.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 238, 245).withAlpha(a)).render(graphics);
        int max = entry.maxStack();
        Text.of(Fonts.MEDIUM, (String)(max > 1 ? "\u0421\u0442\u0430\u043a \u0434\u043e " + max : "\u041d\u0435 \u0441\u0442\u0430\u043a\u0430\u0435\u0442\u0441\u044f"), tx, y + 16.5f, 4.4f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(110, 115, 126).withAlpha(a)).render(graphics);
        float bs = 12.0f;
        float bx = x + w - bs - 5.0f;
        float by = y + (h - bs) * 0.5f;
        RoundedRect.of(bx, by, bs, bs).radius(3.5f).horizontalGradient(accent.withAlpha((0.35f + 0.5f * hv) * a), GuiDraw.getAccent2().withAlpha((0.35f + 0.5f * hv) * a)).render(graphics);
        Text.of(Fonts.MEDIUM, "+", bx + bs * 0.5f, by + bs * 0.5f, 6.0f).centered().color(Color.rgb(250, 250, 255).withAlpha(a)).render(graphics);
        if (GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(x, y, w, h, () -> this.addItem(entry), null);
        }
    }

    private void addItem(AutoSborScreen.ItemEntry entry) {
        ItemStack stack = entry.stack().copy();
        boolean placed = false;
        if (this.targetType >= 0 && AutoSborTab.stackAt(this.targetType, this.targetIndex).isEmpty()) {
            AutoSborTab.setStack(this.targetType, this.targetIndex, stack);
            this.selType = this.targetType;
            this.selIndex = this.targetIndex;
            placed = true;
        } else {
            int i;
            for (i = 0; i < AutoSbor.TOP_SLOTS.length && !placed; ++i) {
                if (!AutoSborTab.stackAt(0, i).isEmpty()) continue;
                AutoSborTab.setStack(0, i, stack);
                this.selType = 0;
                this.selIndex = i;
                placed = true;
            }
            for (i = 0; i < AutoSbor.INVENTORY_SLOTS.length && !placed; ++i) {
                if (!AutoSborTab.stackAt(1, i).isEmpty()) continue;
                AutoSborTab.setStack(1, i, stack);
                this.selType = 1;
                this.selIndex = i;
                placed = true;
            }
        }
        if (!placed) {
            this.ctx.configToast("\u0412\u0441\u0435 \u0441\u043b\u043e\u0442\u044b \u0437\u0430\u043d\u044f\u0442\u044b", Color.rgb(255, 110, 110));
            return;
        }
        this.targetType = -1;
        this.targetIndex = -1;
        ConfigBridge.onSettingChanged();
        ClientSound.ui(ClientSound.Ui.CLICK);
    }

    private float renderConfigs(GuiGraphicsExtractor graphics, float x, float y, float w, float a, Color accent, float top, float bottom, long now) {
        float bw = 66.0f;
        float fh = 15.0f;
        this.ctx.widgets.renderAbField(graphics, CONFIG_FIELD, "\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 \u043a\u043e\u043d\u0444\u0438\u0433\u0430...", "F", x, y, w - bw - 5.0f, fh, a, accent, top, bottom, now, configName);
        this.ctx.widgets.renderConfigButton(graphics, "sbor-save", "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c", "F", x + w - bw, y, bw, fh, a, false, top, bottom, () -> {
            String name;
            if (CONFIG_FIELD.equals(this.ctx.focus.abField)) {
                configName = this.ctx.focus.abInput.toString();
            }
            if ((name = configName.trim()).isEmpty()) {
                this.ctx.configToast("\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u0435", Color.rgb(255, 110, 110));
                return;
            }
            if (AutoSborConfig.saveAutoSborConfigFile(name)) {
                this.ctx.configToast("\u0421\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u043e: " + name, accent);
                configName = "";
                this.ctx.focus.setAbField(null, "");
            }
        });
        y += fh + 8.0f;
        List<String> configs = AutoSborConfig.listAutoSborConfigs();
        y = GuiDraw.sectionTitle(graphics, "\u0421\u043e\u0445\u0440\u0430\u043d\u0451\u043d\u043d\u044b\u0435", x, y, a);
        if (configs.isEmpty()) {
            Text.of(Fonts.MEDIUM, "\u041f\u043e\u043a\u0430 \u043d\u0435\u0442 \u0441\u043e\u0445\u0440\u0430\u043d\u0451\u043d\u043d\u044b\u0445 \u043a\u043e\u043d\u0444\u0438\u0433\u043e\u0432", x + w * 0.5f, y + 14.0f, 5.0f).centered().color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
            return y + 30.0f;
        }
        float rowH = 20.0f;
        for (String cfg : configs) {
            String key = "sbor-cfg-" + cfg;
            boolean hover = GuiDraw.canHit(y, rowH, top, bottom) && this.hovered(x, y, w, rowH);
            float hv = this.anim(key, this.hov(key, hover), 7.0f);
            RoundedRect.of(x, y, w, rowH).radius(4.5f).color(Color.rgba(255, 255, 255, Math.round((6.0f + 4.0f * hv) * a))).render(graphics);
            Text.of(Fonts.ICONS, "F", x + 9.0f, y + rowH * 0.5f, 5.6f).centered().color(Color.rgb(130, 135, 146).withAlpha(a)).render(graphics);
            Text.of(Fonts.MEDIUM, GuiDraw.fit(cfg, 5.4f, w - 110.0f), x + 17.0f, y + rowH * 0.5f, 5.4f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(230, 233, 240).withAlpha(a)).render(graphics);
            float cw = 42.0f;
            float ch = 12.0f;
            float cy = y + (rowH - ch) * 0.5f;
            this.ctx.widgets.renderAbChip(graphics, String.valueOf(key) + "-load", "\u0417\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c", x + w - cw * 2.0f - 9.0f, cy, cw, ch, a, accent, false, true, top, bottom, () -> {
                if (AutoSborConfig.loadAutoSborConfigFile(cfg)) {
                    this.selType = -1;
                    this.selIndex = -1;
                    ConfigBridge.onSettingChanged();
                    this.ctx.configToast("\u0417\u0430\u0433\u0440\u0443\u0436\u0435\u043d\u043e: " + cfg, accent);
                }
                ClientSound.ui(ClientSound.Ui.CLICK);
            });
            this.ctx.widgets.renderAbChip(graphics, String.valueOf(key) + "-del", "\u0423\u0434\u0430\u043b\u0438\u0442\u044c", x + w - cw - 5.0f, cy, cw, ch, a, accent, false, true, top, bottom, () -> {
                AutoSborConfig.deleteAutoSborConfigFile(cfg);
                ClientSound.ui(ClientSound.Ui.CLICK);
            });
            y += rowH + 3.0f;
        }
        return y;
    }

    private void removeSlot(int type, int index) {
        AutoSborTab.setStack(type, index, ItemStack.EMPTY);
        AutoSbor.setMinDurability(type, index, 0);
        if (this.selType == type && this.selIndex == index) {
            this.selType = -1;
            this.selIndex = -1;
        }
        ConfigBridge.onSettingChanged();
    }

    private void clearAll() {
        int i;
        for (i = 0; i < AutoSbor.TOP_SLOTS.length; ++i) {
            AutoSbor.TOP_SLOTS[i] = ItemStack.EMPTY;
            AutoSbor.setMinDurability(0, i, 0);
        }
        for (i = 0; i < AutoSbor.INVENTORY_SLOTS.length; ++i) {
            AutoSbor.INVENTORY_SLOTS[i] = ItemStack.EMPTY;
            AutoSbor.setMinDurability(1, i, 0);
        }
        this.selType = -1;
        this.selIndex = -1;
        ConfigBridge.onSettingChanged();
    }

    private void setCount(ItemStack stack, int value, int max) {
        int count = Math.clamp((long)value, 1, max);
        if (stack.getCount() == count) {
            return;
        }
        stack.setCount(count);
        ConfigBridge.onSettingChanged();
    }

    private static int[] range(int from, int to) {
        int[] r = new int[to - from + 1];
        for (int i = 0; i < r.length; ++i) {
            r[i] = from + i;
        }
        return r;
    }

    private static ItemStack stackAt(int type, int index) {
        ItemStack[] arr;
        ItemStack[] itemStackArray = arr = type == 0 ? AutoSbor.TOP_SLOTS : AutoSbor.INVENTORY_SLOTS;
        if (index < 0 || index >= arr.length) {
            return ItemStack.EMPTY;
        }
        ItemStack s = arr[index];
        return s == null ? ItemStack.EMPTY : s;
    }

    private static void setStack(int type, int index, ItemStack stack) {
        ItemStack[] arr = type == 0 ? AutoSbor.TOP_SLOTS : AutoSbor.INVENTORY_SLOTS;
        arr[index] = stack;
    }

    private static int filledCount() {
        int n = 0;
        for (ItemStack s : AutoSbor.TOP_SLOTS) {
            if (s == null || s.isEmpty()) continue;
            ++n;
        }
        for (ItemStack s : AutoSbor.INVENTORY_SLOTS) {
            if (s == null || s.isEmpty()) continue;
            ++n;
        }
        return n;
    }

    private static List<AutoSborScreen.ItemEntry> catalog(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        ArrayList<AutoSborScreen.ItemEntry> list = new ArrayList<AutoSborScreen.ItemEntry>();
        for (AutoSborScreen.ItemEntry item : AutoSborScreen.CATALOG_ITEMS) {
            if (AutoSborTab.inSlots(item.stack()) || !q.isEmpty() && !item.name().toLowerCase(Locale.ROOT).contains(q)) continue;
            list.add(item);
        }
        return list;
    }

    private static boolean inSlots(ItemStack candidate) {
        for (ItemStack s : AutoSbor.TOP_SLOTS) {
            if (s == null || s.isEmpty() || !AutoSborScreen.isSameItem(s, candidate)) continue;
            return true;
        }
        for (ItemStack s : AutoSbor.INVENTORY_SLOTS) {
            if (s == null || s.isEmpty() || !AutoSborScreen.isSameItem(s, candidate)) continue;
            return true;
        }
        return false;
    }
}

