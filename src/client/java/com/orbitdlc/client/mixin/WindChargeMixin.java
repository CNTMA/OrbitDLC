package com.orbitdlc.client.mixin;

import com.orbitdlc.client.MaceHelper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WindChargeItem.class)
abstract class WindChargeMixin {
    @Inject(method = "use", at = @At("RETURN"))
    private void orbitdlc$onWindCharge(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> callback) {
        if (player instanceof LocalPlayer local && callback.getReturnValue().consumesAction()) {
            MaceHelper.windChargeUsed(local);
        }
    }
}
