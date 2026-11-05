package dev.jax.jaxclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JaxClient implements ClientModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("JaxClient");

    static KeyBinding menuBind;
    private static KeyBinding fullbrightBind;
    private static KeyBinding sprintBind;

    private static Config config = new Config();
    private static long lastFrameNanos;
    private static double fps = 60.0;

    static Config config() {
        return config;
    }

    @Override
    public void onInitializeClient() {
        config = Config.load();
        Features.init(config);

        menuBind = bind("menu", GLFW.GLFW_KEY_RIGHT_SHIFT);
        fullbrightBind = bind("fullbright", GLFW.GLFW_KEY_B);
        sprintBind = bind("autosprint", GLFW.GLFW_KEY_V);
        Features.fullbright.keyBind = fullbrightBind;
        Features.autoSprint.keyBind = sprintBind;

        ClientTickEvents.END_CLIENT_TICK.register(JaxClient::tick);
        HudRenderCallback.EVENT.register(JaxClient::onHud);
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
            while (menuBind.wasPressed()) { }
            while (fullbrightBind.wasPressed()) { }
            while (sprintBind.wasPressed()) { }
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

        if (mc.player == null || mc.options.hudHidden) {
            return;
        }
        Hud.render(ctx, mc, fps, dt);
        Notifications.render(ctx, mc);
    }
}
