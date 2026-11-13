/*
 * Decompiled with CFR 0.152.
 */
package brokke.cc.util.render.display.text;

import brokke.cc.util.color.Color;

public record TextEffect(int color, float offsetX, float offsetY, float spread, float blur) {
    public static TextEffect shadow(int argb, float offsetX, float offsetY) {
        return TextEffect.shadow(argb, offsetX, offsetY, 0.0f);
    }

    public static TextEffect shadow(int argb, float offsetX, float offsetY, float blur) {
        return new TextEffect(argb, offsetX, offsetY, 0.0f, Math.max(blur, 0.0f));
    }

    public static TextEffect shadow(Color color, float offsetX, float offsetY) {
        return TextEffect.shadow(color.argb(), offsetX, offsetY);
    }

    public static TextEffect shadow(Color color, float offsetX, float offsetY, float blur) {
        return TextEffect.shadow(color.argb(), offsetX, offsetY, blur);
    }

    public static TextEffect outline(int argb, float width) {
        return new TextEffect(argb, 0.0f, 0.0f, Math.max(width, 0.0f), 0.0f);
    }

    public static TextEffect outline(Color color, float width) {
        return TextEffect.outline(color.argb(), width);
    }

    public static TextEffect glow(int argb, float blur) {
        float safe = Math.max(blur, 0.0f);
        return new TextEffect(argb, 0.0f, 0.0f, safe * 0.5f, safe);
    }

    public static TextEffect glow(Color color, float blur) {
        return TextEffect.glow(color.argb(), blur);
    }

    public TextEffect withColor(int argb) {
        return new TextEffect(argb, this.offsetX, this.offsetY, this.spread, this.blur);
    }

    public TextEffect withColor(Color color) {
        return this.withColor(color.argb());
    }

    public float reach() {
        return Math.max(Math.abs(this.offsetX), Math.abs(this.offsetY)) + this.spread + this.blur;
    }
}

