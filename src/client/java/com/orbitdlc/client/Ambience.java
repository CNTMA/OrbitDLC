package com.orbitdlc.client;

import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
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
    public String description() { return "Client-side time, weather and wet-world reflections. Right-click to configure."; }

    public static Long clock() { return INSTANCE.isEnabled() ? INSTANCE.time.tick : null; }

    public static Float weather(boolean thunder) {
        if (!INSTANCE.isEnabled() || INSTANCE.weather == Weather.SERVER) return null;
        return switch (INSTANCE.weather) {
            case STORM -> 1f;
            case RAIN -> thunder ? 0f : 1f;
            default -> 0f;
        };
    }

    static boolean wetWorldEnabled() {
        return INSTANCE.isEnabled() && INSTANCE.wet;
    }

    @Override
    public com.google.gson.JsonObject saveSettings() {
        var settings = super.saveSettings();
        settings.addProperty("time", time.name());
        settings.addProperty("weather", weather.name());
        settings.addProperty("wet", wet);
        return settings;
    }

    @Override
    public void loadSettings(com.google.gson.JsonObject settings) {
        time = Time.valueOf(settings.get("time").getAsString());
        weather = Weather.valueOf(settings.get("weather").getAsString());
        wet = settings.get("wet").getAsBoolean();
    }

    ModuleSettings settings(Screen parent) {
        return new ModuleSettings("Ambience", parent) {
            @Override
            protected void init() {
                int x = contentX();
                int y = contentY();
                settingWidget(new OrbitButton(label("Time", time), x, y, contentWidth(), 22, button -> {
                    time = Time.values()[(time.ordinal() + 1) % Time.values().length];
                    button.setMessage(label("Time", time));
                }));
                settingWidget(new OrbitButton(label("Wet world", wet ? "ON" : "OFF"), x, y + 27, contentWidth(), 22, button -> {
                    wet = !wet;
                    button.setMessage(label("Wet world", wet ? "ON" : "OFF"));
                }));
                settingWidget(new OrbitButton(label("Weather", weather), x, y + 54, contentWidth(), 22, button -> {
                    weather = Weather.values()[(weather.ordinal() + 1) % Weather.values().length];
                    button.setMessage(label("Weather", weather));
                }));
            }

        };
    }

    private static Component label(String name, Object value) {
        return Component.literal(name + " · " + value);
    }
}
