package com.orbitdlc.client.mixin;

import com.mojang.authlib.GameProfile;
import com.orbitdlc.client.LocalAccounts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
abstract class LocalAccountMixin {
    @Inject(method = "getUser", at = @At("HEAD"), cancellable = true)
    private void orbitdlc$localUser(CallbackInfoReturnable<User> callback) {
        var user = LocalAccounts.active();
        if (user != null) callback.setReturnValue(user);
    }

    @Inject(method = "getGameProfile", at = @At("HEAD"), cancellable = true)
    private void orbitdlc$localProfile(CallbackInfoReturnable<GameProfile> callback) {
        var user = LocalAccounts.active();
        if (user != null) callback.setReturnValue(new GameProfile(user.getProfileId(), user.getName()));
    }

}
