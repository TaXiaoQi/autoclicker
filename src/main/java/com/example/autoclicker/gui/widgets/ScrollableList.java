package com.example.autoclicker.gui.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ScrollableList extends AbstractWidget implements GuiEventListener, NarratableEntry {
    private final List<AbstractWidget> children = new ArrayList<>();
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private boolean draggingScrollBar = false;
    private double dragStartMouseY = 0;
    private float dragStartScrollRatio = 0.0F;

    // 原版风格常量
    private static final int SCROLL_BAR_WIDTH = 6;
    private static final int SCROLL_BAR_RIGHT_PADDING = 2;
    private static final int CONTENT_TOP_PADDING = 4;
    private static final int CONTENT_BOTTOM_PADDING = 4;
    private static final int CONTENT_LEFT_PADDING = 5;
    private static final int CHILD_SPACING = 4;
    private static final int SCROLL_SPEED = 20;

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
            contentHeight += child.getHeight() + CHILD_SPACING;
        }
        if (!children.isEmpty()) {
            contentHeight -= CHILD_SPACING;
        }
        clampScrollOffset();
    }

    private void clampScrollOffset() {
        int maxScroll = Math.max(0, contentHeight - getVisibleContentHeight());
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, maxScroll);
    }

    private int getVisibleContentHeight() {
        return this.height - CONTENT_TOP_PADDING - CONTENT_BOTTOM_PADDING;
    }

    private int getContentX() {
        return this.getX() + CONTENT_LEFT_PADDING;
    }

    private int getContentWidth() {
        return this.width - CONTENT_LEFT_PADDING - SCROLL_BAR_WIDTH - SCROLL_BAR_RIGHT_PADDING;
    }

    private int getScrollBarX() {
        return this.getX() + this.width - SCROLL_BAR_WIDTH - SCROLL_BAR_RIGHT_PADDING;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int w = width;
        int h = height;

        // 1. 绘制深色半透明背景
        graphics.fill(x, y, x + w, y + h, 0x80000000);

        // 2. ✅ 绘制灰色描边（边框）
        int borderColor = 0xFF8B8B8B; // 灰色
        graphics.fill(x, y, x + w, y + 1, borderColor);         // 上边
        graphics.fill(x, y + h - 1, x + w, y + h, borderColor); // 下边

        // 3. 启用剪裁：仅在可视区域内渲染内容（不含边框区域）
        int clipTop = y + CONTENT_TOP_PADDING;
        int clipBottom = y + h - CONTENT_BOTTOM_PADDING;
        graphics.enableScissor(x, clipTop, x + w, clipBottom);

        // 4. 渲染子控件（居中）
        int yPos = y + CONTENT_TOP_PADDING - scrollOffset;
        int contentWidth = getContentWidth();
        int contentX = getContentX();

        for (AbstractWidget child : children) {
            int childX = contentX + Math.max(0, (contentWidth - child.getWidth()) / 2);
            child.setX(childX);
            child.setY(yPos);

            if (yPos + child.getHeight() >= clipTop && yPos <= clipBottom) {
                child.render(graphics, mouseX, mouseY, delta);
            }

            yPos += child.getHeight() + CHILD_SPACING;
        }

        graphics.disableScissor();

        // 5. 绘制滚动条（如果需要）
        if (contentHeight > getVisibleContentHeight()) {
            drawScrollBar(graphics);
        }
    }

    private void drawScrollBar(GuiGraphics graphics) {
        int scrollBarX = getScrollBarX();
        int scrollBarRight = scrollBarX + SCROLL_BAR_WIDTH;
        int top = getY() + CONTENT_TOP_PADDING;
        int bottom = getY() + height - CONTENT_BOTTOM_PADDING;
        int visibleHeight = bottom - top;

        float visibleRatio = (float) visibleHeight / contentHeight;
        int sliderHeight = Math.max(15, (int) (visibleHeight * visibleRatio));
        sliderHeight = Math.min(sliderHeight, visibleHeight);

        float scrollRatio = contentHeight <= visibleHeight ? 0.0F :
                (float) scrollOffset / (contentHeight - visibleHeight);
        int sliderY = top + (int) ((visibleHeight - sliderHeight) * scrollRatio);

        // 滑块主体
        graphics.fill(scrollBarX, sliderY, scrollBarRight, sliderY + sliderHeight, 0xFF808080);
        // 边框
        graphics.fill(scrollBarX, sliderY, scrollBarRight, sliderY + 1, 0xFF404040);
        graphics.fill(scrollBarX, sliderY + sliderHeight - 1, scrollBarRight, sliderY + sliderHeight, 0xFF404040);
        graphics.fill(scrollBarX, sliderY, scrollBarX + 1, sliderY + sliderHeight, 0xFF404040);
        graphics.fill(scrollBarRight - 1, sliderY, scrollBarRight, sliderY + sliderHeight, 0xFF404040);
    }

    // ===== 鼠标事件辅助逻辑 =====
    private void forEachVisibleChild(double mouseX, double mouseY, ChildAction action) {
        int currentY = getY() + CONTENT_TOP_PADDING - scrollOffset;
        int contentX = getContentX();
        int contentWidth = getContentWidth();
        int clipTop = getY() + CONTENT_TOP_PADDING;
        int clipBottom = getY() + height - CONTENT_BOTTOM_PADDING;

        for (AbstractWidget child : children) {
            int childWidth = child.getWidth() > 0 ? child.getWidth() : 100; // 安全默认值
            int childX = contentX + Math.max(0, (contentWidth - childWidth) / 2);
            int childY = currentY;
            int childBottom = childY + child.getHeight();

            boolean isVisible = (childBottom >= clipTop && childY <= clipBottom);
            if (action.process(child, childX, childY, childWidth, isVisible, mouseX, mouseY)) {
                return;
            }

            currentY += child.getHeight() + CHILD_SPACING;
        }
    }

    @FunctionalInterface
    private interface ChildAction {
        boolean process(AbstractWidget child, int x, int y, int width, boolean isVisible, double mouseX, double mouseY);
    }

    // ===== 鼠标事件重写 =====

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (isPointOverScrollBarHandle(mouseX, mouseY)) {
            draggingScrollBar = true;
            dragStartMouseY = mouseY;
            int visibleHeight = getVisibleContentHeight();
            if (contentHeight > visibleHeight) {
                dragStartScrollRatio = (float) scrollOffset / (contentHeight - visibleHeight);
            } else {
                dragStartScrollRatio = 0.0F;
            }
            return true;
        }

        if (isPointOverScrollBarTrack(mouseX, mouseY)) {
            int visibleHeight = getVisibleContentHeight();
            if (contentHeight > visibleHeight) {
                int trackTop = getY() + CONTENT_TOP_PADDING;
                float clickRatio = (float) (mouseY - trackTop) / visibleHeight;
                scrollOffset = (int) (clickRatio * (contentHeight - visibleHeight));
                clampScrollOffset();
            }
            return true;
        }

        forEachVisibleChild(mouseX, mouseY, (child, x, y, w, isVisible, mx, my) -> {
            if (isVisible && mx >= x && mx < x + w && my >= y && my < y + child.getHeight()) {
                return child.mouseClicked(event, isDoubleClick);
            }
            return false;
        });

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (draggingScrollBar) {
            double mouseY = event.y();
            int visibleHeight = getVisibleContentHeight();
            if (contentHeight > visibleHeight) {
                double mouseDelta = mouseY - dragStartMouseY;
                float ratioDelta = (float) (mouseDelta / visibleHeight);
                scrollOffset = (int) ((dragStartScrollRatio + ratioDelta) * (contentHeight - visibleHeight));
                clampScrollOffset();
            }
            return true;
        }

        double mouseX = event.x();
        double mouseY = event.y();

        forEachVisibleChild(mouseX, mouseY, (child, x, y, w, isVisible, mx, my) -> {
            if (isVisible && mx >= x && mx < x + w && my >= y && my < y + child.getHeight()) {
                return child.mouseDragged(event, deltaX, deltaY);
            }
            return false;
        });

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingScrollBar) {
            draggingScrollBar = false;
            return true;
        }

        double mouseX = event.x();
        double mouseY = event.y();

        forEachVisibleChild(mouseX, mouseY, (child, x, y, w, isVisible, mx, my) -> {
            if (isVisible && mx >= x && mx < x + w && my >= y && my < y + child.getHeight()) {
                return child.mouseReleased(event);
            }
            return false;
        });

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOver(mouseX, mouseY)) {
            scrollOffset -= (int) (scrollY * SCROLL_SPEED);
            clampScrollOffset();
            return true;
        }
        return false;
    }

    // ===== 滚动条检测 =====

    private boolean isPointOverScrollBarTrack(double x, double y) {
        if (contentHeight <= getVisibleContentHeight()) return false;
        return x >= getScrollBarX() && x <= getScrollBarX() + SCROLL_BAR_WIDTH
                && y >= getY() + CONTENT_TOP_PADDING
                && y <= getY() + height - CONTENT_BOTTOM_PADDING;
    }

    private boolean isPointOverScrollBarHandle(double x, double y) {
        if (contentHeight <= getVisibleContentHeight()) return false;

        int scrollBarX = getScrollBarX();
        int visibleHeight = getVisibleContentHeight();
        float visibleRatio = (float) visibleHeight / contentHeight;
        int sliderHeight = Math.max(15, (int) (visibleHeight * visibleRatio));
        sliderHeight = Math.min(sliderHeight, visibleHeight);

        float scrollRatio = (float) scrollOffset / (contentHeight - visibleHeight);
        int sliderY = getY() + CONTENT_TOP_PADDING + (int) ((visibleHeight - sliderHeight) * scrollRatio);

        return x >= scrollBarX && x <= scrollBarX + SCROLL_BAR_WIDTH
                && y >= sliderY && y <= sliderY + sliderHeight;
    }

    // ===== Narration (可选) =====

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }
}