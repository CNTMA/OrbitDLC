/*
 * Decompiled with CFR 0.152.
 */
package brokke.cc.util.render.display.blur;

public enum BlurType {
    KAWASE,
    GAUSSIAN,
    BOX;

    public static final int MAX_STRENGTH = 6;

    public int outputLevel(int strength) {
        return this == KAWASE ? 0 : (strength + 1) / 2;
    }

    public int downDepth(int strength) {
        return this == KAWASE ? strength : this.outputLevel(strength);
    }
}

