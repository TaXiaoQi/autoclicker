package com.example.autoclicker.event;

import com.example.autoclicker.Main;
import com.example.autoclicker.config.ConfigManager;
import com.example.autoclicker.feature.AutoClicker;
import com.example.autoclicker.feature.MuteFeature;
import com.example.autoclicker.tool.ConfigScreenImpl;
import com.example.autoclicker.tool.KeyBindLoader;
import com.example.autoclicker.tool.KeyBindLoaderImpl;
import com.example.autoclicker.tool.ScreenHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class EventHandler {
    private static final AutoClicker autoClick = new AutoClicker();
    private static final MuteFeature audioMute = new MuteFeature();

    // 按键加载器 - 不同版本有不同的实现
    private static final KeyBindLoader keyBindLoader = new KeyBindLoaderImpl();

    // 快捷键定义
    private static KeyMapping muteKey;
    private static KeyMapping toggleAttackKey;
    private static KeyMapping togglePlaceKey;
    private static KeyMapping openGUIKey;

    private static boolean wasInGame = false;

    // 游戏退出流程标志：退出阶段窗口会被 Dynamic FPS 等模组 unfocus，
    // 此时触发的失焦静音来不及恢复，会造成音量残留偏差（切窗音量降低问题）
    private static boolean isStopping = false;

    public static void register() {
        loadKeyBindingsFromConfig();
        registerKeyBindings();
        registerEvents();
    }

    private static void loadKeyBindingsFromConfig() {
        com.example.autoclicker.config.Config config = ConfigManager.getConfig();

        muteKey = keyBindLoader.createKeyBind(
                "key.autoclicker.mute",
                config.keyMute
        );

        toggleAttackKey = keyBindLoader.createKeyBind(
                "key.autoclicker.toggle_attack",
                config.keyToggleAttack
        );

        togglePlaceKey = keyBindLoader.createKeyBind(
                "key.autoclicker.toggle_place",
                config.keyTogglePlace
        );

        openGUIKey = keyBindLoader.createKeyBind(
                "key.autoclicker.open_gui",
                config.keyOpenGUI
        );
    }

    private static void registerKeyBindings() {
        // 使用加载器注册按键
        keyBindLoader.registerKeyBind(muteKey);
        keyBindLoader.registerKeyBind(toggleAttackKey);
        keyBindLoader.registerKeyBind(togglePlaceKey);
        keyBindLoader.registerKeyBind(openGUIKey);
    }

    private static void registerEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(EventHandler::onClientTick);

        // 窗口焦点检测（最小化/失去焦点时静音）
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (isStopping) return; // 退出流程中不再触发失焦静音
                boolean windowActive = client.isWindowActive();
                audioMute.updateMinimizedMute(!windowActive);

        });

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> audioMute.recoverSavedVolume());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            isStopping = true;
            handleGameExit();
        });
        // lambda 形式避免引用 handler 参数类型（1.16.5 为 ClientPlayNetHandler，1.17+ 为 ClientPacketListener）
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> handleLeaveGame());
    }

    // 断开连接时调用
    private static void onClientTick(Minecraft client) {
        if (wasInGame && client.player == null) {
            handleLeaveGame();
            wasInGame = false;
        }
        if (client.player == null) return;
        handleKeyBindings();
        autoClick.tick(client);
        audioMute.checkUserVolumeChange();
        wasInGame = true;
    }

    // 离开单人游戏时
    private static void handleLeaveGame() {
        // 无论自动化开关状态如何都尝试恢复静音（例如超时自动关闭后音量未恢复的残留情况）
        audioMute.forceRestore();
        com.example.autoclicker.config.Config config = ConfigManager.getConfig();
        if (config.autoAttackEnabled || config.autoPlaceEnabled) {
            autoClick.resetAutomationOnDisconnect();
        }
    }

    private static void handleKeyBindings() {
        if (muteKey.consumeClick()) {
            handleMuteKey();
        }
        if (toggleAttackKey.consumeClick()) {
            handleToggleAttackKey();
        }
        if (togglePlaceKey.consumeClick()) {
            handleTogglePlaceKey();
        }
        if (openGUIKey.consumeClick()) {
            handleOpenGUIKey();
        }
    }

    // 自动静音
    private static void handleMuteKey() {
        audioMute.toggleManualMute();
        ConfigManager.save();
        if (audioMute.isManuallyMuted()) {
            Main.sendMessage("autoclicker.message.muted");
        } else {
            Main.sendMessage("autoclicker.message.unmuted");
        }
    }

    // 自动攻击
    private static void handleToggleAttackKey() {
        com.example.autoclicker.config.Config config = ConfigManager.getConfig();
        boolean newState = !config.autoAttackEnabled;
        config.autoAttackEnabled = newState;

        if (config.muteOnAutoAttack) {
            if (newState) {
                audioMute.requestAttackMute();
            } else {
                audioMute.releaseAttackMute();
            }
        }

        autoClick.resetAttackTimer();
        ConfigManager.save();

        String status = newState ? "gui.autoclicker.enabled" : "gui.autoclicker.disabled";

        // 与实际随机范围一致：[interval - randomness, interval + randomness - 1]
        String intervalDisplay;
        if (newState && config.attackRandomnessEnabled && config.attackRandomness > 0) {
            int min = Math.max(1, config.attackInterval - config.attackRandomness);
            int max = config.attackInterval + config.attackRandomness - 1;
            intervalDisplay = min + "~" + max;
        } else {
            intervalDisplay = String.valueOf(config.attackInterval);
        }

        Main.sendMessage("autoclicker.message.attack_toggle",
                com.example.autoclicker.tool.TextCompat.translatable(status),
                intervalDisplay
        );
    }

    // 自动放置
    private static void handleTogglePlaceKey() {
        com.example.autoclicker.config.Config config = ConfigManager.getConfig();
        boolean newState = !config.autoPlaceEnabled;
        config.autoPlaceEnabled = newState;

        if (config.muteOnAutoPlace) {
            if (newState) {
                audioMute.requestPlaceMute();
            } else {
                audioMute.releasePlaceMute();
            }
        }

        autoClick.resetPlaceTimer();
        ConfigManager.save();

        String status = newState ? "gui.autoclicker.enabled" : "gui.autoclicker.disabled";
        String boneMealStatus = config.useBoneMeal
                ? "autoclicker.bonemeal.included"
                : "autoclicker.bonemeal.excluded";

        // 与实际随机范围一致：[interval - randomness, interval + randomness - 1]
        String intervalDisplay;
        if (newState && config.placeRandomnessEnabled && config.placeRandomness > 0) {
            int min = Math.max(1, config.placeInterval - config.placeRandomness);
            int max = config.placeInterval + config.placeRandomness - 1;
            intervalDisplay = min + "~" + max;
        } else {
            intervalDisplay = String.valueOf(config.placeInterval);
        }

        Main.sendMessage("autoclicker.message.place_toggle",
                com.example.autoclicker.tool.TextCompat.translatable(status),
                intervalDisplay,
                com.example.autoclicker.tool.TextCompat.translatable(boneMealStatus)
        );
    }


    private static void handleOpenGUIKey() {
        Minecraft client = Minecraft.getInstance();
        if (ScreenHelper.current(client) == null) {
            ScreenHelper.open(client, new ConfigScreenImpl(null));
        }
    }

    // 游戏完全退出时，恢复所有静音（包括手动）
    private static void handleGameExit() {
        audioMute.forceRestoreAll();
        ConfigManager.save();
    }
}