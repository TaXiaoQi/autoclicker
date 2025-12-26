package com.example.autoclicker.gui.widgets;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class SliderWidget extends AbstractSliderButton {
    private final double minValue;
    private final double maxValue;
    private final Component prefix;
    private final Consumer<Double> onChange;

    public SliderWidget(int x, int y, int width, int height, Component prefix,
                        double minValue, double maxValue, double defaultValue,
                        Consumer<Double> onChange) {
        super(x, y, width, height, Component.empty(),
                clamp01((defaultValue - minValue) / (maxValue - minValue)));
        this.prefix = prefix;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.onChange = onChange;
        updateMessage(); // 初始化显示
    }

    // 工具方法：确保归一化值在 [0,1]
    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    // 获取实际值（非归一化）
    public double getActualValue() {
        return minValue + (maxValue - minValue) * this.value;
    }

    // 设置实际值（外部 API）
    public void setActualValue(double value) {
        value = Math.max(minValue, Math.min(maxValue, value));
        this.value = clamp01((value - minValue) / (maxValue - minValue));
        updateMessage();
        if (onChange != null) {
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
        if (onChange != null) {
            onChange.accept(getActualValue());
        }
    }

}