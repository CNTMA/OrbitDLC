package com.orbitdlc.client;

import com.orbitdlc.OrbitDLC;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class Ambience extends Module {
    static final Ambience INSTANCE = new Ambience();

    private enum Time {
        SERVER(null), DAWN(23000L), DAY(6000L), SUNSET(12000L), NIGHT(18000L);
        final Long tick;
        Time(Long tick) { this.tick = tick; }
    }

    private enum Weather { SERVER, CLEAR, RAIN, STORM }

    private Time time = Time.SERVER;
    private Weather weather = Weather.SERVER;
    private boolean wet;

    private Ambience() { super("ambience", Category.VISUALS); }

    @Override
    public String description() { return "Client-side time, weather and wet-world color grading. Right-click to configure."; }

    public static Long clock() { return INSTANCE.isEnabled() ? INSTANCE.time.tick : null; }

    public static Float weather(boolean thunder) {
        if (!INSTANCE.isEnabled() || INSTANCE.weather == Weather.SERVER) return null;
        return switch (INSTANCE.weather) {
            case STORM -> 1f;
            case RAIN -> thunder ? 0f : 1f;
            default -> 0f;
        };
    }

    void registerHud() {
        HudElementRegistry.addFirst(OrbitDLC.id("wet_world"), (graphics, delta) -> {
            if (isEnabled() && wet) {
                GlassBackdrop.wetWorld(graphics);
                graphics.nextStratum();
            }
        });
    }

    Screen settings(Screen parent) {
        return new Screen(Component.literal("Ambience")) {
            @Override
            protected void init() {
                int x = (width - 172) / 2;
                int y = height / 2 - 54;
                addRenderableWidget(new OrbitButton(label("Time", time), x, y, 172, 22, button -> {
                    time = Time.values()[(time.ordinal() + 1) % Time.values().length];
                    button.setMessage(label("Time", time));
                }));
                addRenderableWidget(new OrbitButton(label("Wet world", wet ? "ON" : "OFF"), x, y + 27, 172, 22, button -> {
                    wet = !wet;
                    button.setMessage(label("Wet world", wet ? "ON" : "OFF"));
                }));
                addRenderableWidget(new OrbitButton(label("Weather", weather), x, y + 54, 172, 22, button -> {
                    weather = Weather.values()[(weather.ordinal() + 1) % Weather.values().length];
                    button.setMessage(label("Weather", weather));
                }));
                addRenderableWidget(new OrbitButton(Component.literal("Back"), x, y + 81, 172, 22, button -> onClose()));
            }

            @Override
            public void onClose() { minecraft.gui.setScreen(parent); }

            @Override
            public boolean isPauseScreen() { return false; }
        };
    }

    private static Component label(String name, Object value) {
        return Component.literal(name + " · " + value);
    }
}
