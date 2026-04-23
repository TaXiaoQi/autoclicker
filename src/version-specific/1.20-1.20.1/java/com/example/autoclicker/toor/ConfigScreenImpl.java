package com.example.autoclicker.toor;

import com.example.autoclicker.gui.ConfigScreen;

public class ConfigScreenImpl extends ConfigScreen {

    public ConfigScreenImpl(Object parent) {
        super(parent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return handleMouseScroll(mouseX, mouseY, amount);
    }

    @Override
    protected boolean handleMouseScroll(double mouseX, double mouseY, double... scrollAmounts) {
        double amount = scrollAmounts.length > 0 ? scrollAmounts[0] : 0;
        if (scrollList != null && scrollList.isMouseOver(mouseX, mouseY)) {
            return scrollList.mouseScrolled(mouseX, mouseY, amount);
        }
        return false;
    }
}