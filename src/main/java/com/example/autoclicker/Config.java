package com.example.autoclicker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("autoclicker.json");

    // 自动攻击配置
    public boolean autoAttackEnabled = false;
    public int attackInterval = 20;
    public int attackRandomness = 5; // 攻击随机性范围
    public boolean attackRandomnessEnabled = true; // 是否启用攻击随机性
    public boolean attackArmorStands = true;
    public boolean attackHostileMobs = true;
    public boolean attackNeutralMobs = false;
    public boolean attackPassiveMobs = false;

    // 自动放置配置
    public boolean autoPlaceEnabled = false;
    public int placeInterval = 10;
    public int placeRandomness = 3; // 放置随机性范围
    public boolean placeRandomnessEnabled = true; // 是否启用放置随机性
    public boolean useBoneMeal = true; // 是否自动使用骨粉
    public boolean avoidInteractableBlocks = true; // 是否避开可交互方块

    // 反检测设置
    public boolean humanizeClicks = true; // 人性化点击

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            AutoClicker.LOGGER.error("保存配置失败", e);
        }
    }

    public static Config load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                Config config = GSON.fromJson(json, Config.class);
                // 验证配置值
                if (config.attackInterval < 1) config.attackInterval = 20;
                if (config.placeInterval < 1) config.placeInterval = 10;
                if (config.attackRandomness < 0) config.attackRandomness = 0;
                if (config.placeRandomness < 0) config.placeRandomness = 0;
                return config;
            }
        } catch (IOException e) {
            AutoClicker.LOGGER.error("加载配置失败", e);
        }
        Config config = new Config();
        config.save();
        return config;
    }
}