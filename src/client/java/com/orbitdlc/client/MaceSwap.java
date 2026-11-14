package com.orbitdlc.client;

import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

final class MaceSwap extends Module {
    private final KeyMapping shortcut;

    MaceSwap(KeyMapping shortcut) {
        super("mace_swap", Category.UTILITIES);
        this.shortcut = shortcut;
    }

    void tick(Minecraft client) {
        if (!shortcut.consumeClick()) return;
        while (shortcut.consumeClick()) {}
        if (!isEnabled() || !UtilityModules.canAct(client) || client.player.isUsingItem()) return;
        selectMace(client.player);
    }

    static void selectMace(net.minecraft.client.player.LocalPlayer player) {
        var inventory = player.getInventory();
        int current = inventory.getSelectedSlot();
        for (int offset = 0; offset < 9; offset++) {
            int slot = (current + offset) % 9;
            if (inventory.getItem(slot).is(Items.MACE)) {
                inventory.setSelectedSlot(slot);
                return;
            }
        }
    }

    @Override
    public String description() { return "Selects a mace from the hotbar on key press."; }

    ModuleSettings settings(ClickGui parent) {
        return new ModuleSettings("MaceSwap", parent) {
            @Override
            protected void init() { bind("Bind", 0, shortcut); }
        };
    }
}
