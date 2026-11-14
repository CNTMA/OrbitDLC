package com.orbitdlc.client;

import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;

final class Theme extends Module {
    static final Theme INSTANCE = new Theme();

    private Theme() {
        super("theme", Category.VISUALS);
    }

    @Override
    public String description() {
        return "Use the active theme color for visual effects and HUD accents.";
    }

    static int color(int fallback) {
        return INSTANCE.isEnabled() ? OrbitTheme.accent() : fallback;
    }
}
