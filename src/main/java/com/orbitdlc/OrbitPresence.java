package com.orbitdlc;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.UUID;

public record OrbitPresence(List<UUID> players) implements CustomPacketPayload {
    public static final Type<OrbitPresence> TYPE = new Type<>(OrbitDLC.id("presence"));
    private static final StreamCodec<RegistryFriendlyByteBuf, OrbitPresence> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list(2048)), OrbitPresence::players, OrbitPresence::new);

    @Override
    public Type<OrbitPresence> type() { return TYPE; }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 40 != 0) return;
            var supported = server.getPlayerList().getPlayers().stream()
                    .filter(player -> ServerPlayNetworking.canSend(player, TYPE)).limit(2048).toList();
            if (supported.isEmpty()) return;
            var payload = new OrbitPresence(supported.stream().map(player -> player.getUUID()).toList());
            supported.forEach(player -> ServerPlayNetworking.send(player, payload));
        });
    }
}

