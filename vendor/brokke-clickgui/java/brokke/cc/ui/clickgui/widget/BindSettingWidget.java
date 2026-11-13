/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.widget;

import brokke.cc.core.setting.BindSetting;
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

public final class BindSettingWidget
extends GuiComponent {
    public BindSettingWidget(ClickGuiContext ctx) {
        super(ctx);
    }

    public float render(GuiGraphicsExtractor graphics, Setting<?> setting, BindSetting bind, float x, float y, float w, float alpha, Color accent, float top, float bottom, float head) {
        boolean listening = this.ctx.bindingSetting == bind;
        String key = listening ? "..." : bind.getKeyName();
        float tw = Text.of(Fonts.MEDIUM, key).size(5.2f).width();
        float bw = Math.max(16.0f, tw + 8.0f);
        float bh = 10.0f;
        this.ctx.settings.renderHead(graphics, setting, x, y, w - bw - 4.0f, alpha);
        float bx = x + w - bw;
        float by = y + (head - bh) * 0.5f;
        float ls = this.anim(setting, listening, 7.0f);
        RoundedRect.of(bx, by, bw, bh).radius(2.5f).color(Color.rgba(255, 255, 255, Math.round(12.0f * (1.0f - ls) * alpha))).render(graphics);
        if (ls > 0.01f) {
            float pulse = listening ? 0.75f + 0.25f * (float)Math.sin((double)System.currentTimeMillis() / 160.0) : 1.0f;
            RoundedRect.of(bx, by, bw, bh).radius(2.5f).color(accent.withAlpha(0.25f * ls * pulse * alpha)).render(graphics);
        }
        Text.of(Fonts.MEDIUM, key, bx + bw * 0.5f, by + bh * 0.5f, 5.2f).centered().color(GuiDraw.mix(Color.rgb(215, 219, 228), accent, ls).withAlpha(alpha)).render(graphics);
        if (GuiDraw.canHit(y, head, top, bottom)) {
            this.hit(x, y, w, head, () -> {
                if (!listening) {
                    this.ctx.focus.clear();
                }
                this.ctx.bindingSetting = listening ? null : bind;
                ClientSound.ui(listening ? ClientSound.Ui.CLICK : ClientSound.Ui.BIND_START);
            }, () -> {
                ClientSound.ui(ClientSound.Ui.BIND_CLEAR);
                bind.setKey(-1);
                this.ctx.bindingSetting = null;
            });
        }
        return head;
    }
}

