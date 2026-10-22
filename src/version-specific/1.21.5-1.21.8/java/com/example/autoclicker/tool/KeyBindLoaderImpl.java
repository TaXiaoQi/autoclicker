package com.example.autoclicker.tool;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;

public class KeyBindLoaderImpl implements KeyBindLoader {

    private static final String CATEGORY = "key.categories.autoclicker";

    @Override
    public KeyMapping createKeyBind(String description, int keyCode) {
        return new KeyMapping(description, InputConstants.Type.KEYSYM, keyCode, CATEGORY);
    }

    @Override
    public void registerKeyBind(KeyMapping keyMapping) {
        KeyBindingHelper.registerKeyBinding(keyMapping);
    }
}