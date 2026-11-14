/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.widget;

import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.animation.Animation;
import brokke.cc.util.animation.Easing;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.rounded.RoundedRect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ScrollbarWidget
extends GuiComponent {
    public float thumbYValue = Float.NaN;
    private float thumbHValue = Float.NaN;
    private final Animation scrollbarAnim = new Animation();
    public boolean scrollDragging = false;
    public float scrollDragOffset;
    public float[] thumbBox = new float[4];
    public float[] trackBox = new float[4];

    public ScrollbarWidget(ClickGuiContext ctx) {
        super(ctx);
    }

    public void renderScrollbar(GuiGraphicsExtractor graphics, float x, float y, float h, float contentH, float scroll, float prog, float delta) {
        this.scrollbarAnim.animate(this.ctx.maxScroll > 0.0f ? 1.0f : 0.0f, delta, 4.0f, Easing.CUBIC_OUT);
        float vis = Math.clamp(this.scrollbarAnim.value(), 0.0f, 1.0f);
        this.trackBox = new float[]{x, y, 2.5f, h};
        if (vis <= 0.01f || contentH <= 0.0f) {
            this.thumbBox = new float[]{x, y, 2.5f, 0.0f};
            return;
        }
        float a = prog * vis;
        boolean hover = this.scrollDragging || this.hovered(x - 3.0f, y, 8.5f, h);
        float hv = this.anim("scrollbar", hover, 6.0f);
        float targetH = Math.max(14.0f, h * Math.min(1.0f, h / contentH));
        float k = this.ctx.maxScroll > 0.0f ? Math.clamp(scroll / this.ctx.maxScroll, 0.0f, 1.0f) : 0.0f;
        float targetY = (h - targetH) * k;
        if (Float.isNaN(this.thumbYValue) || Float.isNaN(this.thumbHValue)) {
            this.thumbYValue = targetY;
            this.thumbHValue = targetH;
        }
        this.thumbHValue = this.smooth(this.thumbHValue, targetH, 12.0f);
        this.thumbYValue = this.scrollDragging ? targetY : this.smooth(this.thumbYValue, targetY, 22.0f);
        float thumbH = this.thumbHValue;
        float thumbY = y + Math.clamp(this.thumbYValue, 0.0f, h - thumbH);
        this.thumbBox = new float[]{x, thumbY, 2.5f, thumbH};
        float w = 2.5f * (0.7f + 0.3f * hv);
        float tx = x + (2.5f - w) * 0.5f;
        RoundedRect.of(tx, y, w, h).radius(w * 0.5f).color(Color.rgba(255, 255, 255, Math.round((8.0f + 6.0f * hv) * a))).render(graphics);
        RoundedRect.of(tx, thumbY, w, thumbH).radius(w * 0.5f).color(GuiDraw.mix(Color.rgb(120, 125, 136), GuiDraw.getAccent(), hv).withAlpha((0.6f + 0.4f * hv) * a)).render(graphics);
    }
}

