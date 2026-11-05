package dev.jax.jaxclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

/** Hold-to-zoom by easing the FOV option toward the configured value and back. */
final class Zoom {
    private Zoom() {}

    private static final Anim AMOUNT = new Anim(0.0, 16.0);
    private static Integer saved;

    /** Runs once per rendered frame. */
    static void frame(MinecraftClient mc, double dt) {
        if (mc.options == null) {
            return;
        }
        boolean armed = Features.zoom.isEnabled() && mc.player != null && mc.currentScreen == null;
        boolean held = armed && JaxClient.zoomBind != null && JaxClient.zoomBind.isPressed();
        AMOUNT.set(held ? 1.0 : 0.0);
        AMOUNT.update(dt);

        SimpleOption<Integer> fov = mc.options.getFov();
        if (held && saved == null) {
            saved = fov.getValue();
        }
        if (saved == null) {
            return;
        }
        if (!held && AMOUNT.get() <= 0.005) {
            restore(mc);
            return;
        }
        int target = (int) Math.round(saved + (JaxClient.config().zoomFov - saved) * AMOUNT.get());
        if (!fov.getValue().equals(target)) {
            fov.setValue(target);
        }
    }

    /** Puts the player's real FOV back. Safe to call any time. */
    static void restore(MinecraftClient mc) {
        if (saved != null && mc != null && mc.options != null) {
            mc.options.getFov().setValue(saved);
        }
        saved = null;
        AMOUNT.snap(0.0);
    }
}
