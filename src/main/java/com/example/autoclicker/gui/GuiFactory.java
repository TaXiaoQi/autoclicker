package com.example.autoclicker.gui;

import com.example.autoclicker.gui.elements.ConfigElement;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

// gui工厂类，负责读取bool给cs屏幕类
public class GuiFactory {

    @SuppressWarnings("unchecked")
    public static ConfigElement<Boolean> createBooleanElement(
            int x, int y, int width, int height,
            Component label, boolean defaultValue,
            Consumer<Boolean> onChanged) {

        try {
            // 修改这里：从 gui.elements 改为 toor
            Class<?> booleanElementClass = Class.forName("com.example.autoclicker.toor.BooleanElement");
            return (ConfigElement<Boolean>) booleanElementClass.getConstructor(
                    int.class, int.class, int.class, int.class,
                    Component.class, boolean.class, Consumer.class
            ).newInstance(x, y, width, height, label, defaultValue, onChanged);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("BooleanElement not found in toor package", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create BooleanElement: " + e.getMessage(), e);
        }
    }
}