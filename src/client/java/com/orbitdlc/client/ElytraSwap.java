package com.orbitdlc.client;

import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

final class ElytraSwap extends Module {
    private final KeyMapping shortcut;
    private InventoryScreen opened;
    private InventoryMenu menu;
    private boolean swapped;

    ElytraSwap(KeyMapping shortcut) {
        super("elytra_swap", Category.UTILITIES);
        this.shortcut = shortcut;
    }

    void tick(Minecraft client) {
        boolean pressed = shortcut.consumeClick();
        while (shortcut.consumeClick()) {}
        if (opened != null) {
            if (client.gui.screen() != opened || client.player == null || client.player.inventoryMenu != menu) {
                opened = null;
                menu = null;
                return;
            }
            if (swapped || !isEnabled() || !client.player.isAlive() || client.player.isSpectator()
                    || client.player.containerMenu != menu || client.gameMode == null) {
                if (menu.getCarried().isEmpty()) opened.onClose();
                opened = null;
                menu = null;
                return;
            }
            swap(client, menu);
            swapped = true;
            return;
        }

        if (!pressed || !isEnabled() || !UtilityModules.canAct(client) || client.player.isCreative()) return;
        var inventory = client.player.inventoryMenu;
        if (findReplacement(client, inventory) == null) return;
        menu = inventory;
        swapped = false;
        opened = new InventoryScreen(client.player);
        client.gui.setScreen(opened);
    }

    private static Slot findReplacement(Minecraft client, InventoryMenu menu) {
        if (!menu.getCarried().isEmpty()) return null;
        var player = client.player;
        var chest = menu.getSlot(InventoryMenu.ARMOR_SLOT_START + 1);
        var worn = chest.getItem();
        boolean wearingElytra = worn.is(Items.ELYTRA);
        if (worn.getCount() > 1 || !chest.mayPickup(player)) return null;
        for (int i = InventoryMenu.INV_SLOT_START; i < InventoryMenu.USE_ROW_SLOT_END; i++) {
            var slot = menu.getSlot(i);
            var item = slot.getItem();
            if (item.getCount() != 1 || !slot.mayPickup(player) || !chest.mayPlace(item)
                    || (!worn.isEmpty() && !slot.mayPlace(worn))) continue;
            var equipment = item.get(DataComponents.EQUIPPABLE);
            if (wearingElytra ? !item.is(Items.ELYTRA) && equipment != null
                    && equipment.slot() == EquipmentSlot.CHEST : item.is(Items.ELYTRA)) return slot;
        }
        return null;
    }

    private static void swap(Minecraft client, InventoryMenu menu) {
        var source = findReplacement(client, menu);
        if (source == null) return;
        int chest = InventoryMenu.ARMOR_SLOT_START + 1;
        click(client, menu, source.index);
        if (menu.getCarried().isEmpty()) return;
        click(client, menu, chest);
        if (!menu.getCarried().isEmpty() && !source.hasItem()) click(client, menu, source.index);
    }

    private static void click(Minecraft client, InventoryMenu menu, int slot) {
        client.gameMode.handleContainerInput(menu.containerId, slot, 0, ContainerInput.PICKUP, client.player);
    }

    @Override public String description() { return "Swap elytra and chest armor through your inventory."; }

    ModuleSettings settings(ClickGui parent) {
        return new ModuleSettings("Elytra Swap", parent) {
            @Override protected void init() { bind("Bind", 0, shortcut); }
        };
    }
}
