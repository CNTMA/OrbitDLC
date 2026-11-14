/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.navigation.ScreenRectangle
 *  net.minecraft.client.gui.screens.ChatScreen
 *  net.minecraft.client.gui.screens.PauseScreen
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
 *  net.minecraft.client.gui.screens.recipebook.RecipeBookComponent
 */
package brokke.cc.util.render.display;

import brokke.cc.ui.clickgui.NewClickGuiScreen;
import brokke.cc.util.render.display.PanelBounds;
import brokke.cc.util.render.display.RecipeBookProvider;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;

public final class HudClip {
    private static List<ScreenRectangle> regions;

    public static boolean permits(Screen screen) {
        return screen == null || screen instanceof ChatScreen || screen instanceof PauseScreen || screen instanceof AbstractContainerScreen || screen instanceof NewClickGuiScreen;
    }

    public static boolean active() {
        return regions != null;
    }

    public static void begin() {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.gui.screen();
        if (!(screen instanceof AbstractContainerScreen) && !(screen instanceof NewClickGuiScreen)) {
            regions = null;
            return;
        }
        ArrayList<ScreenRectangle> visible = new ArrayList<ScreenRectangle>();
        visible.add(new ScreenRectangle(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight()));
        for (ScreenRectangle panel : HudClip.panels(screen)) {
            ArrayList<ScreenRectangle> next = new ArrayList<ScreenRectangle>();
            for (ScreenRectangle area : visible) {
                HudClip.subtract(area, panel, next);
            }
            visible = next;
        }
        regions = visible;
    }

    public static void end() {
        regions = null;
    }

    public static List<ScreenRectangle> clips(ScreenRectangle bounds, ScreenRectangle existing) {
        ArrayList<ScreenRectangle> result = new ArrayList<ScreenRectangle>(4);
        for (ScreenRectangle area : regions) {
            ScreenRectangle clip = existing == null ? area : area.intersection(existing);
            if (clip == null || clip.width() <= 0 || clip.height() <= 0 || bounds != null && !clip.intersects(bounds)) continue;
            result.add(clip);
        }
        return result;
    }

    public static boolean insidePanel(float x, float y, float w, float h) {
        Minecraft mc = Minecraft.getInstance();
        float factor = 2.0f / (float)mc.getWindow().getGuiScale();
        ScreenRectangle rect = new ScreenRectangle((int)Math.floor(x * factor), (int)Math.floor(y * factor), (int)Math.ceil(w * factor), (int)Math.ceil(h * factor));
        return HudClip.panels(mc.gui.screen()).stream().anyMatch(panel -> panel.encompasses(rect));
    }

    private static List<ScreenRectangle> panels(Screen screen) {
        NewClickGuiScreen gui;
        PanelBounds bounds;
        ScreenRectangle rect;
        RecipeBookComponent<?> book;
        ScreenRectangle rect2;
        ArrayList<ScreenRectangle> result = new ArrayList<ScreenRectangle>(2);
        if (screen instanceof PanelBounds && (rect2 = ((PanelBounds)screen).brokke$panelBounds()) != null) {
            result.add(rect2);
        }
        if (screen instanceof RecipeBookProvider && (book = ((RecipeBookProvider)screen).brokke$recipeBook()) instanceof PanelBounds && (rect = (bounds = (PanelBounds)book).brokke$panelBounds()) != null) {
            result.add(rect);
        }
        if (screen instanceof NewClickGuiScreen && (gui = (NewClickGuiScreen)screen).brokke$previousScreen() != null) {
            result.addAll(HudClip.panels(gui.brokke$previousScreen()));
        }
        return result;
    }

    static void subtract(ScreenRectangle area, ScreenRectangle excluded, List<ScreenRectangle> out) {
        ScreenRectangle cut = area.intersection(excluded);
        if (cut == null || cut.width() <= 0 || cut.height() <= 0) {
            out.add(area);
            return;
        }
        HudClip.add(out, area.left(), area.top(), area.width(), cut.top() - area.top());
        HudClip.add(out, area.left(), cut.bottom(), area.width(), area.bottom() - cut.bottom());
        HudClip.add(out, area.left(), cut.top(), cut.left() - area.left(), cut.height());
        HudClip.add(out, cut.right(), cut.top(), area.right() - cut.right(), cut.height());
    }

    private static void add(List<ScreenRectangle> out, int x, int y, int w, int h) {
        if (w > 0 && h > 0) {
            out.add(new ScreenRectangle(x, y, w, h));
        }
    }
}

