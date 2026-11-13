/*
 * Decompiled with CFR 0.152.
 */
package brokke.cc.util.render.display.text;

public record TextBounds(float x, float y, float width, float height) {
    public float right() {
        return this.x + this.width;
    }

    public float bottom() {
        return this.y + this.height;
    }

    public float centerX() {
        return this.x + this.width * 0.5f;
    }

    public float centerY() {
        return this.y + this.height * 0.5f;
    }

    public boolean contains(float px, float py) {
        return px >= this.x && py >= this.y && px < this.right() && py < this.bottom();
    }
}

