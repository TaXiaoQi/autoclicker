package com.example.autoclicker.toor;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class KeyBindLoaderImpl implements KeyBindLoader {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("autoclicker", "main"));

    @Override
    public KeyMapping createKeyBind(String description, InputConstants.Type type, int keyCode) {
        return new KeyMapping(description, type, keyCode, CATEGORY);
    }

    @Override
    public void registerKeyBind(KeyMapping keyMapping) {
        KeyBindingHelper.registerKeyBinding(keyMapping);
    }
}