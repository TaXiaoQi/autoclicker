package com.example.autoclicker.tool;

import net.minecraft.network.chat.Component;

/**
 * 文本组件创建适配：1.19.1+ 有接口静态工厂，main 统一走本类。
 */
public final class TextCompat {

    private TextCompat() {
    }

    public static Component translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static Component literal(String text) {
        return Component.literal(text);
    }
}
