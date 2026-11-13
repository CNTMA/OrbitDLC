/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.resources.Identifier
 */
package brokke.cc.ui.clickgui.tab;

import brokke.cc.core.module.Category;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.ClickGuiLayout;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.animation.Animation;
import brokke.cc.util.animation.Easing;
import brokke.cc.util.color.Color;
import brokke.cc.util.discord.ProfileAvatar;
import brokke.cc.util.render.display.image.Image;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.TextAlign;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.utils.User;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class Sidebar
extends GuiComponent {
    public final Animation selectAnim = new Animation();
    private final Animation brandAnim = new Animation();
    private final Animation[] hoverAnims = new Animation[ClickGuiLayout.CATEGORIES.size()];

    public Sidebar(ClickGuiContext ctx) {
        super(ctx);
        for (int i = 0; i < this.hoverAnims.length; ++i) {
            this.hoverAnims[i] = new Animation();
            this.hoverAnims[i].setEasing(Easing.CUBIC_OUT);
        }
    }

    public int categoryAt(float mx, float my) {
        float ix = this.ctx.panelX + 5.0f;
        float iw = 70.0f;
        float top = ClickGuiLayout.itemsTop(this.ctx.panelY);
        for (int i = 0; i < ClickGuiLayout.CATEGORIES.size(); ++i) {
            float iy = ClickGuiLayout.itemY(top, i);
            if (!(mx >= ix) || !(mx <= ix + iw) || !(my >= iy) || !(my <= iy + 13.0f)) continue;
            return i;
        }
        return -1;
    }

    public void renderSidebar(GuiGraphicsExtractor graphics, float px, float py, float prog, float delta) {
        int hovered;
        float target;
        Color accent = GuiDraw.getAccent();
        float radius = GuiDraw.panelRadius();
        RoundedRect.of(px, py, 80.0f, 250.0f).radius(radius, 0.0f, 0.0f, radius).color(Color.rgba(0, 0, 0, Math.round(35.0f * prog))).render(graphics);
        boolean logoHover = !this.ctx.closing && this.hovered(px + 5.0f, py + 2.0f, 70.0f, 18.0f);
        float f = target = logoHover ? 1.0f : 0.0f;
        if (GuiDraw.guiAnimationsEnabled()) {
            this.brandAnim.animate(target, delta, 5.0f, Easing.CUBIC_OUT);
        } else {
            this.brandAnim.set(target);
        }
        float reveal = Math.clamp(this.brandAnim.value(), 0.0f, 1.0f);
        Text.of(Fonts.ICONS, "N", px + 40.0f, py + 11.0f, 11.0f).centered().color(Color.rgb(235, 238, 245).withAlpha(prog * Math.clamp(1.0f - reveal * 2.0f, 0.0f, 1.0f))).render(graphics);
        if (reveal > 0.001f) {
            double brandPhase = (double)System.nanoTime() / 4.5E8;
            Text.of(Fonts.SEMIBOLD, "Brokke.cc 3.0", px + 40.0f, py + 11.0f, 6.5f).centered().colors((glyph, tx, ty) -> {
                float wave = 0.5f + 0.5f * (float)Math.sin((double)tx * Math.PI * 2.0 - brandPhase);
                return accent.lerp(GuiDraw.getAccent2(), wave).lerp(Color.WHITE, wave * 0.45f).withAlpha(prog * Math.clamp((reveal - 0.3f) / 0.7f, 0.0f, 1.0f)).argb();
            }).render(graphics);
        }
        float top = ClickGuiLayout.itemsTop(py);
        float ix = px + 5.0f;
        float iw = 70.0f;
        this.selectAnim.animate(this.ctx.selected, delta, 4.0f);
        float selY = ClickGuiLayout.itemY(top, this.selectAnim.value());
        RoundedRect.of(px + 1.5f, selY + 3.0f, 1.2f, 7.0f).radius(0.6f).verticalGradient(accent.withAlpha(prog), GuiDraw.getAccent2().withAlpha(prog)).render(graphics);
        RoundedRect.of(px + 5.0f + 3.0f, ClickGuiLayout.itemY(top, ClickGuiLayout.mainCount - 1) + 13.0f + 1.0f + 3.5f - 0.25f, 64.0f, 0.5f).color(Color.rgba(255, 255, 255, Math.round(14.0f * prog))).render(graphics);
        int n = hovered = this.ctx.closing ? -1 : this.categoryAt(this.getMouseX(), this.getMouseY());
        if (hovered >= 0) {
            this.ctx.hoverNow = "category-" + hovered;
        }
        for (int i = 0; i < ClickGuiLayout.CATEGORIES.size(); ++i) {
            Category category = ClickGuiLayout.CATEGORIES.get(i);
            float iy = ClickGuiLayout.itemY(top, i);
            float midY = iy + 6.5f;
            this.hoverAnims[i].animate(i == hovered && i != this.ctx.selected ? 1.0f : 0.0f, delta, 6.0f);
            float hover = this.hoverAnims[i].value();
            float active = Math.clamp(1.0f - Math.abs(this.selectAnim.value() - (float)i), 0.0f, 1.0f);
            float t = Math.max(active, hover * 0.5f);
            Color color = GuiDraw.mix(Color.rgb(135, 140, 150), Color.rgb(240, 242, 248), t).withAlpha(prog);
            Text.of(Fonts.ICONS, category == null ? "R" : category.icon(), ix + 6.0f, midY, 6.2f).centered().color(color).render(graphics);
            Text.of(Fonts.MEDIUM, ClickGuiLayout.categoryName(category), ix + 13.0f, midY, 6.0f).verticalAlign(VerticalAlign.MIDDLE).color(color).render(graphics);
            if (category == null || !category.isMainCategory()) continue;
            Text.of(Fonts.MEDIUM, String.valueOf(this.ctx.guiModules().stream().filter(module -> module.category() == category).count()), ix + iw - 3.0f, midY, 5.2f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(115, 120, 130).withAlpha(prog)).render(graphics);
        }
    }

    public void renderProfile(GuiGraphicsExtractor graphics, float px, float py, float prog) {
        Identifier avatar;
        String role;
        float head = 13.0f;
        float hx = px + 5.0f + 1.0f;
        float hy = py + 250.0f - head - 6.0f;
        int a = Math.round(255.0f * prog);
        String username = User.getInstance().getUsername();
        if (username == null || username.isBlank()) {
            String string = username = this.ctx.minecraft().getUser() != null ? this.ctx.minecraft().getUser().getName() : "Player";
        }
        if ((role = User.getInstance().getRole()) == null || role.isBlank()) {
            role = "user";
        }
        if ((avatar = ProfileAvatar.getTexture()) != null) {
            RoundedRect.of(hx, hy, head, head).radius(head * 0.5f).color(Color.rgba(22, 24, 32, a)).render(graphics);
            Image.of(avatar, hx, hy, head, head).circle().render(graphics);
        } else {
            RoundedRect.of(hx, hy, head, head).radius(head * 0.5f).color(GuiDraw.getAccent().withAlpha(a)).render(graphics);
            Text.of(Fonts.MEDIUM, username.substring(0, 1).toUpperCase(), hx + head * 0.5f, hy + head * 0.5f, 6.0f).centered().color(Color.rgba(255, 255, 255, a)).render(graphics);
        }
        float tx = hx + head + 4.0f;
        Text.of(Fonts.MEDIUM, username, tx, hy + head * 0.32f, 5.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgba(238, 241, 248, a)).render(graphics);
        Text.of(Fonts.MEDIUM, role, tx, hy + head * 0.75f, 4.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgba(125, 130, 140, a)).render(graphics);
        this.hit(hx - 1.0f, hy - 2.0f, 70.0f, head + 4.0f, ProfileAvatar::choose, null);
    }
}

