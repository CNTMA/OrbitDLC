package com.orbitdlc.client;

enum OrbitTheme {
    ORCHID("Orchid", 0xFFF3DBF8, 0xFFA77B99),
    SMOKE("Smoke", 0xFFE5E3E1, 0xFF8E8A87),
    AMBER("Amber", 0xFFFFDFAD, 0xFFB48653),
    JADE("Jade", 0xFFC9F5DD, 0xFF679980);

    private static OrbitTheme current = ORCHID;
    final String label;
    final int accent;
    final int tint;

    OrbitTheme(String label, int accent, int tint) {
        this.label = label;
        this.accent = accent;
        this.tint = tint;
    }

    static OrbitTheme current() { return current; }
    static void select(OrbitTheme theme) { current = theme; }
    static int accent() { return current.accent; }
    static int tint(int alpha) { return (alpha << 24) | (current.tint & 0xFFFFFF); }
}
