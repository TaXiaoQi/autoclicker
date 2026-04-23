package com.example.autoclicker.toor;

import com.example.autoclicker.gui.ConfigScreen;

public class ConfigScreenImpl extends ConfigScreen {

    public ConfigScreenImpl(Object parent) {
        super(parent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return handleMouseScroll(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    protected boolean handleMouseScroll(double mouseX, double mouseY, double... scrollAmounts) {
        double horizontal = scrollAmounts.length > 0 ? scrollAmounts[0] : 0;
        double vertical = scrollAmounts.length > 1 ? scrollAmounts[1] : 0;
        if (scrollList != null && scrollList.isMouseOver(mouseX, mouseY)) {
            return scrollList.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        }
        return false;
    }
}
