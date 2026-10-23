package com.example.autoclicker.tool;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;

public class KeyBindLoaderImpl implements KeyBindLoader {

    private static final String CATEGORY = "key.categories.autoclicker";

    @Override
    public KeyMapping createKeyBind(String description, int keyCode) {
        KeyMapping keyMapping = new KeyMapping(description, InputConstants.Type.KEYSYM, keyCode, CATEGORY);
        // 修正按键显示名：KEYSYM 构造生成的 key 名为数字码（如 key.keyboard.119），语言文件只有字母名条目（key.keyboard.f8），
        // 改用 getKey(keyCode, 0) 取带正确名字的 Key，绑定界面才会显示本地化按键名
        keyMapping.setKey(InputConstants.getKey(keyCode, 0));
        return keyMapping;
    }

    @Override
    public void registerKeyBind(KeyMapping keyMapping) {
        KeyBindingHelper.registerKeyBinding(keyMapping);
    }
}