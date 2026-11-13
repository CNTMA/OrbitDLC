/*
 * Decompiled with CFR 0.152.
 */
package brokke.cc.ui.clickgui.screen;

import brokke.cc.core.module.Category;
import brokke.cc.core.module.impl.render.GuiSettings;
import brokke.cc.util.render.display.DisplayUtil;
import java.util.ArrayList;
import java.util.List;

public final class ClickGuiLayout {
    public static final float W = 400.0f;
    public static final float H = 250.0f;
    public static final float SIDEBAR_W = 80.0f;
    public static final float ITEM_H = 13.0f;
    public static final float ITEM_GAP = 1.0f;
    public static final float SECTION_GAP = 7.0f;
    public static final float PICKER_SV_H = 52.0f;
    public static final float PICKER_HUE_H = 6.0f;
    public static final float ITEM_PAD = 5.0f;
    public static final float LOGO_AREA = 22.0f;
    public static final List<Category> CATEGORIES = new ArrayList<Category>();
    public static int mainCount = 0;
    public static final float CONTENT_PAD = 7.0f;
    public static final float HEADER_H = 20.0f;
    public static final float SEARCH_W = 85.0f;
    public static final float SEARCH_H = 13.0f;
    public static final float COL_GAP = 5.0f;
    public static final float CARD_HEAD = 27.0f;
    public static final float ROW_H = 14.0f;
    public static final float ROW_DESC_H = 19.5f;
    public static final float OPTION_H = 12.0f;
    public static final float BUTTON_H = 13.0f;
    public static final float SCROLLBAR_W = 2.5f;

    private ClickGuiLayout() {
    }

    public static float guiScale() {
        GuiSettings settings = GuiSettings.getInstance();
        float desired = settings != null ? settings.guiSize.getFloat() / 100.0f : 1.0f;
        float fit = Math.min((DisplayUtil.width() - 12.0f) / 400.0f, (DisplayUtil.height() - 12.0f) / 250.0f);
        return Math.max(0.1f, Math.min(desired, fit));
    }

    public static String categoryName(Category category) {
        return category == null ? "Customs" : category.displayName();
    }

    public static float itemsTop(float py) {
        return py + 22.0f + 2.0f;
    }

    public static float itemY(float top, float index) {
        float gap = index > (float)(mainCount - 1) ? 7.0f * Math.clamp(index - (float)(mainCount - 1), 0.0f, 1.0f) : 0.0f;
        return top + index * 14.0f + gap;
    }

    static {
        for (Category category : Category.values()) {
            if (!category.isMainCategory()) continue;
            CATEGORIES.add(category);
            ++mainCount;
        }
        for (Category category : Category.values()) {
            if (category.isMainCategory() || category == Category.SETTINGS || category == Category.MARKET) continue;
            CATEGORIES.add(category);
        }
        CATEGORIES.add(null);
    }
}

