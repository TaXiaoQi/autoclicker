package com.example.autoclicker;


import com.example.autoclicker.gui.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

// 模组菜单mod用
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // 返回你的配置屏幕工厂
        return ConfigScreen::new;
    }
}