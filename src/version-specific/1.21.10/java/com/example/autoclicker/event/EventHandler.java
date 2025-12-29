package com.example.autoclicker.event;

import com.example.autoclicker.config.ConfigManager;
import com.example.autoclicker.feature.AutoClicker;
import com.example.autoclicker.feature.MuteFeature;
import com.example.autoclicker.gui.ConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
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
        registerCustomCategory();
        loadKeyBindingsFromConfig();
        registerKeyBindings();
        registerEvents();
    }

    private static void registerCustomCategory() {
        AUTO_CLICKER_CATEGORY = KeyMapping.Category.register(
                ResourceLocation.fromNamespaceAndPath("autoclicker", "category")
        );
    }

    private static void loadKeyBindingsFromConfig() {
        var config = ConfigManager.getConfig();

        muteKey = new KeyMapping(
                "key.autoclicker.mute",
                InputConstants.Type.KEYSYM,
                config.keyMute,
                AUTO_CLICKER_CATEGORY
        );

        toggleAttackKey = new KeyMapping(
                "key.autoclicker.toggle_attack",
                InputConstants.Type.KEYSYM,
                config.keyToggleAttack,
                AUTO_CLICKER_CATEGORY
        );

        togglePlaceKey = new KeyMapping(
                "key.autoclicker.toggle_place",
                InputConstants.Type.KEYSYM,
                config.keyTogglePlace,
                AUTO_CLICKER_CATEGORY
        );

        openGUIKey = new KeyMapping(
                "key.autoclicker.open_gui",
                InputConstants.Type.KEYSYM,
                config.keyOpenGUI,
                AUTO_CLICKER_CATEGORY
        );
    }

    private static void registerKeyBindings() {
        KeyBindingHelper.registerKeyBinding(muteKey);
        KeyBindingHelper.registerKeyBinding(toggleAttackKey);
        KeyBindingHelper.registerKeyBinding(togglePlaceKey);
        KeyBindingHelper.registerKeyBinding(openGUIKey);
    }

    private static void registerEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(EventHandler::onClientTick);

        // 窗口焦点检测（最小化/失去焦点时静音）
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                boolean windowActive = client.isWindowActive();
                audioMute.updateMinimizedMute(!windowActive);
            }
        });

        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof PauseScreen) {
                handlePauseMenu();
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> handleGameExit());
    }

    private static void onClientTick(Minecraft client) {
        if (client.player == null) return;

        handleKeyBindings();
        autoClick.tick(client);
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

    private static void handleMuteKey() {
        audioMute.toggleManualMute();

        var config = ConfigManager.getConfig();
        if (audioMute.isManuallyMuted()) {
            // 手动静音时，可选择关闭自动功能（按需保留）
            if (config.muteOnAutoAttack) {
                config.autoAttackEnabled = false;
            }
            if (config.muteOnAutoPlace) {
                config.autoPlaceEnabled = false;
            }
        }
        ConfigManager.save();
    }

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
    }

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
    }

    private static void handleOpenGUIKey() {
        Minecraft client = Minecraft.getInstance();
        if (client.screen == null) {
            client.setScreen(new ConfigScreen(null));
        }
    }


    private static void handlePauseMenu() {
        if (!audioMute.isManuallyMuted()) {
            audioMute.forceRestore();
        }
    }

    private static void handleGameExit() {
        audioMute.forceRestore();
        ConfigManager.save();
    }
}