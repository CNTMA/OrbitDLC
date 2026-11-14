package com.orbitdlc.client;

enum VisualColor {
    ORIGINAL("Original", 0xFFFFFF), ROSE("Rose", 0xFF648C), AMBER("Amber", 0xFFBD59),
    JADE("Jade", 0x54EDAB), ICE("Ice", 0x65C7FF), VIOLET("Violet", 0xB485FF),
    WHITE("White", 0xFFFFFF);

    private static final VisualColor[] COLORS = values();
    final String label;
    final int rgb;

    VisualColor(String label, int rgb) {
        this.label = label;
        this.rgb = rgb;
    }

    boolean isOriginal() { return this == ORIGINAL && !Theme.INSTANCE.isEnabled(); }
    int renderColor() { return Theme.color(rgb) & 0xFFFFFF; }

    VisualColor next() { return COLORS[(ordinal() + 1) % COLORS.length]; }

    static VisualColor load(com.google.gson.JsonObject settings) {
        return settings.has("color") ? valueOf(settings.get("color").getAsString()) : ORIGINAL;
    }
}
