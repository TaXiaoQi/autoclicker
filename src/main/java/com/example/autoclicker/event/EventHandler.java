package com.example.autoclicker.event;

import com.example.autoclicker.Main;
import com.example.autoclicker.config.ConfigManager;
import com.example.autoclicker.feature.AutoClicker;
import com.example.autoclicker.feature.MuteFeature;
import com.example.autoclicker.gui.ConfigScreen;
import com.example.autoclicker.toor.KeyBindLoader;
import com.example.autoclicker.toor.KeyBindLoaderImpl;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
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

    public static void register() {
        loadKeyBindingsFromConfig();
        registerKeyBindings();
        registerEvents();
    }

    private static void loadKeyBindingsFromConfig() {
        var config = ConfigManager.getConfig();

        // 使用加载器创建按键，不再需要手动传入分类
        muteKey = keyBindLoader.createKeyBind(
                "key.autoclicker.mute",
                InputConstants.Type.KEYSYM,
                config.keyMute
        );

        toggleAttackKey = keyBindLoader.createKeyBind(
                "key.autoclicker.toggle_attack",
                InputConstants.Type.KEYSYM,
                config.keyToggleAttack
        );

        togglePlaceKey = keyBindLoader.createKeyBind(
                "key.autoclicker.toggle_place",
                InputConstants.Type.KEYSYM,
                config.keyTogglePlace
        );

        openGUIKey = keyBindLoader.createKeyBind(
                "key.autoclicker.open_gui",
                InputConstants.Type.KEYSYM,
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
                boolean windowActive = client.isWindowActive();
                audioMute.updateMinimizedMute(!windowActive);

        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> handleGameExit());
        ClientPlayConnectionEvents.DISCONNECT.register(EventHandler::onDisconnect);
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

    // 离开多人游戏时
    private static void onDisconnect(ClientPacketListener handler, Minecraft client) {
        handleLeaveGame();
    }

    // 离开单人游戏时
    private static void handleLeaveGame() {
        var config = ConfigManager.getConfig();
        if (!config.autoAttackEnabled && !config.autoPlaceEnabled) {
            // 自动化已经关闭，不需要重复处理
            return;
        }
        autoClick.resetAutomationOnDisconnect();
        audioMute.forceRestore();
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
        var config = ConfigManager.getConfig();
        boolean newState = !config.autoAttackEnabled;
        config.autoAttackEnabled = newState;

        if (config.muteOnAutoAttack) {
            if (newState) {
                audioMute.requestAutoMute();
            } else {
                audioMute.releaseAutoMute();
            }
        }

        autoClick.resetAttackTimer();
        ConfigManager.save();

        String status = newState ? "gui.autoclicker.enabled" : "gui.autoclicker.disabled";

        String intervalDisplay;
        if (newState && config.attackRandomnessEnabled) {
            int min = config.attackInterval;
            int max = config.attackInterval + config.attackRandomness;
            intervalDisplay = min + "~" + max;
        } else {
            intervalDisplay = String.valueOf(config.attackInterval);
        }

        Main.sendMessage("autoclicker.message.attack_toggle",
                Component.translatable(status),
                intervalDisplay
        );
    }

    // 自动放置
    private static void handleTogglePlaceKey() {
        var config = ConfigManager.getConfig();
        boolean newState = !config.autoPlaceEnabled;
        config.autoPlaceEnabled = newState;

        if (config.muteOnAutoPlace) {
            if (newState) {
                audioMute.requestAutoMute();
            } else {
                audioMute.releaseAutoMute();
            }
        }

        autoClick.resetPlaceTimer();
        ConfigManager.save();

        String status = newState ? "gui.autoclicker.enabled" : "gui.autoclicker.disabled";
        String boneMealStatus = config.useBoneMeal
                ? "autoclicker.bonemeal.included"
                : "autoclicker.bonemeal.excluded";

        String intervalDisplay;
        if (newState && config.placeRandomnessEnabled) {
            int min = config.placeInterval;
            int max = config.placeInterval + config.placeRandomness;
            intervalDisplay = min + "~" + max;
        } else {
            intervalDisplay = String.valueOf(config.placeInterval);
        }

        Main.sendMessage("autoclicker.message.place_toggle",
                Component.translatable(status),
                intervalDisplay,
                Component.translatable(boneMealStatus)
        );
    }


    private static void handleOpenGUIKey() {
        Minecraft client = Minecraft.getInstance();
        if (client.screen == null) {
            client.setScreen(new ConfigScreen(null));
        }
    }

    // 游戏完全退出时，恢复所有静音（包括手动）
    private static void handleGameExit() {
        audioMute.forceRestoreAll();
        ConfigManager.save();
    }
}