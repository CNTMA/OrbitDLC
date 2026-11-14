package com.orbitdlc.client;

import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.minecraft.client.gui.screens.Screen;

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
            private int weatherHeading;

            @Override
            protected void init() {
                var times = Time.values();
                int offset = choicesAt(java.util.List.of("Server", "Dawn", "Day", "Sunset", "Night"),
                        index -> time == times[index], index -> time = times[index], 0);
                weatherHeading = offset + 7;
                var weathers = Weather.values();
                offset = choicesAt(java.util.List.of("Server", "Clear", "Rain", "Storm"),
                        index -> weather == weathers[index], index -> weather = weathers[index], weatherHeading + 13);
                toggleAt("Wet world", offset + 8, () -> wet, () -> wet = !wet);
            }

            @Override protected String summary() { return "Time"; }

            @Override void drawHeading(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
                super.drawHeading(graphics);
                graphics.text(font, net.minecraft.network.chat.Component.literal("Weather").setStyle(OrbitButton.TYPE),
                        contentX(), contentY() + weatherHeading, 0xFFD0C7D5, false);
            }
        };
    }
}
