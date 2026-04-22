package com.example.autoclicker.toor;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;

public class KeyBindLoaderImpl implements KeyBindLoader {

    private static final String CATEGORY = "key.categories.autoclicker";

    @Override
    public KeyMapping createKeyBind(String description, InputConstants.Type type, int keyCode) {
        return new KeyMapping(description, type, keyCode, CATEGORY);
    }

    @Override
    public void registerKeyBind(KeyMapping keyMapping) {
        KeyBindingHelper.registerKeyBinding(keyMapping);
    }
}