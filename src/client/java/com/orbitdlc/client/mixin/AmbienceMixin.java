package com.orbitdlc.client.mixin;

import com.orbitdlc.client.Ambience;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
abstract class AmbienceMixin {
    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void orbitdlc$rain(float delta, CallbackInfoReturnable<Float> callback) {
        apply(false, callback);
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void orbitdlc$thunder(float delta, CallbackInfoReturnable<Float> callback) {
        apply(true, callback);
    }

    @org.spongepowered.asm.mixin.Unique
    private void apply(boolean thunder, CallbackInfoReturnable<Float> callback) {
        if ((Object) this instanceof ClientLevel) {
            Float intensity = Ambience.weather(thunder);
            if (intensity != null) callback.setReturnValue(intensity);
        }
    }
}

