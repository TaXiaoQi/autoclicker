package com.example.autoclicker.gui;

import com.example.autoclicker.gui.elements.ConfigElement;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class GuiFactory {

    public static ConfigElement<Boolean> createBooleanElement(
            int x, int y, int width, int height, Component label,
            boolean defaultValue, Consumer<Boolean> onChanged) {

        try {
            // 首先尝试1.21.11版本
            Class<?> clazz1211 = Class.forName("com.example.autoclicker.gui.elements.v1211.BooleanElement");
            return (ConfigElement<Boolean>) clazz1211.getConstructor(
                    int.class, int.class, int.class, int.class,
                    Component.class, boolean.class, Consumer.class
            ).newInstance(x, y, width, height, label, defaultValue, onChanged);
        } catch (ClassNotFoundException e) {
            // 回退到默认版本
            try {
                Class<?> clazz = Class.forName("com.example.autoclicker.gui.elements.BooleanElement");
                return (ConfigElement<Boolean>) clazz.getConstructor(
                        int.class, int.class, int.class, int.class,
                        Component.class, boolean.class, Consumer.class
                ).newInstance(x, y, width, height, label, defaultValue, onChanged);
            } catch (Exception ex) {
                throw new RuntimeException("无法创建 BooleanElement", ex);
            }
        } catch (Exception e) {
            throw new RuntimeException("创建 BooleanElement 失败", e);
        }
    }
}