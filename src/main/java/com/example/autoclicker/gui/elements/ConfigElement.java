package com.example.autoclicker.gui.elements;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;

public interface ConfigElement<T> {
    void render(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta);

    T getValue();
    void setValue(T value);
    boolean isChanged();
    void save();
    void reset();
    AbstractWidget getWidget();
}