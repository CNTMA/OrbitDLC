package com.orbitdlc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;

final class ChinaHat extends Module {
    static final ChinaHat INSTANCE = new ChinaHat();
    private static final int SIDES = 64;
    private static final float RADIUS = 0.65f;
    private static final float BRIM = -0.60f;
    private static final float TOP = -0.94f;
    private static final float[] RING_X = new float[SIDES + 1];
    private static final float[] RING_Z = new float[SIDES + 1];

    static {
        for (int i = 0; i <= SIDES; i++) {
            double angle = Math.TAU * i / SIDES;
            RING_X[i] = (float) Math.cos(angle);
            RING_Z[i] = (float) Math.sin(angle);
        }
    }

    private ChinaHat() { super("chinahat", Category.VISUALS); }

    @Override
    public String description() { return "A conical hat visible in third person."; }

    void submit(PoseStack pose, SubmitNodeCollector collector) {
        int accent = Theme.color(0xFFE2ACFF);
        int tip = OrbitButton.blend(accent, 0xFFFFFFFF, 0.64f);
        int inner = OrbitButton.blend(accent, 0xFF302031, 0.6f);
        collector.submitCustomGeometry(pose, RenderTypes.debugQuads(), (hat, vertices) -> {
            for (int i = 0; i < SIDES; i++) {
                float x = RING_X[i], z = RING_Z[i];
                float nextX = RING_X[i + 1], nextZ = RING_Z[i + 1];
                int shade = OrbitButton.blend(inner, accent, (x * 0.5f + 0.5f) * 0.55f);
                vertices.addVertex(hat, 0, TOP, 0).setColor(tip);
                vertices.addVertex(hat, x * RADIUS, BRIM, z * RADIUS).setColor(shade);
                vertices.addVertex(hat, nextX * RADIUS, BRIM, nextZ * RADIUS).setColor(shade);
                vertices.addVertex(hat, 0, TOP, 0).setColor(tip);

                float edge = RADIUS + 0.025f;
                vertices.addVertex(hat, x * RADIUS, BRIM, z * RADIUS).setColor(accent);
                vertices.addVertex(hat, x * edge, BRIM + 0.018f, z * edge).setColor(tip);
                vertices.addVertex(hat, nextX * edge, BRIM + 0.018f, nextZ * edge).setColor(tip);
                vertices.addVertex(hat, nextX * RADIUS, BRIM, nextZ * RADIUS).setColor(accent);
            }
        });
    }
}
