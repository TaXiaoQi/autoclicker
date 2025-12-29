// file: src/main/java/com/example/autoclicker/Main.java
package com.example.autoclicker;

import com.example.autoclicker.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("AutoClicker");

    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        // 使用反射加载 EventHandler
        try {
            Class<?> handlerClass = Class.forName("com.example.autoclicker.event.EventHandler");
            java.lang.reflect.Method registerMethod = handlerClass.getDeclaredMethod("register");
            registerMethod.invoke(null); // 调用静态方法
            LOGGER.info("EventHandler loaded successfully");
        } catch (ClassNotFoundException e) {
            LOGGER.error("EventHandler class not found. Make sure it exists in the version-specific directory");
        } catch (Exception e) {
            LOGGER.error("Failed to load EventHandler", e);
        }
    }
}