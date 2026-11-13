/*
 * Decompiled with CFR 0.152.
 */
package brokke.cc.util.render.display.blur;

import brokke.cc.util.render.display.blur.BlurType;

public record BlurKey(BlurType type, int strength) {
    public BlurKey {
        strength = Math.clamp((long)strength, 1, 6);
    }
}

