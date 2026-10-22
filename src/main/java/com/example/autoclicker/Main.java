// file: src/main/java/com/example/autoclicker/Main.java
package com.example.autoclicker;

import com.example.autoclicker.config.ConfigManager;
import com.example.autoclicker.event.EventHandler;
import com.example.autoclicker.tool.ScreenHelper;
import com.example.autoclicker.tool.TextCompat;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main implements ClientModInitializer {
    // 1.16.5 无 slf4j，改用 Minecraft 全版本自带的 log4j2
    public static final Logger LOGGER = LogManager.getLogger("AutoClicker");

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        EventHandler.register();
        LOGGER.info("EventHandler registered successfully");
    }

    // 模组启动反馈
    public static void sendMessage(String translationKey, Object... args) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            Component message = TextCompat.translatable(translationKey, args);
            ScreenHelper.showOverlayMessage(client, message);  // ✅ 快捷栏上方显示
        }
    }
}