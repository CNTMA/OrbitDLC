package com.orbitdlc.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
abstract class WindowTitleMixin {
    @Inject(method = "createTitle", at = @At("HEAD"), cancellable = true)
    private void orbitdlc$windowTitle(CallbackInfoReturnable<String> callback) {
        callback.setReturnValue("OrbitDLC Beta - 26.3");
    }
}
