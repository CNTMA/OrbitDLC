/*
 * Decompiled with CFR 0.152.
 */
package brokke.cc.util.render.display.image;

public record ImageSize(int width, int height) {
    public float aspect() {
        return this.height > 0 ? (float)this.width / (float)this.height : 1.0f;
    }
}

