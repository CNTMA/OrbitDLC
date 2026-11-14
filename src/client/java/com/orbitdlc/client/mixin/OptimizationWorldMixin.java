package com.orbitdlc.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.orbitdlc.client.Optimization;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class OptimizationWorldMixin {
    @Shadow @Final private LevelRenderState levelRenderState;
    @Unique private final ObjectArrayList<RenderSection> orbitdlc$visibleTerrain = new ObjectArrayList<>();

    @Redirect(method = "extractSectionDrawGroups", at = @At(value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;iterator()Lit/unimi/dsi/fastutil/objects/ObjectListIterator;"))
    private ObjectListIterator<RenderSection> orbitdlc$clipTerrain(ObjectArrayList<RenderSection> candidates) {
        var frustum = Optimization.worldFrustum(levelRenderState.cameraRenderState);
        if (frustum == null) return candidates.iterator();

        // Keep the occlusion graph's list intact for section building and translucent sorting.
        orbitdlc$visibleTerrain.clear();
        for (int i = 0; i < candidates.size(); i++) {
            var section = candidates.get(i);
            if (frustum.isVisible(section.getBoundingBox())) orbitdlc$visibleTerrain.add(section);
        }
        return orbitdlc$visibleTerrain.iterator();
    }

    @Inject(method = "submitBlockEntities", at = @At("HEAD"))
    private void orbitdlc$clipBlockModels(PoseStack pose, LevelRenderState state, SubmitNodeCollector collector,
                                        CallbackInfo callback) {
        var frustum = Optimization.worldFrustum(state.cameraRenderState);
        if (frustum == null) return;

        state.blockEntityRenderStates.removeIf(block -> {
            var type = block.blockEntityType;
            // These models stay near their block. Renderers such as beacons can extend far beyond it.
            boolean local = type == BlockEntityTypes.CHEST || type == BlockEntityTypes.TRAPPED_CHEST
                    || type == BlockEntityTypes.ENDER_CHEST || type == BlockEntityTypes.SIGN
                    || type == BlockEntityTypes.HANGING_SIGN || type == BlockEntityTypes.BANNER
                    || type == BlockEntityTypes.SKULL || type == BlockEntityTypes.SHULKER_BOX;
            if (!local) return false;
            var pos = block.blockPos;
            return !frustum.isVisible(new AABB(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
                    pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2));
        });
    }
}
