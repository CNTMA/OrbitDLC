package com.orbitdlc.client.mixin;

import com.orbitdlc.client.Ambience;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientClockManager.class)
abstract class AmbienceClockMixin {
    @Inject(method = "getInstance(Lnet/minecraft/core/Holder;)Lnet/minecraft/client/ClientClockManager$ClientClockInstance;",
            at = @At("RETURN"), cancellable = true)
    private void orbitdlc$overrideClock(Holder<WorldClock> clock,
            CallbackInfoReturnable<ClientClockManager.ClientClockInstance> callback) {
        Long time = Ambience.clock();
        if (time != null && clock.unwrapKey().filter(WorldClocks.OVERWORLD::equals).isPresent()) {
            callback.setReturnValue(new ClientClockManager.ClientClockInstance() {
                @Override public long totalTicks() { return time; }
                @Override public float partialTick() { return 0f; }
                @Override public float rate() { return 0f; }
                @Override public boolean isPaused() { return true; }
            });
        }
    }
}
