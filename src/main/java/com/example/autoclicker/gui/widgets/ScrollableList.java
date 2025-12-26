
package com.example.autoclicker.gui.widgets;

import com.example.autoclicker.gui.utils.DrawingUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

// 可滚动列表
public class ScrollableList extends AbstractWidget implements GuiEventListener, NarratableEntry {
    private final List<AbstractWidget> children = new ArrayList<>();
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private boolean dragging = false;
    private double lastMouseY;
    private final int scrollBarWidth = 6;
    private final int childSpacing = 4;

    public ScrollableList(int x, int y, int width, int height, Component title) {
        super(x, y, width, height, title);
    }

    public <T extends AbstractWidget> void addChild(T child) {
        children.add(child);
        updateContentHeight();
    }

    public void clearChildren() {
        children.clear();
        contentHeight = 0;
        scrollOffset = 0;
    }

    private void updateContentHeight() {
        contentHeight = 0;
        for (AbstractWidget child : children) {
            contentHeight += child.getHeight() + childSpacing;
        }
        if (!children.isEmpty()) {
            contentHeight -= childSpacing;
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.pose().pushPose();
        graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x40000000);

        // 修改这里：传递 graphics 参数
        DrawingUtils.enableScissor(graphics, getX(), getY(), width, height);

        int yPos = getY() - scrollOffset;
        for (AbstractWidget child : children) {
            child.setX(getX() + 5);
            child.setY(yPos);

            if (yPos + child.getHeight() >= getY() && yPos <= getY() + height) {
                child.render(graphics, mouseX, mouseY, delta);
            }

            yPos += child.getHeight() + childSpacing;
        }

        // 修改这里：传递 graphics 参数
        DrawingUtils.disableScissor(graphics);
        graphics.pose().popPose();

        if (contentHeight > height) {
            drawScrollBar(graphics);
        }
    }

    private void drawScrollBar(GuiGraphics graphics) {
        int scrollBarX = getX() + width - scrollBarWidth;
        graphics.fill(scrollBarX, getY(), scrollBarX + scrollBarWidth, getY() + height, 0x80000000);

        float visibleRatio = (float) height / contentHeight;
        int sliderHeight = Math.max(20, (int) (height * visibleRatio));

        float scrollRatio = (float) scrollOffset / (contentHeight - height);
        int sliderY = getY() + (int) ((height - sliderHeight) * scrollRatio);

        graphics.fill(scrollBarX, sliderY, scrollBarX + scrollBarWidth, sliderY + sliderHeight, 0xFF808080);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverScrollBar(mouseX, mouseY)) {
            dragging = true;
            lastMouseY = mouseY;
            return true;
        }

        for (AbstractWidget child : children) {
            int childY = child.getY();
            if (childY >= getY() && childY + child.getHeight() <= getY() + height) {
                if (child.isMouseOver(mouseX, mouseY)) {
                    return child.mouseClicked(mouseX, mouseY, button);
                }
            }
        }

        return false;
    }


    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            double mouseDelta = mouseY - lastMouseY;
            scrollOffset -= (int) (mouseDelta * (contentHeight / (double) height));
            scrollOffset = Math.max(0, Math.min(scrollOffset, contentHeight - height));
            lastMouseY = mouseY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return false;
    }



    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOver(mouseX, mouseY)) {
            scrollOffset -= (int) (scrollY * 20);
            scrollOffset = Math.max(0, Math.min(scrollOffset, contentHeight - height));
            return true;
        }
        return false;
    }

    private boolean isMouseOverScrollBar(double mouseX, double mouseY) {
        return contentHeight > height &&
                mouseX >= getX() + width - scrollBarWidth &&
                mouseX <= getX() + width &&
                mouseY >= getY() &&
                mouseY <= getY() + height;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // 无障碍功能
    }

    @Override
    public NarratableEntry.@NotNull NarrationPriority narrationPriority() {
        return NarratableEntry.NarrationPriority.NONE;
    }
}