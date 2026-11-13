/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants$Type
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
 *  net.minecraft.client.KeyMapping
 *  net.minecraft.client.KeyMapping$Category
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.input.KeyEvent
 *  net.minecraft.resources.Identifier
 */
package brokke.cc.ui.clickgui;

import brokke.cc.core.module.Category;
import brokke.cc.ui.clickgui.NewClickGuiScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;

public final class ClickGui {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register((Identifier)Identifier.fromNamespaceAndPath((String)"brokke", (String)"clickgui"));
    private static final KeyMapping OPEN_GUI = KeyMappingHelper.registerKeyMapping((KeyMapping)new KeyMapping("key.brokke.clickgui", InputConstants.Type.KEYBOARD, 229, CATEGORY));

    private ClickGui() {
    }

    public static boolean matchesOpenKey(KeyEvent event) {
        return event.key() == 229 || OPEN_GUI.matches(event);
    }

    public static void openAutoBuy() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null) {
            mc.gui.setScreen((Screen)new NewClickGuiScreen(mc.gui.screen(), Category.AUTOBUY));
        }
    }

    public static void openAutoSbor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui == null) {
            return;
        }
        Screen screen = mc.gui.screen();
        if (screen instanceof NewClickGuiScreen) {
            NewClickGuiScreen screen2 = (NewClickGuiScreen)screen;
            screen2.openAutoSbor();
            return;
        }
        NewClickGuiScreen screen3 = new NewClickGuiScreen(mc.gui.screen());
        screen3.openAutoSbor();
        mc.gui.setScreen((Screen)screen3);
    }

    public static void openMarket() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null) {
            mc.gui.setScreen((Screen)new NewClickGuiScreen(mc.gui.screen(), Category.MARKET));
        }
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            while (OPEN_GUI.consumeClick()) {
                if (minecraft.gui == null || minecraft.gui.screen() instanceof NewClickGuiScreen) continue;
                minecraft.gui.setScreen((Screen)new NewClickGuiScreen(minecraft.gui.screen()));
            }
        });
    }
}

