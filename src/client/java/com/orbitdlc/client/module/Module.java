package com.orbitdlc.client.module;

import java.util.Objects;

public abstract class Module {
    private final String id;
    private final Category category;
    private boolean enabled;

    protected Module(String id, Category category) {
        if (!id.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid module ID: " + id);
        }
        this.id = id;
        this.category = Objects.requireNonNull(category);
    }

    public final String id() {
        return id;
    }

    public final Category category() {
        return category;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
        this.enabled = enabled;
    }

    public com.google.gson.JsonObject saveSettings() { return new com.google.gson.JsonObject(); }
    public void loadSettings(com.google.gson.JsonObject settings) {}

    public abstract String description();

    protected void onEnable() {}
    protected void onDisable() {}
}
