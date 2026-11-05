package dev.jax.jaxclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Two-line toast cards stacked top-right: slide in, run a progress line, fade out. */
final class Notifications {
    private Notifications() {}

    private static final long LIFE_MS = 3000L;
    private static final long IN_MS = 280L;
    private static final long OUT_MS = 420L;
    private static final int CARD_H = 30;
    private static final int GAP = 4;
    private static final int TOP = 6;
    private static final int MAX = 4;

    private static final class Toast {
        final String title;
        final String sub;
        final String icon;
        final boolean lit;
        final long born;
        final Anim y;

        Toast(String title, String sub, String icon, boolean lit, long born, double y0) {
            this.title = title;
            this.sub = sub;
            this.icon = icon;
            this.lit = lit;
            this.born = born;
            this.y = new Anim(y0, 14.0);
        }
    }

    private static final List<Toast> TOASTS = new ArrayList<>();
    private static final Anim OCCUPIED = new Anim(0.0, 10.0);

    private static int slotY(int index) {
        return TOP + index * (CARD_H + GAP);
    }

    private static void add(String title, String sub, String icon, boolean lit) {
        if (!JaxClient.config().notifications) {
            return;
        }
        TOASTS.add(new Toast(title, sub, icon, lit, System.currentTimeMillis(), slotY(TOASTS.size())));
        while (TOASTS.size() > MAX) {
            TOASTS.remove(0);
        }
    }

    /** Feature on/off toast. */
    static void push(String name, boolean on) {
        add(name, on ? "Enabled" : "Disabled", on ? "\u2713" : "\u2717", on);
    }

    /** Free-form toast, e.g. weather changes. */
    static void info(String title, String sub, String icon) {
        add(title, sub, icon, true);
    }

    /** Expires old toasts and eases the remaining ones into their slots. */
    static void update(double dt) {
        long now = System.currentTimeMillis();
        TOASTS.removeIf(t -> now - t.born >= LIFE_MS);
        for (int i = 0; i < TOASTS.size(); i++) {
            Anim y = TOASTS.get(i).y;
            y.set(slotY(i));
            y.update(dt);
        }
        OCCUPIED.set(TOASTS.isEmpty() ? 0.0 : TOASTS.size() * (double) (CARD_H + GAP));
        OCCUPIED.update(dt);
    }

    /** Height taken by the toast stack, so the module list can sit below it. */
    static int occupied() {
        return (int) Math.round(OCCUPIED.get());
    }

    static void render(DrawContext ctx, MinecraftClient mc) {
        if (TOASTS.isEmpty()) {
            return;
        }
        TextRenderer tr = mc.textRenderer;
        int sw = mc.getWindow().getScaledWidth();
        int accent = Theme.accent();
        long now = System.currentTimeMillis();

        for (Toast t : TOASTS) {
            long age = now - t.born;
            double in = Draw.clamp01(age / (double) IN_MS);
            double slideIn = 1.0 - Math.pow(1.0 - in, 3.0);
            double fade = Draw.clamp01((LIFE_MS - age) / (double) OUT_MS);

            int w = Math.max(132, Math.max(tr.getWidth(t.title), tr.getWidth(t.sub)) + 44);
            int x = sw - w - 6 + (int) Math.round((1.0 - slideIn) * 60.0);
            int y = (int) Math.round(t.y.get());

            Draw.round(ctx, x, y, w, CARD_H, 6, Draw.alpha(Theme.PANEL, fade));
            int iconBg = t.lit ? Draw.alpha(accent, 0.28) : Theme.PANEL_HI;
            Draw.round(ctx, x + 5, y + 5, 20, 20, 5, Draw.alpha(iconBg, fade));
            Draw.textCenter(ctx, tr, t.icon, x + 15, y + 11,
                    Draw.alpha(t.lit ? accent : Theme.TEXT_DIM, fade));
            Draw.text(ctx, tr, t.title, x + 32, y + 6, Draw.alpha(accent, fade));
            Draw.text(ctx, tr, t.sub, x + 32, y + 16, Draw.alpha(Theme.TEXT, fade));

            double left = Draw.clamp01(1.0 - age / (double) LIFE_MS);
            int bar = (int) Math.round((w - 12) * left);
            Draw.round(ctx, x + 6, y + CARD_H - 4, bar, 2, 1, Draw.alpha(accent, 0.8 * fade));
        }
    }
}
