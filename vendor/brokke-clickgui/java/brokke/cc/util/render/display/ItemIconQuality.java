/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix3x2fc
 */
package brokke.cc.util.render.display;

import org.joml.Matrix3x2fc;

public final class ItemIconQuality {
    private ItemIconQuality() {
    }

    public static int atlasSlotSize(int size) {
        return Math.max(64, size);
    }

    public static boolean needsSmoothing(Matrix3x2fc pose) {
        return Math.abs(pose.m00() - 1.0f) > 1.0E-4f || Math.abs(pose.m11() - 1.0f) > 1.0E-4f || Math.abs(pose.m01()) > 1.0E-4f || Math.abs(pose.m10()) > 1.0E-4f;
    }
}

