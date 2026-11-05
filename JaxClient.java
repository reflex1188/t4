package dev.jax.jaxclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JaxClient implements ClientModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("JaxClient");

    static KeyBinding menuBind;
    static KeyBinding zoomBind;
    private static KeyBinding fullbrightBind;
    private static KeyBinding sprintBind;
    private static KeyBinding flightBind;
    private static KeyBinding espBind;

    private static Config config = new Config();
    private static long lastFrameNanos;
    private static double fps = 60.0;

    static Config config() {
        return config;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onInitializeClient() {
        config = Config.load();
        Features.init(config);

        menuBind = bind("menu", GLFW.GLFW_KEY_RIGHT_SHIFT);
        fullbrightBind = bind("fullbright", GLFW.GLFW_KEY_B);
        sprintBind = bind("autosprint", GLFW.GLFW_KEY_V);
        zoomBind = bind("zoom", GLFW.GLFW_KEY_C);
        flightBind = bind("flight", GLFW.GLFW_KEY_G);
        espBind = bind("playeresp", GLFW.GLFW_KEY_H);
        Features.fullbright.keyBind = fullbrightBind;
        Features.autoSprint.keyBind = sprintBind;
        Features.zoom.keyBind = zoomBind;
        Features.flight.keyBind = flightBind;
        Features.playerEsp.keyBind = espBind;

        ClientTickEvents.END_CLIENT_TICK.register(JaxClient::tick);
        HudRenderCallback.EVENT.register(JaxClient::onHud);
        ClientLifecycleEvents.CLIENT_STOPPING.register(Zoom::restore);
        LOG.info("JaxClient loaded: {} features, accent={}", Features.ALL.size(), config.accent);
    }

    private static KeyBinding bind(String id, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.jaxclient." + id, InputUtil.Type.KEYSYM, key, "key.categories.jaxclient"));
    }

    private static void tick(MinecraftClient mc) {
        boolean inWorld = mc.player != null && mc.world != null;
        if (!inWorld) {
            Features.resetAll(mc);
            Zoom.restore(mc);
            while (menuBind.wasPressed()) { }
            while (fullbrightBind.wasPressed()) { }
            while (sprintBind.wasPressed()) { }
            while (zoomBind.wasPressed()) { }
            while (flightBind.wasPressed()) { }
            while (espBind.wasPressed()) { }
            return;
        }

        while (menuBind.wasPressed()) {
            if (mc.currentScreen == null) {
                mc.setScreen(new JaxMenu());
            }
        }
        while (fullbrightBind.wasPressed()) {
            Features.fullbright.toggle(mc);
        }
        while (sprintBind.wasPressed()) {
            Features.autoSprint.toggle(mc);
        }
        while (zoomBind.wasPressed()) {
            // zoom is hold-to-use and read per frame; this only drains queued presses
        }
        while (flightBind.wasPressed()) {
            Features.flight.toggle(mc);
        }
        while (espBind.wasPressed()) {
            Features.playerEsp.toggle(mc);
        }
        Features.tick(mc);
    }

    private static void onHud(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();

        long now = System.nanoTime();
        double dt = lastFrameNanos == 0L ? 0.016 : Math.min(0.1, (now - lastFrameNanos) / 1.0e9);
        lastFrameNanos = now;
        dt = Math.max(dt, 0.0005);
        fps += ((1.0 / dt) - fps) * 0.05;

        Features.updateAnims(dt);
        Notifications.update(dt);
        Zoom.frame(mc, dt);

        if (mc.player == null || mc.options.hudHidden) {
            return;
        }
        Hud.render(ctx, mc, fps, dt);
        Notifications.render(ctx, mc);
    }

    /** Called from EntityMixin on every isGlowing() check. */
    public static boolean wantsGlow(Entity entity) {
        Feature esp = Features.playerEsp;
        if (esp == null || !esp.isEnabled() || !(entity instanceof PlayerEntity)) {
            return false;
        }
        if (!entity.getWorld().isClient) {
            return false;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || entity == mc.player || entity.getWorld() != mc.world) {
            return false;
        }
        double range = config.espRange;
        return mc.player.squaredDistanceTo(entity) <= range * range;
    }
}
