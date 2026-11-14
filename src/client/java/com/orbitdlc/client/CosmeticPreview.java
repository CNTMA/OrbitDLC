package com.orbitdlc.client;

import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.orbitdlc.OrbitDLC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class CosmeticPreview {
    private record Point(float x, float y, float u, float v) {}
    private record Face(List<Point> points, float depth, int color) {}

    private CosmeticPreview() {}

    static void draw(GuiGraphicsExtractor graphics, String id, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        var model = GeckoLibResources.getBakedModels().getModel(OrbitDLC.id(id));
        if (model.isMissingno()) return;
        var faces = new ArrayList<Face>();
        float angle = (System.nanoTime() % 9_000_000_000L) / 9_000_000_000f * (float) Math.TAU;
        var camera = new Matrix4f().rotateX(0.23f).rotateY(angle);
        var motion = CosmeticMotion.byId(id);
        double seconds = System.nanoTime() * 1e-9;
        for (var bone : model.topLevelBones()) collect(bone, camera, faces, motion, seconds);
        if (faces.isEmpty()) return;
        faces.sort(Comparator.comparingDouble(Face::depth));

        float minX = Float.POSITIVE_INFINITY, minY = minX;
        float maxX = Float.NEGATIVE_INFINITY, maxY = maxX;
        for (var face : faces) {
            for (var point : face.points()) {
                minX = Math.min(minX, point.x());
                minY = Math.min(minY, point.y());
                maxX = Math.max(maxX, point.x());
                maxY = Math.max(maxY, point.y());
            }
        }
        float scale = 0.85f * Math.min(width / Math.max(0.01f, maxX - minX),
                height / Math.max(0.01f, maxY - minY));
        var pose = new Matrix3x2f(graphics.pose());
        var bounds = new ScreenRectangle(x, y, width, height).transformMaxBounds(pose);
        var scissor = graphics.scissorStack.peek();
        if (scissor != null) {
            bounds = bounds.intersection(scissor);
            if (bounds == null || bounds.width() <= 0 || bounds.height() <= 0) return;
        }
        pose.translate(x + width / 2f - (minX + maxX) * scale / 2f,
                y + height / 2f + (minY + maxY) * scale / 2f);
        pose.scale(scale, -scale);
        var texture = Minecraft.getInstance().getTextureManager().getTexture(OrbitDLC.id("textures/" + id + ".png"));
        var setup = TextureSetup.singleTexture(texture.getTextureView(),
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        graphics.guiRenderState.addGuiElement(new PreviewState(pose, faces, setup, scissor, bounds));
    }

    static Matrix4f boneTransform(GeoBone bone, Matrix4f parent, CosmeticMotion motion, double seconds) {
        float x = bone.pivotX() / 16f, y = bone.pivotY() / 16f, z = bone.pivotZ() / 16f;
        var transform = new Matrix4f(parent).translate(x, y, z)
                .rotateZYX(bone.baseRotZ(), bone.baseRotY(), bone.baseRotX());
        motion.apply(transform, bone.name(), seconds);
        return transform.translate(-x, -y, -z);
    }

    private static void collect(GeoBone bone, Matrix4f parent, List<Face> faces, CosmeticMotion motion, double seconds) {
        var transform = boneTransform(bone, parent, motion, seconds);
        if (bone instanceof CuboidGeoBone cuboid) {
            for (var cube : cuboid.cubes) {
                var local = new PoseStack();
                cube.translateToPivotPoint(local);
                cube.rotate(local);
                cube.translateAwayFromPivotPoint(local);
                var matrix = new Matrix4f(transform).mul(local.last().pose());
                for (var quad : cube.quads()) {
                    if (quad == null) continue;
                    var points = new ArrayList<Point>(4);
                    float depth = 0f;
                    for (var vertex : quad.vertices()) {
                        var point = matrix.transformPosition(vertex.posX(), vertex.posY(), vertex.posZ(), new Vector3f());
                        points.add(new Point(point.x, point.y, vertex.texU(), vertex.texV()));
                        depth += point.z;
                    }
                    var normal = matrix.transformDirection(quad.normalVec()).normalize();
                    int shade = (int) (190 + 65 * Math.abs(normal.y));
                    faces.add(new Face(points, depth / 4f, 0xFF000000 | shade * 0x010101));
                }
            }
        }
        for (var child : bone.children()) collect(child, transform, faces, motion, seconds);
    }

    private record PreviewState(Matrix3x2f pose, List<Face> faces, TextureSetup textureSetup,
                                ScreenRectangle scissorArea, ScreenRectangle bounds) implements GuiElementRenderState {
        @Override
        public RenderPipeline pipeline() { return RenderPipelines.GUI_TEXTURED; }

        @Override
        public void buildVertices(VertexConsumer consumer) {
            for (var face : faces) {
                for (var point : face.points()) {
                    consumer.addVertexWith2DPose(pose, point.x(), point.y())
                            .setUv(point.u(), point.v()).setColor(face.color());
                }
            }
        }
    }
}
