package com.example.autoclicker.gui.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class SliderWidget extends AbstractSliderButton {
    private final Consumer<Double> onChange;
    public final double minValue;  // 改为 protected
    public final double maxValue;  // 改为 protected
    private final Component prefix;
    private int displayValue;

    public SliderWidget(int x, int y, int width, int height, Component prefix,
                        double minValue, double maxValue, double defaultValue,
                        Consumer<Double> onChange) {
        super(x, y, width, height, Component.empty(),
                (defaultValue - minValue) / (maxValue - minValue));
        this.onChange = onChange;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.prefix = prefix;
        this.displayValue = (int) Math.round(getValue());
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(prefix.getString() + ": " + displayValue));
    }

    @Override
    protected void applyValue() {
        displayValue = (int) Math.round(getValue());
        updateMessage();
        if (onChange != null) {
            onChange.accept(getValue());
        }
    }
    public void setIntValue(int value) {
        // 确保值在范围内
        value = Math.max((int)minValue, Math.min(value, (int)maxValue));

        // 设置内部值（直接计算并赋值）
        this.value = (value - minValue) / (maxValue - minValue);
        this.displayValue = value;

        // 更新显示文本
        updateMessage();

        // 触发回调（如果存在）
        if (onChange != null) {
            onChange.accept((double) value);
        }
    }

    public double getValue() {
        return minValue + (maxValue - minValue) * value;
    }

    public int getIntValue() {
        return (int) Math.round(getValue());
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // 先调用父类方法绘制基础按钮
        super.renderWidget(graphics, mouseX, mouseY, delta);

        // 自定义滑块绘制（在现有按钮上添加指示器）
        int sliderX = getX() + (int)(value * (width - 8));
        graphics.fill(sliderX, getY() - 2, sliderX + 8, getY() + height + 2, 0xFFFFFFFF);
    }
}