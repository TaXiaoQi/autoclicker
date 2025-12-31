package com.example.autoclicker;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            try {
                // 动态加载版本特定的 ConfigScreen
                Class<?> configScreenClass = Class.forName("com.example.autoclicker.gui.ConfigScreen");
                return (Screen) configScreenClass.getConstructor(Screen.class).newInstance(parent);
            } catch (ClassNotFoundException e) {
                System.err.println("ConfigScreen not found in this version. ModMenu integration disabled.");
                return null;
            } catch (Exception e) {
                System.err.println("Failed to create ConfigScreen: " + e.getMessage());
                return null;
            }
        };
    }
}