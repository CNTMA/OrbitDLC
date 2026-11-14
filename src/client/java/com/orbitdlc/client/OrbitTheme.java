package com.orbitdlc.client;

enum OrbitTheme {
    ORCHID("Orchid", 0xFFF3DBF8, 0xFFA77B99),
    SMOKE("Smoke", 0xFFE5E3E1, 0xFF8E8A87),
    AMBER("Amber", 0xFFFFDFAD, 0xFFB48653),
    JADE("Jade", 0xFFC9F5DD, 0xFF679980),
    CUSTOM("Custom", 0xFFF3DBF8, 0xFFA77B99);

    private static OrbitTheme current = ORCHID;
    final String label;
    final int accent;
    final int tint;

    OrbitTheme(String label, int accent, int tint) {
        this.label = label;
        this.accent = accent;
        this.tint = tint;
    }

    private static int customAccent = CUSTOM.accent;
    private static int customTint = CUSTOM.tint;
    private static boolean flowing;
    private static int fromAccent = ORCHID.accent;
    private static int fromTint = ORCHID.tint;
    private static long changedAt = System.nanoTime();

    static OrbitTheme current() { return current; }
    static boolean flowing() { return flowing; }
    static int customAccent() { return customAccent; }
    static int customTint() { return customTint; }

    static void select(OrbitTheme theme) {
        startTransition();
        current = theme;
    }

    static void toggleFlow() {
        startTransition();
        flowing = !flowing;
    }

    static String hex(int color) {
        return String.format(java.util.Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }

    private static int parseColor(String value) {
        String hex = value.strip();
        if (!hex.matches("#?[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Enter colors as #RRGGBB");
        return 0xFF000000 | Integer.parseInt(hex.replace("#", ""), 16);
    }

    static void restore(OrbitTheme theme, int accent, int tint, boolean flow) {
        startTransition();
        current = theme;
        customAccent = accent | 0xFF000000;
        customTint = tint | 0xFF000000;
        flowing = flow;
    }

    private static int surface = 0xFF111016;
    private static int foreground = 0xFFFDF7FF;
    private static int outline = 0xFF59415D;

    static int surface() { return surface; }
    static int foreground() { return foreground; }
    static int outline() { return outline; }

    static void palette(int background, int text, int border) {
        surface = background | 0xFF000000;
        foreground = text | 0xFF000000;
        outline = border | 0xFF000000;
    }

    static void custom(String accent, String tint, String background, String text, String border) {
        int nextAccent = parseColor(accent), nextTint = parseColor(tint);
        palette(parseColor(background), parseColor(text), parseColor(border));
        restore(CUSTOM, nextAccent, nextTint, flowing);
    }

    static int accent() { return color(true); }
    static int tint(int alpha) { return (alpha << 24) | (color(false) & 0xFFFFFF); }

    private static void startTransition() {
        fromAccent = accent();
        fromTint = color(false);
        changedAt = System.nanoTime();
    }

    private static int color(boolean accent) {
        double seconds = (System.nanoTime() - changedAt) * 1e-9;
        int target = current == CUSTOM ? (accent ? customAccent : customTint) : (accent ? current.accent : current.tint);
        if (flowing) {
            int other = current == CUSTOM
                    ? (accent ? customTint : customAccent)
                    : switch (current) {
                        case ORCHID -> accent ? JADE.accent : JADE.tint;
                        case JADE -> accent ? AMBER.accent : AMBER.tint;
                        default -> accent ? ORCHID.accent : ORCHID.tint;
                    };
            float wave = (float) ((1 - Math.cos(seconds * Math.PI / 3)) / 2);
            target = OrbitButton.blend(target, other, wave);
        }
        float progress = Math.clamp((float) seconds / 0.45f, 0f, 1f);
        return OrbitButton.blend(accent ? fromAccent : fromTint, target, progress * progress * (3 - 2 * progress));
    }
}
