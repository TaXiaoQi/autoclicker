package com.example.autoclicker.tool;

import com.example.autoclicker.gui.elements.ConfigElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

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
    public void onPress() {
        // 点击音效由 vanilla playDownSound 播放，避免重复
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
    public void updateNarration(NarrationElementOutput output) {
        // 1.19.3 之前无 updateWidgetNarration，使用父类的默认按钮旁白
        this.defaultButtonNarrationText(output);
    }
}