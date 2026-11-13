/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.pipeline.TextureTarget
 *  com.mojang.renderpearl.api.GpuFormat
 */
package brokke.cc.util.render.display.blur;

import brokke.cc.core.module.impl.render.ported.Optimizer;
import brokke.cc.util.render.display.blur.BlurKey;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.renderpearl.api.GpuFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

final class BlurTargets
implements AutoCloseable {
    private static final int LEVELS = 7;
    private final TextureTarget[] down = new TextureTarget[7];
    private final TextureTarget[] temp = new TextureTarget[7];
    private final Map<BlurKey, TextureTarget> outputs = new HashMap<BlurKey, TextureTarget>();
    private int baseWidth;
    private int baseHeight;
    private int downsample;

    BlurTargets() {
    }

    void ensure(RenderTarget main) {
        int desired = Optimizer.getBlurDownsample();
        if (main.width == this.baseWidth && main.height == this.baseHeight && this.downsample == desired) {
            return;
        }
        this.close();
        this.baseWidth = main.width;
        this.baseHeight = main.height;
        this.downsample = desired;
    }

    TextureTarget down(int level) {
        return this.obtain(this.down, level, "down");
    }

    TextureTarget temp(int level) {
        return this.obtain(this.temp, level, "temp");
    }

    TextureTarget output(BlurKey key) {
        return this.outputs.computeIfAbsent(key, k -> this.create("out " + String.valueOf((Object)k.type()) + " " + k.strength(), k.type().outputLevel(k.strength())));
    }

    @Override
    public void close() {
        BlurTargets.destroy(this.down);
        BlurTargets.destroy(this.temp);
        this.outputs.values().forEach(RenderTarget::destroyBuffers);
        this.outputs.clear();
    }

    private TextureTarget obtain(TextureTarget[] pool, int level, String label) {
        if (pool[level] == null) {
            pool[level] = this.create(label, level);
        }
        return pool[level];
    }

    private TextureTarget create(String label, int level) {
        return new TextureTarget("blur " + label + " L" + level, BlurTargets.levelSize(Math.max(1, this.baseWidth * 2 / this.downsample), level), BlurTargets.levelSize(Math.max(1, this.baseHeight * 2 / this.downsample), level), GpuFormat.RGBA8_UNORM, null);
    }

    private static int levelSize(int base, int level) {
        int size = base;
        for (int i = 0; i < level; ++i) {
            size = Math.max(1, size + 1 >> 1);
        }
        return size;
    }

    private static void destroy(TextureTarget[] pool) {
        Arrays.stream(pool).filter(target -> target != null).forEach(RenderTarget::destroyBuffers);
        Arrays.fill(pool, null);
    }
}

