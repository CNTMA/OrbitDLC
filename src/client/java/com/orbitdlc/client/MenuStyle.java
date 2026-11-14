package com.orbitdlc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MenuStyle {
    private MenuStyle() {}

    public static boolean applies(Screen screen) {
        if (screen == null) return false;
        String name = screen.getClass().getName();
        return name.startsWith("net.minecraft.client.gui.screens.")
                && !name.startsWith("net.minecraft.client.gui.screens.inventory.")
                && !(screen instanceof net.minecraft.client.gui.screens.ChatScreen);
    }

    public static boolean active() { return applies(Minecraft.getInstance().gui.screen()); }

    public static Component text(Component text) {
        return text.copy().withStyle(style -> style.withFont(OrbitButton.TYPE.getFont()));
    }
}
