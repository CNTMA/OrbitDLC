/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.widget;

import brokke.cc.core.module.Module;
import brokke.cc.core.setting.BindSetting;
import brokke.cc.core.setting.BooleanSetting;
import brokke.cc.core.setting.ButtonSetting;
import brokke.cc.core.setting.ColorSetting;
import brokke.cc.core.setting.ConfigManager;
import brokke.cc.core.setting.FloatSetting;
import brokke.cc.core.setting.ModeSetting;
import brokke.cc.core.setting.MultiBooleanSetting;
import brokke.cc.core.setting.NumberSetting;
import brokke.cc.core.setting.Setting;
import brokke.cc.core.setting.StringSetting;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.ui.clickgui.widget.BindSettingWidget;
import brokke.cc.ui.clickgui.widget.BooleanSettingWidget;
import brokke.cc.ui.clickgui.widget.ButtonSettingWidget;
import brokke.cc.ui.clickgui.widget.ColorSettingWidget;
import brokke.cc.ui.clickgui.widget.ModeSettingWidget;
import brokke.cc.ui.clickgui.widget.MultiBooleanSettingWidget;
import brokke.cc.ui.clickgui.widget.SliderSettingWidget;
import brokke.cc.util.animation.Animation;
import brokke.cc.util.animation.Easing;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.scissor.Scissor;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class SettingWidgets
extends GuiComponent {
    private final ColorSettingWidget colorWidget;
    private final BooleanSettingWidget booleanWidget;
    private final BindSettingWidget bindWidget;
    private final ButtonSettingWidget buttonWidget;
    private final ModeSettingWidget modeWidget;
    private final MultiBooleanSettingWidget multiWidget;
    private final SliderSettingWidget sliderWidget;
    public final Map<String, StringSetting> textSettings = new HashMap<String, StringSetting>();
    private final Map<StringSetting, String> textIds = new IdentityHashMap<StringSetting, String>();
    public final Map<Setting<?>, Animation> dropAnims = new IdentityHashMap();
    public final Set<Setting<?>> animFrame = Collections.newSetFromMap(new IdentityHashMap());
    private final Map<Setting<?>, Animation> visibilityAnims = new IdentityHashMap();
    private final Set<Setting<?>> visibilityFrame = Collections.newSetFromMap(new IdentityHashMap());
    public final Map<Setting<?>, Float> sliderPos = new IdentityHashMap();

    public SettingWidgets(ClickGuiContext ctx) {
        super(ctx);
        this.colorWidget = new ColorSettingWidget(ctx);
        this.booleanWidget = new BooleanSettingWidget(ctx);
        this.bindWidget = new BindSettingWidget(ctx);
        this.buttonWidget = new ButtonSettingWidget(ctx);
        this.modeWidget = new ModeSettingWidget(ctx);
        this.multiWidget = new MultiBooleanSettingWidget(ctx);
        this.sliderWidget = new SliderSettingWidget(ctx);
    }

    public void beginFrame() {
        this.animFrame.clear();
        this.visibilityFrame.clear();
    }

    public void toggleDrop(Runnable toggle, BooleanSupplier open) {
        toggle.run();
        ClientSound.ui(open.getAsBoolean() ? ClientSound.Ui.EXPAND : ClientSound.Ui.COLLAPSE);
    }

    public float settingsHeight(Module module) {
        float h = 0.0f;
        for (Setting<?> setting : module.getSettings()) {
            h += this.settingHeight(setting) * this.settingVisibility(setting);
        }
        return h;
    }

    public float settingVisibility(Setting<?> setting) {
        boolean visible = setting.isVisible();
        Animation animation = this.visibilityAnims.computeIfAbsent(setting, s -> new Animation(visible ? 1.0f : 0.0f));
        if (this.visibilityFrame.add(setting)) {
            animation.animate(visible ? 1.0f : 0.0f, 1.0f, 9.0f, Easing.CUBIC_OUT);
        }
        return Math.clamp(animation.value(), 0.0f, 1.0f);
    }

    public float renderAnimatedSetting(GuiGraphicsExtractor graphics, Setting<?> setting, float x, float y, float w, float alpha, Color accent, float top, float bottom) {
        float visibility = this.settingVisibility(setting);
        float height = this.settingHeight(setting) * visibility;
        if (this.ctx.colorDragSetting == setting && !setting.isVisible()) {
            ConfigManager.saveConfig("backup");
            this.ctx.colorDragSetting = null;
            this.ctx.colorDragPart = 0;
        }
        float clipTop = Math.max(y, top == Float.MAX_VALUE ? y : top);
        float clipBottom = Math.min(y + height, bottom);
        if (visibility > 0.001f && clipBottom - clipTop >= 1.0f) {
            Scissor.push(graphics, x - 5.0f, clipTop, w + 10.0f, clipBottom - clipTop);
            this.renderSetting(graphics, setting, x, y - (1.0f - visibility) * 3.0f, w, alpha * visibility, accent, setting.isVisible() && visibility > 0.95f ? top : Float.MAX_VALUE, clipBottom);
            Scissor.pop(graphics);
        }
        return height;
    }

    public boolean hasDesc(Setting<?> setting) {
        String d = setting.description();
        return d != null && !d.isBlank() && !d.equals(setting.name());
    }

    private float headHeight(Setting<?> setting) {
        return this.hasDesc(setting) ? 19.5f : 14.0f;
    }

    public float settingHeight(Setting<?> setting) {
        float head = this.headHeight(setting);
        if (setting instanceof StringSetting) {
            return head + 17.0f;
        }
        if (setting instanceof ColorSetting) {
            ColorSetting color = (ColorSetting)setting;
            return head + this.dropAnim(setting, color.isExpanded()) * (72.0f + (color.hasAlpha() ? 12.0f : 0.0f));
        }
        if (setting instanceof NumberSetting || setting instanceof FloatSetting) {
            return head + 10.0f;
        }
        if (setting instanceof ModeSetting) {
            ModeSetting mode = (ModeSetting)setting;
            return head + this.dropAnim(setting, mode.isExpanded()) * ((float)mode.getModes().size() * 12.0f + 4.0f);
        }
        if (setting instanceof MultiBooleanSetting) {
            MultiBooleanSetting multi = (MultiBooleanSetting)setting;
            return head + this.dropAnim(setting, multi.isExpanded()) * ((float)multi.getOptions().size() * 12.0f + 4.0f);
        }
        if (setting instanceof BooleanSetting || setting instanceof BindSetting) {
            return head;
        }
        if (setting instanceof ButtonSetting) {
            return 16.0f;
        }
        return 0.0f;
    }

    public float dropAnim(Setting<?> setting, boolean open) {
        Animation anim = this.dropAnims.computeIfAbsent(setting, s -> {
            Animation a = new Animation(open ? 1.0f : 0.0f);
            a.setEasing(Easing.CUBIC_OUT);
            return a;
        });
        if (this.animFrame.add(setting)) {
            anim.animate(open ? 1.0f : 0.0f, 1.0f, 5.0f);
        }
        return Math.clamp(anim.value(), 0.0f, 1.0f);
    }

    public void renderHead(GuiGraphicsExtractor graphics, Setting<?> setting, float x, float y, float maxW, float alpha) {
        boolean desc = this.hasDesc(setting);
        Text.of(Fonts.MEDIUM, GuiDraw.fit(setting.name(), 5.6f, maxW), x, y + (desc ? 6.0f : 7.0f), 5.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(228, 231, 238).withAlpha(alpha)).render(graphics);
        if (desc) {
            Text.of(Fonts.MEDIUM, GuiDraw.fit(setting.description(), 4.6f, maxW), x, y + 13.5f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(112, 117, 128).withAlpha(alpha)).render(graphics);
        }
    }

    public void renderOptions(GuiGraphicsExtractor graphics, List<String> options, Predicate<String> selected, Consumer<String> onClick, float x, float y, float w, float open, float alpha, Color accent, float top, float bottom) {
        if (open <= 0.01f) {
            return;
        }
        float listH = (float)options.size() * 12.0f + 4.0f;
        float a = alpha * open;
        RoundedRect.of(x, y, w, listH * open).radius(3.0f).color(Color.rgba(0, 0, 0, Math.round(55.0f * a))).render(graphics);
        float oy = y + 2.0f;
        for (String option : options) {
            boolean on = selected.test(option);
            List<Object> optKey = List.of(Integer.valueOf(options.hashCode()), option);
            float hover = this.anim(optKey, this.hov(optKey, open > 0.95f && this.hovered(x, oy, w, 12.0f) && this.hovered(x, top, w, bottom - top)), 7.0f);
            float sel = this.anim(List.of(Integer.valueOf(options.hashCode()), option, "sel"), on, 6.0f);
            if (hover > 0.01f) {
                RoundedRect.of(x + 1.5f, oy, w - 3.0f, 12.0f).radius(2.0f).color(Color.rgba(255, 255, 255, Math.round(7.0f * hover * a))).render(graphics);
            }
            Color c = GuiDraw.mix(GuiDraw.mix(Color.rgb(140, 145, 156), Color.rgb(215, 219, 228), hover), accent, sel);
            Text.of(Fonts.MEDIUM, GuiDraw.fit(option, 5.3f, w - 16.0f), x + 5.0f + 1.5f * hover, oy + 6.0f, 5.3f).verticalAlign(VerticalAlign.MIDDLE).color(c.withAlpha(a)).render(graphics);
            if (sel > 0.01f) {
                GuiDraw.renderCheck(graphics, x + w - 6.0f, oy + 6.0f, accent.withAlpha(a * sel));
            }
            if (open > 0.95f && GuiDraw.canHit(oy, 12.0f, top, bottom)) {
                this.hit(x, oy, w, 12.0f, () -> onClick.accept(option), null);
            }
            oy += 12.0f;
        }
    }

    public float renderSetting(GuiGraphicsExtractor graphics, Setting<?> setting, float x, float y, float w, float alpha, Color accent, float top, float bottom) {
        float head = this.headHeight(setting);
        if (setting instanceof StringSetting) {
            StringSetting string = (StringSetting)setting;
            String id = this.textIds.computeIfAbsent(string, s -> "module-setting:" + this.textIds.size());
            this.textSettings.put(id, string);
            this.renderHead(graphics, setting, x, y, w, alpha);
            this.ctx.widgets.renderAbField(graphics, id, "\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u0437\u043d\u0430\u0447\u0435\u043d\u0438\u0435...", null, x, y + head, w, 13.0f, alpha, accent, top, bottom, System.currentTimeMillis(), string.get());
            return head + 17.0f;
        }
        if (!(setting instanceof ButtonSetting) && GuiDraw.canHit(y, head, top, bottom)) {
            this.hov(setting, this.hovered(x, y, w, head));
        }
        if (setting instanceof ColorSetting) {
            ColorSetting color = (ColorSetting)setting;
            return this.colorWidget.render(graphics, setting, color, x, y, w, alpha, accent, top, bottom, head);
        }
        if (setting instanceof BooleanSetting) {
            BooleanSetting bool = (BooleanSetting)setting;
            return this.booleanWidget.render(graphics, setting, bool, x, y, w, alpha, accent, top, bottom, head);
        }
        if (setting instanceof BindSetting) {
            BindSetting bind = (BindSetting)setting;
            return this.bindWidget.render(graphics, setting, bind, x, y, w, alpha, accent, top, bottom, head);
        }
        if (setting instanceof ButtonSetting) {
            ButtonSetting button = (ButtonSetting)setting;
            return this.buttonWidget.render(graphics, setting, button, x, y, w, alpha, accent, top, bottom, head);
        }
        if (setting instanceof ModeSetting) {
            ModeSetting mode = (ModeSetting)setting;
            return this.modeWidget.render(graphics, setting, mode, x, y, w, alpha, accent, top, bottom, head);
        }
        if (setting instanceof MultiBooleanSetting) {
            MultiBooleanSetting multi = (MultiBooleanSetting)setting;
            return this.multiWidget.render(graphics, setting, multi, x, y, w, alpha, accent, top, bottom, head);
        }
        if (setting instanceof NumberSetting || setting instanceof FloatSetting) {
            return this.sliderWidget.render(graphics, setting, x, y, w, alpha, accent, top, bottom, head);
        }
        return 0.0f;
    }
}

