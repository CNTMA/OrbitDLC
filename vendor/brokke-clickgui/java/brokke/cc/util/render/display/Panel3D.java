/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.pipeline.TextureTarget
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.renderpearl.api.GpuFormat
 *  com.mojang.renderpearl.api.buffers.GpuBuffer
 *  com.mojang.renderpearl.api.commands.CommandEncoder
 *  com.mojang.renderpearl.api.commands.RenderPass
 *  com.mojang.renderpearl.api.pipeline.BlendFunction
 *  com.mojang.renderpearl.api.pipeline.PrimitiveTopology
 *  com.mojang.renderpearl.api.pipeline.RenderPipeline
 *  com.mojang.renderpearl.api.pipeline.UniformType
 *  com.mojang.renderpearl.api.textures.FilterMode
 *  com.mojang.renderpearl.api.vertex.VertexFormat
 *  net.minecraft.client.Minecraft
 *  net.minecraft.resources.Identifier
 *  org.joml.Matrix3f
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector3f
 *  org.joml.Vector4f
 *  org.joml.Vector4fc
 *  org.lwjgl.system.MemoryUtil
 */
package brokke.cc.util.render.display;

import brokke.cc.util.render.ported.LegacyPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryUtil;

public final class Panel3D {
    public static final int MAX_PANELS = 3;
    private static final int UNIFORM_SIZE = 144;
    private static final Vector4f CLEAR = new Vector4f(0.0f, 0.0f, 0.0f, 0.0f);
    private static RenderPipeline pipeline;
    private static TextureTarget target;
    private static final GpuBuffer[] uniformBuffers;
    private static ByteBuffer uniformData;
    private static boolean requested;
    private static boolean valid;
    private static float alpha;
    private static int count;
    private static final Matrix3f[] inverses;
    private static final float[][] rects;
    private static final float[] tooltip;

    private Panel3D() {
    }

    public static void begin(float opacity) {
        alpha = opacity;
        count = 0;
        Arrays.fill(tooltip, -1.0f);
    }

    public static void tooltip(int x, int y, int w, int h) {
        if (count == 0) {
            return;
        }
        float s = Minecraft.getInstance().getWindow().getGuiScale();
        Panel3D.tooltip[0] = (float)x * s;
        Panel3D.tooltip[1] = (float)y * s;
        Panel3D.tooltip[2] = (float)(x + w) * s;
        Panel3D.tooltip[3] = (float)(y + h) * s;
    }

    public static void panel(float x0, float y0, float x1, float y1, Matrix4f transform) {
        if (count >= 3) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float s = mc.getWindow().getGuiScale();
        float cx = (float)mc.getWindow().getWidth() / 2.0f;
        float cy = (float)mc.getWindow().getHeight() / 2.0f;
        float d = (float)mc.getWindow().getHeight() * 1.6f;
        Matrix4f m = new Matrix4f().scale(s, s, s).mul((Matrix4fc)transform).scale(1.0f / s, 1.0f / s, 1.0f);
        float a0 = m.m00();
        float a1 = m.m10();
        float a2 = m.m30();
        float b0 = m.m01();
        float b1 = m.m11();
        float b2 = m.m31();
        float c0 = m.m02();
        float c1 = m.m12();
        float c2 = m.m32() + d;
        Matrix3f h = Panel3D.rows(cx * c0 + d * a0, cx * c1 + d * a1, cx * c2 + d * (a2 - cx), cy * c0 + d * b0, cy * c1 + d * b1, cy * c2 + d * (b2 - cy), c0, c1, c2);
        h.invert(inverses[count]);
        Panel3D.rects[Panel3D.count][0] = x0 * s;
        Panel3D.rects[Panel3D.count][1] = y0 * s;
        Panel3D.rects[Panel3D.count][2] = x1 * s;
        Panel3D.rects[Panel3D.count][3] = y1 * s;
        ++count;
        requested = true;
        valid = true;
    }

    public static void clear() {
        valid = false;
        requested = false;
        count = 0;
    }

    public static boolean consume() {
        boolean r = requested;
        requested = false;
        return r;
    }

