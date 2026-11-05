package dev.jax.jaxclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A toggleable client feature. Subclasses override the hooks they need. */
class Feature {
    enum Category {
        RENDER("Render"),
        MOVEMENT("Movement"),
        UTILITY("Utility");

        final String label;

        Category(String label) {
            this.label = label;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger("JaxClient/Feature");

    final String name;
    final String description;
    final Category category;
    final boolean persist;
    final Anim anim = new Anim(0.0, 14.0);
    KeyBinding keyBind;
    private boolean enabled;

    Feature(String name, String description, Category category, boolean persist) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.persist = persist;
    }

    final boolean isEnabled() {
        return enabled;
    }

    /** Sets state without hooks or notifications (used when loading config). */
    final void restore(boolean on) {
        enabled = on;
        anim.snap(on ? 1.0 : 0.0);
    }

    final void toggle(MinecraftClient mc) {
        setEnabled(mc, !enabled);
    }

    final void setEnabled(MinecraftClient mc, boolean on) {
        if (on == enabled) {
            return;
        }
        enabled = on;
        try {
            if (on) {
                onEnable(mc);
            } else {
                onDisable(mc);
            }
        } catch (RuntimeException e) {
            LOG.error("{} {} failed", name, on ? "enable" : "disable", e);
            enabled = false;
        }
        anim.set(enabled ? 1.0 : 0.0);
        Notifications.push(name, enabled);
        if (persist) {
            Config cfg = JaxClient.config();
            cfg.modules.put(name, enabled);
            cfg.save();
        }
    }

    /** Silent shutdown when leaving a world. Persistent (HUD) features keep their state. */
    final void reset(MinecraftClient mc) {
        if (persist || !enabled) {
            return;
        }
        enabled = false;
        try {
            onDisable(mc);
        } catch (RuntimeException e) {
            LOG.error("{} reset failed", name, e);
        }
        anim.snap(0.0);
    }

    final void tick(MinecraftClient mc) {
        if (!enabled) {
            return;
        }
        try {
            onTick(mc);
        } catch (RuntimeException e) {
            LOG.error("{} tick failed, disabling", name, e);
            setEnabled(mc, false);
        }
    }

    void onEnable(MinecraftClient mc) {}

    void onDisable(MinecraftClient mc) {}

    void onTick(MinecraftClient mc) {}
}
