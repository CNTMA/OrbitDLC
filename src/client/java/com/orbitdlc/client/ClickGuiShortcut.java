package com.orbitdlc.client;

import com.orbitdlc.client.module.ModuleManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

final class ClickGuiShortcut {
    private ClickGuiShortcut() {}

    static void open(Minecraft client, ModuleManager modules, Screen parent, KeyMapping shortcut) {
        client.gui.setScreen(new ClickGui(modules, parent, shortcut));
        GuiAudio.open();
    }
}
