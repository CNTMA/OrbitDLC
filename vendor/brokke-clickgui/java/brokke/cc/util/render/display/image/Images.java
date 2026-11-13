/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.NativeImage
 *  com.mojang.renderpearl.api.textures.GpuTexture
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.texture.AbstractTexture
 *  net.minecraft.client.renderer.texture.DynamicTexture
 *  net.minecraft.resources.Identifier
 */
package brokke.cc.util.render.display.image;

import brokke.cc.util.render.display.image.ImageAsset;
import brokke.cc.util.render.display.image.ImageSize;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.renderpearl.api.textures.GpuTexture;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class Images {
    private static final String NAMESPACE = "brokke";
    private static final String DIRECTORY = "images/";
    private static final Map<String, ImageAsset> REGISTRY = new LinkedHashMap<String, ImageAsset>();
    private static final Map<Identifier, ImageSize> SIZES = new HashMap<Identifier, ImageSize>();

    private Images() {
    }

    public static void init() {
    }

    public static ImageAsset register(String name) {
        return Images.register(name, DIRECTORY + name + ".png");
    }

    public static ImageAsset register(String name, String path) {
        return REGISTRY.computeIfAbsent(name, key -> new ImageAsset((String)key, Identifier.fromNamespaceAndPath((String)NAMESPACE, (String)path)));
    }

    public static ImageAsset get(String name) {
        ImageAsset asset = REGISTRY.get(name);
        if (asset == null) {
            throw new IllegalArgumentException("Unknown image: " + name);
        }
        return asset;
    }

    public static boolean has(String name) {
        return REGISTRY.containsKey(name);
    }

    public static Collection<ImageAsset> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static ImageSize size(Identifier texture) {
        AbstractTexture registered = Minecraft.getInstance().getTextureManager().getTexture(texture);
        if (registered instanceof DynamicTexture) {
            DynamicTexture dynamic = (DynamicTexture)registered;
            NativeImage pixels = dynamic.getPixels();
            if (pixels != null) {
                return new ImageSize(pixels.getWidth(), pixels.getHeight());
            }
            GpuTexture gpuTexture = dynamic.getTexture();
            if (gpuTexture != null) {
                return new ImageSize(gpuTexture.getWidth(0), gpuTexture.getHeight(0));
            }
        }
        return SIZES.computeIfAbsent(texture, Images::load);
    }

    public static void reload() {
        SIZES.clear();
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private static ImageSize load(Identifier texture) {
        try (InputStream stream = Minecraft.getInstance().getResourceManager().getResourceOrThrow(texture).open();){
            ImageSize imageSize;
            block14: {
                NativeImage image = NativeImage.read((InputStream)stream);
                try {
                    imageSize = new ImageSize(image.getWidth(), image.getHeight());
                    if (image == null) break block14;
                }
                catch (Throwable throwable) {
                    if (image != null) {
                        try {
                            image.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                image.close();
            }
            return imageSize;
        }
        catch (IOException exception) {
            throw new UncheckedIOException("Failed to read image " + String.valueOf(texture), exception);
        }
    }
}

