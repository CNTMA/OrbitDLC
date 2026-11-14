package com.orbitdlc.client;

import com.google.gson.JsonObject;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import java.util.Arrays;
import java.util.EnumSet;

public final class NoRender extends Module {
    public enum Effect {
        FIRE("Fire"), BOSS_BAR("Boss bar"), SHAKE("Screen shake"), SCOREBOARD("Scoreboard"),
        BOBBING("View bobbing"), WATER("Water overlay"), VIGNETTE("Vignette"),
        PORTAL("Portal overlay"), TITLES("Titles"),
        POTION_PARTICLES("Potion FX −50%"), HIT_PARTICLES("Hit FX −50%"),
        EXPLOSION_PARTICLES("Explosion FX −50%");

        final String label;
        Effect(String label) { this.label = label; }
    }

    static final NoRender INSTANCE = new NoRender();
    private final EnumSet<Effect> hidden = EnumSet.range(Effect.FIRE, Effect.SCOREBOARD);

    private NoRender() { super("no_render", Category.VISUALS); }

    public static boolean hides(Effect effect) {
        return INSTANCE.isEnabled() && INSTANCE.hidden.contains(effect);
    }

    @Override
    public String description() { return "Hides selected screen effects and HUD overlays."; }

    @Override
    public JsonObject saveSettings() {
        var settings = super.saveSettings();
        for (var effect : Effect.values()) settings.addProperty(effect.name(), hidden.contains(effect));
        return settings;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        for (var effect : Effect.values()) {
            boolean enabled = settings.has(effect.name()) ? settings.get(effect.name()).getAsBoolean()
                    : effect.ordinal() <= Effect.SCOREBOARD.ordinal();
            if (enabled) hidden.add(effect);
            else hidden.remove(effect);
        }
    }

    ModuleSettings settings(ClickGui parent) {
        return new ModuleSettings("NoRender", parent) {
            @Override
            protected void init() {
                var effects = Effect.values();
                choices(Arrays.stream(effects).map(effect -> effect.label).toList(),
                        index -> hidden.contains(effects[index]), index -> {
                            if (!hidden.remove(effects[index])) hidden.add(effects[index]);
                        });
            }
        };
    }
}
