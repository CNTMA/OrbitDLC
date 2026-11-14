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
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public record OrbitPresence(List<UUID> players) implements CustomPacketPayload {
    public static final Type<OrbitPresence> TYPE = new Type<>(OrbitDLC.id("presence"));
    private static final StreamCodec<RegistryFriendlyByteBuf, OrbitPresence> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list(2048)), OrbitPresence::players, OrbitPresence::new);

    @Override
    public Type<OrbitPresence> type() { return TYPE; }

    private static final StreamCodec<RegistryFriendlyByteBuf, List<String>> ITEMS =
            ByteBufCodecs.stringUtf8(96).apply(ByteBufCodecs.list(5)).cast();
    private static final Map<ServerPlayer, List<String>> EQUIPMENT = new WeakHashMap<>();

    public record Wear(List<String> items) implements CustomPacketPayload {
        public static final Type<Wear> TYPE = new Type<>(OrbitDLC.id("wear"));
        private static final StreamCodec<RegistryFriendlyByteBuf, Wear> CODEC =
                StreamCodec.composite(ITEMS, Wear::items, Wear::new);
        @Override public Type<Wear> type() { return TYPE; }
    }

    public record Outfit(UUID player, List<String> items) {
        private static final StreamCodec<RegistryFriendlyByteBuf, Outfit> CODEC =
                StreamCodec.composite(UUIDUtil.STREAM_CODEC, Outfit::player, ITEMS, Outfit::items, Outfit::new);
    }

    public record Outfits(List<Outfit> players) implements CustomPacketPayload {
        public static final Type<Outfits> TYPE = new Type<>(OrbitDLC.id("outfits"));
        private static final StreamCodec<RegistryFriendlyByteBuf, Outfits> CODEC =
                StreamCodec.composite(Outfit.CODEC.apply(ByteBufCodecs.list(2048)), Outfits::players, Outfits::new);
        @Override public Type<Outfits> type() { return TYPE; }
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(Wear.TYPE, Wear.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Outfits.TYPE, Outfits.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Wear.TYPE, (payload, context) ->
                context.server().execute(() -> EQUIPMENT.put(context.player(), List.copyOf(payload.items()))));
        PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 40 != 0) return;
            var supported = server.getPlayerList().getPlayers().stream()
                    .filter(player -> ServerPlayNetworking.canSend(player, TYPE)).limit(2048).toList();
            if (supported.isEmpty()) return;
            var payload = new OrbitPresence(supported.stream().map(player -> player.getUUID()).toList());
            var outfits = new Outfits(supported.stream().filter(EQUIPMENT::containsKey)
                    .map(player -> new Outfit(player.getUUID(), EQUIPMENT.get(player))).toList());
            for (var player : supported) {
                ServerPlayNetworking.send(player, payload);
                if (ServerPlayNetworking.canSend(player, Outfits.TYPE)) ServerPlayNetworking.send(player, outfits);
            }
        });
    }
}

