package com.orbitdlc.client;

import com.orbitdlc.client.module.DiscordPresence;
import com.orbitdlc.client.module.ModuleManager;
import com.orbitdlc.client.module.MovementParticles;
import com.orbitdlc.client.module.PlannedModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public final class OrbitDLCClient implements ClientModInitializer {
    private final ModuleManager modules = new ModuleManager();

    @Override
    public void onInitializeClient() {
        GlassBackdrop.init();
        VisualModules.registerShader(modules);
        modules.register(Ambience.INSTANCE);
        Ambience.INSTANCE.registerHud();
        ShaderSky.init();
        modules.register(ShaderSky.INSTANCE);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ShaderSky.INSTANCE.close());
        PlannedModule.registerAll(modules);
        var openGui = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(
                new net.minecraft.client.KeyMapping("key.orbitdlc.gui", com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT, net.minecraft.client.KeyMapping.Category.MISC));
        var guiHud = new GuiHud(openGui);
        modules.register(guiHud);
        guiHud.registerHud();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.gui.screen() != null
                    && client.gui.screen().getClass() == net.minecraft.client.gui.screens.TitleScreen.class) {
                client.gui.setScreen(new MainMenu(modules, openGui));
            }
            while (openGui.consumeClick()) {
                var screen = client.gui.screen();
                if (screen instanceof ClickGui gui) {
                    gui.onClose();
                } else if (ClickGuiShortcut.isBrokkeScreen(screen)) {
                    screen.onClose();
                } else if (screen == null || screen instanceof MainMenu) {
                    ClickGuiShortcut.open(client, modules, screen, openGui);
                }
            }
        });
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            var options = client.options;
            VisualModules.registerOptions(modules, options);
            modules.register(new com.orbitdlc.client.module.OptionModule<>("fullbright", com.orbitdlc.client.module.Category.VISUALS, "Maximum vanilla brightness", options.gamma(), 1.0));
            modules.register(new com.orbitdlc.client.module.OptionModule<>("no_vignette", com.orbitdlc.client.module.Category.VISUALS, "Remove dark screen edges", options.vignette(), false));
            modules.register(new com.orbitdlc.client.module.OptionModule<>("no_damage_tilt", com.orbitdlc.client.module.Category.COMBAT, "Keep the camera level when taking damage", options.damageTiltStrength(), 0.0));
            modules.register(new com.orbitdlc.client.module.OptionModule<>("auto_jump", com.orbitdlc.client.module.Category.MOVEMENT, "Enable vanilla obstacle jumping", options.autoJump(), true));
            modules.register(new com.orbitdlc.client.module.OptionModule<>("toggle_sprint", com.orbitdlc.client.module.Category.MOVEMENT, "Press sprint once to toggle", options.toggleSprint(), true));
            modules.register(new com.orbitdlc.client.module.OptionModule<>("toggle_sneak", com.orbitdlc.client.module.Category.MOVEMENT, "Press sneak once to toggle", options.toggleCrouch(), true));
            modules.register(new com.orbitdlc.client.module.OptionModule<>("steady_view", com.orbitdlc.client.module.Category.MOVEMENT, "Disable walking camera bob", options.bobView(), false));
            modules.register(new com.orbitdlc.client.module.OptionModule<>("subtitles", com.orbitdlc.client.module.Category.PLAYER, "Show nearby sound captions", options.showSubtitles(), true));
            modules.register(new com.orbitdlc.client.module.OptionModule<>("directional_audio", com.orbitdlc.client.module.Category.MISC, "Enable directional audio processing", options.directionalAudio(), true));
        });
        var particleModules = java.util.List.of(
                new MovementParticles("jump_particles", MovementParticles.Trigger.JUMP, ParticleTypes.END_ROD),
                new MovementParticles("landing_particles", MovementParticles.Trigger.LAND, ParticleTypes.CLOUD),
                new MovementParticles("particle_trails", MovementParticles.Trigger.TRAIL, ParticleTypes.WAX_ON)
        );
        particleModules.forEach(modules::register);
        ClientTickEvents.END_CLIENT_TICK.register(client -> particleModules.forEach(module -> module.tick(client)));

        DiscordPresence presence = new DiscordPresence();
        modules.register(presence);
        presence.setEnabled(true);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> presence.setEnabled(false));
    }
}
