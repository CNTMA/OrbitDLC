/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.widget;

import brokke.cc.core.module.Module;
import brokke.cc.core.setting.ConfigManager;
import brokke.cc.core.setting.Setting;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.ui.clickgui.widget.ServerBadge;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.scissor.Scissor;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ModuleCard
extends GuiComponent {
    public ModuleCard(ClickGuiContext ctx) {
        super(ctx);
    }

    private void toggleModule(Module module) {
        module.toggle();
        ClientSound.module(module.isEnabled());
    }

    private void startModuleBind(Module module) {
        this.ctx.focus.clear();
        this.ctx.bindingSetting = null;
        this.ctx.bindingModule = module;
        ClientSound.ui(ClientSound.Ui.BIND_START);
    }

    private void cycleModuleBindMode(Module module) {
        module.cycleBindMode();
        ClientSound.ui(ClientSound.Ui.CLICK);
        ConfigManager.onSettingChanged();
    }

    public void finishModuleBind(int key) {
        if (this.ctx.bindingModule == null) {
            return;
        }
        this.ctx.bindingModule.setKey(key);
        this.ctx.bindingModule = null;
        ClientSound.ui(key == 0 || key == -1 ? ClientSound.Ui.BIND_CLEAR : ClientSound.Ui.BIND_SET);
        ConfigManager.onSettingChanged();
    }

    private void expandModule(Module module) {
        module.toggleExpanded();
        ClientSound.ui(module.isExpanded() ? ClientSound.Ui.EXPAND : ClientSound.Ui.COLLAPSE);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public float renderCard(GuiGraphicsExtractor graphics, Module module, float x, float y, float w, float prog, float clipTop, float clipBottom) {
        Object bindName;
        float expand = module.expandAnimation();
        float settingsH = this.ctx.settings.settingsHeight(module);
        boolean hasSettings = settingsH > 0.0f;
        float h = 27.0f + (hasSettings ? (settingsH + 3.0f) * expand : 0.0f);
        if (y > clipBottom || y + h < clipTop) {
            return h;
        }
        Color accent = GuiDraw.getAccent();
        float hover = this.anim(module, this.hov(module, this.hovered(x, y, w, 27.0f) && this.hovered(x, clipTop, w, clipBottom - clipTop)), 6.0f);
        RoundedRect.of(x, y, w, h).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round((6.0f + 4.0f * hover) * prog))).render(graphics);
        float toggle = module.toggleAnimation();
        List<ServerBadge> badges = ServerBadge.forModule(module.name());
        boolean listening = this.ctx.bindingModule == module;
        Object object = bindName = listening ? "..." : Module.getKeyName(module.key());
        if (!listening && !((String)bindName).isEmpty() && module.bindMode() == Module.BindMode.HOLD) {
            bindName = (String)bindName + " H";
        }
        float bindWidth = ((String)bindName).isEmpty() ? 0.0f : Math.max(12.0f, Text.of(Fonts.MEDIUM, (String)bindName).size(4.8f).width() + 7.0f);
        float titleEnd = x + 8.0f + Text.of(Fonts.MEDIUM, module.name()).size(6.6f).width();
        Text.of(Fonts.MEDIUM, module.name(), x + 8.0f, y + 9.5f, 6.6f).verticalAlign(VerticalAlign.MIDDLE).color(GuiDraw.mix(Color.rgb(200, 204, 214), Color.rgb(245, 247, 252), toggle).withAlpha(prog)).render(graphics);
        float badgesWidth = 0.0f;
        for (ServerBadge badge : badges) {
            badgesWidth += badge.width() + 4.0f;
        }
        float badgesAvailable = x + w - (hasSettings ? 36.0f : 28.0f) - titleEnd - (bindWidth > 0.0f ? bindWidth + 4.0f : 0.0f);
        float badgeScale = badgesWidth > 0.0f ? Math.clamp(badgesAvailable / badgesWidth, 0.5f, 1.0f) : 1.0f;
        for (ServerBadge badge : badges) {
            graphics.pose().pushMatrix();
            try {
                graphics.pose().translate(titleEnd + 4.0f * badgeScale, y + 9.5f);
                graphics.pose().scale(badgeScale);
                badge.render(graphics, 0.0f, 0.0f, prog);
            }
            finally {
                graphics.pose().popMatrix();
            }
            titleEnd += (badge.width() + 4.0f) * badgeScale;
        }
        String desc = GuiDraw.fit(module.description(), 5.0f, w - 42.0f);
        Text.of(Fonts.MEDIUM, desc, x + 8.0f, y + 18.5f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(115, 120, 132).withAlpha(prog)).render(graphics);
        float swW = 16.0f;
        float swH = 8.5f;
        float swX = x + w - swW - (hasSettings ? 16.0f : 8.0f);
        float swY = y + (27.0f - swH) * 0.5f;
        GuiDraw.renderSwitch(graphics, swX, swY, swW, swH, toggle, accent, prog);
        float bindAnim = this.anim(module.name() + "-bind", listening, 7.0f);
        float[] bindChip = null;
        if (!((String)bindName).isEmpty()) {
            float bw = bindWidth;
            float bh = 8.0f;
            float bx = titleEnd + 4.0f;
            float by = y + 9.5f - bh * 0.5f;
            bindChip = new float[]{bx, by, bw, bh};
            float pulse = listening ? 0.75f + 0.25f * (float)Math.sin((double)System.currentTimeMillis() / 160.0) : 1.0f;
            RoundedRect.of(bx, by, bw, bh).radius(2.5f).color(Color.rgba(255, 255, 255, Math.round(10.0f * (1.0f - bindAnim) * prog))).render(graphics);
            if (bindAnim > 0.01f) {
                RoundedRect.of(bx, by, bw, bh).radius(2.5f).color(accent.withAlpha(0.28f * bindAnim * pulse * prog)).render(graphics);
            }
            Text.of(Fonts.MEDIUM, (String)bindName, bx + bw * 0.5f, by + bh * 0.5f, 4.8f).centered().color(GuiDraw.mix(Color.rgb(160, 165, 176), accent, bindAnim).withAlpha(prog)).render(graphics);
        }
        if (hasSettings) {
            Text.of(Fonts.ICONS, expand > 0.5f ? "P" : "O", x + w - 8.5f, y + 13.5f, 5.5f).centered().color(Color.rgb(140, 145, 155).withAlpha(prog)).render(graphics);
        }
        float hitTop = Math.max(y, clipTop);
        float hitBottom = Math.min(y + 27.0f, clipBottom);
        if (hitBottom > hitTop) {
            this.hit(x, hitTop, w, hitBottom - hitTop, () -> this.toggleModule(module), hasSettings ? () -> this.expandModule(module) : null);
            this.ctx.hits.middle(x, hitTop, w, hitBottom - hitTop, () -> this.startModuleBind(module));
            if (bindChip != null && !listening && bindChip[1] >= hitTop && bindChip[1] + bindChip[3] <= hitBottom) {
                this.hit(bindChip[0], bindChip[1], bindChip[2], bindChip[3], () -> this.cycleModuleBindMode(module), () -> this.startModuleBind(module));
            }
            if (hasSettings) {
                this.hit(x + w - 15.0f, hitTop, 15.0f, hitBottom - hitTop, () -> this.expandModule(module), () -> this.expandModule(module));
            }
        }
        if (expand > 0.01f && hasSettings) {
            float sy = y + 27.0f;
            float visibleTop = Math.max(sy, clipTop);
            float visibleBottom = Math.min(y + h, clipBottom);
            if (visibleBottom - visibleTop >= 1.5f) {
                Scissor.push(graphics, x, visibleTop, w, visibleBottom - visibleTop);
                float top = expand > 0.95f ? visibleTop : Float.MAX_VALUE;
                for (Setting<?> setting : module.getSettings()) {
                    sy += this.ctx.settings.renderAnimatedSetting(graphics, setting, x + 8.0f, sy, w - 16.0f, prog * expand, accent, top, visibleBottom);
                }
                Scissor.pop(graphics);
            }
        }
        return h;
    }
}

