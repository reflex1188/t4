package dev.jax.jaxclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

final class Features {
    private Features() {}

    static final List<Feature> ALL = new ArrayList<>();

    static Feature fullbright;
    static Feature zoom;
    static Feature lowHealth;
    static Feature autoSprint;
    static Feature flight;
    static Feature speed;
    static Feature playerEsp;
    static Feature fps;
    static Feature ping;
    static Feature clock;
    static Feature memory;
    static Feature coords;
    static Feature keystrokes;
    static Feature keybindList;
    static Feature armorHud;
    static Feature potions;
    static Feature weather;
    static Feature biomeHud;
    static Feature directionHud;
    static Feature speedometer;
    static Feature worldTime;
    static Feature coordinatesDetails;

    private static final double SPEED = 0.32;

    private static double axis(boolean positive, boolean negative) {
        return (positive ? 1.0 : 0.0) - (negative ? 1.0 : 0.0);
    }

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

        zoom = add(new Feature("Zoom", "Hold the zoom key to zoom in",
                Feature.Category.RENDER, true, true) {
            @Override
            void onDisable(MinecraftClient mc) {
                Zoom.restore(mc);
            }
        });
        zoom.listInHud = false;

        lowHealth = add(new Feature("Low Health Warning", "Red pulse when health is low",
                Feature.Category.RENDER, true, true));
        lowHealth.listInHud = false;

        autoSprint = add(new Feature("AutoSprint", "Keeps you sprinting while moving forward",
                Feature.Category.MOVEMENT, false) {
            @Override
            void onTick(MinecraftClient mc) {
                GameOptions o = mc.options;
                if (o.forwardKey.isPressed() && !o.sneakKey.isPressed()) {
                    o.sprintKey.setPressed(true);
                }
            }

            @Override
            void onDisable(MinecraftClient mc) {
                mc.options.sprintKey.setPressed(false);
            }
        });

        flight = add(new Feature("Flight", "Fly with WASD, Space and Shift",
                Feature.Category.FRIENDS, false) {
            @Override
            void onTick(MinecraftClient mc) {
                ClientPlayerEntity p = mc.player;
                GameOptions o = mc.options;
                double s = JaxClient.config().flySpeed;
                double fwd = axis(o.forwardKey.isPressed(), o.backKey.isPressed());
                double side = axis(o.leftKey.isPressed(), o.rightKey.isPressed());
                double vert = axis(o.jumpKey.isPressed(), o.sneakKey.isPressed());
                double yaw = Math.toRadians(p.getYaw());
                double dx = -Math.sin(yaw) * fwd + Math.cos(yaw) * side;
                double dz = Math.cos(yaw) * fwd + Math.sin(yaw) * side;
                double len = Math.hypot(dx, dz);
                if (len > 1.0E-6) {
                    dx = dx / len * s;
                    dz = dz / len * s;
                } else {
                    dx = 0.0;
                    dz = 0.0;
                }
                p.setNoGravity(true);
                p.setVelocity(dx, vert * s, dz);
                p.fallDistance = 0;
            }

            @Override
            void onDisable(MinecraftClient mc) {
                if (mc.player != null) {
                    mc.player.setNoGravity(false);
                }
            }
        });
        flight.serverLocked = true;

        speed = add(new Feature("Speed", "Faster ground movement",
                Feature.Category.FRIENDS, false) {
            @Override
            void onTick(MinecraftClient mc) {
                ClientPlayerEntity p = mc.player;
                GameOptions o = mc.options;
                double fwd = axis(o.forwardKey.isPressed(), o.backKey.isPressed());
                double side = axis(o.leftKey.isPressed(), o.rightKey.isPressed());
                double yaw = Math.toRadians(p.getYaw());
                double dx = -Math.sin(yaw) * fwd + Math.cos(yaw) * side;
                double dz = Math.cos(yaw) * fwd + Math.sin(yaw) * side;
                double len = Math.hypot(dx, dz);
                if (len < 1.0E-6) {
                    return;
                }
                Vec3d v = p.getVelocity();
                p.setVelocity(dx / len * SPEED, v.y, dz / len * SPEED);
            }
        });
        speed.serverLocked = true;

        playerEsp = add(new Feature("Player ESP", "Glow outline on nearby players",
                Feature.Category.FRIENDS, false));
        playerEsp.serverLocked = true;

        fps = hud("FPS Counter", "Frames per second, bottom-right", true);
        ping = hud("Ping", "Server latency, bottom-right", true);
        clock = hud("Clock", "Local time, bottom-right", true);
        memory = hud("Memory", "RAM usage, bottom-right", false);
        coords = hud("Coordinates", "Your XYZ position, top-left", false);
        keystrokes = hud("Keystrokes", "WASD, mouse, space and CPS", true);
        keybindList = hud("Keybind List", "Hotkeys for each module", true);
        armorHud = hud("Armor HUD", "Armor and held item durability", true);
        potions = hud("Potion Effects", "Active effects with timers", true);

        biomeHud = hud("Biome", "Current biome name, top-left", false);
        directionHud = hud("Direction", "Compass direction and yaw", false);
        speedometer = hud("Speedometer", "Horizontal movement speed in blocks per second", false);
        worldTime = hud("World Time", "In-game world clock", false);
        coordinatesDetails = hud("Coordinate Details", "XYZ plus chunk coordinates", false);

        weather = add(new Feature("Weather Notifier", "Toast when the weather changes",
                Feature.Category.UTILITY, true, false) {
            private boolean primed;
            private boolean wasRain;
            private boolean wasThunder;

            @Override
            void onEnable(MinecraftClient mc) {
                primed = false;
            }

            @Override
            void onTick(MinecraftClient mc) {
                boolean rain = mc.world.isRaining();
                boolean thunder = mc.world.isThundering();
                if (!primed) {
                    primed = true;
                } else if (thunder && !wasThunder) {
                    Notifications.info("Weather", "Thunderstorm started", "!");
                } else if (!thunder && wasThunder) {
                    Notifications.info("Weather", "Thunderstorm ended", "\u2713");
                } else if (rain && !wasRain) {
                    Notifications.info("Weather", "Rain started", "!");
                } else if (!rain && wasRain) {
                    Notifications.info("Weather", "Weather cleared", "\u2713");
                }
                wasRain = rain;
                wasThunder = thunder;
            }
        });

        applyConfig(cfg);
    }

    private static Feature hud(String name, String description, boolean defaultOn) {
        return add(new Feature(name, description, Feature.Category.UTILITY, true, defaultOn));
    }

    private static Feature add(Feature f) {
        ALL.add(f);
        return f;
    }

    /** Applies saved on/off state (or each feature's default) to every persistent feature. */
    static void applyConfig(Config cfg) {
        for (Feature f : ALL) {
            if (f.persist) {
                Boolean saved = cfg.modules.get(f.name);
                f.restore(saved != null ? saved : f.defaultOn);
            }
        }
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
