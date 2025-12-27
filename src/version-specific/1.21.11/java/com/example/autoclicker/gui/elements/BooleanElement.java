package com.example.autoclicker.gui.elements;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.NonNull;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Consumer;

public class BooleanElement extends AbstractButton implements ConfigElement<Boolean> {
    private boolean value;
    private final boolean defaultValue;
    private final Consumer<Boolean> onChanged;
    private final Component originalLabel;

    public BooleanElement(int x, int y, int width, int height, Component label,
                          boolean defaultValue, Consumer<Boolean> onChanged) {
        super(x, y, width, height, Component.empty());
        this.originalLabel = label;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.onChanged = onChanged;
        updateText();
    }

    @Override
    public void onPress(@NonNull InputWithModifiers inputWithModifiers) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        this.value = !this.value;
        updateText();
        if (this.onChanged != null) {
            this.onChanged.accept(this.value);
        }
    }

    private void updateText() {
        Component status = value ?
                Component.literal("true").withStyle(ChatFormatting.GREEN) :
                Component.literal("false").withStyle(ChatFormatting.RED);
        setMessage(originalLabel.copy().append(": ").append(status));
    }


    @Override
    protected void renderContents(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta) {
        // 只有按钮激活时才显示文字（原版行为）
        if (this.active) {
            var font = Minecraft.getInstance().font;
            Component message = this.getMessage();

            // 计算水平居中
            int textX = this.getX() + this.width / 2 - font.width(message) / 2;
            // 垂直居中（原版按钮使用 (height - 8) / 2）
            int textY = this.getY() + (this.height - 8) / 2;

            // 颜色：原版使用 0xE0E0E0（非悬停），0xFFFFFF（悬停）
            int color = this.isHoveredOrFocused() ? 0xFFFFFF : 0xE0E0E0;

            guiGraphics.drawString(font, message, textX, textY, color, false);
        }
    }

    // ========== ConfigElement ==========

    @Override
    public Boolean getValue() { return this.value; }

    @Override
    public void setValue(Boolean value) {
        this.value = value;
        updateText();
    }

    @Override
    public boolean isChanged() { return this.value != this.defaultValue; }

    @Override
    public void save() {}

    @Override
    public void reset() {
        this.value = this.defaultValue;
        updateText();
    }

    @Override
    public AbstractButton getWidget() {
        return this;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.getMessage());
    }
}