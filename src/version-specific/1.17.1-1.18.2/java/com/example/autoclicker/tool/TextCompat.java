package com.example.autoclicker.tool;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

/**
 * 文本组件创建适配：1.18.2 及之前无接口静态工厂，使用具体组件类构造。
 */
public final class TextCompat {

    private TextCompat() {
    }

    public static Component translatable(String key, Object... args) {
        return new TranslatableComponent(key, args);
    }

    public static Component literal(String text) {
        return new TextComponent(text);
    }
}
