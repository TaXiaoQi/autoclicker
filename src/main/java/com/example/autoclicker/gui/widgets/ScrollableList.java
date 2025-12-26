
package com.example.autoclicker.gui.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
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
        // 绘制背景
        graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x40000000);

        // 启用剪裁区域
        graphics.enableScissor(getX(), getY(), getX() + width, getY() + height);

        int yPos = getY() - scrollOffset;
        for (AbstractWidget child : children) {
            child.setX(getX() + 5);
            child.setY(yPos);

            if (yPos + child.getHeight() >= getY() && yPos <= getY() + height) {
                child.render(graphics, mouseX, mouseY, delta);
            }

            yPos += child.getHeight() + childSpacing;
        }

        // 禁用剪裁
        graphics.disableScissor();

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
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (isMouseOverScrollBar(mouseX, mouseY)) {
            dragging = true;
            lastMouseY = mouseY;
            return true;
        }

        for (AbstractWidget child : children) {
            int childY = child.getY();
            // 检查子控件是否在可视区域内
            if (childY >= getY() && childY + child.getHeight() <= getY() + height) {
                if (child.isMouseOver(mouseX, mouseY)) {
                    return child.mouseClicked(event, isDoubleClick); // 传递整个事件对象
                }
            }
        }

        return false;
    }


    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (dragging) {
            double mouseY = event.y();
            double mouseDelta = mouseY - lastMouseY;
            // 计算滚动比例：内容总高 / 可视区域高
            scrollOffset -= (int) (mouseDelta * ((double) contentHeight / (double) height));
            // 限制滚动范围
            scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, contentHeight - height)));
            lastMouseY = mouseY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
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