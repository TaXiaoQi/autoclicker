package com.example.autoclicker.gui.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ScrollableList extends AbstractWidget
        implements GuiEventListener, NarratableEntry {
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

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int w = width;
        int h = height;

        graphics.fill(x, y, x + w, y + h, BACKGROUND_COLOR);
        graphics.fill(x, y, x + w, y + 1, 0x60FFFFFF);
        graphics.fill(x, y + 1, x + w, y + 2, 0x80000000);
        graphics.fill(x, y + h - 2, x + w, y + h - 1, 0x80000000);
        graphics.fill(x, y + h - 1, x + w, y + h, 0x60FFFFFF);

        int clipTop = y + CONTENT_TOP_PADDING;
        int clipBottom = y + h - CONTENT_BOTTOM_PADDING;
        graphics.enableScissor(x, clipTop, x + w, clipBottom);

        int yPos = y + CONTENT_TOP_PADDING - scrollOffset;
        int contentWidth = getContentWidth();
        int contentX = getContentX();

        for (AbstractWidget child : children) {
            int childWidth = Math.max(1, child.getWidth());
            int childX = contentX + Math.max(0, (contentWidth - childWidth) / 2);

            int oldX = child.getX();
            int oldY = child.getY();
            child.setX(childX);
            child.setY(yPos);
            child.render(graphics, mouseX, mouseY, delta);
            child.setX(oldX);
            child.setY(oldY);

            yPos += child.getHeight() + CHILD_SPACING;
        }

        graphics.disableScissor();

        if (needsScrollBar()) {
            drawScrollBar(graphics);
        }
    }

    private void drawScrollBar(GuiGraphics graphics) {
        if (!needsScrollBar()) return;

        int scrollBarX = getScrollBarX();
        int scrollBarRight = scrollBarX + SCROLL_BAR_WIDTH;
        int trackTop = getY() + CONTENT_TOP_PADDING;
        int trackBottom = getY() + height - CONTENT_BOTTOM_PADDING;
        int trackHeight = trackBottom - trackTop;

        graphics.fill(scrollBarX, trackTop, scrollBarRight, trackBottom, 0xFF000000);

        int sliderHeight = calculateSliderHeight(trackHeight);
        float scrollRatio = (float) scrollOffset / Math.max(1, contentHeight - trackHeight);
        int sliderY = trackTop + (int) ((trackHeight - sliderHeight) * scrollRatio);

        graphics.fill(scrollBarX, sliderY, scrollBarRight, sliderY + sliderHeight, 0xFFCCCCCC);

        int edgeColor = 0xFFAAAAAA;
        graphics.fill(scrollBarX, sliderY, scrollBarRight, sliderY + 1, edgeColor);
        graphics.fill(scrollBarX, sliderY + sliderHeight - 1, scrollBarRight, sliderY + sliderHeight, edgeColor);
        graphics.fill(scrollBarX, sliderY, scrollBarX + 1, sliderY + sliderHeight, edgeColor);
        graphics.fill(scrollBarRight - 1, sliderY, scrollBarRight, sliderY + sliderHeight, edgeColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

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
                int oldX = child.getX();
                int oldY = child.getY();
                child.setX(childX);
                child.setY(childY);
                boolean result = child.mouseClicked(mouseX, mouseY, button);
                child.setX(oldX);
                child.setY(oldY);
                return result;
            }

            currentY += child.getHeight() + CHILD_SPACING;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingScrollBar && needsScrollBar()) {
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
                int oldX = child.getX();
                int oldY = child.getY();
                child.setX(childX);
                child.setY(childY);
                boolean result = child.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
                child.setX(oldX);
                child.setY(oldY);
                return result;
            }

            currentY += child.getHeight() + CHILD_SPACING;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingScrollBar) {
            draggingScrollBar = false;
            return true;
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
                int oldX = child.getX();
                int oldY = child.getY();
                child.setX(childX);
                child.setY(childY);
                boolean result = child.mouseReleased(mouseX, mouseY, button);
                child.setX(oldX);
                child.setY(oldY);
                return result;
            }

            currentY += child.getHeight() + CHILD_SPACING;
        }

        return false;
    }

    // 🔧 1.21.1 版本的正确实现：4个参数的 mouseScrolled
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // 在 1.21.1 中，这个方法有4个参数：
        // mouseX, mouseY, horizontalAmount, verticalAmount
        // 我们主要关心垂直滚动 (verticalAmount)

        if (this.isMouseOver(mouseX, mouseY) && needsScrollBar()) {
            // verticalAmount 的值：
            // -1.0 表示向上滚动滚轮（内容向下移动）
            //  1.0 表示向下滚动滚轮（内容向上移动）
            this.scrollOffset -= (int) (verticalAmount * SCROLL_SPEED);
            clampScrollOffset();
            return true;
        }
        return false;
    }

    // 确保鼠标悬停检测正确
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.getX() && mouseX < this.getX() + this.width &&
                mouseY >= this.getY() && mouseY < this.getY() + this.height;
    }

    // ===== 滚动条检测 =====
    private boolean isPointOverScrollBarTrack(double x, double y) {
        if (x < getScrollBarX() || x > getScrollBarX() + SCROLL_BAR_WIDTH) {
            return false;
        }
        return y >= getY() + CONTENT_TOP_PADDING &&
                y <= getY() + height - CONTENT_BOTTOM_PADDING;
    }

    private boolean isPointOverScrollBarHandle(double x, double y) {
        if (!needsScrollBar()) return false;
        if (y < getY() + CONTENT_TOP_PADDING ||
                y > getY() + height - CONTENT_BOTTOM_PADDING) {
            return false;
        }

        int scrollBarX = getScrollBarX();
        int trackTop = getY() + CONTENT_TOP_PADDING;
        int trackHeight = getVisibleContentHeight();
        int sliderHeight = calculateSliderHeight(trackHeight);

        float scrollRatio = (float) scrollOffset / Math.max(1, contentHeight - trackHeight);
        int sliderY = trackTop + (int) ((trackHeight - sliderHeight) * scrollRatio);

        return x >= scrollBarX && x <= scrollBarX + SCROLL_BAR_WIDTH
                && y >= sliderY && y <= sliderY + sliderHeight;
    }

    // ===== Narration =====
    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {}

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }
}