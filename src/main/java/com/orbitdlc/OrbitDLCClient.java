package com.orbitdlc;

import com.mojang.blaze3d.platform.InputConstants;
import com.orbitdlc.ui.ClickGuiScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class OrbitDLCClient implements ClientModInitializer {
    public static KeyMapping openClickGui;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("orbitdlc", "main")
        );

        openClickGui = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.orbitdlc.clickgui",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_RSHIFT, // 26.3: KEY_RSHIFT вместо KEY_RIGHT_SHIFT
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openClickGui.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) {
                    client.gui.setScreen(new ClickGuiScreen(null));
                }
            }
        });
    }
}