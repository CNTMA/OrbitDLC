/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.widget;

import brokke.cc.core.setting.ColorSetting;
import brokke.cc.core.setting.ConfigManager;
import brokke.cc.core.setting.Setting;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.scissor.Scissor;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ColorSettingWidget
extends GuiComponent {
    public ColorSettingWidget(ClickGuiContext ctx) {
        super(ctx);
    }

    public float render(GuiGraphicsExtractor graphics, Setting<?> setting, ColorSetting color, float x, float y, float w, float alpha, Color accent, float top, float bottom, float head) {
        this.ctx.settings.renderHead(graphics, setting, x, y, w - 57.0f, alpha);
        Text.of(Fonts.MEDIUM, "#" + color.getHexDigits().substring(0, 6), x + w - 52.0f, y + head * 0.5f, 4.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(170, 177, 190).withAlpha(alpha)).render(graphics);
        RoundedRect.of(x + w - 14.0f, y + (head - 8.0f) * 0.5f, 14.0f, 8.0f).radius(2.5f).color(color.getColor().withAlpha(alpha)).render(graphics);
        if (GuiDraw.canHit(y, head, top, bottom)) {
            this.hit(x, y, w, head, () -> {
                color.toggleExpanded();
                if (!color.isExpanded() && this.ctx.colorDragSetting == color) {
                    ConfigManager.saveConfig("backup");
                    this.ctx.colorDragSetting = null;
                    this.ctx.colorDragPart = 0;
                }
            }, null);
        }
        float open = this.ctx.settings.dropAnim(setting, color.isExpanded());
        float height = this.ctx.settings.settingHeight(setting);
        float bodyTop = Math.max(y + head, top == Float.MAX_VALUE ? y + head : top);
        float bodyBottom = Math.min(y + height, bottom);
        if (open <= 0.001f || bodyBottom - bodyTop < 1.0f) {
            return height;
        }
        Scissor.push(graphics, x - 5.0f, bodyTop, w + 10.0f, bodyBottom - bodyTop);
        alpha *= open;
        if (!color.isExpanded() || open < 0.95f) {
            top = Float.MAX_VALUE;
        }
        bottom = bodyBottom;
        float bx = x + 3.0f;
        float bw = w - 6.0f;
        float svY = y + head + 3.0f;
        float hueY = svY + 52.0f + 6.0f;
        float alphaY = hueY + 12.0f;
        if (this.ctx.colorDragSetting == color) {
            float fraction = Math.clamp((this.getMouseX() - bx) / bw, 0.0f, 1.0f);
            if (this.ctx.colorDragPart == 1) {
                color.setSV(fraction, 1.0f - Math.clamp((this.getMouseY() - svY) / 52.0f, 0.0f, 1.0f));
            } else if (this.ctx.colorDragPart == 2) {
                color.setHue(Math.min(fraction, 0.999f));
            } else if (this.ctx.colorDragPart == 3) {
                color.setAlpha(Math.round(fraction * 255.0f));
            }
        }
        Color pure = Color.hsb(color.getHue(), 1.0f, 1.0f);
        RoundedRect.of(bx, svY, bw, 52.0f).radius(4.0f).horizontalGradient(Color.WHITE.withAlpha(alpha), pure.withAlpha(alpha)).render(graphics);
        RoundedRect.of(bx, svY, bw, 52.0f).radius(4.0f).verticalGradient(Color.rgba(0, 0, 0, 0), Color.rgb(0, 0, 0).withAlpha(alpha)).render(graphics);
        GuiDraw.renderKnob(graphics, bx + bw * color.getSaturation(), svY + 52.0f * (1.0f - color.getBrightness()), color.getColor(), alpha, this.ctx.colorDragSetting == color && this.ctx.colorDragPart == 1);
        for (int i = 0; i < 6; ++i) {
            RoundedRect.of(bx + bw * (float)i / 6.0f, hueY, bw / 6.0f + 0.2f, 6.0f).horizontalGradient(Color.hsb((float)i / 6.0f, 1.0f, 1.0f).withAlpha(alpha), Color.hsb((float)(i + 1) / 6.0f, 1.0f, 1.0f).withAlpha(alpha)).render(graphics);
        }
        GuiDraw.renderKnob(graphics, bx + bw * color.getHue(), hueY + 3.0f, pure, alpha, this.ctx.colorDragSetting == color && this.ctx.colorDragPart == 2);
        if (GuiDraw.canHit(svY, 52.0f, top, bottom)) {
            this.hit(bx, svY, bw, 52.0f, () -> {
                this.ctx.colorDragSetting = color;
                this.ctx.colorDragPart = 1;
            }, null);
        }
        if (GuiDraw.canHit(hueY, 6.0f, top, bottom)) {
            this.hit(bx, hueY - 2.0f, bw, 10.0f, () -> {
                this.ctx.colorDragSetting = color;
                this.ctx.colorDragPart = 2;
            }, null);
        }
        if (color.hasAlpha()) {
            RoundedRect.of(bx, alphaY, bw, 6.0f).radius(2.0f).color(Color.rgb(55, 58, 66).withAlpha(alpha)).render(graphics);
            RoundedRect.of(bx, alphaY, bw, 6.0f).radius(2.0f).horizontalGradient(color.getColor().withAlpha(0.0f), color.getColor().withAlpha(alpha)).render(graphics);
            GuiDraw.renderKnob(graphics, bx + bw * (float)color.getAlpha() / 255.0f, alphaY + 3.0f, color.getColor(), alpha, this.ctx.colorDragSetting == color && this.ctx.colorDragPart == 3);
            if (GuiDraw.canHit(alphaY, 6.0f, top, bottom)) {
                this.hit(bx, alphaY - 2.0f, bw, 10.0f, () -> {
                    this.ctx.colorDragSetting = color;
                    this.ctx.colorDragPart = 3;
                }, null);
            }
        }
        Scissor.pop(graphics);
        return height;
    }
}

