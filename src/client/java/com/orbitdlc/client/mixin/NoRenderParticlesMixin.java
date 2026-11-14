package com.orbitdlc.client.mixin;

import com.orbitdlc.client.NoRender;
import com.orbitdlc.client.Optimization;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ThreadLocalRandom;

@Mixin(ParticleEngine.class)
abstract class NoRenderParticlesMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void orbitdlc$reduceParticles(ParticleOptions options, double x, double y, double z,
            double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> callback) {
        var type = options.getType();
        boolean reduce = switch (type) {
            case net.minecraft.core.particles.ParticleType<?> particle when particle == ParticleTypes.ENTITY_EFFECT || particle == ParticleTypes.EFFECT
                    || particle == ParticleTypes.INSTANT_EFFECT -> NoRender.hides(NoRender.Effect.POTION_PARTICLES);
            case net.minecraft.core.particles.ParticleType<?> particle when particle == ParticleTypes.CRIT || particle == ParticleTypes.ENCHANTED_HIT
                    || particle == ParticleTypes.DAMAGE_INDICATOR || particle == ParticleTypes.SWEEP_ATTACK ->
                    NoRender.hides(NoRender.Effect.HIT_PARTICLES);
            case net.minecraft.core.particles.ParticleType<?> particle when particle == ParticleTypes.EXPLOSION ->
                    NoRender.hides(NoRender.Effect.EXPLOSION_PARTICLES);
            default -> false;
        };
        boolean ambient = type == ParticleTypes.ASH || type == ParticleTypes.WHITE_ASH
                || type == ParticleTypes.MYCELIUM || type == ParticleTypes.CRIMSON_SPORE
                || type == ParticleTypes.WARPED_SPORE;
        reduce |= ambient && Optimization.level() >= 2;
        if (reduce && ThreadLocalRandom.current().nextBoolean()) callback.setReturnValue(null);
    }
}
