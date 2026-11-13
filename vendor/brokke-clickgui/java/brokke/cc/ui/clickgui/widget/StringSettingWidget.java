/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.widget;

import brokke.cc.core.setting.Setting;
import brokke.cc.core.setting.StringSetting;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class StringSettingWidget
extends GuiComponent {
    public StringSettingWidget(ClickGuiContext ctx) {
        super(ctx);
    }

    public float render(GuiGraphicsExtractor graphics, Setting<?> setting, StringSetting str, float x, float y, float w, float alpha, Color accent, float top, float bottom, float head) {
        boolean editing = this.ctx.editingString == str;
        String raw = str.get();
        String shown = str.isMasked() ? "*".repeat(raw.length()) : raw;
        boolean caret = editing && System.currentTimeMillis() / 500L % 2L == 0L;
        float bw = Math.min(70.0f, w * 0.5f);
        float bh = 10.0f;
        this.ctx.settings.renderHead(graphics, setting, x, y, w - bw - 4.0f, alpha);
        float bx = x + w - bw;
        float by = y + (head - bh) * 0.5f;
        float ls = this.anim(setting, editing, 7.0f);
        RoundedRect.of(bx, by, bw, bh).radius(2.5f).color(Color.rgba(255, 255, 255, Math.round(12.0f * (1.0f - ls) * alpha))).render(graphics);
        if (ls > 0.01f) {
            RoundedRect.of(bx, by, bw, bh).radius(2.5f).color(accent.withAlpha(0.2f * ls * alpha)).render(graphics);
        }
        String text = shown.isEmpty() && !editing ? "..." : shown;
        float maxTw = bw - 8.0f;
        while (!text.isEmpty()) {
            String string = editing ? "_" : "";
            if (!(Text.of(Fonts.MEDIUM, text + string).size(5.2f).width() > maxTw)) break;
            text = editing ? text.substring(1) : text.substring(0, text.length() - 1);
        }
        Color tc = shown.isEmpty() && !editing ? Color.rgb(112, 117, 128) : GuiDraw.mix(Color.rgb(215, 219, 228), accent, ls);
        Text.of(Fonts.MEDIUM, text + (caret ? "_" : ""), bx + 4.0f, by + bh * 0.5f, 5.2f).verticalAlign(VerticalAlign.MIDDLE).color(tc.withAlpha(alpha)).render(graphics);
        if (GuiDraw.canHit(y, head, top, bottom)) {
            this.hit(x, y, w, head, () -> {
                this.ctx.focus.clear();
                this.ctx.bindingSetting = null;
                this.ctx.editingString = editing ? null : str;
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, () -> {
                str.clear();
                ClientSound.ui(ClientSound.Ui.BIND_CLEAR);
            });
        }
        return head;
    }
}

