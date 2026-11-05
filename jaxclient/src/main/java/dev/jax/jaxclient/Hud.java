package dev.jax.jaxclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Overlay pieces: coordinates + keybind list (left), keystrokes (bottom-left),
 * status strip (bottom-right), plain right-aligned module list (top-right).
 */
final class Hud {
    private Hud() {}

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Anim[] KEYS = new Anim[7];

    static {
        for (int i = 0; i < KEYS.length; i++) {
            KEYS[i] = new Anim(0.0, 22.0);
        }
    }

    static void render(DrawContext ctx, MinecraftClient mc, double fps, double dt) {
        if (!JaxClient.config().hud || mc.player == null) {
            return;
        }
        TextRenderer tr = mc.textRenderer;
        int accent = Theme.accent();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        int y = 6;
        y += coordsPanel(ctx, mc, tr, accent, y);
        directionPanel(ctx, mc, tr, accent, y);
        keybindList(ctx, tr, accent, y + (Features.direction.isEnabled() ? 20 : 0));
        armorStatus(ctx, mc, tr, accent, sw, sh);
        keystrokes(ctx, mc, tr, accent, sh, dt);
        statusStrip(ctx, mc, tr, accent, sw, sh, fps);
        moduleList(ctx, tr, sw);
    }


    private static void directionPanel(DrawContext ctx, MinecraftClient mc, TextRenderer tr,
                                       int accent, int y) {
        double a = Features.direction.anim.get();
        if (a < 0.02 || mc.player == null) return;
        float yaw = mc.player.getYaw();
        String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        int index = Math.floorMod(Math.round(yaw / 45.0f), 8);
        String text = "FACING  " + dirs[index];
        int w = tr.getWidth(text) + 18;
        Draw.round(ctx, 6, y, w, 16, 5, Draw.alpha(Theme.HUD_BG, a));
        Draw.round(ctx, 6, y + 3, 2, 10, 1, Draw.alpha(accent, a));
        Draw.text(ctx, tr, text, 15, y + 4, Draw.alpha(Theme.TEXT, a));
    }

