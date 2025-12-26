package com.example.autoclicker.gui.elements;

import com.example.autoclicker.gui.widgets.SliderWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class IntSliderElement implements ConfigElement<Integer> {
    private final SliderWidget slider;
    private final int defaultValue;

    public IntSliderElement(int x, int y, int width, int height, Component label,
                            int minValue, int maxValue, int defaultValue,
                            Consumer<Integer> onChanged) {
        this.defaultValue = defaultValue;

        this.slider = new SliderWidget(x, y, width, height, label,
                minValue, maxValue, defaultValue, value -> {
            if (onChanged != null) {
                onChanged.accept(value.intValue());
            }
        });
    }

    @Override
    public Integer getValue() {
        return slider.getIntValue();
    }

    @Override
    public void setValue(Integer value) {
        slider.setIntValue(value);
    }


    @Override
    public boolean isChanged() {
        return getValue() != defaultValue;
    }

    @Override
    public void save() {
        // 保存逻辑在外部处理
    }

    @Override
    public void reset() {
        setValue(defaultValue);
    }

    @Override
    public AbstractWidget getWidget() {
        return slider;
    }
}