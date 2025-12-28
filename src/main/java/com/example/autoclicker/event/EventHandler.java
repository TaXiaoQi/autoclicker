package com.example.autoclicker.event;

import com.example.autoclicker.feature.AutoClicker;
import com.example.autoclicker.feature.MuteFeature;
import com.example.autoclicker.config.ConfigManager;
import com.example.autoclicker.gui.ConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.resources.ResourceLocation;

public class EventHandler {
    private static final AutoClicker autoClick = new AutoClicker();
    private static final MuteFeature audioMute = new MuteFeature();

    // 快捷键定义
    private static KeyMapping muteKey;
    private static KeyMapping toggleAttackKey;
    private static KeyMapping togglePlaceKey;
    private static KeyMapping openGUIKey;

    // 自定义分类
    private static KeyMapping.Category AUTO_CLICKER_CATEGORY;

    public static void register() {
        // 注册自定义分类
        registerCustomCategory();

        // 从配置加载按键设置
        loadKeyBindingsFromConfig();

        // 注册所有快捷键
        registerKeyBindings();

        // 注册事件监听器
        registerEvents();
    }

    private static void registerCustomCategory() {
        // 注册自定义分类
        AUTO_CLICKER_CATEGORY = KeyMapping.Category.register(
                ResourceLocation.fromNamespaceAndPath("autoclicker", "category")
        );
    }

    private static void loadKeyBindingsFromConfig() {
        var config = ConfigManager.getConfig();

        // 使用自定义分类
        muteKey = new KeyMapping(
                "key.autoclicker.mute",
                InputConstants.Type.KEYSYM,
                config.keyMute,
                AUTO_CLICKER_CATEGORY  // 使用自定义分类
        );

        toggleAttackKey = new KeyMapping(
                "key.autoclicker.toggle_attack",
                InputConstants.Type.KEYSYM,
                config.keyToggleAttack,
                AUTO_CLICKER_CATEGORY  // 使用自定义分类
        );

        togglePlaceKey = new KeyMapping(
                "key.autoclicker.toggle_place",
                InputConstants.Type.KEYSYM,
                config.keyTogglePlace,
                AUTO_CLICKER_CATEGORY  // 使用自定义分类
        );

        openGUIKey = new KeyMapping(
                "key.autoclicker.open_gui",
                InputConstants.Type.KEYSYM,
                config.keyOpenGUI,
                AUTO_CLICKER_CATEGORY  // 使用自定义分类
        );
    }

    private static void registerKeyBindings() {
        KeyBindingHelper.registerKeyBinding(muteKey);
        KeyBindingHelper.registerKeyBinding(toggleAttackKey);
        KeyBindingHelper.registerKeyBinding(togglePlaceKey);
        KeyBindingHelper.registerKeyBinding(openGUIKey);
    }

    private static void registerEvents() {
        // 主游戏循环
        ClientTickEvents.END_CLIENT_TICK.register(EventHandler::onClientTick);

        // 窗口焦点检测
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                // 检测窗口是否活动
                boolean windowActive = client.isWindowActive();
                audioMute.updateMinimizedMute(!windowActive);
            }
        });

        // 暂停菜单事件
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof PauseScreen) {
                handlePauseMenu();
            }
        });

        // 退出游戏事件
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> handleGameExit());
    }

    private static void onClientTick(Minecraft client) {
        if (client.player == null) return;

        // 处理快捷键输入
        handleKeyBindings();

        // 执行自动点击功能
        autoClick.tick(client);

        // 更新音频静音状态（基于配置）
        updateAudioMuteStatus();
    }

    private static void handleKeyBindings() {
        // F7: 一键静音/恢复（独立于其他功能）
        if (muteKey.consumeClick()) {
            handleMuteKey();
        }

        // F8: 一键开关自动攻击（根据配置同步静音）
        if (toggleAttackKey.consumeClick()) {
            handleToggleAttackKey();
        }

        // F9: 一键开关自动放置（根据配置同步静音）
        if (togglePlaceKey.consumeClick()) {
            handleTogglePlaceKey();
        }

        // F10: 打开配置GUI
        if (openGUIKey.consumeClick()) {
            handleOpenGUIKey();
        }
    }

    private static void handleMuteKey() {
        // 切换全局静音状态
        audioMute.toggleManualMute();

        // F7静音时，如果配置要求同步，则关闭自动功能
        var config = ConfigManager.getConfig();
        if (config.muteOnAutoAttack) {
            config.autoAttackEnabled = false;
        }
        if (config.muteOnAutoPlace) {
            config.autoPlaceEnabled = false;
        }
        ConfigManager.save();
    }

    private static void handleToggleAttackKey() {
        var config = ConfigManager.getConfig();
        boolean newState = !config.autoAttackEnabled;

        // 更新攻击开关状态
        config.autoAttackEnabled = newState;

        // 如果配置要求静音，同步音频状态
        if (config.muteOnAutoAttack) {
            if (newState) {
                // 开启攻击时静音
                audioMute.setMutedByAutoFeature(true);
            } else {
                // 关闭攻击时，如果没有其他自动功能，恢复音频
                if (!config.autoPlaceEnabled) {
                    audioMute.setMutedByAutoFeature(false);
                }
            }
        }

        // 重置计时器
        autoClick.resetAttackTimer();

        ConfigManager.save();
    }

    private static void handleTogglePlaceKey() {
        var config = ConfigManager.getConfig();
        boolean newState = !config.autoPlaceEnabled;

        // 更新放置开关状态
        config.autoPlaceEnabled = newState;

        // 如果配置要求静音，同步音频状态
        if (config.muteOnAutoPlace) {
            if (newState) {
                // 开启放置时静音
                audioMute.setMutedByAutoFeature(true);
            } else {
                // 关闭放置时，如果没有其他自动功能，恢复音频
                if (!config.autoAttackEnabled) {
                    audioMute.setMutedByAutoFeature(false);
                }
            }
        }

        // 重置计时器
        autoClick.resetPlaceTimer();

        ConfigManager.save();
    }

    private static void handleOpenGUIKey() {
        Minecraft client = Minecraft.getInstance();
        if (client.screen == null) {
            client.setScreen(new ConfigScreen(null));
        }
    }

    private static void updateAudioMuteStatus() {
        var config = ConfigManager.getConfig();

        // 只有当不是手动静音时才更新自动功能静音
        if (!audioMute.isManuallyMuted()) {
            boolean shouldMuteByAttack = config.autoAttackEnabled && config.muteOnAutoAttack;
            boolean shouldMuteByPlace = config.autoPlaceEnabled && config.muteOnAutoPlace;

            audioMute.updateAutoFeatureMute(shouldMuteByAttack || shouldMuteByPlace);
        }
    }

    private static void handlePauseMenu() {
        // 暂停游戏时恢复音频（如果是自动功能触发的静音）
        if (!audioMute.isManuallyMuted()) {
            audioMute.forceRestore();
        }
    }

    private static void handleGameExit() {
        // 退出游戏前恢复音频
        audioMute.forceRestore();
        // 保存配置
        ConfigManager.save();
    }
}