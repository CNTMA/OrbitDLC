package com.orbitdlc.client.mixin;

import com.orbitdlc.client.AspectRatio;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(GameRenderer.class)
abstract class AspectRatioHandMixin {
    @ModifyArgs(method = "render3dHud", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Projection;setupPerspective(FFFFF)V"))
    private void orbitdlc$applyHandAspectRatio(Args args) {
        args.set(3, AspectRatio.projectionWidth(args.get(3), args.get(4)));
    }
}
