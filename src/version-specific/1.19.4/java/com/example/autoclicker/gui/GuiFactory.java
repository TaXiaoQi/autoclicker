package com.example.autoclicker.gui;

import com.example.autoclicker.gui.elements.ConfigElement;
import com.example.autoclicker.tool.BooleanElement;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

// gui工厂类，负责读取bool给cs屏幕类
public class GuiFactory {

    public static ConfigElement<Boolean> createBooleanElement(
            int x, int y, int width, int height,
            Component label, boolean defaultValue,
            Consumer<Boolean> onChanged) {
        return new BooleanElement(x, y, width, height, label, defaultValue, onChanged);
    }
}
