package dev.jax.jaxclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.util.ArrayList;
import java.util.List;

final class Features {
    private Features() {}

    static final List<Feature> ALL = new ArrayList<>();

    static Feature fullbright;
    static Feature autoSprint;
    static Feature fps;
    static Feature ping;
    static Feature clock;
    static Feature coords;
    static Feature keystrokes;
    static Feature keybindList;
    static Feature armorStatus;
    static Feature direction;

    static void init(Config cfg) {
        ALL.clear();

        fullbright = add(new Feature("Fullbright", "Maximum brightness in dark areas",
                Feature.Category.RENDER, false) {
            @Override
            void onTick(MinecraftClient mc) {
                StatusEffectInstance nv = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
                if (nv == null || nv.getDuration() < 400) {
                    mc.player.addStatusEffect(
                            new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1200, 0, false, false));
                }
            }

            @Override
            void onDisable(MinecraftClient mc) {
                if (mc.player != null) {
                    mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                }
            }
        });

        autoSprint = add(new Feature("AutoSprint", "Keeps you sprinting while moving forward",
                Feature.Category.MOVEMENT, false) {
            @Override
            void onTick(MinecraftClient mc) {
                GameOptions o = mc.options;
                // Recompute every tick so releasing forward or pressing sneak cannot leave sprint latched.
                o.sprintKey.setPressed(o.forwardKey.isPressed() && !o.sneakKey.isPressed());
            }

            @Override
            void onDisable(MinecraftClient mc) {
                mc.options.sprintKey.setPressed(false);
            }
        });

        fps = hud("FPS Counter", "Frames per second, bottom-right");
        ping = hud("Ping", "Server latency, bottom-right");
        clock = hud("Clock", "Local time, bottom-right");
        coords = hud("Coordinates", "Your XYZ position, top-left");
        keystrokes = hud("Keystrokes", "WASD, mouse and space overlay");
        keybindList = hud("Keybind List", "Hotkeys for each module");
        armorStatus = hud("Armor Status", "Armor durability and equipped items");
        direction = hud("Direction", "Compass direction indicator");

        restore(cfg, fps, true);
        restore(cfg, ping, true);
        restore(cfg, clock, true);
        restore(cfg, coords, false);
        restore(cfg, keystrokes, true);
        restore(cfg, keybindList, true);
        restore(cfg, armorStatus, true);
        restore(cfg, direction, true);
    }

    private static Feature hud(String name, String description) {
        return add(new Feature(name, description, Feature.Category.UTILITY, true));
    }

    private static Feature add(Feature f) {
        ALL.add(f);
        return f;
    }

    private static void restore(Config cfg, Feature f, boolean defaultOn) {
        Boolean saved = cfg.modules.get(f.name);
        f.restore(saved != null ? saved : defaultOn);
    }

    static List<Feature> byCategory(Feature.Category category) {
        List<Feature> out = new ArrayList<>();
        for (Feature f : ALL) {
            if (f.category == category) {
                out.add(f);
            }
        }
        return out;
    }

    static void tick(MinecraftClient mc) {
        for (Feature f : ALL) {
            f.tick(mc);
        }
    }

    static void resetAll(MinecraftClient mc) {
        for (Feature f : ALL) {
            f.reset(mc);
        }
    }

    static void updateAnims(double dt) {
        for (Feature f : ALL) {
            f.anim.update(dt);
        }
    }
}
