package dev.jax.jaxclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class Config {
    static final int MIN_ZOOM_FOV = 30;
    static final int MAX_ZOOM_FOV = 60;

    private static final Logger LOG = LoggerFactory.getLogger("JaxClient/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    int accent = 0;
    boolean hud = true;
    boolean notifications = true;
    int zoomFov = MIN_ZOOM_FOV;
    double flySpeed = 0.5;
    double espRange = 128.0;
    List<String> allowedServers = new ArrayList<>();
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
            Config loaded = GSON.fromJson(reader, Config.class);
            if (loaded == null) {
                throw new IOException("config file is empty");
            }
            loaded.sanitize();
            return loaded;
        } catch (Exception e) {
            LOG.error("Config unreadable, using defaults: {}", e.toString());
            return new Config();
        }
    }

    private void sanitize() {
        if (modules == null) {
            modules = new HashMap<>();
        }
        if (accent < 0 || accent >= Theme.ACCENTS.length) {
            accent = 0;
        }
        zoomFov = Math.max(MIN_ZOOM_FOV, Math.min(MAX_ZOOM_FOV, zoomFov));
        if (allowedServers == null) {
            allowedServers = new ArrayList<>();
        }
        if (Double.isNaN(flySpeed) || flySpeed < 0.05 || flySpeed > 3.0) {
            flySpeed = 0.5;
        }
        if (Double.isNaN(espRange) || espRange < 8.0 || espRange > 512.0) {
            espRange = 128.0;
        }
    }

    void resetDefaults() {
        accent = 0;
        hud = true;
        notifications = true;
        zoomFov = MIN_ZOOM_FOV;
        flySpeed = 0.5;
        espRange = 128.0;
        allowedServers.clear();
        modules.clear();
    }

    void save() {
        try (Writer writer = Files.newBufferedWriter(path())) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            LOG.error("Config write failed: {}", e.toString());
        }
    }
}
