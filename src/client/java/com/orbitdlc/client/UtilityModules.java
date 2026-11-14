package com.orbitdlc.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import com.orbitdlc.client.module.ModuleManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

final class UtilityModules {
    private UtilityModules() {}

    static void register(ModuleManager modules) {
        var pearl = new QuickUse("click_pearl", Items.ENDER_PEARL,
                mapping("click_pearl", InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_MIDDLE));
        var rocket = new QuickUse("quick_rocket", Items.FIREWORK_ROCKET,
                mapping("quick_rocket", InputConstants.UNKNOWN.getType(), InputConstants.UNKNOWN.getValue()));
        var swap = new MaceSwap(unbound("mace_swap"));
        var elytraSwap = new ElytraSwap(unbound("elytra_swap"));
        modules.register(elytraSwap);
        modules.register(pearl);
        modules.register(swap);
        modules.register(MaceHelper.INSTANCE);
        modules.register(rocket);
        UtilityHud.register(modules);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            elytraSwap.tick(client);
            pearl.tick(client);
            swap.tick(client);
            MaceHelper.INSTANCE.tick(client);
            rocket.tick(client);
        });
    }

    private static KeyMapping mapping(String name, InputConstants.Type type, int key) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.orbitdlc." + name, type, key,
                KeyMapping.Category.MISC));
    }

    private static KeyMapping unbound(String name) {
        return mapping(name, InputConstants.UNKNOWN.getType(), InputConstants.UNKNOWN.getValue());
    }

    static boolean canAct(Minecraft client) {
        return client.gui.screen() == null && client.gameMode != null && client.player != null
                && client.player.isAlive() && !client.player.isSpectator()
                && client.player.containerMenu == client.player.inventoryMenu;
    }

    static final class QuickUse extends Module {
        private final Item item;
        private final KeyMapping shortcut;

        QuickUse(String id, Item item, KeyMapping shortcut) {
            super(id, Category.UTILITIES);
            this.item = item;
            this.shortcut = shortcut;
        }

        void tick(Minecraft client) {
            if (!shortcut.consumeClick()) return;
            while (shortcut.consumeClick()) {}
            if (!isEnabled() || !canAct(client)) return;
            var player = client.player;
            if (player.getOffhandItem().is(item)) {
                client.gameMode.useItem(player, InteractionHand.OFF_HAND);
                return;
            }
            var inventory = player.getInventory();
            int selected = inventory.getSelectedSlot();
            for (int offset = 0; offset < 9; offset++) {
                int slot = (selected + offset) % 9;
                if (!inventory.getItem(slot).is(item)) continue;
                inventory.setSelectedSlot(slot);
                try {
                    client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
                } finally {
                    inventory.setSelectedSlot(selected);
                }
                return;
            }
        }

        @Override
        public String description() { return "Uses the item from your hotbar or offhand on key press."; }

        ModuleSettings settings(ClickGui parent) {
            return new ModuleSettings(item == Items.ENDER_PEARL ? "ClickPearl" : "QuickRocket", parent) {
                @Override
                protected void init() {
                    bind("Bind", 0, shortcut);
                }
            };
        }
    }
}
