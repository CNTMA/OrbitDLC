package com.orbitdlc.client.mixin;

import com.orbitdlc.client.AspectRatio;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
abstract class AspectRatioMixin {
    @ModifyArgs(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;setupPerspective(FFFFF)V"))
    private void orbitdlc$applyAspectRatio(Args args) {
        args.set(3, AspectRatio.projectionWidth(args.get(3), args.get(4)));
    }

    @ModifyArg(method = "createProjectionMatrixForCulling", at = @At(value = "INVOKE",
            target = "Lorg/joml/Matrix4f;perspective(FFFFZ)Lorg/joml/Matrix4f;"), index = 1)
    private float orbitdlc$applyCullingAspectRatio(float aspect) {
        return AspectRatio.resolve(aspect);
    }
}
