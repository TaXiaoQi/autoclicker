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

    // === 自动攻击配置 ===
    public boolean autoAttackEnabled = false;
    public int attackInterval = 20;                // 基础间隔（ticks）
    public int attackRandomness = 5;               // 随机性范围（±）
    public boolean attackRandomnessEnabled = true; // 是否启用随机性
    public boolean attackArmorStands = true;       // 攻击盔甲架
    public boolean attackHostileMobs = true;       // 攻击敌对生物（怪物）
    public boolean attackNeutralMobs = false;      // 攻击中立生物（当前实际作用于被动生物）
    public boolean attackPassiveMobs = false;      // 攻击被动生物（预留，当前未使用）

    // === 自动放置配置 ===
    public boolean autoPlaceEnabled = false;
    public int placeInterval = 5;                 // 基础间隔（ticks）
    public int placeRandomness = 3;                // 随机性范围
    public boolean placeRandomnessEnabled = true;  // 是否启用随机性
    public boolean useBoneMeal = true;             // 是否自动使用骨粉

    // === 反检测设置 ===
    public boolean humanizeClicks = true;          // 人性化点击（随机跳过）

    // 保存配置到文件
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            AutoClicker.LOGGER.error("保存配置失败", e);
        }
    }

    // 从文件加载配置，并验证合理性
    public static Config load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                Config config = GSON.fromJson(json, Config.class);
                // 验证并修正非法值
                if (config.attackInterval < 1) config.attackInterval = 20;
                if (config.placeInterval < 1) config.placeInterval = 10;
                if (config.attackRandomness < 0) config.attackRandomness = 0;
                if (config.placeRandomness < 0) config.placeRandomness = 0;
                return config;
            }
        } catch (IOException e) {
            AutoClicker.LOGGER.error("加载配置失败", e);
        }
        // 首次运行：创建默认配置
        Config config = new Config();
        config.save();
        return config;
    }
}