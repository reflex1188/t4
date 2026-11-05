package dev.jax.jaxclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Left column (top to bottom): coordinates, keybind list. Bottom-left: keystrokes + CPS.
 * Right column (bottom to top): status strip, armor, potion effects.
 * Top-right: toasts, then the plain module list. Screen edges: low-health pulse.
 */
final class Hud {
    private Hud() {}

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int BAD = 0xFFE5484D;
    private static final int GOOD = 0xFF4ADE80;
    private static final EquipmentSlot[] GEAR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND
    };
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private static final Anim[] KEYS = new Anim[7];
    private static final Anim WARN = new Anim(0.0, 6.0);
    private static final ArrayDeque<Long> LEFT_CLICKS = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT_CLICKS = new ArrayDeque<>();
    private static boolean prevLeft;
    private static boolean prevRight;

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

        lowHealth(ctx, mc, sw, sh, dt);

        int y = 6;
        y += coordsPanel(ctx, mc, tr, accent, y);
        y += infoPanels(ctx, mc, tr, accent, y);
        keybindList(ctx, tr, accent, y);
        keystrokes(ctx, mc, tr, accent, sh, dt);

        int bottom = sh - 6;
        bottom -= statusStrip(ctx, mc, tr, accent, sw, bottom, fps);
        bottom -= armorHud(ctx, mc, tr, sw, bottom);
        potions(ctx, mc, tr, sw, bottom);

        moduleList(ctx, tr, sw);
    }

    // ------------------------------------------------------------------ helpers

    private static int ping(MinecraftClient mc) {
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        UUID id = mc.getSession().getUuidOrNull();
        if (handler == null || id == null) {
            return 0;
        }
        PlayerListEntry entry = handler.getPlayerListEntry(id);
        return entry == null ? 0 : entry.getLatency();
    }

    private static String keyLabel(KeyBinding bind) {
        return bind.getBoundKeyLocalizedText().getString();
    }

    // ------------------------------------------------------------------ low health

    private static void lowHealth(DrawContext ctx, MinecraftClient mc, int sw, int sh, double dt) {
        float hp = mc.player.getHealth();
        boolean low = Features.lowHealth.isEnabled() && hp > 0.0f && hp <= 6.0f;
        WARN.set(low ? 1.0 : 0.0);
        WARN.update(dt);
        double a = WARN.get();
        if (a < 0.02) {
            return;
        }
        double pulse = 0.65 + 0.35 * Math.sin(System.currentTimeMillis() / 220.0);
        int steps = 14;
        int step = 3;
        for (int i = 0; i < steps; i++) {
            double f = 1.0 - i / (double) steps;
            int col = Draw.alpha(0xFFE11D48, 0.30 * f * f * a * pulse);
            int d = i * step;
            ctx.fill(0, d, sw, d + step, col);
            ctx.fill(0, sh - d - step, sw, sh - d, col);
            ctx.fill(d, 0, d + step, sh, col);
            ctx.fill(sw - d - step, 0, sw - d, sh, col);
        }
    }

    // ------------------------------------------------------------------ left column

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

    private static int infoPanels(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int accent, int y) {
        List<String> lines = new ArrayList<>();
        if (Features.biomeHud.isEnabled()) {
            String biome = mc.world.getBiome(mc.player.getBlockPos()).getKey()
                    .map(key -> key.getValue().getPath().replace('_', ' '))
                    .orElse("unknown");
            lines.add("BIOME  " + titleCase(biome));
        }
        if (Features.directionHud.isEnabled()) {
            float yaw = mc.player.getYaw();
            String[] directions = {"South", "West", "North", "East"};
            String cardinal = directions[Math.floorMod(Math.round(yaw / 90.0f), 4)];
            lines.add("FACING  " + cardinal + "  " + Math.round(yaw) + "°");
        }
        if (Features.speedometer.isEnabled()) {
            double speed = Math.hypot(mc.player.getVelocity().x, mc.player.getVelocity().z) * 20.0;
            lines.add(String.format("SPEED  %.2f m/s", speed));
        }
        if (Features.coordinatesDetails.isEnabled()) {
            int x = mc.player.getBlockX();
            int z = mc.player.getBlockZ();
            lines.add("CHUNK  " + Math.floorDiv(x, 16) + ", " + Math.floorDiv(z, 16));
        }
        if (lines.isEmpty()) return 0;
        double alpha = 1.0;
        int width = 0;
        for (String line : lines) width = Math.max(width, tr.getWidth(line));
        int rowHeight = 13;
        int height = 8 + lines.size() * rowHeight;
        Draw.round(ctx, 6, y, width + 20, height, 5, Draw.alpha(Theme.HUD_BG, alpha));
        Draw.round(ctx, 6, y + 4, 2, height - 8, 1, Draw.alpha(accent, alpha));
        for (int i = 0; i < lines.size(); i++) {
            Draw.text(ctx, tr, lines.get(i), 15, y + 5 + i * rowHeight, Theme.TEXT);
        }
        return height + 4;
    }

    private static String titleCase(String value) {
        StringBuilder out = new StringBuilder();
        for (String word : value.split(" ")) {
            if (word.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) out.append(word.substring(1));
        }
        return out.toString();
    }

    // ------------------------------------------------------------------ keystrokes

    private static int cps(ArrayDeque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) {
            clicks.pollFirst();
        }
        return clicks.size();
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

        long now = System.currentTimeMillis();
        if (down[4] && !prevLeft) {
            LEFT_CLICKS.addLast(now);
        }
        if (down[5] && !prevRight) {
            RIGHT_CLICKS.addLast(now);
        }
        prevLeft = down[4];
        prevRight = down[5];
        int left = cps(LEFT_CLICKS, now);
        int right = cps(RIGHT_CLICKS, now);

        double a = Features.keystrokes.anim.get();
        if (a < 0.02) {
            return;
        }

        final int k = 18;
        final int g = 2;
        int x0 = 8;
        int y0 = sh - 8 - 80;
        int width = 3 * k + 2 * g;
        int mouseW = (width - g) / 2;

        keyBox(ctx, tr, x0 + k + g, y0, k, k, "W", KEYS[0].get(), a, accent);
        keyBox(ctx, tr, x0, y0 + k + g, k, k, "A", KEYS[1].get(), a, accent);
        keyBox(ctx, tr, x0 + k + g, y0 + k + g, k, k, "S", KEYS[2].get(), a, accent);
        keyBox(ctx, tr, x0 + 2 * (k + g), y0 + k + g, k, k, "D", KEYS[3].get(), a, accent);
        keyBox(ctx, tr, x0, y0 + 2 * (k + g), mouseW, 16, "LMB", KEYS[4].get(), a, accent);
        keyBox(ctx, tr, x0 + mouseW + g, y0 + 2 * (k + g), mouseW, 16, "RMB", KEYS[5].get(), a, accent);
        int spaceY = y0 + 2 * (k + g) + 16 + g;
        keyBox(ctx, tr, x0, spaceY, width, 12, "", KEYS[6].get(), a, accent);
        Draw.textCenter(ctx, tr, left + " | " + right + " CPS", x0 + width / 2, spaceY + 12 + g + 1,
                Draw.alpha(Theme.TEXT_DIM, a));
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

    // ------------------------------------------------------------------ right column

    /** Each right-column block draws ending at {@code bottom} and returns the height it used. */
    private static int statusStrip(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int accent,
                                   int sw, int bottom, double fps) {
        double a = Math.max(Math.max(Features.fps.anim.get(), Features.ping.anim.get()),
                Math.max(Math.max(Features.clock.anim.get(), Features.memory.anim.get()), Features.worldTime.anim.get()));
        if (a < 0.02) {
            return 0;
        }
        List<String> parts = new ArrayList<>();
        if (Features.clock.isEnabled()) {
            parts.add(LocalTime.now().format(CLOCK));
        }
        if (Features.worldTime.isEnabled()) {
            long ticks = Math.floorMod(mc.world.getTimeOfDay(), 24000L);
            long totalMinutes = (ticks * 60L) / 1000L;
            int hour = (int) ((totalMinutes / 60L + 6L) % 24L);
            int minute = (int) (totalMinutes % 60L);
            parts.add(String.format("World %02d:%02d", hour, minute));
        }
        if (Features.fps.isEnabled()) {
            parts.add(Math.round(fps) + " fps");
        }
        if (Features.ping.isEnabled()) {
            parts.add(ping(mc) + " ms");
        }
        if (Features.memory.isEnabled()) {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) >> 20;
            long max = rt.maxMemory() >> 20;
            parts.add(used + "/" + max + " MB");
        }
        if (parts.isEmpty()) {
            return 0;
        }
        String text = String.join("   ", parts);
        int w = tr.getWidth(text) + 22;
        int h = 14;
        int x = sw - w - 6;
        int y = bottom - h;
        Draw.round(ctx, x, y, w, h, 5, Draw.alpha(Theme.HUD_BG, a));
        Draw.circle(ctx, x + 8, y + h / 2, 2, Draw.alpha(accent, a));
        Draw.text(ctx, tr, text, x + 15, y + 3, Draw.alpha(Theme.TEXT, a));
        return (int) Math.round((h + 4) * a);
    }

    private static int armorHud(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int sw, int bottom) {
        double a = Features.armorHud.anim.get();
        if (a < 0.02) {
            return 0;
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (EquipmentSlot slot : GEAR) {
            ItemStack s = mc.player.getEquippedStack(slot);
            if (!s.isEmpty()) {
                stacks.add(s);
            }
        }
        if (stacks.isEmpty()) {
            return 0;
        }
        int rowH = 18;
        int y = bottom - stacks.size() * rowH;
        for (ItemStack s : stacks) {
            String pct = "";
            int col = Theme.TEXT;
            if (s.isDamageable() && s.getMaxDamage() > 0) {
                int left = 100 - (int) Math.round(s.getDamage() * 100.0 / s.getMaxDamage());
                pct = left + "%";
                col = Draw.lerp(BAD, GOOD, left / 100.0);
            }
            int x = sw - 6 - tr.getWidth(pct) - 20;
            if (a > 0.6) {
                ctx.drawItem(s, x, y);
            }
            Draw.textShadow(ctx, tr, pct, x + 20, y + 4, Draw.alpha(col, a));
            y += rowH;
        }
        return (int) Math.round((stacks.size() * rowH + 4) * a);
    }

    private static String roman(int n) {
        return n >= 1 && n < ROMAN.length ? ROMAN[n] : String.valueOf(n);
    }

    private static String timeLeft(StatusEffectInstance e) {
        if (e.isInfinite()) {
            return "inf";
        }
        int s = e.getDuration() / 20;
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }

    private static void potions(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int sw, int bottom) {
        double a = Features.potions.anim.get();
        if (a < 0.02) {
            return;
        }
        Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();
        if (effects.isEmpty()) {
            return;
        }
        int rows = Math.min(effects.size(), 8);
        int y = bottom - rows * 11;
        int shown = 0;
        for (StatusEffectInstance e : effects) {
            if (shown++ >= rows) {
                break;
            }
            StatusEffect type = e.getEffectType().value();
            String level = e.getAmplifier() > 0 ? " " + roman(e.getAmplifier() + 1) : "";
            String text = type.getName().getString() + level + "  " + timeLeft(e);
            int col = type.isBeneficial() ? Theme.TEXT : BAD;
            Draw.textShadow(ctx, tr, text, sw - 6 - tr.getWidth(text), y, Draw.alpha(col, a));
            y += 11;
        }
    }

    // ------------------------------------------------------------------ top-right

    private static void moduleList(DrawContext ctx, TextRenderer tr, int sw) {
        int y = 6 + Notifications.occupied();
        for (Feature f : Features.ALL) {
            if (f.category == Feature.Category.UTILITY || !f.listInHud) {
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
