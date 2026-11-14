package com.orbitdlc.client;

import com.google.gson.JsonObject;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class MaceHelper extends Module {
    static final MaceHelper INSTANCE = new MaceHelper();
    private int height = 3;
    private LocalPlayer armedPlayer;
    private double launchY;
    private int expiresAt;
    private int originalSlot;

    private MaceHelper() { super("mace_helper", Category.UTILITIES); }

    public static void windChargeUsed(LocalPlayer player) {
        var helper = INSTANCE;
        if (!helper.isEnabled()) return;
        helper.armedPlayer = player;
        helper.launchY = player.getY();
        helper.expiresAt = player.tickCount + 60;
        helper.originalSlot = player.getInventory().getSelectedSlot();
    }

    void tick(Minecraft client) {
        if (armedPlayer == null) return;
        var player = client.player;
        if (player != armedPlayer || !UtilityModules.canAct(client) || player.tickCount >= expiresAt
                || player.getInventory().getSelectedSlot() != originalSlot) {
            armedPlayer = null;
            return;
        }
        if (!player.isUsingItem() && !player.onGround() && player.getY() - launchY >= height) {
            MaceSwap.selectMace(player);
            armedPlayer = null;
        }
    }

    @Override
    protected void onDisable() { armedPlayer = null; }

    @Override
    public String description() { return "Selects a hotbar mace after climbing the configured height from a wind charge launch."; }

    @Override
    public JsonObject saveSettings() {
        var settings = super.saveSettings();
        settings.addProperty("height", height);
        return settings;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        height = settings.has("height") ? Math.clamp(settings.get("height").getAsInt(), 1, 12) : 3;
        armedPlayer = null;
    }

    ModuleSettings settings(ClickGui parent) {
        return new ModuleSettings("MaceHelper", parent) {
            @Override
            protected void init() {
                slider("Height", " m", 0, 1, 12, () -> height, value -> height = value);
            }
        };
    }
}
