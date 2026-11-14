package com.orbitdlc.client.mixin;

import com.orbitdlc.client.Optimization;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
abstract class OptimizationCloudsMixin {
    @Inject(method = "getCloudStatus", at = @At("HEAD"), cancellable = true)
    private void orbitdlc$reduceClouds(CallbackInfoReturnable<CloudStatus> callback) {
        if (Optimization.level() >= 2) callback.setReturnValue(CloudStatus.OFF);
    }
}
