package com.example.autoclicker.tool;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class KeyBindLoaderImpl implements KeyBindLoader {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("autoclicker", "main"));

    @Override
    public KeyMapping createKeyBind(String description, int keyCode) {
        // MC 26.3 起 InputConstants.Type.KEYSYM 更名为 KEYBOARD
        KeyMapping keyMapping = new KeyMapping(description, InputConstants.Type.KEYBOARD, keyCode, CATEGORY);
        return KeyMappingHelper.registerKeyMapping(keyMapping);
    }

    @Override
    public void registerKeyBind(KeyMapping keyMapping) {
        // KeyMappingHelper.registerKeyMapping 已经注册了
    }
}