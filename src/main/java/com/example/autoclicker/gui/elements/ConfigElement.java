package com.example.autoclicker.gui.elements;

import net.minecraft.client.gui.components.AbstractWidget;

// 配置元素接口
public interface ConfigElement<T> {

    T getValue();
    void setValue(T value);
    boolean isChanged();
    void save();
    void reset();
    AbstractWidget getWidget();
}