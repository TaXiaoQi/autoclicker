package com.example.autoclicker.toor;  // 包名保持一致

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;

public class KeyBindLoaderImpl implements KeyBindLoader {

    private static final KeyMapping.Category CATEGORY;

    static {
        CATEGORY = KeyMapping.Category.register(
                ResourceLocation.fromNamespaceAndPath("autoclicker", "main")
        );
    }

    @Override
    public KeyMapping createKeyBind(String description, InputConstants.Type type, int keyCode) {
        // 新版：使用 Category 对象
        return new KeyMapping(description, type, keyCode, CATEGORY);
    }

    @Override
    public void registerKeyBind(KeyMapping keyMapping) {
        KeyBindingHelper.registerKeyBinding(keyMapping);
    }
}
