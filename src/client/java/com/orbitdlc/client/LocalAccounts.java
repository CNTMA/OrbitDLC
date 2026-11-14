package com.orbitdlc.client;

import net.minecraft.client.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;

public final class LocalAccounts {
    private static User active;

    private LocalAccounts() {}

    public static User active() { return active; }

    static void load() {
        var path = OrbitProfiles.ROOT.resolve("local-account.txt");
        try {
            if (Files.isRegularFile(path) && Files.size(path) <= 64) {
                String name = Files.readString(path).strip();
                if (valid(name)) active = create(name);
            }
        } catch (IOException error) {
            com.orbitdlc.OrbitDLC.LOGGER.warn("Could not read local account", error);
        }
    }

    static boolean valid(String name) { return name.matches("[A-Za-z0-9_]{3,16}"); }

    static void select(String name) throws IOException {
        var path = OrbitProfiles.ROOT.resolve("local-account.txt");
        if (name == null) {
            Files.deleteIfExists(path);
            active = null;
            return;
        }
        if (!valid(name)) throw new IllegalArgumentException("Invalid Minecraft username");
        Files.createDirectories(path.getParent());
        Files.writeString(path, name);
        active = create(name);
    }

    private static User create(String name) {
        var id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        return new User(name, id, "", Optional.empty(), Optional.empty());
    }
}
