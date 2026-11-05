package dev.jax.jaxclient;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

/** Small 2D drawing helpers: rounded rectangles, circles, colour math, text. */
final class Draw {
    private Draw() {}

    static double clamp01(double v) {
        return v < 0.0 ? 0.0 : Math.min(v, 1.0);
    }

    static int alpha(int color, double mul) {
        int a = (int) Math.round(((color >>> 24) & 0xFF) * clamp01(mul));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static int mix(int a, int b, double k) {
        return (int) Math.round(a + (b - a) * k);
    }

    static int lerp(int from, int to, double t) {
        double k = clamp01(t);
        int a = mix((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, k);
        int r = mix((from >>> 16) & 0xFF, (to >>> 16) & 0xFF, k);
        int g = mix((from >>> 8) & 0xFF, (to >>> 8) & 0xFF, k);
        int b = mix(from & 0xFF, to & 0xFF, k);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Rounded rectangle built from disjoint strips, so translucent colours never double-blend. */
    static void round(DrawContext c, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) {
            c.fill(x, y, x + w, y + h, color);
            return;
        }
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0.0, (double) r * r - dy * dy)));
            if (x + inset < x + w - inset) {
                c.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
                c.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
            }
        }
        if (h - 2 * r > 0) {
            c.fill(x, y + r, x + w, y + h - r, color);
        }
    }

    static void circle(DrawContext c, int cx, int cy, int radius, int color) {
        round(c, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
    }

    /** Vanilla text treats alpha below 4 as opaque, so near-invisible text is skipped. */
    static void text(DrawContext c, TextRenderer tr, String s, int x, int y, int color) {
        if ((color >>> 24) < 8) {
            return;
        }
        c.drawText(tr, s, x, y, color, false);
    }

    static void textShadow(DrawContext c, TextRenderer tr, String s, int x, int y, int color) {
        if ((color >>> 24) < 8) {
            return;
        }
        c.drawText(tr, s, x, y, color, true);
    }

    static void textCenter(DrawContext c, TextRenderer tr, String s, int centerX, int y, int color) {
        text(c, tr, s, centerX - tr.getWidth(s) / 2, y, color);
    }

    static void scaled(DrawContext c, TextRenderer tr, String s, int x, int y, float scale, int color) {
        if ((color >>> 24) < 8) {
            return;
        }
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate((float) x, (float) y, 0.0f);
        m.scale(scale, scale, 1.0f);
        c.drawText(tr, s, 0, 0, color, false);
        m.pop();
    }
}
