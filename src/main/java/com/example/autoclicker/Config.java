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

    public int attackInterval = 20; // 默认攻击间隔（ticks）
    public boolean attackArmorStands = true; // 是否攻击盔甲架
    public boolean attackHostileMobs = true; // 是否攻击敌对生物
    public boolean attackNeutralMobs = false; // 是否攻击中立生物
    public boolean attackPassiveMobs = false; // 是否攻击被动生物
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            AutoClicker.LOGGER.error("保存配置失败", e);
        }
    }

    // 从文件加载配置
    public static Config load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                return GSON.fromJson(json, Config.class);
            }
        } catch (IOException e) {
            AutoClicker.LOGGER.error("加载配置失败", e);
        }
        // 如果文件不存在或加载失败，返回默认配置
        Config config = new Config();
        config.save(); // 保存默认配置
        return config;
    }
}
