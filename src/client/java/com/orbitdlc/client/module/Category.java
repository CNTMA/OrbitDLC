package com.orbitdlc.client.module;

public enum Category {
    COMBAT("Combat"),
    VISUALS("Visuals"),
    MISC("Misc"),
    PLAYER("Player"),
    MOVEMENT("Movement");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
