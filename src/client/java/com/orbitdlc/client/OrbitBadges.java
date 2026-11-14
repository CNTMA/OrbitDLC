package com.orbitdlc.client;

import com.orbitdlc.OrbitDLC;
import com.orbitdlc.OrbitPresence;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class OrbitBadges {
    private static final Style LOGO = Style.EMPTY.withFont(new FontDescription.Resource(OrbitDLC.id("badge")));
    private static Set<UUID> confirmed = Set.of();
    private static Map<UUID, List<Cosmetics.Entry>> outfits = Map.of();
    private static List<String> sent;

    private OrbitBadges() {}

    public static Component decorate(UUID player, Component name) {
        if (!confirmed.contains(player)) return name;
        return Component.literal("\uE000").setStyle(LOGO).append(Component.literal(" ")).append(name.copy());
    }

    static List<Cosmetics.Entry> outfit(UUID player) {
        return outfits.getOrDefault(player, List.of());
    }

    static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OrbitPresence.TYPE,
                (payload, context) -> confirmed = Set.copyOf(payload.players()));
        ClientPlayNetworking.registerGlobalReceiver(OrbitPresence.Outfits.TYPE, (payload, context) -> {
            var received = new HashMap<UUID, List<Cosmetics.Entry>>();
            for (var outfit : payload.players()) {
                var items = Cosmetics.resolve(outfit.items());
                if (!items.isEmpty()) received.put(outfit.player(), items);
            }
            outfits = Map.copyOf(received);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            confirmed = Set.of();
            outfits = Map.of();
            sent = null;
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.player.tickCount % 20 != 0
                    || !ClientPlayNetworking.canSend(OrbitPresence.Wear.TYPE)) return;
            var items = Cosmetics.equipped().stream().map(Cosmetics.Entry::id).toList();
            if (items.equals(sent)) return;
            ClientPlayNetworking.send(new OrbitPresence.Wear(items));
            sent = items;
        });
    }
}
