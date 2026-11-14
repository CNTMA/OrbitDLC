package com.orbitdlc.client;

import com.orbitdlc.OrbitDLC;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.TreeSet;

final class OrbitFriends {
    private static final java.nio.file.Path FILE = OrbitProfiles.ROOT.resolve("friends.txt");
    private static final TreeSet<String> NAMES = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    private OrbitFriends() {}

    static void register() {
        try {
            if (Files.isRegularFile(FILE)) {
                for (String line : Files.readAllLines(FILE)) {
                    if (LocalAccounts.valid(line)) NAMES.add(line);
                }
            }
        } catch (IOException error) {
            OrbitDLC.LOGGER.warn("Could not read friends list", error);
        }
        AttackEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
            if (level.isClientSide() && player == Minecraft.getInstance().player
                    && target instanceof Player friend && NAMES.contains(friend.getGameProfile().name())) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

    static List<String> names() { return List.copyOf(NAMES); }

    static void add(String name) throws IOException {
        if (!LocalAccounts.valid(name)) throw new IllegalArgumentException("Enter a Minecraft username");
        var updated = new TreeSet<>(NAMES);
        if (!updated.add(name)) throw new IllegalArgumentException("Already on your friends list");
        save(updated);
    }

    static void remove(String name) throws IOException {
        var updated = new TreeSet<>(NAMES);
        updated.remove(name);
        save(updated);
    }

    private static void save(TreeSet<String> updated) throws IOException {
        Files.createDirectories(FILE.getParent());
        var temporary = Files.createTempFile(FILE.getParent(), "friends-", ".tmp");
        try {
            Files.write(temporary, updated);
            Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
            NAMES.clear();
            NAMES.addAll(updated);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
