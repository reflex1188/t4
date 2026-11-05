package dev.jax.jaxclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

final class Config {
    private static final Logger LOG = LoggerFactory.getLogger("JaxClient/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    int accent = 0;
    boolean hud = true;
    boolean notifications = true;
    Map<String, Boolean> modules = new HashMap<>();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("jaxclient.json");
    }

    static Config load() {
        Path p = path();
        if (Files.notExists(p)) {
            Config fresh = new Config();
            fresh.save();
            return fresh;
        }
        try (Reader reader = Files.newBufferedReader(p)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            Config loaded = GSON.fromJson(json, Config.class);
            if (loaded == null) {
                throw new IOException("config file is empty");
            }

            // Preserve defaults when loading older or partially written config files.
            if (!json.has("hud")) {
                loaded.hud = true;
            }
            if (!json.has("notifications")) {
                loaded.notifications = true;
            }
            if (loaded.modules == null) {
                loaded.modules = new HashMap<>();
            }
            if (loaded.accent < 0 || loaded.accent >= Theme.ACCENTS.length) {
                loaded.accent = 0;
            }
            return loaded;
        } catch (Exception e) {
            LOG.error("Config unreadable, using defaults: {}", e.toString());
            return new Config();
        }
    }

    void save() {
        Path destination = path();
        Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");
        try {
            Files.createDirectories(destination.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(this, writer);
            }
            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveUnavailable) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOG.error("Config write failed: {}", e.toString());
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupError) {
                LOG.warn("Could not remove temporary config: {}", cleanupError.toString());
            }
        }
    }
}
