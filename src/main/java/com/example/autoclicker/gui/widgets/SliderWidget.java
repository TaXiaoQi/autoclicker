package com.example.autoclicker.gui.widgets;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class SliderWidget extends AbstractSliderButton {
    private final double minValue;
    private final double maxValue;
    private final Component prefix;
    private final Consumer<Double> onChange;
    private final boolean wasChanged;

    public SliderWidget(int x, int y, int width, int height, Component prefix,
                        double minValue, double maxValue, double defaultValue,
                        Consumer<Double> onChange) {
        super(x, y, width, height, Component.empty(),
                (defaultValue - minValue) / (maxValue - minValue));
        this.prefix = prefix;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.onChange = onChange;
        this.wasChanged = true; // 初始化完成
        updateMessage();
    }

    public double getActualValue() {
        return minValue + (maxValue - minValue) * this.value;
    }

    public void setActualValue(double value) {
        value = Math.max(minValue, Math.min(maxValue, value));
        this.value = (value - minValue) / (maxValue - minValue);
        updateMessage();
        if (wasChanged && onChange != null) {
            onChange.accept(value);
        }
    }

    public int getIntValue() {
        return (int) Math.round(getActualValue());
    }

    public void setIntValue(int value) {
        setActualValue(value);
    }

    @Override
    protected void updateMessage() {
        int display = (int) Math.round(getActualValue());
        setMessage(Component.literal(prefix.getString() + ": " + display));
    }

    @Override
    protected void applyValue() {
        if (wasChanged && onChange != null) {
            onChange.accept(getActualValue());
        }
    }
}