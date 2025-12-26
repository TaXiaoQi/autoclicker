package com.example.autoclicker.gui.elements;

import net.minecraft.client.gui.components.AbstractWidget;

public interface ConfigElement<T> {

    T getValue();
    void setValue(T value);
    boolean isChanged();
    void save();
    void reset();
    AbstractWidget getWidget();
}