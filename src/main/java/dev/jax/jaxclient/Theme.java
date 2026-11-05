package dev.jax.jaxclient;

final class Theme {
    private Theme() {}

    static final int BG = 0xFF0F1115;
    static final int PANEL = 0xF2161920;
    static final int SIDEBAR = 0xFF12151B;
    static final int PANEL_ALT = 0xFF1C2029;
    static final int PANEL_HI = 0xFF262B36;
    static final int BORDER = 0xFF2A2F3A;
    static final int TEXT = 0xFFE6E8EE;
    static final int TEXT_DIM = 0xFF8A90A0;
    static final int OFF = 0xFF3A404C;
    static final int HUD_BG = 0xB0161920;

    static final int[] ACCENTS = {0xFF8B5CF6, 0xFF22D3EE, 0xFFE11D48, 0xFF7DD3FC, 0xFFF5B301};
    static final String[] ACCENT_NAMES = {"Purple", "Cyan", "Crimson", "Ice", "Gold"};

    static int accent() {
        return ACCENTS[Math.floorMod(JaxClient.config().accent, ACCENTS.length)];
    }
}
