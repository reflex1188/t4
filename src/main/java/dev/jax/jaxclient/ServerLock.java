package dev.jax.jaxclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.util.Locale;

/**
 * Friends-tab features only run in singleplayer or on servers the user has explicitly allowed.
 * This is a guard rail against accidental use, not a security boundary: the list lives in
 * the user's own config file.
 */
final class ServerLock {
    private ServerLock() {}

    static String normalize(String address) {
        return address == null ? "" : address.trim().toLowerCase(Locale.ROOT);
    }

    /** The address of the multiplayer server we are on, or null in singleplayer / no server. */
    static String currentAddress(MinecraftClient mc) {
        if (mc == null || mc.isInSingleplayer()) {
            return null;
        }
        ServerInfo info = mc.getCurrentServerEntry();
        if (info == null || info.address == null || info.address.isBlank()) {
            return null;
        }
        return normalize(info.address);
    }

    static boolean isListed(String normalizedAddress) {
        return normalizedAddress != null && JaxClient.config().allowedServers.contains(normalizedAddress);
    }

    static boolean allowed(MinecraftClient mc) {
        if (mc != null && mc.isInSingleplayer()) {
            return true;
        }
        return isListed(currentAddress(mc));
    }
}
