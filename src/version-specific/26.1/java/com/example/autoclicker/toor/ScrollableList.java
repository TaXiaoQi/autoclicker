package com.example.autoclicker.toor;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    private double dragStartSliderOffset = 0.0;

    private static final int CONTENT_TOP_PADDING = 4;
    private static final int CONTENT_BOTTOM_PADDING = 4;
    private static final int SCROLL_BAR_WIDTH = 6;
    private static final int CHILD_SPACING = 4;
    private static final int SCROLL_SPEED = 20;
    private static final int BACKGROUND_COLOR = 0x80101010;
    private static final int SCROLL_BAR_RIGHT_PADDING = 55;

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

    private boolean needsScrollBar() {
        return contentHeight > getVisibleContentHeight();
    }

    private int getContentX() {
        return this.getX();
    }

    private int getContentWidth() {
        return this.width;
    }

    private int getScrollBarX() {
        return this.getX() + this.width - SCROLL_BAR_WIDTH - SCROLL_BAR_RIGHT_PADDING;
    }

    private int calculateSliderHeight(int trackHeight) {
        if (!needsScrollBar()) return trackHeight;
        float ratio = (float) trackHeight / contentHeight;
        int h = Math.max(15, (int) (trackHeight * ratio));
        return Math.min(h, trackHeight);
    }

    // ========== 26.1+ 核心变更：使用 extractWidgetRenderState 替代 renderWidget ==========
    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int w = width;
        int h = height;

        // 背景
        graphics.fill(x, y, x + w, y + h, BACKGROUND_COLOR);

        // 剪裁内容区
        int clipTop = y + CONTENT_TOP_PADDING;
        int clipBottom = y + h - CONTENT_BOTTOM_PADDING;
        graphics.enableScissor(x, clipTop, x + w, clipBottom);

        // 渲染子控件
        int yPos = y + CONTENT_TOP_PADDING - scrollOffset;
        int contentWidth = getContentWidth();
        int contentX = getContentX();

        for (AbstractWidget child : children) {
            int childWidth = Math.max(1, child.getWidth());
            int childX = contentX + Math.max(0, (contentWidth - childWidth) / 2);

            child.setX(childX);
            child.setY(yPos);
            child.extractRenderState(graphics, mouseX, mouseY, delta);
            yPos += child.getHeight() + CHILD_SPACING;
        }

        graphics.disableScissor();

        // 绘制滚动条
        if (needsScrollBar()) {
            drawScrollBar(graphics);
        }
    }

    private void drawScrollBar(GuiGraphicsExtractor graphics) {
        if (!needsScrollBar()) return;

        int scrollBarX = getScrollBarX();
        int scrollBarRight = scrollBarX + SCROLL_BAR_WIDTH;

        int trackTop = getY() + CONTENT_TOP_PADDING;
        int trackBottom = getY() + height - CONTENT_BOTTOM_PADDING;
        int trackHeight = trackBottom - trackTop;

        // 滑槽
        graphics.fill(scrollBarX, trackTop, scrollBarRight, trackBottom, 0xFF000000);

        // 滑块
        int sliderHeight = calculateSliderHeight(trackHeight);
        float scrollRatio = (float) scrollOffset / Math.max(1, contentHeight - trackHeight);
        int sliderY = trackTop + (int) ((trackHeight - sliderHeight) * scrollRatio);

        graphics.fill(scrollBarX, sliderY, scrollBarRight, sliderY + sliderHeight, 0xFFCCCCCC);
    }
    // ========== 新渲染方法结束 ==========

    // ===== 鼠标事件处理 (保持不变，略作调整) =====

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (needsScrollBar()) {
            if (isPointOverScrollBarHandle(mouseX, mouseY)) {
                draggingScrollBar = true;
                int trackTop = getY() + CONTENT_TOP_PADDING;
                int trackHeight = getVisibleContentHeight();
                int sliderHeight = calculateSliderHeight(trackHeight);
                float scrollRatio = (float) scrollOffset / Math.max(1, contentHeight - trackHeight);
                int sliderY = trackTop + (int) ((trackHeight - sliderHeight) * scrollRatio);
                dragStartSliderOffset = mouseY - sliderY;
                return true;
            }

            if (isPointOverScrollBarTrack(mouseX, mouseY)) {
                int trackTop = getY() + CONTENT_TOP_PADDING;
                int trackHeight = getVisibleContentHeight();
                int sliderHeight = calculateSliderHeight(trackHeight);
                double clickInTrack = mouseY - trackTop;
                double ratio = (clickInTrack - sliderHeight / 2.0) / (trackHeight - sliderHeight);
                ratio = Mth.clamp(ratio, 0.0, 1.0);
                scrollOffset = (int) (ratio * Math.max(1, contentHeight - trackHeight));
                clampScrollOffset();
                return true;
            }
        }

        int currentY = getY() + CONTENT_TOP_PADDING - scrollOffset;
        int contentX = getContentX();
        int contentWidth = getContentWidth();
        int clipTop = getY() + CONTENT_TOP_PADDING;
        int clipBottom = getY() + height - CONTENT_BOTTOM_PADDING;

        for (AbstractWidget child : children) {
            int childWidth = Math.max(1, child.getWidth());
            int childX = contentX + Math.max(0, (contentWidth - childWidth) / 2);
            int childY = currentY;
            int childBottom = childY + child.getHeight();

            boolean isVisible = (childBottom >= clipTop && childY <= clipBottom);
            if (isVisible && mouseX >= childX && mouseX < childX + childWidth && mouseY >= childY && mouseY < childY + child.getHeight()) {
                child.setX(childX);
                child.setY(childY);
                boolean result = child.mouseClicked(event, isDoubleClick);
                return result;
            }
            currentY += child.getHeight() + CHILD_SPACING;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double deltaX, double deltaY) {
        if (draggingScrollBar && needsScrollBar()) {
            double mouseY = event.y();
            int trackTop = getY() + CONTENT_TOP_PADDING;
            int trackHeight = getVisibleContentHeight();
            int sliderHeight = calculateSliderHeight(trackHeight);
            double desiredSliderTop = mouseY - dragStartSliderOffset;
            double maxSliderTop = trackTop + trackHeight - sliderHeight;
            double clampedSliderTop = Mth.clamp(desiredSliderTop, trackTop, maxSliderTop);
            double ratio = (clampedSliderTop - trackTop) / Math.max(1, trackHeight - sliderHeight);
            ratio = Mth.clamp(ratio, 0.0, 1.0);
            scrollOffset = (int) (ratio * Math.max(1, contentHeight - trackHeight));
            clampScrollOffset();
            return true;
        }

        double mouseX = event.x();
        double mouseY = event.y();
        int currentY = getY() + CONTENT_TOP_PADDING - scrollOffset;
        int contentX = getContentX();
        int contentWidth = getContentWidth();
        int clipTop = getY() + CONTENT_TOP_PADDING;
        int clipBottom = getY() + height - CONTENT_BOTTOM_PADDING;

        for (AbstractWidget child : children) {
            int childWidth = Math.max(1, child.getWidth());
            int childX = contentX + Math.max(0, (contentWidth - childWidth) / 2);
            int childY = currentY;
            int childBottom = childY + child.getHeight();

            boolean isVisible = (childBottom >= clipTop && childY <= clipBottom);
            if (isVisible && mouseX >= childX && mouseX < childX + childWidth && mouseY >= childY && mouseY < childY + child.getHeight()) {
                child.setX(childX);
                child.setY(childY);
                boolean result = child.mouseDragged(event, deltaX, deltaY);
                return result;
            }
            currentY += child.getHeight() + CHILD_SPACING;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        if (draggingScrollBar) {
            draggingScrollBar = false;
            return true;
        }
        // 简化处理，不分发给子控件
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOver(mouseX, mouseY) && needsScrollBar()) {
            scrollOffset -= (int) (scrollY * SCROLL_SPEED);
            clampScrollOffset();
            return true;
        }
        return false;
    }

    private boolean isPointOverScrollBarTrack(double x, double y) {
        if (x < getScrollBarX() || x > getScrollBarX() + SCROLL_BAR_WIDTH) {
            return false;
        }
        return y >= getY() + CONTENT_TOP_PADDING && y <= getY() + height - CONTENT_BOTTOM_PADDING;
    }

    private boolean isPointOverScrollBarHandle(double x, double y) {
        if (!needsScrollBar()) return false;
        if (y < getY() + CONTENT_TOP_PADDING || y > getY() + height - CONTENT_BOTTOM_PADDING) {
            return false;
        }
        int scrollBarX = getScrollBarX();
        int trackTop = getY() + CONTENT_TOP_PADDING;
        int trackHeight = getVisibleContentHeight();
        int sliderHeight = calculateSliderHeight(trackHeight);
        float scrollRatio = (float) scrollOffset / Math.max(1, contentHeight - trackHeight);
        int sliderY = trackTop + (int) ((trackHeight - sliderHeight) * scrollRatio);
        return x >= scrollBarX && x <= scrollBarX + SCROLL_BAR_WIDTH && y >= sliderY && y <= sliderY + sliderHeight;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {}

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }
}