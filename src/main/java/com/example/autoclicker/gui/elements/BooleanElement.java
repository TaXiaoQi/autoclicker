package com.example.autoclicker.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.function.Consumer;

// 布尔值元素
public class BooleanElement extends AbstractWidget implements ConfigElement<Boolean>, GuiEventListener {
    private boolean value;
    private final boolean defaultValue;
    private final Consumer<Boolean> onChanged;
    private final Font font;
    private final Component label;

    public BooleanElement(int x, int y, int width, int height, Component label,
                          boolean defaultValue, Consumer<Boolean> onChanged) {
        super(x, y, width, height, label);
        this.font = Minecraft.getInstance().font;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.onChanged = onChanged;
        this.label = label;
        updateText();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (this.active && this.visible && this.isMouseOver(event.x(), event.y())) {
            if (event.buttonInfo().button() == 0) { // 左键
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                this.value = !this.value;
                this.updateText();
                if (this.onChanged != null) {
                    this.onChanged.accept(this.value);
                }
                return true;
            }
        }
        return false;
    }

    private void updateText() {
        Component status = value ?
                Component.literal("启用").withStyle(ChatFormatting.GREEN) :
                Component.literal("禁用").withStyle(ChatFormatting.RED);

        setMessage(Component.literal(label.getString() + ": ").append(status));
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // 背景颜色
        int backgroundColor = this.isHoveredOrFocused() ? 0xFF555555 : 0xFF333333;
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, backgroundColor);

        // 文字颜色
        int textColor = value ? 0xFF00FF00 : 0xFFFF0000;

        // 绘制文字
        graphics.drawCenteredString(
                font,
                getMessage(),
                this.getX() + this.width / 2,
                this.getY() + (this.height - 8) / 2,
                textColor
        );

        // 悬停效果
        if (this.isHoveredOrFocused()) {
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x20FFFFFF);
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
        // 使用 NarrationElementOutput 的 add 方法
        output.add(NarratedElementType.TITLE, getMessage());
    }
}