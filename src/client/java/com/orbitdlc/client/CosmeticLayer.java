package com.orbitdlc.client;

import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.orbitdlc.OrbitDLC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

public final class CosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public CosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state,
                       float yaw, float pitch) {
        var player = Minecraft.getInstance().player;
        if (player == null || state.isInvisible) return;
        boolean local = state.id == player.getId();
        var equipped = local ? Cosmetics.equipped()
                : player.level().getEntity(state.id) instanceof net.minecraft.world.entity.player.Player remote
                        ? OrbitBadges.outfit(remote.getUUID()) : java.util.List.<Cosmetics.Entry>of();
        boolean hat = local && ChinaHat.INSTANCE.isEnabled();
        if (equipped.isEmpty() && !hat) return;
        var playerModel = getParentModel();
        playerModel.setupAnim(state);
        if (hat) {
            pose.pushPose();
            try {
                playerModel.head.translateAndRotate(pose);
                ChinaHat.INSTANCE.submit(pose, collector);
            } finally {
                pose.popPose();
            }
        }
        double seconds = System.nanoTime() * 1e-9;
        for (var entry : equipped) {
            var model = GeckoLibResources.getBakedModels().getModel(OrbitDLC.id(entry.id()));
            if (model.isMissingno()) continue;
            var motion = CosmeticMotion.byId(entry.id());
            pose.pushPose();
            try {
                boolean head = entry.category().equals("Hats") || entry.category().equals("Masks");
                (head ? playerModel.head : playerModel.body).translateAndRotate(pose);
                pose.rotateDegrees(Axis.ZP, 180f);
                // Wearables use a ground-level origin in the GeckoLib assets.
                // The player head and body pivots are at shoulder height.
                if (entry.category().equals("Pets")) {
                    pose.translate(0.48f, 0.12f, 0f);
                    pose.scale(0.7f, 0.7f, 0.7f);
                } else {
                    pose.translate(0f, -24f / 16f, 0f);
                }
                var texture = OrbitDLC.id("textures/" + entry.id() + ".png");
                collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (captured, vertices) -> {
                    var modelPose = new PoseStack();
                    modelPose.last().pose().set(captured.pose());
                    modelPose.last().normal().set(captured.normal());
                    for (var bone : model.topLevelBones()) {
                        renderBone(modelPose, vertices, bone, new Matrix4f(), motion, seconds, light);
                    }
                });
            } finally {
                pose.popPose();
            }
        }
    }

    private static void renderBone(PoseStack pose, VertexConsumer vertices, GeoBone bone, Matrix4f parent,
                                   CosmeticMotion motion, double seconds, int light) {
        var transform = CosmeticPreview.boneTransform(bone, parent, motion, seconds);
        if (bone instanceof CuboidGeoBone cuboid) {
            for (var cube : cuboid.cubes) {
                pose.pushPose();
                try {
                    pose.mulPose(transform);
                    cube.render(pose, vertices, light, OverlayTexture.NO_OVERLAY, -1);
                } finally {
                    pose.popPose();
                }
            }
        }
        for (var child : bone.children()) renderBone(pose, vertices, child, transform, motion, seconds, light);
    }
}
