package com.example.autoclicker;

import com.example.autoclicker.config.ConfigManager;
import com.example.autoclicker.event.EventHandler;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("AutoClicker");

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        EventHandler.register();
    }
}