    private static void armorStatus(DrawContext ctx, MinecraftClient mc, TextRenderer tr,
                                    int accent, int sw, int sh) {
        double a = Features.armorStatus.anim.get();
        if (a < 0.02 || mc.player == null) return;
        int x = 8;
        int y = sh - 8 - 16;
        int slot = 0;
        for (var stack : mc.player.getArmorItems()) {
            String label;
            if (stack.isEmpty()) {
                label = new String[]{"HELM", "CHEST", "LEGS", "BOOTS"}[slot];
            } else if (stack.isDamageable()) {
                int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamage());
                int percent = Math.round(remaining * 100.0f / stack.getMaxDamage());
                label = stack.getName().getString() + " " + percent + "%";
            } else {
                label = stack.getName().getString();
            }
            int w = tr.getWidth(label) + 14;
            Draw.round(ctx, x, y, w, 14, 4, Draw.alpha(Theme.HUD_BG, a));
            Draw.round(ctx, x + 4, y + 4, 2, 6, 1, Draw.alpha(accent, a));
            Draw.text(ctx, tr, label, x + 9, y + 3, Draw.alpha(Theme.TEXT, a));
            x += w + 4;
            slot++;
        }
    }

    private static int ping(MinecraftClient mc) {
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler == null || mc.player == null) {
            return 0;
        }
        PlayerListEntry entry = handler.getPlayerListEntry(mc.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }

    private static String keyLabel(KeyBinding bind) {
        return bind.getBoundKeyLocalizedText().getString();
    }

    /** Returns the vertical space used (including the gap below), eased by the fade. */
    private static int coordsPanel(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int accent, int y) {
        double a = Features.coords.anim.get();
        if (a < 0.02) {
            return 0;
        }
        String s = "XYZ  " + (int) Math.floor(mc.player.getX()) + "  "
                + (int) Math.floor(mc.player.getY()) + "  "
                + (int) Math.floor(mc.player.getZ());
        int w = tr.getWidth(s) + 18;
        int h = 16;
        Draw.round(ctx, 6, y, w, h, 5, Draw.alpha(Theme.HUD_BG, a));
        Draw.round(ctx, 6, y + 3, 2, h - 6, 1, Draw.alpha(accent, a));
        Draw.text(ctx, tr, s, 15, y + 4, Draw.alpha(Theme.TEXT, a));
        return (int) Math.round((h + 4) * a);
    }

    private static void keybindList(DrawContext ctx, TextRenderer tr, int accent, int y) {
        double a = Features.keybindList.anim.get();
        if (a < 0.02) {
            return;
        }
        List<String> names = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        List<Boolean> lit = new ArrayList<>();
        for (Feature f : Features.ALL) {
            if (f.keyBind != null) {
                names.add(f.name);
                keys.add(keyLabel(f.keyBind));
                lit.add(f.isEnabled());
            }
        }
        if (JaxClient.menuBind != null) {
            names.add("Menu");
            keys.add(keyLabel(JaxClient.menuBind));
            lit.add(false);
        }
        int n = names.size();
        if (n == 0) {
            return;
        }

        int nameW = tr.getWidth("KEYBINDS");
        int chipW = 0;
        for (int i = 0; i < n; i++) {
            nameW = Math.max(nameW, tr.getWidth(names.get(i)));
            chipW = Math.max(chipW, tr.getWidth(keys.get(i)) + 8);
        }
        int rowH = 13;
        int w = nameW + chipW + 28;
        int h = 18 + n * rowH + 4;

        Draw.round(ctx, 6, y, w, h, 6, Draw.alpha(Theme.HUD_BG, a));
        Draw.round(ctx, 6, y + 4, 2, h - 8, 1, Draw.alpha(accent, a));
        Draw.text(ctx, tr, "KEYBINDS", 15, y + 5, Draw.alpha(Theme.TEXT_DIM, a));
        for (int i = 0; i < n; i++) {
            int ry = y + 17 + i * rowH;
            boolean on = lit.get(i);
            Draw.text(ctx, tr, names.get(i), 15, ry + 2, Draw.alpha(on ? Theme.TEXT : Theme.TEXT_DIM, a));
            int cx = 6 + w - 8 - chipW;
            Draw.round(ctx, cx, ry, chipW, 11, 3, Draw.alpha(Theme.PANEL_HI, a));
            Draw.textCenter(ctx, tr, keys.get(i), cx + chipW / 2, ry + 2,
                    Draw.alpha(on ? accent : Theme.TEXT_DIM, a));
        }
    }

    private static void keystrokes(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int accent,
                                   int sh, double dt) {
        GameOptions o = mc.options;
        boolean[] down = {
                o.forwardKey.isPressed(), o.leftKey.isPressed(), o.backKey.isPressed(), o.rightKey.isPressed(),
                o.attackKey.isPressed(), o.useKey.isPressed(), o.jumpKey.isPressed()
        };
        for (int i = 0; i < KEYS.length; i++) {
            KEYS[i].set(down[i] ? 1.0 : 0.0);
            KEYS[i].update(dt);
        }
        double a = Features.keystrokes.anim.get();
        if (a < 0.02) {
            return;
        }

        final int k = 18;
        final int g = 2;
        int x0 = 8;
        int y0 = sh - 8 - 70;
        int mouseW = (3 * k + 2 * g - g) / 2;

        keyBox(ctx, tr, x0 + k + g, y0, k, k, "W", KEYS[0].get(), a, accent);
        keyBox(ctx, tr, x0, y0 + k + g, k, k, "A", KEYS[1].get(), a, accent);
        keyBox(ctx, tr, x0 + k + g, y0 + k + g, k, k, "S", KEYS[2].get(), a, accent);
        keyBox(ctx, tr, x0 + 2 * (k + g), y0 + k + g, k, k, "D", KEYS[3].get(), a, accent);
        keyBox(ctx, tr, x0, y0 + 2 * (k + g), mouseW, 16, "LMB", KEYS[4].get(), a, accent);
        keyBox(ctx, tr, x0 + mouseW + g, y0 + 2 * (k + g), mouseW, 16, "RMB", KEYS[5].get(), a, accent);
        keyBox(ctx, tr, x0, y0 + 2 * (k + g) + 16 + g, 3 * k + 2 * g, 12, "", KEYS[6].get(), a, accent);
    }

    private static void keyBox(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h,
                               String label, double press, double a, int accent) {
        int bg = Draw.lerp(Theme.HUD_BG, Draw.alpha(accent, 0.9), press);
        Draw.round(ctx, x, y, w, h, 4, Draw.alpha(bg, a));
        int fg = Draw.lerp(Theme.TEXT_DIM, 0xFFFFFFFF, press);
        if (label.isEmpty()) {
            Draw.round(ctx, x + w / 2 - 12, y + h / 2 - 1, 24, 2, 1, Draw.alpha(fg, a));
        } else {
            Draw.textCenter(ctx, tr, label, x + w / 2, y + (h - 8) / 2, Draw.alpha(fg, a));
        }
    }

    private static void statusStrip(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int accent,
                                    int sw, int sh, double fps) {
        double a = Math.max(Features.fps.anim.get(),
                Math.max(Features.ping.anim.get(), Features.clock.anim.get()));
        if (a < 0.02) {
            return;
        }
        List<String> parts = new ArrayList<>();
        if (Features.clock.isEnabled()) {
            parts.add(LocalTime.now().format(CLOCK));
        }
        if (Features.fps.isEnabled()) {
            parts.add(Math.round(fps) + " fps");
        }
        if (Features.ping.isEnabled()) {
            parts.add(ping(mc) + " ms");
        }
        if (parts.isEmpty()) {
            return;
        }
        String text = String.join("   ", parts);
        int w = tr.getWidth(text) + 22;
        int h = 14;
        int x = sw - w - 6;
        int y = sh - h - 6;
        Draw.round(ctx, x, y, w, h, 5, Draw.alpha(Theme.HUD_BG, a));
        Draw.circle(ctx, x + 8, y + h / 2, 2, Draw.alpha(accent, a));
        Draw.text(ctx, tr, text, x + 15, y + 3, Draw.alpha(Theme.TEXT, a));
    }

    private static void moduleList(DrawContext ctx, TextRenderer tr, int sw) {
        int y = 6 + Notifications.occupied();
        for (Feature f : Features.ALL) {
            if (f.category == Feature.Category.UTILITY) {
                continue;
            }
            double a = f.anim.get();
            if (a < 0.02) {
                continue;
            }
            int x = sw - 6 - tr.getWidth(f.name) + (int) Math.round((1.0 - a) * 20.0);
            Draw.textShadow(ctx, tr, f.name, x, y, Draw.alpha(Theme.TEXT, a));
            y += (int) Math.round(10.0 * a);
        }
    }
}
