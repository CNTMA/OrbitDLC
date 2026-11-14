package com.orbitdlc.client.mixin;

import com.orbitdlc.client.CosmeticLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
abstract class CosmeticLayerMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void orbitdlc$addCosmeticLayer(EntityRendererProvider.Context context, boolean slim, CallbackInfo callback) {
        var avatar = (AvatarRenderer<?>) (Object) this;
        ((CosmeticLayerAccess) avatar).orbitdlc$addLayer(new CosmeticLayer(avatar));
    }
}
