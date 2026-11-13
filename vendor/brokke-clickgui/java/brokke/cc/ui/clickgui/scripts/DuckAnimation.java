/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.Identifier
 */
package brokke.cc.ui.clickgui.scripts;

import net.minecraft.resources.Identifier;

public final class DuckAnimation {
    private static final int FRAMES = 151;
    private static final long FRAME_MS = 20L;
    private static final Identifier[] TEXTURES = new Identifier[151];

    private DuckAnimation() {
    }

    public static Identifier frame() {
        return TEXTURES[(int)(System.currentTimeMillis() / 20L % 151L)];
    }

    static {
        for (int i = 0; i < 151; ++i) {
            DuckAnimation.TEXTURES[i] = Identifier.fromNamespaceAndPath((String)"brokke", (String)("textures/duck/" + i + ".png"));
        }
    }
}

