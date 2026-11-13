/*
 * Decompiled with CFR 0.152.
 */
package brokke.cc.util.render.display.text;

public enum TextAlign {
    LEFT(0.0f),
    CENTER(0.5f),
    RIGHT(1.0f);

    private final float factor;

    private TextAlign(float factor) {
        this.factor = factor;
    }

    public float factor() {
        return this.factor;
    }
}

