// KeyBindLoader.java
package com.example.autoclicker.tool;  // tool 包

import net.minecraft.client.KeyMapping;

public interface KeyBindLoader {

    /**
     * 创建按键映射（键盘 keyCode 为 GLFW 键码；输入类型由各版本实现决定）
     */
    KeyMapping createKeyBind(String description, int keyCode);

    /**
     * 注册按键到游戏
     */
    void registerKeyBind(KeyMapping keyMapping);
}