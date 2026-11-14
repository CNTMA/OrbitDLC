package com.orbitdlc.client.mixin;

import com.orbitdlc.client.Skin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
abstract class SkinMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void orbitdlc$localSkin(CallbackInfoReturnable<PlayerSkin> callback) {
        if ((Object) this == Minecraft.getInstance().player) {
            callback.setReturnValue(Skin.apply(callback.getReturnValue()));
        }
    }
}