    public static double[] unproject(double guiX, double guiY) {
        if (!valid) {
            return new double[]{guiX, guiY};
        }
        float s = Minecraft.getInstance().getWindow().getGuiScale();
        for (int i = count - 1; i >= 0; --i) {
            float y;
            float x;
            Vector3f p = inverses[i].transform(new Vector3f((float)guiX * s, (float)guiY * s, 1.0f));
            if (p.z <= 0.0f || !Panel3D.inside(rects[i], x = p.x / p.z, y = p.y / p.z) || i == 0 && (count > 1 && Panel3D.inside(rects[1], x, y) || count > 2 && Panel3D.inside(rects[2], x, y))) continue;
            return new double[]{x / s, y / s};
        }
        return new double[]{-10000.0, -10000.0};
    }

    private static boolean inside(float[] r, float x, float y) {
        return x >= r[0] && y >= r[1] && x <= r[2] && y <= r[3];
    }

    public static RenderTarget target(RenderTarget main) {
        if (target == null || Panel3D.target.width != main.width || Panel3D.target.height != main.height) {
            if (target != null) {
                target.destroyBuffers();
            }
            target = new TextureTarget("brokke panel3d", main.width, main.height, GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
        }
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), (Vector4fc)CLEAR, target.getDepthTexture(), 0.0);
        return target;
    }

    public static void composite(RenderTarget main) {
        if (target == null) {
            return;
        }
        Panel3D.init();
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        for (int i = 0; i < count; ++i) {
            Matrix3f inv = inverses[i];
            uniformData.clear();
            for (int col = 0; col < 3; ++col) {
                uniformData.putFloat(inv.get(col, 0)).putFloat(inv.get(col, 1)).putFloat(inv.get(col, 2)).putFloat(0.0f);
            }
            uniformData.putFloat(0.0f).putFloat(0.0f).putFloat(0.0f).putFloat(0.0f);
            for (float v : rects[i]) {
                uniformData.putFloat(v);
            }
            uniformData.putFloat(alpha).putFloat(i == 0 ? 1.0f : 0.0f).putFloat(0.0f).putFloat(0.0f);
            for (int k = 1; k <= 2; ++k) {
                boolean ex = i == 0 && k < count;
                for (int j = 0; j < 4; ++j) {
                    uniformData.putFloat(ex ? rects[k][j] : -1.0f);
                }
            }
            for (float v : tooltip) {
                uniformData.putFloat(v);
            }
            uniformData.flip();
            encoder.writeToBuffer(uniformBuffers[i].slice(), uniformData);
            try (RenderPass pass = encoder.createRenderPass(() -> "Brokke Panel3D", main.getColorTextureView(), Optional.empty());){
                pass.setPipeline(RenderSystem.getCompiledPipeline((RenderPipeline)pipeline));
                pass.setUniform("texture_in", target.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                pass.setUniform("Panel3DData", uniformBuffers[i]);
                pass.draw(3, 1, 0, 0);
                continue;
            }
        }
    }

    public static void close() {
        if (target != null) {
            target.destroyBuffers();
        }
        target = null;
    }

    private static void init() {
        if (pipeline == null) {
            pipeline = LegacyPipeline.builder().withLocation(Identifier.fromNamespaceAndPath((String)"brokke", (String)"panel3d")).withVertexShader(Identifier.fromNamespaceAndPath((String)"brokke", (String)"panel3d_vertex")).withFragmentShader(Identifier.fromNamespaceAndPath((String)"brokke", (String)"panel3d_fragment")).withVertexFormat(VertexFormat.builder((int)0).build(), PrimitiveTopology.TRIANGLES).withUniform("Panel3DData", UniformType.UNIFORM_BUFFER).withSampler("texture_in").withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA).withCull(false).build();
        }
        if (uniformData == null) {
            uniformData = MemoryUtil.memAlloc((int)144).order(ByteOrder.nativeOrder());
        }
        for (int i = 0; i < 3; ++i) {
            if (uniformBuffers[i] != null) continue;
            Panel3D.uniformBuffers[i] = RenderSystem.getDevice().createBuffer(() -> "brokke:panel3d_uniforms", 136, 144L);
        }
    }

    private static Matrix3f rows(float a00, float a01, float a02, float a10, float a11, float a12, float a20, float a21, float a22) {
        return new Matrix3f(a00, a10, a20, a01, a11, a21, a02, a12, a22);
    }

    static {
        uniformBuffers = new GpuBuffer[3];
        alpha = 1.0f;
        inverses = new Matrix3f[3];
        rects = new float[3][4];
        tooltip = new float[]{-1.0f, -1.0f, -1.0f, -1.0f};
        for (int i = 0; i < 3; ++i) {
            Panel3D.inverses[i] = new Matrix3f();
        }
    }
}

