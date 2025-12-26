package com.example.autoclicker.gui.elements;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.function.Consumer;

// 布尔值配置元素，用于 GUI 开关
public class BooleanElement extends AbstractWidget implements ConfigElement<Boolean> {
    private boolean value;
    private final boolean defaultValue;
    private final Consumer<Boolean> onChanged;
    private final Component originalLabel; // 仅用于重建显示文本

    public BooleanElement(int x, int y, int width, int height, Component label,
                          boolean defaultValue, Consumer<Boolean> onChanged) {
        super(x, y, width, height, Component.empty()); // 初始消息为空
        this.originalLabel = label;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.onChanged = onChanged;
        updateText();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (this.active && this.visible && this.isMouseOver(event.x(), event.y())) {
            if (event.buttonInfo().button() == 0) { // 左键点击
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                this.value = !this.value;
                updateText();
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
                Component.literal("✓ 启用").withStyle(ChatFormatting.GREEN) :
                Component.literal("✗ 禁用").withStyle(ChatFormatting.RED);
        setMessage(originalLabel.copy().append(": ").append(status));
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        // 使用 setMessage 设置好的完整文本直接绘制
        int textColor = this.active ? 0xFFFFFF : 0xAAAAAA;
        guiGraphics.drawString(Minecraft.getInstance().font, this.getMessage(), this.getX(), this.getY(), textColor);

        // 悬停时绘制半透明高亮边框
        if (this.isHovered()) {
            guiGraphics.fill(this.getX() - 1, this.getY() - 1,
                    this.getRight() + 1, this.getBottom() + 1,
                    0x40FFFFFF);
        }
    }

    // ———————— ConfigElement<Boolean> 接口实现 ————————

    @Override
    public Boolean getValue() {
        return this.value;
    }

    @Override
    public void setValue(Boolean value) {
        this.value = value;
        updateText();
    }

    @Override
    public boolean isChanged() {
        return this.value != this.defaultValue;
    }

    @Override
    public void save() {
        // 保存逻辑由外部配置系统处理，此处留空
    }

    @Override
    public void reset() {
        this.value = this.defaultValue;
        updateText();
    }

    @Override
    public AbstractWidget getWidget() {
        return this;
    }

    // ———————— 叙述支持（无障碍） ————————

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.getMessage());
    }
}