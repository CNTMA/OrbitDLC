package com.orbitdlc.client;

import com.orbitdlc.OrbitDLC;
import com.orbitdlc.client.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

final class ClickGuiShortcut {
    private static final String BROKKE_SCREEN = "brokke.cc.ui.clickgui.NewClickGuiScreen";

    private ClickGuiShortcut() {}

    static void open(Minecraft client, ModuleManager modules, Screen parent, KeyMapping shortcut) {
        if (FabricLoader.getInstance().isModLoaded("brokke")) {
            try {
                Class<? extends Screen> screen = Class.forName(BROKKE_SCREEN).asSubclass(Screen.class);
                client.gui.setScreen(screen.getConstructor(Screen.class).newInstance(parent));
                return;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
                OrbitDLC.LOGGER.warn("Could not open the Brokke ClickGUI", error);
            }
        }
        client.gui.setScreen(new ClickGui(modules, parent, shortcut));
    }

    static boolean isBrokkeScreen(Screen screen) {
        return screen != null && screen.getClass().getName().equals(BROKKE_SCREEN);
    }
}
