package com.orbitdlc.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.orbitdlc.client.module.ModuleManager;
import com.orbitdlc.client.module.PlannedModule;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

final class OrbitProfiles {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    static final Path ROOT = FabricLoader.getInstance().getConfigDir().resolve("orbitdlc");
    static final Path PROFILES = ROOT.resolve("profiles");
    private static final Path SESSION = ROOT.resolve("session.json");

    static List<String> list(Path folder, String extension) throws IOException {
        Files.createDirectories(folder);
        try (var files = Files.list(folder)) {
            return files.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(extension))
                    .sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
    }

    static Path profile(String name) {
        if (!name.matches("[a-zA-Z0-9][a-zA-Z0-9 _-]{0,31}") || !name.equals(name.strip())) {
            throw new IllegalArgumentException("Use 1–32 letters, digits, spaces, _ or -");
        }
        return PROFILES.resolve(name + ".json");
    }

    static void save(Path path, ModuleManager modules, boolean overwrite) throws IOException {
        var root = new JsonObject();
        root.addProperty("theme", OrbitTheme.current().name());
        root.addProperty("customAccent", OrbitTheme.customAccent());
        root.addProperty("customTint", OrbitTheme.customTint());
        root.addProperty("themeFlow", OrbitTheme.flowing());
        root.addProperty("themeSurface", OrbitTheme.surface());
        root.addProperty("themeForeground", OrbitTheme.foreground());
        root.addProperty("themeOutline", OrbitTheme.outline());
        var states = new JsonObject();
        for (var module : modules.modules()) {
            if (module instanceof PlannedModule) continue;
            var state = module.saveSettings();
            state.addProperty("enabled", module.isEnabled());
            states.add(module.id(), state);
        }
        root.add("modules", states);
        root.add("hudPositions", HudDrag.savePositions());
        Files.createDirectories(path.getParent());
        var temporary = Files.createTempFile(path.getParent(), "orbit-", ".tmp");
        try {
            Files.writeString(temporary, JSON.toJson(root));
            if (overwrite) Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            else Files.move(temporary, path);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static void load(Path path, ModuleManager modules) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > 262144) {
            throw new IOException("Invalid profile file");
        }
        var root = JSON.fromJson(Files.readString(path), JsonObject.class);
        var theme = OrbitTheme.valueOf(root.get("theme").getAsString());
        int accent = root.has("customAccent") ? root.get("customAccent").getAsInt() : OrbitTheme.customAccent();
        int tint = root.has("customTint") ? root.get("customTint").getAsInt() : OrbitTheme.customTint();
        boolean flow = root.has("themeFlow") && root.get("themeFlow").getAsBoolean();
        var states = root.getAsJsonObject("modules");
        var rollback = new java.util.LinkedHashMap<com.orbitdlc.client.module.Module, JsonObject>();
        try {
            for (var module : modules.modules()) {
                if (module instanceof PlannedModule || !states.has(module.id())) continue;
                var state = states.getAsJsonObject(module.id());
                var previous = module.saveSettings();
                previous.addProperty("enabled", module.isEnabled());
                rollback.put(module, previous);
                module.loadSettings(state);
                module.setEnabled(state.get("enabled").getAsBoolean());
            }
            if (root.has("hudPositions")) HudDrag.loadPositions(root.getAsJsonObject("hudPositions"));
        } catch (RuntimeException error) {
            rollback.forEach((module, previous) -> {
                module.loadSettings(previous);
                module.setEnabled(previous.get("enabled").getAsBoolean());
            });
            throw error;
        }
        OrbitTheme.restore(theme, accent, tint, flow);
        OrbitTheme.palette(themeColor(root, "themeSurface", 0xFF111016),
                themeColor(root, "themeForeground", 0xFFFDF7FF), themeColor(root, "themeOutline", 0xFF59415D));
    }

    private static int themeColor(JsonObject root, String key, int fallback) {
        return root.has(key) ? root.get(key).getAsInt() : fallback;
    }

    static void loadSession(ModuleManager modules) {
        if (!Files.exists(SESSION)) return;
        try {
            load(SESSION, modules);
        } catch (IOException | RuntimeException error) {
            org.slf4j.LoggerFactory.getLogger("OrbitDLC").warn("Unable to load OrbitDLC session", error);
        }
    }

    static void saveSession(ModuleManager modules) {
        try {
            save(SESSION, modules, true);
        } catch (IOException | RuntimeException error) {
            org.slf4j.LoggerFactory.getLogger("OrbitDLC").warn("Unable to save OrbitDLC session", error);
        }
    }
}
