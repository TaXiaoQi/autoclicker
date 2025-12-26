package com.example.autoclicker.gui.widgets;

import com.example.autoclicker.gui.utils.DrawingUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ScrollableList extends AbstractWidget implements GuiEventListener, NarratableEntry {
    private final List<AbstractWidget> children = new ArrayList<>();
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private boolean dragging = false;
    private double lastMouseY;
    private int scrollBarWidth = 6;
    private int childSpacing = 4; // 元素间距

    public ScrollableList(int x, int y, int width, int height, Component title) {
        super(x, y, width, height, title);
    }

    public <T extends AbstractWidget> T addChild(T child) {
        children.add(child);
        updateContentHeight();
        return child;
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
            contentHeight -= childSpacing; // 最后一个元素不需要间距
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // 保存当前变换
        graphics.pose().pushPose();

        // 绘制背景
        graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x40000000);

        // 启用剪裁
        DrawingUtils.enableScissor(getX(), getY(), width, height);

        // 绘制所有子元素（考虑滚动偏移）
        int yPos = getY() - scrollOffset;

        for (AbstractWidget child : children) {
            // 设置子元素位置（相对于容器）
            child.setX(getX() + 5); // 左边距
            child.setY(yPos);

            // 只绘制可见部分
            if (yPos + child.getHeight() >= getY() && yPos <= getY() + height) {
                child.render(graphics, mouseX, mouseY, delta);
            }

            yPos += child.getHeight() + childSpacing;
        }

        // 禁用剪裁
        DrawingUtils.disableScissor();
        graphics.pose().popPose();

        // 绘制滚动条（如果需要）
        if (contentHeight > height) {
            drawScrollBar(graphics);
        }
    }

    private void drawScrollBar(GuiGraphics graphics) {
        int scrollBarX = getX() + width - scrollBarWidth;

        // 滚动条背景
        graphics.fill(scrollBarX, getY(), scrollBarX + scrollBarWidth, getY() + height, 0x80000000);

        // 计算滑块大小和位置
        float visibleRatio = (float) height / contentHeight;
        int sliderHeight = Math.max(20, (int) (height * visibleRatio));

        float scrollRatio = (float) scrollOffset / (contentHeight - height);
        int sliderY = getY() + (int) ((height - sliderHeight) * scrollRatio);

        // 滑块
        graphics.fill(scrollBarX, sliderY, scrollBarX + scrollBarWidth, sliderY + sliderHeight, 0xFF808080);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 检查是否点击滚动条
        if (isMouseOverScrollBar(mouseX, mouseY)) {
            dragging = true;
            lastMouseY = mouseY;
            return true;
        }

        // 转发点击事件给子元素（考虑滚动偏移）
        double adjustedY = mouseY + scrollOffset;
        for (AbstractWidget child : children) {
            // 检查子元素是否在可见区域内
            int childScreenY = child.getY() + scrollOffset;
            if (childScreenY >= getY() && childScreenY <= getY() + height) {
                if (child.isMouseOver(mouseX, mouseY)) {
                    return child.mouseClicked(mouseX, mouseY, button);
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            double mouseDelta = mouseY - lastMouseY;
            scrollOffset -= mouseDelta * (contentHeight / (double) height);
            scrollOffset = Math.max(0, Math.min(scrollOffset, contentHeight - height));
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOver(mouseX, mouseY)) {
            scrollOffset -= scrollY * 20; // 滚动速度
            scrollOffset = Math.max(0, Math.min(scrollOffset, contentHeight - height));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
}


