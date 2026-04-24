package com.example.autoclicker.toor;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class KeyBindLoaderImpl implements KeyBindLoader {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("autoclicker", "main"));

    @Override
    public KeyMapping createKeyBind(String description, InputConstants.Type type, int keyCode) {
        KeyMapping keyMapping = new KeyMapping(description, type, keyCode, CATEGORY);
        return KeyMappingHelper.registerKeyMapping(keyMapping);
    }

    @Override
    public void registerKeyBind(KeyMapping keyMapping) {
        // KeyMappingHelper.registerKeyMapping 已经注册了
    }
}