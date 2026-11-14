package com.orbitdlc.client.mixin;

import com.orbitdlc.client.Animations;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerTabOverlay.class)
abstract class TabAnimationsMixin {
    @Unique private long orbitdlc$lastFrame, orbitdlc$openedAt;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void orbitdlc$slideIn(GuiGraphicsExtractor graphics, int width, net.minecraft.world.scores.Scoreboard scoreboard,
                                      net.minecraft.world.scores.Objective objective, CallbackInfo callback) {
        long now = System.nanoTime();
        if (now - orbitdlc$lastFrame > 150_000_000L) orbitdlc$openedAt = now;
        orbitdlc$lastFrame = now;
        graphics.pose().pushMatrix();
        if (Animations.plays(Animations.Part.TAB))
            graphics.pose().translate(0, -12 * Animations.remaining(orbitdlc$openedAt, 260));
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void orbitdlc$restore(GuiGraphicsExtractor graphics, int width, net.minecraft.world.scores.Scoreboard scoreboard,
                                      net.minecraft.world.scores.Objective objective, CallbackInfo callback) {
        graphics.pose().popMatrix();
    }
}

