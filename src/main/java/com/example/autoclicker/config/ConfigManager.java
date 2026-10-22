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
                // Java 8 兼容：readString 是 Java 11 API，改用 readAllBytes
                String json = new String(Files.readAllBytes(CONFIG_PATH), java.nio.charset.StandardCharsets.UTF_8);
                config = GSON.fromJson(json, Config.class);
            } else {
                config = new Config();
                save();
            }
        } catch (IOException | com.google.gson.JsonParseException e) {
            // 配置文件损坏时回退到默认配置，而不是让游戏崩溃
            Main.LOGGER.error("Failed to load config, falling back to defaults", e);
            config = new Config();
        }
        if (config == null) {
            config = new Config();
        }
        validateAndFix(config);
    }

    /**
     * 生成当前配置的深拷贝快照（供 GUI「取消」回滚使用）
     */
    public static Config snapshot() {
        return GSON.fromJson(GSON.toJsonTree(getConfig()), Config.class);
    }

    /**
     * 用快照恢复内存中的当前配置（不落盘）
     */
    public static void restore(Config snapshot) {
        if (snapshot != null) {
            config = snapshot;
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            // Java 8 兼容：writeString 是 Java 11 API，改用 write
            Files.write(CONFIG_PATH, GSON.toJson(getConfig()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException e) {
            Main.LOGGER.error("Failed to save config", e);
        }
    }

    private static void validateAndFix(Config cfg) {
        if (cfg.attackInterval < 1) cfg.attackInterval = 20;
        if (cfg.placeInterval < 1) cfg.placeInterval = 5;
        if (cfg.attackRandomness < 0) cfg.attackRandomness = 0;
        if (cfg.placeRandomness < 0) cfg.placeRandomness = 0;
        if (cfg.autoDisableTimeout < 1) cfg.autoDisableTimeout = 1;

        // 自动关闭时间验证
        if (cfg.autoDisableTimeout > 240) cfg.autoDisableTimeout = 240;

        // 补充数量阈值的验证！
        if (cfg.refillCountThreshold < 0) cfg.refillCountThreshold = 0;
        if (cfg.refillCountThreshold > 63) cfg.refillCountThreshold = 63;

        // 补充耐久阈值的验证！
        if (cfg.refillDurabilityThreshold < 0) cfg.refillDurabilityThreshold = 0;
        if (cfg.refillDurabilityThreshold > 100) cfg.refillDurabilityThreshold = 100;
    }
}