package com.example.autoclicker.gui.elements;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class BooleanElement extends AbstractWidget implements ConfigElement<Boolean>, GuiEventListener, NarratableEntry {
    private boolean value;
    private final boolean defaultValue;
    private final Consumer<Boolean> onChanged;
    private final Component label;

    public BooleanElement(int x, int y, int width, int height, Component label,
                          boolean defaultValue, Consumer<Boolean> onChanged) {
        super(x, y, width, height, label);
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.onChanged = onChanged;
        this.label = label;
        updateText();
    }

    @Override
    public void onClick(double mouseY) {
        value = !value;
        updateText();
        if (onChanged != null) {
            onChanged.accept(value);
        }
    }

    private void updateText() {
        Component status = value ?
                Component.literal("启用").withStyle(ChatFormatting.GREEN) :
                Component.literal("禁用").withStyle(ChatFormatting.RED);

        setMessage(Component.literal(label.getString() + ": ").append(status));
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // 自定义渲染
        int color = isHovered() ? 0xFF555555 : 0xFF333333;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, color);

        int textColor = value ? 0xFF00FF00 : 0xFFFF0000;
        graphics.drawCenteredString(
                font,
                getMessage(),
                getX() + width / 2,
                getY() + (height - 8) / 2,
                textColor
        );

        if (isHovered()) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x20FFFFFF);
        }
    }

    // ConfigElement 实现
    @Override
    public Boolean getValue() {
        return value;
    }

    @Override
    public void setValue(Boolean value) {
        this.value = value;
        updateText();
    }

    @Override
    public boolean isChanged() {
        return value != defaultValue;
    }

    @Override
    public void save() {
        // 保存逻辑在外部处理
    }

    @Override
    public void reset() {
        value = defaultValue;
        updateText();
    }

    @Override
    public AbstractWidget getWidget() {
        return this;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarrationElementOutput.TITLE, getMessage());
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.HOVERED;
    }
}