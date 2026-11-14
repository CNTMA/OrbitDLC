/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.widget;

import brokke.cc.core.setting.BooleanSetting;
import brokke.cc.core.setting.Setting;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.animation.Animation;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class BooleanSettingWidget
extends GuiComponent {
    public BooleanSettingWidget(ClickGuiContext ctx) {
        super(ctx);
    }

    public float render(GuiGraphicsExtractor graphics, Setting<?> setting, BooleanSetting bool, float x, float y, float w, float alpha, Color accent, float top, float bottom, float head) {
        boolean bound;
        boolean listening = this.ctx.bindingSetting == bool.toggleBind();
        boolean bl = bound = bool.toggleBind().getKey() != -1;
        String key = listening ? "..." : (bound ? bool.toggleBind().getKeyName() : null);
        float kw = key == null ? 0.0f : Text.of(Fonts.MEDIUM, key).size(4.6f).width() + 4.0f;
        this.ctx.settings.renderHead(graphics, setting, x, y, w - 16.0f - kw, alpha);
        if (key != null) {
            Text.of(Fonts.MEDIUM, key, x + w - 16.0f - kw * 0.5f - 1.0f, y + head * 0.5f, 4.6f).centered().color((listening ? accent : Color.rgb(150, 155, 168)).withAlpha(alpha)).render(graphics);
        }
        Animation anim = this.ctx.settings.dropAnims.computeIfAbsent(setting, s -> new Animation(bool.isEnabled() ? 1.0f : 0.0f));
        if (this.ctx.settings.animFrame.add(setting)) {
            anim.animate(bool.isEnabled() ? 1.0f : 0.0f, 1.0f, 8.0f);
        }
        float sw = 14.0f;
        float sh = 7.5f;
        GuiDraw.renderSwitch(graphics, x + w - sw, y + (head - sh) * 0.5f, sw, sh, anim.value(), accent, alpha);
        if (GuiDraw.canHit(y, head, top, bottom)) {
            this.hit(x, y, w, head, () -> {
                bool.toggle();
                ClientSound.checkbox(bool.isEnabled());
            }, () -> {
                if (!listening) {
                    this.ctx.focus.clear();
                }
                this.ctx.bindingSetting = listening ? null : bool.toggleBind();
                ClientSound.ui(listening ? ClientSound.Ui.CLICK : ClientSound.Ui.BIND_START);
            });
        }
        return head;
    }
}

