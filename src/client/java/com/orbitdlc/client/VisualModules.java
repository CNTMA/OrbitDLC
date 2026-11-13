package com.orbitdlc.client;

import com.orbitdlc.OrbitDLC;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import com.orbitdlc.client.module.ModuleManager;
import com.orbitdlc.client.module.OptionModule;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Options;

final class VisualModules {
    private VisualModules() {}

    static void registerShader(ModuleManager modules) {
        Module saturation = new Module("world_saturation", Category.VISUALS) {
            @Override
            public String description() {
                return "Keeps vivid blue tones and desaturates other colors in the world.";
            }
        };
        modules.register(saturation);
        HudElementRegistry.addFirst(OrbitDLC.id("world_saturation"), (graphics, delta) -> {
            if (saturation.isEnabled()) {
                GlassBackdrop.saturation(graphics);
                graphics.nextStratum();
            }
        });
    }

    static void registerOptions(ModuleManager modules, Options options) {
        modules.register(new OptionModule<>("no_clouds", Category.VISUALS,
                "Hide vanilla clouds without affecting ShaderSky.", options.cloudStatus(), CloudStatus.OFF));
        modules.register(new OptionModule<>("no_entity_shadows", Category.VISUALS,
                "Hide the circular ground shadows cast by entities.", options.entityShadows(), false));
        modules.register(new OptionModule<>("no_lightning_flash", Category.VISUALS,
                "Suppress the fullscreen lightning flash.", options.hideLightningFlash(), true));
        modules.register(new OptionModule<>("no_enchant_glint", Category.VISUALS,
                "Hide the enchantment shimmer.", options.glintStrength(), 0.0));
        modules.register(new OptionModule<>("steady_fov", Category.VISUALS,
                "Keep field of view stable during sprinting and other gameplay effects.", options.fovEffectScale(), 0.0));
        modules.register(new OptionModule<>("reduce_darkness", Category.VISUALS,
                "Suppress the intensity of the pulsing Darkness effect.", options.darknessEffectScale(), 0.0));
        modules.register(new OptionModule<>("reduce_screen_distortion", Category.VISUALS,
                "Reduce the screen distortion from nausea and Nether portals.", options.screenEffectScale(), 0.0));
    }
}
