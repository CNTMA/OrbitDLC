/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.widget;

import brokke.cc.core.setting.ButtonSetting;
import brokke.cc.core.setting.Setting;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ButtonSettingWidget
extends GuiComponent {
    public ButtonSettingWidget(ClickGuiContext ctx) {
        super(ctx);
    }

    public float render(GuiGraphicsExtractor graphics, Setting<?> setting, ButtonSetting button, float x, float y, float w, float alpha, Color accent, float top, float bottom, float head) {
        float by = y + 1.5f;
        float hover = this.anim(setting, this.hov(setting, this.hovered(x, by, w, 13.0f) && this.hovered(x, top, w, bottom - top)), 7.0f);
        RoundedRect.of(x, by, w, 13.0f).radius(3.0f).color(Color.rgba(255, 255, 255, Math.round(10.0f * (1.0f - hover) * alpha))).render(graphics);
        if (hover > 0.01f) {
            RoundedRect.of(x, by, w, 13.0f).radius(3.0f).color(accent.withAlpha(0.22f * hover * alpha)).render(graphics);
        }
        Text.of(Fonts.MEDIUM, GuiDraw.fit(button.name(), 5.4f, w - 8.0f), x + w * 0.5f, by + 6.5f, 5.4f).centered().color(GuiDraw.mix(Color.rgb(205, 209, 218), Color.rgb(245, 247, 252), hover).withAlpha(alpha)).render(graphics);
        if (GuiDraw.canHit(by, 13.0f, top, bottom)) {
            this.hit(x, by, w, 13.0f, () -> {
                ClientSound.ui(ClientSound.Ui.CLICK);
                button.runAction();
            }, null);
        }
        return 16.0f;
    }
}

