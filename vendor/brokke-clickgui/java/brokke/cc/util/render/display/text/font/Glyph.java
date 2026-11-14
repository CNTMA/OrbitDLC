/*
 * Decompiled with CFR 0.152.
 */
package brokke.cc.util.render.display.text.font;

public record Glyph(int unicode, float advance, float left, float bottom, float right, float top, float u0, float v0, float u1, float v1) {
    public boolean visible() {
        return this.right > this.left && this.top > this.bottom;
    }

    public float width() {
        return this.right - this.left;
    }

    public float height() {
        return this.top - this.bottom;
    }
}

