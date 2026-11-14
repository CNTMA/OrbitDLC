package com.orbitdlc.client;

import com.orbitdlc.OrbitDLC;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.util.LinkedHashMap;

final class Cosmetics {
    record Entry(String id, String name, String category) {}
    private static final java.nio.file.Path EQUIPPED_FILE = OrbitProfiles.ROOT.resolve("cosmetics.txt");
    private static final List<Entry> CATALOG = readCatalog();
    private static final LinkedHashMap<String, Entry> EQUIPPED = new LinkedHashMap<>();

    static {
        try {
            if (Files.isRegularFile(EQUIPPED_FILE)) {
                var saved = Files.readAllLines(EQUIPPED_FILE);
                for (var entry : catalog()) {
                    if (saved.contains(entry.id())) EQUIPPED.put(slot(entry), entry);
                }
            }
        } catch (IOException error) {
            OrbitDLC.LOGGER.warn("Could not read equipped cosmetics", error);
        }
    }

    private static String slot(Entry entry) {
        return entry.category().equals("Wings") || entry.category().equals("Backpacks") ? "Back" : entry.category();
    }

    static boolean equipped(Entry entry) { return entry.equals(EQUIPPED.get(slot(entry))); }

    static List<Entry> equipped() { return List.copyOf(EQUIPPED.values()); }

    static void toggle(Entry entry) throws IOException {
        var next = new LinkedHashMap<>(EQUIPPED);
        if (equipped(entry)) next.remove(slot(entry));
        else next.put(slot(entry), entry);
        Files.createDirectories(EQUIPPED_FILE.getParent());
        var temp = Files.createTempFile(EQUIPPED_FILE.getParent(), "cosmetics-", ".tmp");
        try {
            Files.write(temp, next.values().stream().map(Entry::id).toList());
            Files.move(temp, EQUIPPED_FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            EQUIPPED.clear();
            EQUIPPED.putAll(next);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private Cosmetics() {}

    static List<Entry> catalog() { return CATALOG; }

    static List<Entry> resolve(List<String> ids) {
        var slots = new LinkedHashMap<String, Entry>();
        for (var entry : CATALOG) {
            if (ids.contains(entry.id())) slots.putIfAbsent(slot(entry), entry);
        }
        return List.copyOf(slots.values());
    }

    private static List<Entry> readCatalog() {
        var source = Cosmetics.class.getResourceAsStream("/assets/orbitdlc/cosmetics.txt");
        if (source == null) return List.of();
        try (var reader = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8))) {
            return reader.lines().filter(line -> line.matches("cosmetics/[a-z_]+/[a-z0-9_]+/[a-z0-9_]+"))
                    .map(line -> {
                        String[] parts = line.split("/");
                        return new Entry(line, title(parts[3]), title(parts[1]));
                    }).toList();
        } catch (IOException error) {
            OrbitDLC.LOGGER.warn("Could not load cosmetics catalog", error);
            return List.of();
        }
    }

    private static String title(String name) {
        return Arrays.stream(name.split("_"))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}
