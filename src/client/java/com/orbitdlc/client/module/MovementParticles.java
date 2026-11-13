package com.orbitdlc.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public final class MovementParticles extends Module {
    public enum Trigger { JUMP, LAND, TRAIL }

    private final Trigger trigger;
    private final ParticleOptions particle;
    private Object player;
    private boolean grounded;
    private int ticks;

    public MovementParticles(String id, Trigger trigger, ParticleOptions particle) {
        super(id, Category.VISUALS);
        this.trigger = trigger;
        this.particle = particle;
    }

    @Override
    public String description() {
        return switch (trigger) {
            case JUMP -> "Spawn a burst of particles when you leave the ground upward.";
            case LAND -> "Spawn a burst of particles when you land.";
            case TRAIL -> "Leave particles behind while sprinting.";
        };
    }

    @Override
    protected void onEnable() {
        player = null;
        ticks = 0;
    }

    public void tick(Minecraft client) {
        if (!isEnabled() || client.level == null || client.player == null) {
            player = null;
            return;
        }

        var current = client.player;
        boolean onGround = current.onGround();
        if (player != current) {
            player = current;
            grounded = onGround;
            return;
        }

        boolean emit = switch (trigger) {
            case JUMP -> grounded && !onGround && current.getDeltaMovement().y > 0;
            case LAND -> !grounded && onGround;
            case TRAIL -> ++ticks % 4 == 0 && current.isSprinting() && onGround;
        };
        grounded = onGround;
        if (!emit) return;

        Vec3 origin = current.position();
        var random = ThreadLocalRandom.current();
        int count = trigger == Trigger.TRAIL ? 2 : 8;
        for (int i = 0; i < count; i++) {
            double x = random.nextDouble(-0.35, 0.35);
            double z = random.nextDouble(-0.35, 0.35);
            client.level.addParticle(particle, origin.x + x, origin.y + 0.15, origin.z + z,
                    x * 0.25, 0.06, z * 0.25);
        }
    }
}
