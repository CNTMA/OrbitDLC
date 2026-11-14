/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.widget;

import brokke.cc.core.setting.MultiBooleanSetting;
import brokke.cc.core.setting.Setting;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.color.Color;
import brokke.cc.util.sound.ClientSound;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class MultiBooleanSettingWidget
extends GuiComponent {
    public MultiBooleanSettingWidget(ClickGuiContext ctx) {
        super(ctx);
    }

    public float render(GuiGraphicsExtractor graphics, Setting<?> setting, MultiBooleanSetting multi, float x, float y, float w, float alpha, Color accent, float top, float bottom, float head) {
        String label = "\u0412\u044b\u0431\u0440\u0430\u043d\u043e " + multi.getSelectedCount() + "/" + multi.getTotalCount();
        float open = this.ctx.settings.dropAnim(setting, multi.isExpanded());
        float hw = GuiDraw.dropHeaderWidth(label);
        this.ctx.settings.renderHead(graphics, setting, x, y, w - hw - 4.0f, alpha);
        boolean hover = this.hovered(x, y, w, head);
        GuiDraw.renderDropHeader(graphics, label, x + w, y + head * 0.5f, open, alpha, hover);
        if (GuiDraw.canHit(y, head, top, bottom)) {
            this.hit(x, y, w, head, () -> this.ctx.settings.toggleDrop(multi::toggleExpanded, multi::isExpanded), () -> this.ctx.settings.toggleDrop(multi::toggleExpanded, multi::isExpanded));
        }
        this.ctx.settings.renderOptions(graphics, multi.getOptions(), multi::isSelected, opt -> {
            multi.toggleOption((String)opt);
            ClientSound.checkbox(multi.isSelected((String)opt));
        }, x, y + head, w, open, alpha, accent, top, bottom);
        return head + open * ((float)multi.getOptions().size() * 12.0f + 4.0f);
    }
}

