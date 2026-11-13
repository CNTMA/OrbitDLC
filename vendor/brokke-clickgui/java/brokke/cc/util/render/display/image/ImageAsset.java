/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.Identifier
 */
package brokke.cc.util.render.display.image;

import brokke.cc.util.render.display.image.Image;
import brokke.cc.util.render.display.image.ImageSize;
import brokke.cc.util.render.display.image.Images;
import net.minecraft.resources.Identifier;

public final class ImageAsset {
    private final String name;
    private final Identifier texture;

    ImageAsset(String name, Identifier texture) {
        this.name = name;
        this.texture = texture;
    }

    public String name() {
        return this.name;
    }

    public Identifier texture() {
        return this.texture;
    }

    public ImageSize size() {
        return Images.size(this.texture);
    }

    public int width() {
        return this.size().width();
    }

    public int height() {
        return this.size().height();
    }

    public float aspect() {
        return this.size().aspect();
    }

    public Image image() {
        return Image.of(this);
    }
}

