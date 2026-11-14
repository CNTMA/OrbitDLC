package com.orbitdlc.client.module;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ModuleManager {
    private final Map<String, Module> modules = new LinkedHashMap<>();

    public void register(Module module) {
        Objects.requireNonNull(module);
        if (modules.putIfAbsent(module.id(), module) != null) {
            throw new IllegalArgumentException("Duplicate module ID: " + module.id());
        }
    }

    public List<Module> modules(Category category) {
        return modules.values().stream().filter(module -> module.category() == category).toList();
    }

    public List<Module> modules() {
        return List.copyOf(modules.values());
    }
}
