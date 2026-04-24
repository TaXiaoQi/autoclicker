package com.example.autoclicker.toor;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class KeyBindLoaderImpl implements KeyBindLoader {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("autoclicker", "main"));

    @Override
    public KeyMapping createKeyBind(String description, InputConstants.Type type, int keyCode) {
        // 26.1 中 KeyMapping 构造时会自注册
        return new KeyMapping(description, type, keyCode, CATEGORY);
    }

    @Override
    public void registerKeyBind(KeyMapping keyMapping) {
        // 已在构造函数中自动注册
    }
}