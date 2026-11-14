package com.orbitdlc.client;

import com.orbitdlc.client.module.ModuleManager;
import com.orbitdlc.client.module.MovementParticles;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import com.orbitdlc.client.discord.DiscordRPC;

public final class OrbitDLCClient implements ClientModInitializer {
    private final ModuleManager modules = new ModuleManager();

    @Override
    public void onInitializeClient() {
        DiscordRPC.init();
        GlassBackdrop.init();
        modules.register(JumpCircles.INSTANCE);
        ClientTickEvents.END_CLIENT_TICK.register(JumpCircles.INSTANCE::tick);
        VisualModules.registerShader(modules);
        ExtraVisuals.register(modules);
        UtilityModules.register(modules);
        modules.register(Ambience.INSTANCE);
        ShaderSky.init();
        modules.register(ShaderSky.INSTANCE);
        modules.register(ShaderHand.INSTANCE);
        modules.register(ShaderCrystal.INSTANCE);
        modules.register(BlockOverlay.INSTANCE);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> BlockOverlay.INSTANCE.close());
        modules.register(ChinaHat.INSTANCE);
        modules.register(Skin.INSTANCE);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ShaderCrystal.INSTANCE.close());
        modules.register(Beautiful.INSTANCE);
        modules.register(Bloom.INSTANCE);
        modules.register(BadTrip.INSTANCE);
        BadTrip.registerHud();
        ClientTickEvents.END_CLIENT_TICK.register(BadTrip.INSTANCE::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> BadTrip.INSTANCE.stop());
        modules.register(MotionBlur.INSTANCE);
        modules.register(Trail.INSTANCE);
        ClientTickEvents.END_CLIENT_TICK.register(Trail.INSTANCE::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Trail.INSTANCE.close());
        modules.register(Animations.INSTANCE);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Bloom.INSTANCE.close());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> MotionBlur.INSTANCE.close());
        modules.register(CustomFog.INSTANCE);
        modules.register(NoRender.INSTANCE);
        modules.register(Viewmodel.INSTANCE);
        modules.register(SwingAnimations.INSTANCE);
        modules.register(Optimization.INSTANCE);
        modules.register(AspectRatio.INSTANCE);
        modules.register(Theme.INSTANCE);
        ClientTickEvents.END_CLIENT_TICK.register(Optimization.INSTANCE::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> CustomFog.INSTANCE.close());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Beautiful.INSTANCE.close());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ShaderHand.INSTANCE.close());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ShaderSky.INSTANCE.close());
        var openGui = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(
                new net.minecraft.client.KeyMapping("key.orbitdlc.gui", com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT, net.minecraft.client.KeyMapping.Category.MISC));
        var guiHud = new GuiHud(openGui);
        modules.register(guiHud);
        guiHud.registerHud();
        HudDrag.register(guiHud);
        OrbitBadges.register();
        OrbitFriends.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.gui.screen() != null
                    && client.gui.screen().getClass() == net.minecraft.client.gui.screens.TitleScreen.class) {
                client.gui.setScreen(new MainMenu(modules, openGui));
            }
            while (openGui.consumeClick()) {
                var screen = client.gui.screen();
                if (screen instanceof ClickGui gui) {
                    gui.onClose();
                } else if (screen == null || screen instanceof MainMenu) {
                    ClickGuiShortcut.open(client, modules, screen, openGui);
                }
            }
        });
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            LocalAccounts.load();
            var options = client.options;
            VisualModules.registerOptions(modules, options);
            modules.register(new com.orbitdlc.client.module.OptionModule<>("fullbright", com.orbitdlc.client.module.Category.VISUALS, "Maximum vanilla brightness", options.gamma(), 1.0));
            modules.register(new com.orbitdlc.client.module.OptionModule<>("no_vignette", com.orbitdlc.client.module.Category.VISUALS, "Remove dark screen edges", options.vignette(), false));
            modules.register(new com.orbitdlc.client.module.OptionModule<>("no_damage_tilt", com.orbitdlc.client.module.Category.VISUALS, "Keep the camera level when taking damage", options.damageTiltStrength(), 0.0));
            modules.register(new com.orbitdlc.client.module.OptionModule<>("steady_view", com.orbitdlc.client.module.Category.VISUALS, "Disable walking camera bob", options.bobView(), false));
            OrbitProfiles.loadSession(modules);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> OrbitProfiles.saveSession(modules));
        var particleModules = java.util.List.of(
                new MovementParticles("jump_particles", MovementParticles.Trigger.JUMP, ParticleTypes.END_ROD),
                new MovementParticles("landing_particles", MovementParticles.Trigger.LAND, ParticleTypes.CLOUD),
                new MovementParticles("particle_trails", MovementParticles.Trigger.TRAIL, ParticleTypes.WAX_ON)
        );
        particleModules.forEach(modules::register);
        ClientTickEvents.END_CLIENT_TICK.register(client -> particleModules.forEach(module -> module.tick(client)));

    }
}
