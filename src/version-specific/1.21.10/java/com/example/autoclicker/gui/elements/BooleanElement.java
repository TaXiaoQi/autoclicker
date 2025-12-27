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
    public void onPress(InputWithModifiers inputWithModifiers) {
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