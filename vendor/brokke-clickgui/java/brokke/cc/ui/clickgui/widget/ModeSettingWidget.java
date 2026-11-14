/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.widget;

import brokke.cc.core.setting.ModeSetting;
import brokke.cc.core.setting.Setting;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.color.Color;
import brokke.cc.util.sound.ClientSound;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ModeSettingWidget
extends GuiComponent {
    public ModeSettingWidget(ClickGuiContext ctx) {
        super(ctx);
    }

    public float render(GuiGraphicsExtractor graphics, Setting<?> setting, ModeSetting mode, float x, float y, float w, float alpha, Color accent, float top, float bottom, float head) {
        String current = mode.getCurrent();
        float open = this.ctx.settings.dropAnim(setting, mode.isExpanded());
        float hw = GuiDraw.dropHeaderWidth(current);
        this.ctx.settings.renderHead(graphics, setting, x, y, w - hw - 4.0f, alpha);
        boolean hover = this.hovered(x, y, w, head);
        GuiDraw.renderDropHeader(graphics, current, x + w, y + head * 0.5f, open, alpha, hover);
        if (GuiDraw.canHit(y, head, top, bottom)) {
            this.hit(x, y, w, head, () -> this.ctx.settings.toggleDrop(mode::toggleExpanded, mode::isExpanded), () -> this.ctx.settings.toggleDrop(mode::toggleExpanded, mode::isExpanded));
        }
        this.ctx.settings.renderOptions(graphics, mode.getModes(), current::equals, opt -> {
            mode.set((String)opt);
            ClientSound.ui(ClientSound.Ui.MODE);
        }, x, y + head, w, open, alpha, accent, top, bottom);
        return head + open * ((float)mode.getModes().size() * 12.0f + 4.0f);
    }
}

