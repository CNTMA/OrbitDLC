/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.renderer.state.GameRenderState
 */
package brokke.cc.util.render.display;

import brokke.cc.Brokke;
import brokke.cc.core.event.impl.EventDisplay;
import brokke.cc.system.scripting.api.ScriptScreenshot;
import brokke.cc.ui.clickgui.ClickGuiWorldHologram;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.HudClip;
import brokke.cc.util.render.display.WorldToScreen;
import brokke.cc.util.render.display.batch.DisplayBatcher;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.ported.RenderPort;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.GameRenderState;

public final class DisplayUtil {
    public static final float SCALE = 2.0f;

    private DisplayUtil() {
    }

    public static float width() {
        return (float)Minecraft.getInstance().getWindow().getWidth() / 2.0f;
    }

    public static float height() {
        return (float)Minecraft.getInstance().getWindow().getHeight() / 2.0f;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void render(Minecraft minecraft, GameRenderState state) {
        if (ScriptScreenshot.hidingHud()) {
            return;
        }
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(minecraft, state.guiRenderState, 0, 0);
        graphics.nextStratum();
        graphics.pose().pushMatrix();
        graphics.pose().scale(2.0f / (float)minecraft.getWindow().getGuiScale());
        DisplayBatcher.begin(graphics);
        try {
            WorldToScreen.update(state != null && state.levelRenderState != null ? state.levelRenderState.cameraRenderState : null);
            HudClip.begin();
            try {
                RenderPort.display(EventDisplay.get(graphics, state));
                Brokke.getEventBus().post(EventDisplay.get(graphics, state));
                DisplayBatcher.flush();
            }
            finally {
                HudClip.end();
            }
            ClickGuiWorldHologram.render(graphics, minecraft, state);
        }
        finally {
            DisplayBatcher.end();
            graphics.pose().popMatrix();
            graphics.nextStratum();
        }
    }

    public static void flush() {
        DisplayBatcher.flush();
    }

    public static void nextStratum(GuiGraphicsExtractor graphics) {
        DisplayBatcher.flush();
        graphics.nextStratum();
    }

    public static void renderCross(GuiGraphicsExtractor graphics, float cx, float cy, float arm, float thick, Color color) {
        int steps = 14;
        for (int i = 0; i <= steps; ++i) {
            float t = (float)i / (float)steps * 2.0f - 1.0f;
            float o = t * arm;
            RoundedRect.of(cx + o - thick * 0.5f, cy + o - thick * 0.5f, thick, thick).radius(thick * 0.5f).color(color).render(graphics);
            RoundedRect.of(cx + o - thick * 0.5f, cy - o - thick * 0.5f, thick, thick).radius(thick * 0.5f).color(color).render(graphics);
        }
    }
}

