package com.example.autoclicker.config;

public class Config {
    // === 快捷键配置 ===
    // 注意：MC 26.3 起编译类路径不再包含 org.lwjgl.glfw，故使用 GLFW 键码字面量
    // （GLFW_KEY_F7=118, F8=119, F9=120, F10=121）
    public int keyMute = 118;        // F7
    public int keyToggleAttack = 119; // F8
    public int keyTogglePlace = 120;  // F9
    public int keyOpenGUI = 121;      // F10

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
    // === 自动补充触发阈值 ===
    public int refillCountThreshold = 0;        // 数量低于此值触发补充，0表示用完才补充
    public int refillDurabilityThreshold = 1;   // 耐久剩余百分比低于此值触发补充（%）

    // 自动关闭功能开关
    public boolean autoDisableAttack = true;  // 自动攻击超时关闭
    public boolean autoDisablePlace = true;   // 自动放置超时关闭
    public int autoDisableTimeout = 120;       // 超时时间（秒），默认120秒

    // === 音频静音配置 ===
    public boolean muteOnAutoAttack = true;
    public boolean muteOnAutoPlace = true;
    public boolean muteWhenMinimized = true;
}