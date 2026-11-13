package com.orbitdlc.client.module;

import com.orbitdlc.discord.DiscordRPC;

public final class DiscordPresence extends Module {
    public DiscordPresence() {
        super("discord_rpc", Category.MISC);
    }

    @Override
    public String description() {
        return "Shows your OrbitDLC session in Discord Rich Presence.";
    }

    @Override
    protected void onEnable() {
        DiscordRPC.init();
    }

    @Override
    protected void onDisable() {
        DiscordRPC.stop();
    }
}
