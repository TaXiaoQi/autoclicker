
package com.example.autoclicker.gui;

import com.example.autoclicker.gui.elements.ConfigElement;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class GuiFactory {

    @SuppressWarnings("unchecked")
    public static ConfigElement<Boolean> createBooleanElement(
            int x, int y, int width, int height,
            Component label, boolean defaultValue,
            Consumer<Boolean> onChanged) {

        try {
            Class<?> booleanElementClass = Class.forName("com.example.autoclicker.gui.elements.BooleanElement");
            return (ConfigElement<Boolean>) booleanElementClass.getConstructor(
                    int.class, int.class, int.class, int.class,
                    Component.class, boolean.class, Consumer.class
            ).newInstance(x, y, width, height, label, defaultValue, onChanged);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("BooleanElement not found. Make sure it exists in the version-specific directory", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create BooleanElement: " + e.getMessage(), e);
        }
    }
}