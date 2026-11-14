/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.widget;

import brokke.cc.core.setting.FloatSetting;
import brokke.cc.core.setting.NumberSetting;
import brokke.cc.core.setting.Setting;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.DisplayUtil;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.TextAlign;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class SliderSettingWidget
extends GuiComponent {
    public SliderSettingWidget(ClickGuiContext ctx) {
        super(ctx);
    }

    public float render(GuiGraphicsExtractor graphics, Setting<?> setting, float x, float y, float w, float alpha, Color accent, float top, float bottom, float head) {
        Float prev;
        float t;
        float target;
        String text;
        double value;
        double max;
        double min;
        if (setting instanceof NumberSetting) {
            NumberSetting num = (NumberSetting)setting;
            min = num.min();
            max = num.max();
            double def = (Double)num.defaultValue();
        } else {
            FloatSetting fl = (FloatSetting)setting;
            min = fl.getMin();
            max = fl.getMax();
            double def = ((Float)fl.defaultValue()).floatValue();
        }
        if (this.ctx.dragSetting == setting) {
            double d;
            double t2 = this.ctx.sliderDragFraction();
            this.applySlider(setting, min + (max - min) * t2);
            if (setting instanceof NumberSetting) {
                NumberSetting n = (NumberSetting)setting;
                d = n.getValue();
            } else {
                d = ((Float)((FloatSetting)setting).get()).floatValue();
            }
            ClientSound.slider(d, min, max);
        }
        if (setting instanceof NumberSetting) {
            NumberSetting num = (NumberSetting)setting;
            value = num.getValue();
            text = num.getFormattedValue() + num.getSuffix();
        } else {
            FloatSetting fl = (FloatSetting)setting;
            value = ((Float)fl.get()).floatValue();
            text = GuiDraw.format(((Float)fl.get()).floatValue(), fl.getStep()) + (fl.getSuffix() != null ? fl.getSuffix() : "");
        }
        float tw = Text.of(Fonts.MEDIUM, text).size(5.3f).width();
        this.ctx.settings.renderHead(graphics, setting, x, y, w - tw - 4.0f, alpha);
        Text.of(Fonts.MEDIUM, text, x + w, y + (this.ctx.settings.hasDesc(setting) ? 6.0f : 7.0f), 5.3f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(accent.withAlpha(alpha)).render(graphics);
        float f = target = max > min ? (float)Math.clamp((value - min) / (max - min), 0.0, 1.0) : 0.0f;
        if (this.ctx.dragSetting == setting) {
            target = this.ctx.sliderDragFraction();
        }
        float f2 = t = (prev = this.ctx.settings.sliderPos.get(setting)) == null ? target : prev.floatValue();
        if (this.ctx.settings.animFrame.add(setting)) {
            if (Math.abs((t = this.smooth(t, target, this.ctx.dragSetting == setting ? 28.0f : 16.0f)) - target) < 5.0E-4f) {
                t = target;
            }
            this.ctx.settings.sliderPos.put(setting, Float.valueOf(t));
        }
        float barY = y + head + 2.0f;
        boolean hover = this.ctx.dragSetting == setting || this.hovered(x - 2.0f, barY - 4.0f, w + 4.0f, 9.0f);
        float barH = hover ? 2.2f : 1.6f;
        RoundedRect.of(x, barY - barH * 0.5f, w, barH).radius(barH * 0.5f).color(Color.rgba(255, 255, 255, Math.round(16.0f * alpha))).render(graphics);
        RoundedRect.of(x, barY - barH * 0.5f, Math.max(barH, w * t), barH).radius(barH * 0.5f).horizontalGradient(accent.withAlpha(alpha), GuiDraw.getAccent2().withAlpha(alpha)).render(graphics);
        float knob = hover ? 5.5f : 5.3f;
        float kx = x + w * t;
        RoundedRect.of(kx - knob * 0.5f - 1.0f, barY - knob * 0.5f - 1.0f, knob + 2.0f, knob + 2.0f).radius(knob * 0.5f + 1.0f).color(accent.withAlpha(0.25f * alpha)).render(graphics);
        RoundedRect.of(kx - knob * 0.5f, barY - knob * 0.5f, knob, knob).radius(knob * 0.5f).color(Color.rgb(245, 246, 250).withAlpha(alpha)).render(graphics);
        if (GuiDraw.canHit(y, head + 10.0f, top, bottom)) {
            this.hit(x - 2.0f, barY - 4.0f, w + 4.0f, 8.0f, () -> {
                this.ctx.dragSetting = setting;
                float centerX = DisplayUtil.width() * 0.5f;
                this.ctx.dragScreenX = centerX + (x - centerX) * this.ctx.renderScale;
                this.ctx.dragScreenW = w * this.ctx.renderScale;
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, () -> {
                this.ctx.dragSetting = null;
                setting.reset();
                ClientSound.ui(ClientSound.Ui.CLICK);
            });
            this.ctx.hits.middle(x - 2.0f, y, w + 4.0f, head + 10.0f, () -> {
                this.ctx.dragSetting = null;
                setting.reset();
                ClientSound.ui(ClientSound.Ui.CLICK);
            });
        }
        return head + 10.0f;
    }

    private void applySlider(Setting<?> setting, double raw) {
        if (setting instanceof NumberSetting) {
            NumberSetting num = (NumberSetting)setting;
            double step = num.step() > 0.0 ? num.step() : 0.01;
            num.setValue(Math.clamp((double)Math.round(raw / step) * step, num.min(), num.max()));
        } else if (setting instanceof FloatSetting) {
            FloatSetting fl = (FloatSetting)setting;
            float step = fl.getStep() > 0.0f ? fl.getStep() : 0.01f;
            fl.set(Math.clamp((float)Math.round((float)raw / step) * step, fl.getMin(), fl.getMax()));
        }
    }
}

