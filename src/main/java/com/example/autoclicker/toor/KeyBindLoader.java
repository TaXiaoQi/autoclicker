// KeyBindLoader.java
package com.example.autoclicker.toor;  // 注意是 toor

import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;

public interface KeyBindLoader {

    /**
     * 创建按键映射
     */
    KeyMapping createKeyBind(String description, InputConstants.Type type, int keyCode);

    /**
     * 注册按键到游戏
     */
    void registerKeyBind(KeyMapping keyMapping);
}