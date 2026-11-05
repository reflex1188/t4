package dev.jax.jaxclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

/** Click GUI: sidebar categories, animated toggles, slider, drag-to-move, smooth scroll. */
final class JaxMenu extends Screen {
    private static final int PW = 380;
    private static final int PH = 232;
    private static final int SIDEBAR_W = 98;
    private static final int TAB_H = 22;
    private static final int TAB_STEP = 26;
    private static final int TAB_Y0 = 62;
    private static final int ROW_H = 30;
    private static final int ROW_STEP = 36;
    private static final int VIEW_TOP = 50;
    private static final int TOGGLE_W = 30;
    private static final int TOGGLE_H = 14;
    private static final int SLIDER_W = 90;

    private static final int SET_ACCENT = 0;
    private static final int SET_HUD = 1;
    private static final int SET_NOTIF = 2;
    private static final int SET_ZOOM = 3;
    private static final int SET_ALLOW = 4;
    private static final int SET_RESET = 5;
    private static final int SETTINGS_ROWS = 6;

    private static final Feature.Category[] CATEGORIES = Feature.Category.values();
    private static final int TAB_COUNT = CATEGORIES.length + 1;
    private static final int SETTINGS_TAB = CATEGORIES.length;

    private static int panelX = Integer.MIN_VALUE;
    private static int panelY = Integer.MIN_VALUE;
    private static int lastTab;

    private final Anim open = new Anim(0.0, 12.0);
    private final Anim slide = new Anim(0.0, 14.0);
    private final Anim scroll = new Anim(0.0, 14.0);
    private final Anim indicator = new Anim(0.0, 16.0);
    private final Anim closeHover = new Anim(0.0, 16.0);
    private final Anim hudToggle;
    private final Anim notifToggle;
    private final Anim[] tabHover = newAnims(TAB_COUNT);
    private final Anim[] rowHover = newAnims(16);

    private int tab;
    private double scrollTarget;
    private boolean closing;
    private boolean dragging;
    private boolean sliderDrag;
    private long resetArmedUntil;
    private int dragDx;
    private int dragDy;
    private long lastNanos;
    private int px;
    private int py;
    private int pw;
    private int ph;

    JaxMenu() {
        super(Text.literal("JaxClient"));
        Config cfg = JaxClient.config();
        this.tab = Math.max(0, Math.min(lastTab, TAB_COUNT - 1));
        this.open.set(1.0);
        this.indicator.snap(this.tab * TAB_STEP);
        this.hudToggle = new Anim(cfg.hud ? 1.0 : 0.0, 14.0);
        this.notifToggle = new Anim(cfg.notifications ? 1.0 : 0.0, 14.0);
    }

    private static Anim[] newAnims(int n) {
        Anim[] arr = new Anim[n];
        for (int i = 0; i < n; i++) {
            arr[i] = new Anim(0.0, 16.0);
        }
        return arr;
    }

    // ------------------------------------------------------------------ layout

