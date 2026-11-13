/*
 * Decompiled with CFR 0.152.
 */
package brokke.cc.ui.clickgui.screen;

import brokke.cc.ui.clickgui.screen.ClickGuiContext;

public abstract class GuiComponent {
    protected final ClickGuiContext ctx;

    protected GuiComponent(ClickGuiContext ctx) {
        this.ctx = ctx;
    }

    protected void hit(float x, float y, float w, float h, Runnable left, Runnable right) {
        this.ctx.hits.hit(x, y, w, h, left, right);
    }

    protected boolean hov(Object key, boolean on) {
        return this.ctx.hov(key, on);
    }

    protected float anim(Object key, boolean on, float speed) {
        return this.ctx.anim(key, on, speed);
    }

    protected float smooth(float current, float target, float rate) {
        return this.ctx.smooth(current, target, rate);
    }

    protected boolean hovered(float x, float y, float w, float h) {
        return this.ctx.hovered(x, y, w, h);
    }

    protected float getMouseX() {
        return this.ctx.getMouseX();
    }

    protected float getMouseY() {
        return this.ctx.getMouseY();
    }
}

