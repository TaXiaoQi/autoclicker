package com.example.autoclicker.config;

import com.example.autoclicker.Main;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static Config config;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("autoclicker.json");

    public static Config getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                config = GSON.fromJson(json, Config.class);
                validateAndFix(config);
            } else {
                config = new Config();
                save();
            }
        } catch (IOException e) {
            Main.LOGGER.error("Failed to load config", e);
            config = new Config();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(getConfig()));
        } catch (IOException e) {
            Main.LOGGER.error("Failed to save config", e);
        }
    }

    private static void validateAndFix(Config cfg) {
        if (cfg.attackInterval < 1) cfg.attackInterval = 20;
        if (cfg.placeInterval < 1) cfg.placeInterval = 10;
        if (cfg.attackRandomness < 0) cfg.attackRandomness = 0;
        if (cfg.placeRandomness < 0) cfg.placeRandomness = 0;
        if (cfg.autoDisableTimeout < 1) cfg.autoDisableTimeout = 1;
        if (cfg.autoDisableTimeout > 240) cfg.autoDisableTimeout = 240;
    }
}