    private static int clampInt(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int oy() {
        return (int) Math.round((1.0 - open.get()) * 10.0);
    }

    private void layout() {
        pw = Math.min(PW, width - 16);
        ph = Math.min(PH, height - 16);
        if (panelX == Integer.MIN_VALUE) {
            panelX = (width - pw) / 2;
            panelY = (height - ph) / 2;
        }
        panelX = clampInt(panelX, 0, Math.max(0, width - pw));
        panelY = clampInt(panelY, 0, Math.max(0, height - ph));
        px = panelX;
        py = panelY + oy();
    }

    private int contentX() {
        return px + SIDEBAR_W + 18 + (int) Math.round(slide.get() * 28.0);
    }

    private int rowW() {
        return pw - SIDEBAR_W - 28;
    }

    private int viewTop() {
        return py + VIEW_TOP;
    }

    private int viewBottom() {
        return py + ph - 8;
    }

    private int rowTop(int i) {
        return viewTop() + i * ROW_STEP - (int) Math.round(scroll.get());
    }

    private int rowCount() {
        return tab == SETTINGS_TAB ? SETTINGS_ROWS : Features.byCategory(CATEGORIES[tab]).size();
    }

    private boolean inRow(int i, double mx, double my) {
        int y = rowTop(i);
        return in(mx, my, contentX(), y, rowW(), ROW_H) && my >= viewTop() && my < viewBottom();
    }

    private static String tabName(int t) {
        return t == SETTINGS_TAB ? "Settings" : CATEGORIES[t].label;
    }

    private int toggleX(int cx, int rowW) {
        return cx + rowW - 12 - TOGGLE_W;
    }

    private int swatchX(int cx, int rowW, int k) {
        return cx + rowW - 14 - (Theme.ACCENTS.length - 1 - k) * 20;
    }

    private int sliderX(int cx, int rowW) {
        return cx + rowW - 14 - SLIDER_W;
    }

    private double zoomFraction() {
        Config cfg = JaxClient.config();
        return (cfg.zoomFov - Config.MIN_ZOOM_FOV) / (double) (Config.MAX_ZOOM_FOV - Config.MIN_ZOOM_FOV);
    }

    private boolean resetArmed() {
        return System.currentTimeMillis() < resetArmedUntil;
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        long now = System.nanoTime();
        double dt = lastNanos == 0L ? 0.016 : Math.min(0.05, (now - lastNanos) / 1.0e9);
        lastNanos = now;

        open.update(dt);
        slide.update(dt);
        indicator.update(dt);
        scroll.update(dt);
        Config cfg = JaxClient.config();
        hudToggle.set(cfg.hud ? 1.0 : 0.0);
        hudToggle.update(dt);
        notifToggle.set(cfg.notifications ? 1.0 : 0.0);
        notifToggle.update(dt);

        layout();
        updateHover(dt, mouseX, mouseY);

        double o = open.get();
        int accent = Theme.accent();
        TextRenderer tr = this.textRenderer;

        ctx.fill(0, 0, width, height, Draw.alpha(Theme.BG, 0.45 * o));
        drawPanel(ctx, o);
        drawSidebar(ctx, tr, o, accent);
        drawHeader(ctx, tr, o);
        drawContent(ctx, tr, o, accent);
    }

    private void updateHover(double dt, int mx, int my) {
        boolean live = !closing && !dragging && !sliderDrag;
        closeHover.set(live && in(mx, my, px + pw - 26, py + 10, 16, 16) ? 1.0 : 0.0);
        closeHover.update(dt);
        for (int i = 0; i < TAB_COUNT; i++) {
            boolean over = in(mx, my, px + 12, py + TAB_Y0 + i * TAB_STEP, SIDEBAR_W - 12, TAB_H);
            tabHover[i].set(live && over ? 1.0 : 0.0);
            tabHover[i].update(dt);
        }
        int n = rowCount();
        for (int i = 0; i < rowHover.length; i++) {
            boolean over = i < n && inRow(i, mx, my);
            rowHover[i].set(live && over ? 1.0 : 0.0);
            rowHover[i].update(dt);
        }
    }

    private void drawPanel(DrawContext ctx, double o) {
        for (int i = 5; i >= 1; i--) {
            Draw.round(ctx, px - i * 2, py - i * 2 + 4, pw + i * 4, ph + i * 4, 10 + i * 2,
                    Draw.alpha(0xFF000000, 0.05 * o));
        }
        Draw.round(ctx, px - 1, py - 1, pw + 2, ph + 2, 11, Draw.alpha(Theme.BORDER, o));
        Draw.round(ctx, px, py, pw, ph, 10, Draw.alpha(Theme.PANEL, o));
    }

    private void drawSidebar(DrawContext ctx, TextRenderer tr, double o, int accent) {
        Draw.round(ctx, px + 6, py + 6, SIDEBAR_W, ph - 12, 8, Draw.alpha(Theme.SIDEBAR, o));
        Draw.scaled(ctx, tr, "JAX", px + 18, py + 16, 1.6f, Draw.alpha(Theme.TEXT, o));
        Draw.text(ctx, tr, "CLIENT", px + 18, py + 33, Draw.alpha(accent, o));
        Draw.round(ctx, px + 18, py + 46, 22, 2, 1, Draw.alpha(accent, o));

        for (int i = 0; i < TAB_COUNT; i++) {
            int ty = py + TAB_Y0 + i * TAB_STEP;
            Draw.round(ctx, px + 12, ty, SIDEBAR_W - 12, TAB_H, 6,
                    Draw.alpha(Theme.PANEL_HI, 0.85 * tabHover[i].get() * o));
        }

        int selY = py + TAB_Y0 + (int) Math.round(indicator.get());
        Draw.round(ctx, px + 12, selY, SIDEBAR_W - 12, TAB_H, 6, Draw.alpha(accent, 0.16 * o));
        Draw.round(ctx, px + 12, selY + 5, 2, TAB_H - 10, 1, Draw.alpha(accent, o));

        for (int i = 0; i < TAB_COUNT; i++) {
            int ty = py + TAB_Y0 + i * TAB_STEP;
            int col = i == tab ? Theme.TEXT : Draw.lerp(Theme.TEXT_DIM, Theme.TEXT, tabHover[i].get());
            Draw.text(ctx, tr, tabName(i), px + 24, ty + 7, Draw.alpha(col, o));
        }

        Draw.text(ctx, tr, "[" + keyName() + "] close", px + 16, py + ph - 22, Draw.alpha(Theme.TEXT_DIM, o));
    }

    private static String keyName() {
        if (JaxClient.menuBind == null) {
            return "Right Shift";
        }
        return JaxClient.menuBind.getBoundKeyLocalizedText().getString();
    }

    private void drawHeader(DrawContext ctx, TextRenderer tr, double o) {
        double ca = o * (1.0 - slide.get());
        int cx = contentX();
        Draw.scaled(ctx, tr, tabName(tab), cx, py + 14, 1.25f, Draw.alpha(Theme.TEXT, ca));

        String sub;
        if (tab == SETTINGS_TAB) {
            sub = "Client preferences";
        } else {
            int n = rowCount();
            sub = n + (n == 1 ? " module" : " modules");
        }
        Draw.text(ctx, tr, sub, cx, py + 30, Draw.alpha(Theme.TEXT_DIM, ca));

        double h = closeHover.get();
        Draw.round(ctx, px + pw - 26, py + 10, 16, 16, 5, Draw.alpha(Theme.PANEL_HI, 0.9 * h * o));
        Draw.textCenter(ctx, tr, "\u00d7", px + pw - 18, py + 14,
                Draw.alpha(Draw.lerp(Theme.TEXT_DIM, Theme.TEXT, h), o));
    }

    private void drawContent(DrawContext ctx, TextRenderer tr, double o, int accent) {
        double ca = o * (1.0 - slide.get());
        int cx = contentX();
        int rw = rowW();
        int n = rowCount();
        List<Feature> feats = tab == SETTINGS_TAB ? null : Features.byCategory(CATEGORIES[tab]);

        ctx.enableScissor(px + SIDEBAR_W + 10, viewTop(), px + pw - 8, viewBottom());
        for (int i = 0; i < n; i++) {
            int y = rowTop(i);
            if (y + ROW_H < viewTop() || y > viewBottom()) {
                continue;
            }
            double h = i < rowHover.length ? rowHover[i].get() : 0.0;
            int card = Draw.lerp(Theme.PANEL_ALT, Theme.PANEL_HI, h);
            Draw.round(ctx, cx, y, rw, ROW_H, 7, Draw.alpha(card, ca));

            if (feats != null) {
                Feature f = feats.get(i);
                boolean locked = f.serverLocked && !ServerLock.allowed(MinecraftClient.getInstance());
                label(ctx, tr, cx, y, f.name, locked ? "Locked: allow server in Settings" : f.description,
                        locked ? 0xFFE5484D : Theme.TEXT_DIM, ca);
                drawToggle(ctx, toggleX(cx, rw), y + (ROW_H - TOGGLE_H) / 2, f.anim.get(), ca, accent);
            } else {
                drawSettingsRow(ctx, tr, i, cx, y, rw, ca, accent, card);
            }
        }
        ctx.disableScissor();
    }

    private void label(DrawContext ctx, TextRenderer tr, int cx, int y, String name, String desc, double ca) {
        label(ctx, tr, cx, y, name, desc, Theme.TEXT_DIM, ca);
    }

    private void label(DrawContext ctx, TextRenderer tr, int cx, int y, String name, String desc,
                       int descColor, double ca) {
        Draw.text(ctx, tr, name, cx + 12, y + 6, Draw.alpha(Theme.TEXT, ca));
        Draw.text(ctx, tr, desc, cx + 12, y + 17, Draw.alpha(descColor, ca));
    }

    private void drawSettingsRow(DrawContext ctx, TextRenderer tr, int i, int cx, int y, int rw,
                                 double ca, int accent, int card) {
        Config cfg = JaxClient.config();
        int cy = y + ROW_H / 2;
        switch (i) {
            case SET_ACCENT -> {
                label(ctx, tr, cx, y, "Accent Color", "Client theme color", ca);
                int selected = Math.floorMod(cfg.accent, Theme.ACCENTS.length);
                for (int k = 0; k < Theme.ACCENTS.length; k++) {
                    int sx = swatchX(cx, rw, k);
                    if (k == selected) {
                        Draw.circle(ctx, sx, cy, 8, Draw.alpha(Theme.TEXT, ca));
                        Draw.circle(ctx, sx, cy, 7, Draw.alpha(card, ca));
                        Draw.circle(ctx, sx, cy, 5, Draw.alpha(Theme.ACCENTS[k], ca));
                    } else {
                        Draw.circle(ctx, sx, cy, 6, Draw.alpha(Theme.ACCENTS[k], ca));
                    }
                }
            }
            case SET_HUD -> {
                label(ctx, tr, cx, y, "HUD Overlay", "Show the on-screen overlay", ca);
                drawToggle(ctx, toggleX(cx, rw), y + (ROW_H - TOGGLE_H) / 2, hudToggle.get(), ca, accent);
            }
            case SET_NOTIF -> {
                label(ctx, tr, cx, y, "Notifications", "Slide-in toggle alerts", ca);
                drawToggle(ctx, toggleX(cx, rw), y + (ROW_H - TOGGLE_H) / 2, notifToggle.get(), ca, accent);
            }
            case SET_ZOOM -> {
                label(ctx, tr, cx, y, "Zoom FOV", "FOV while zooming", ca);
                int sx = sliderX(cx, rw);
                double f = zoomFraction();
                Draw.round(ctx, sx, cy - 2, SLIDER_W, 4, 2, Draw.alpha(Theme.OFF, ca));
                Draw.round(ctx, sx, cy - 2, (int) Math.round(f * SLIDER_W), 4, 2, Draw.alpha(accent, ca));
                int kx = sx + (int) Math.round(f * SLIDER_W);
                if (sliderDrag) {
                    Draw.circle(ctx, kx, cy, 8, Draw.alpha(accent, 0.25 * ca));
                }
                Draw.circle(ctx, kx, cy, 5, Draw.alpha(Theme.TEXT, ca));
                String value = cfg.zoomFov + "\u00b0";
                Draw.text(ctx, tr, value, sx - 10 - tr.getWidth(value), y + 11, Draw.alpha(Theme.TEXT, ca));
            }
            case SET_ALLOW -> {
                String addr = ServerLock.currentAddress(MinecraftClient.getInstance());
                int count = cfg.allowedServers.size();
                label(ctx, tr, cx, y, "Server Lock", count + " allowed server" + (count == 1 ? "" : "s"), ca);
                String text;
                int bg;
                if (addr == null) {
                    text = "Singleplayer";
                    bg = Theme.OFF;
                } else if (ServerLock.isListed(addr)) {
                    text = "Remove";
                    bg = Theme.OFF;
                } else {
                    text = "Allow here";
                    bg = accent;
                }
                int bw = 68;
                int bx = cx + rw - 12 - bw;
                Draw.round(ctx, bx, cy - 8, bw, 16, 8, Draw.alpha(bg, ca));
                Draw.textCenter(ctx, tr, text, bx + bw / 2, cy - 4, Draw.alpha(Theme.TEXT, ca));
            }
            default -> {
                boolean armed = resetArmed();
                label(ctx, tr, cx, y, "Reset Settings", "Restore all defaults", ca);
                String text = armed ? "Confirm?" : "Reset";
                int bw = 56;
                int bx = cx + rw - 12 - bw;
                int bg = armed ? 0xFFE11D48 : Theme.OFF;
                Draw.round(ctx, bx, cy - 8, bw, 16, 8, Draw.alpha(bg, ca));
                Draw.textCenter(ctx, tr, text, bx + bw / 2, cy - 4, Draw.alpha(Theme.TEXT, ca));
            }
        }
    }

    private void drawToggle(DrawContext ctx, int x, int y, double t, double a, int accent) {
        if (t > 0.05) {
            Draw.round(ctx, x - 2, y - 2, TOGGLE_W + 4, TOGGLE_H + 4, 9, Draw.alpha(accent, 0.18 * t * a));
        }
        Draw.round(ctx, x, y, TOGGLE_W, TOGGLE_H, 7, Draw.alpha(Draw.lerp(Theme.OFF, accent, t), a));
        int knobX = x + 2 + (int) Math.round(t * (TOGGLE_W - 4 - 10));
        Draw.circle(ctx, knobX + 5, y + TOGGLE_H / 2, 5, Draw.alpha(Theme.TEXT, a));
    }

    // ------------------------------------------------------------------ input

    private void selectTab(int i) {
        if (i == tab) {
            return;
        }
        tab = i;
        lastTab = i;
        slide.snap(1.0);
        slide.set(0.0);
        indicator.set(i * TAB_STEP);
        scroll.snap(0.0);
        scrollTarget = 0.0;
        for (Anim a : rowHover) {
            a.snap(0.0);
        }
    }

    private void applySlider(double mx) {
        int sx = sliderX(contentX(), rowW());
        double f = Math.max(0.0, Math.min(1.0, (mx - sx) / SLIDER_W));
        JaxClient.config().zoomFov =
                Config.MIN_ZOOM_FOV + (int) Math.round(f * (Config.MAX_ZOOM_FOV - Config.MIN_ZOOM_FOV));
    }

    private void clickRow(int i, double mx, double my) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Config cfg = JaxClient.config();
        if (tab != SETTINGS_TAB) {
            List<Feature> feats = Features.byCategory(CATEGORIES[tab]);
            if (i < feats.size()) {
                feats.get(i).toggle(mc);
            }
            return;
        }
        switch (i) {
            case SET_ACCENT -> {
                int cx = contentX();
                int sy = rowTop(i) + ROW_H / 2;
                for (int k = 0; k < Theme.ACCENTS.length; k++) {
                    double dx = mx - swatchX(cx, rowW(), k);
                    double dy = my - sy;
                    if (dx * dx + dy * dy <= 100.0) {
                        cfg.accent = k;
                        cfg.save();
                        return;
                    }
                }
            }
            case SET_HUD -> {
                cfg.hud = !cfg.hud;
                cfg.save();
            }
            case SET_NOTIF -> {
                cfg.notifications = !cfg.notifications;
                cfg.save();
            }
            case SET_ZOOM -> {
                int sx = sliderX(contentX(), rowW());
                if (in(mx, my, sx - 8, rowTop(i), SLIDER_W + 16, ROW_H)) {
                    sliderDrag = true;
                    applySlider(mx);
                }
            }
            case SET_ALLOW -> {
                String addr = ServerLock.currentAddress(mc);
                if (addr != null) {
                    if (ServerLock.isListed(addr)) {
                        cfg.allowedServers.remove(addr);
                    } else {
                        cfg.allowedServers.add(addr);
                    }
                    cfg.save();
                }
            }
            case SET_RESET -> {
                if (resetArmed()) {
                    cfg.resetDefaults();
                    Features.applyConfig(cfg);
                    cfg.save();
                    resetArmedUntil = 0L;
                } else {
                    resetArmedUntil = System.currentTimeMillis() + 3000L;
                }
            }
            default -> { }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) {
            return true;
        }
        if (button > 0 && JaxClient.menuBind != null && JaxClient.menuBind.matchesMouse(button)) {
            close();
            return true;
        }
        if (button == 0) {
            layout();
            if (in(mouseX, mouseY, px + pw - 26, py + 10, 16, 16)) {
                close();
                return true;
            }
            for (int i = 0; i < TAB_COUNT; i++) {
                if (in(mouseX, mouseY, px + 12, py + TAB_Y0 + i * TAB_STEP, SIDEBAR_W - 12, TAB_H)) {
                    selectTab(i);
                    return true;
                }
            }
            int n = rowCount();
            for (int i = 0; i < n; i++) {
                if (inRow(i, mouseX, mouseY)) {
                    clickRow(i, mouseX, mouseY);
                    return true;
                }
            }
            if (in(mouseX, mouseY, px, py, pw - 30, 40)) {
                dragging = true;
                dragDx = (int) mouseX - panelX;
                dragDy = (int) mouseY - panelY - oy();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        if (sliderDrag) {
            sliderDrag = false;
            JaxClient.config().save();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (sliderDrag) {
            applySlider(mouseX);
            return true;
        }
        if (dragging) {
            panelX = (int) mouseX - dragDx;
            panelY = (int) mouseY - dragDy - oy();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double content = rowCount() * (double) ROW_STEP;
        double view = ph - VIEW_TOP - 8;
        double max = Math.max(0.0, content - view);
        scrollTarget = Math.max(0.0, Math.min(max, scrollTarget - verticalAmount * 24.0));
        scroll.set(scrollTarget);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (JaxClient.menuBind != null && JaxClient.menuBind.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Starts the fade-out; the screen is removed in tick() once it has faded. */
    @Override
    public void close() {
        if (!closing) {
            closing = true;
            dragging = false;
            if (sliderDrag) {
                sliderDrag = false;
                JaxClient.config().save();
            }
            open.set(0.0);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (closing && open.get() <= 0.04 && client != null) {
            client.setScreen(null);
        }
    }
}
