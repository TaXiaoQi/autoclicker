package com.example.autoclicker.config;

import org.lwjgl.glfw.GLFW;

public class Config {
    // === 快捷键配置 ===
    public int keyMute = GLFW.GLFW_KEY_F7;
    public int keyToggleAttack = GLFW.GLFW_KEY_F8;
    public int keyTogglePlace = GLFW.GLFW_KEY_F9;
    public int keyOpenGUI = GLFW.GLFW_KEY_F10;

    // === 自动攻击配置 ===
    public boolean autoAttackEnabled = false;
    public int attackInterval = 20;
    public int attackRandomness = 5;
    public boolean attackRandomnessEnabled = true;
    public boolean attackArmorStands = true;
    public boolean attackHostileMobs = true;
    public boolean attackNeutralMobs = false;

    // === 自动放置配置 ===
    public boolean autoPlaceEnabled = false;
    public int placeInterval = 5;
    public int placeRandomness = 3;
    public boolean placeRandomnessEnabled = true;
    public boolean useBoneMeal = true;

    // === 自动补充配置 ===
    public boolean autoRefillMainHand = true;  // 主手补充开关
    public boolean autoRefillOffHand = true;   // 副手补充开关

    // === 音频静音配置 ===
    public boolean muteOnAutoAttack = true;
    public boolean muteOnAutoPlace = true;
    public boolean muteWhenMinimized = true;

    // === 反检测设置 ===
    public boolean humanizeClicks = true;
}