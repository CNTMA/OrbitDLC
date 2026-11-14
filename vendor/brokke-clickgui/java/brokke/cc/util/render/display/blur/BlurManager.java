/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.pipeline.TextureTarget
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.renderpearl.api.commands.RenderPass
 *  com.mojang.renderpearl.api.pipeline.RenderPipeline
 *  com.mojang.renderpearl.api.textures.FilterMode
 *  com.mojang.renderpearl.api.textures.GpuSampler
 *  com.mojang.renderpearl.api.textures.GpuTextureView
 *  net.minecraft.client.Minecraft
 */
package brokke.cc.util.render.display.blur;

import brokke.cc.util.render.display.blur.BlurKey;
import brokke.cc.util.render.display.blur.BlurPipelines;
import brokke.cc.util.render.display.blur.BlurTargets;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;

public final class BlurManager {
    private static final BlurTargets TARGETS = new BlurTargets();
    private static final Set<BlurKey> REQUESTED = new LinkedHashSet<BlurKey>();
    private static final Map<RenderPipeline, Supplier<String>> LABELS = new IdentityHashMap<RenderPipeline, Supplier<String>>();

    private BlurManager() {
    }

    public static void request(BlurKey key) {
        REQUESTED.add(key);
    }

    public static GpuTextureView outputView(BlurKey key) {
        TARGETS.ensure(BlurManager.mainTarget());
        return TARGETS.output(key).getColorTextureView();
    }

    public static void process() {
        if (REQUESTED.isEmpty()) {
            return;
        }
        RenderTarget main = BlurManager.mainTarget();
        TARGETS.ensure(main);
        int depth = 0;
        for (BlurKey key : REQUESTED) {
            depth = Math.max(depth, key.type().downDepth(key.strength()));
        }
        GpuTextureView source = main.getColorTextureView();
        for (int level = 1; level <= depth; ++level) {
            TextureTarget target = TARGETS.down(level);
            BlurManager.pass(BlurPipelines.DOWNSAMPLE, source, target);
            source = target.getColorTextureView();
        }
        REQUESTED.forEach(BlurManager::process);
        REQUESTED.clear();
    }

    public static void close() {
        REQUESTED.clear();
        TARGETS.close();
    }

    private static void process(BlurKey key) {
        switch (key.type()) {
            case KAWASE: {
                BlurManager.kawase(key);
                break;
            }
            case GAUSSIAN: {
                BlurManager.separable(key, BlurPipelines.GAUSSIAN_HORIZONTAL, BlurPipelines.GAUSSIAN_VERTICAL);
                break;
            }
            case BOX: {
                BlurManager.separable(key, BlurPipelines.BOX_HORIZONTAL, BlurPipelines.BOX_VERTICAL);
            }
        }
    }

    private static void kawase(BlurKey key) {
        GpuTextureView source = TARGETS.down(key.strength()).getColorTextureView();
        for (int level = key.strength() - 1; level >= 0; --level) {
            TextureTarget target = level == 0 ? TARGETS.output(key) : TARGETS.temp(level);
            BlurManager.pass(BlurPipelines.UPSAMPLE, source, target);
            source = target.getColorTextureView();
        }
    }

    private static void separable(BlurKey key, RenderPipeline horizontal, RenderPipeline vertical) {
        int level = key.type().outputLevel(key.strength());
        int repeats = key.strength() % 2 == 0 ? 2 : 1;
        TextureTarget temp = TARGETS.temp(level);
        TextureTarget output = TARGETS.output(key);
        GpuTextureView source = TARGETS.down(level).getColorTextureView();
        for (int i = 0; i < repeats; ++i) {
            BlurManager.pass(horizontal, source, temp);
            BlurManager.pass(vertical, temp.getColorTextureView(), output);
            source = output.getColorTextureView();
        }
    }

    private static void pass(RenderPipeline pipeline, GpuTextureView source, TextureTarget target) {
        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(BlurManager.label(pipeline), target.getColorTextureView(), Optional.empty());){
            renderPass.setPipeline(RenderSystem.getCompiledPipeline((RenderPipeline)pipeline));
            RenderSystem.bindDefaultUniforms((RenderPass)renderPass);
            renderPass.setUniform("InSampler", source, sampler);
            renderPass.draw(3, 1, 0, 0);
        }
    }

    private static Supplier<String> label(RenderPipeline pipeline) {
        return LABELS.computeIfAbsent(pipeline, key -> {
            String value = "Blur " + key.getLocation().getPath();
            return () -> value;
        });
    }

    private static RenderTarget mainTarget() {
        return Minecraft.getInstance().gameRenderer.mainRenderTarget();
    }
}

