package com.example.autoclicker.tool;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
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
        return this.x;
    }

    private int getContentWidth() {
        return this.width;
    }

    private int getScrollBarX() {
        return this.x + this.width - SCROLL_BAR_WIDTH - SCROLL_BAR_RIGHT_PADDING;
    }

    private int calculateSliderHeight(int trackHeight) {
        if (!needsScrollBar()) return trackHeight;
        float ratio = (float) trackHeight / contentHeight;
        int h = Math.max(15, (int) (trackHeight * ratio));
        return Math.min(h, trackHeight);
    }

    // 1.19.3 之前无 renderWidget，直接 override render；x/y 为公共字段
    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        int x = this.x;
        int y = this.y;
        int w = width;
        int h = height;

        GuiComponent.fill(poseStack, x, y, x + w, y + h, BACKGROUND_COLOR);
        GuiComponent.fill(poseStack, x, y, x + w, y + 1, 0x60FFFFFF);
        GuiComponent.fill(poseStack, x, y + 1, x + w, y + 2, 0x80000000);
        GuiComponent.fill(poseStack, x, y + h - 2, x + w, y + h - 1, 0x80000000);
        GuiComponent.fill(poseStack, x, y + h - 1, x + w, y + h, 0x60FFFFFF);

        int clipTop = y + CONTENT_TOP_PADDING;
        int clipBottom = y + h - CONTENT_BOTTOM_PADDING;

        // RenderSystem 剪裁使用窗口像素坐标，需按 GUI 缩放换算（原点在窗口左下角）
        Window window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        RenderSystem.enableScissor(
                (int) (x * scale),
                (int) (window.getHeight() - clipBottom * scale),
                (int) (w * scale),
                (int) ((clipBottom - clipTop) * scale));

        int yPos = y + CONTENT_TOP_PADDING - scrollOffset;
        int contentWidth = getContentWidth();
        int contentX = getContentX();

        for (AbstractWidget child : children) {
            int childWidth = Math.max(1, child.getWidth());
            int childX = contentX + Math.max(0, (contentWidth - childWidth) / 2);

            int oldX = child.x;
            int oldY = child.y;
            child.x = childX;
            child.y = yPos;
            child.render(poseStack, mouseX, mouseY, delta);
            child.x = oldX;
            child.y = oldY;

            yPos += child.getHeight() + CHILD_SPACING;
        }

        RenderSystem.disableScissor();

        if (needsScrollBar()) {
            drawScrollBar(poseStack);
        }
    }

    private void drawScrollBar(PoseStack poseStack) {
        if (!needsScrollBar()) return;

        int scrollBarX = getScrollBarX();
        int scrollBarRight = scrollBarX + SCROLL_BAR_WIDTH;
        int trackTop = y + CONTENT_TOP_PADDING;
        int trackBottom = y + height - CONTENT_BOTTOM_PADDING;
        int trackHeight = trackBottom - trackTop;

        GuiComponent.fill(poseStack, scrollBarX, trackTop, scrollBarRight, trackBottom, 0xFF000000);

        int sliderHeight = calculateSliderHeight(trackHeight);
        float scrollRatio = (float) scrollOffset / Math.max(1, contentHeight - trackHeight);
        int sliderY = trackTop + (int) ((trackHeight - sliderHeight) * scrollRatio);

        GuiComponent.fill(poseStack, scrollBarX, sliderY, scrollBarRight, sliderY + sliderHeight, 0xFFCCCCCC);

        int edgeColor = 0xFFAAAAAA;
        GuiComponent.fill(poseStack, scrollBarX, sliderY, scrollBarRight, sliderY + 1, edgeColor);
        GuiComponent.fill(poseStack, scrollBarX, sliderY + sliderHeight - 1, scrollBarRight, sliderY + sliderHeight, edgeColor);
        GuiComponent.fill(poseStack, scrollBarX, sliderY, scrollBarX + 1, sliderY + sliderHeight, edgeColor);
        GuiComponent.fill(poseStack, scrollBarRight - 1, sliderY, scrollBarRight, sliderY + sliderHeight, edgeColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (needsScrollBar()) {
            if (isPointOverScrollBarHandle(mouseX, mouseY)) {
                draggingScrollBar = true;

                int trackTop = y + CONTENT_TOP_PADDING;
                int trackHeight = getVisibleContentHeight();
                int sliderHeight = calculateSliderHeight(trackHeight);

                float scrollRatio = (float) scrollOffset / Math.max(1, contentHeight - trackHeight);
                int sliderY = trackTop + (int) ((trackHeight - sliderHeight) * scrollRatio);

                dragStartSliderOffset = mouseY - sliderY;
                return true;
            }

            if (isPointOverScrollBarTrack(mouseX, mouseY)) {
                int trackTop = y + CONTENT_TOP_PADDING;
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

        int currentY = y + CONTENT_TOP_PADDING - scrollOffset;
        int contentX = getContentX();
        int contentWidth = getContentWidth();
        int clipTop = y + CONTENT_TOP_PADDING;
        int clipBottom = y + height - CONTENT_BOTTOM_PADDING;

        for (AbstractWidget child : children) {
            int childWidth = Math.max(1, child.getWidth());
            int childX = contentX + Math.max(0, (contentWidth - childWidth) / 2);
            int childY = currentY;
            int childBottom = childY + child.getHeight();

            boolean isVisible = (childBottom >= clipTop && childY <= clipBottom);
            if (isVisible && mouseX >= childX && mouseX < childX + childWidth && mouseY >= childY && mouseY < childY + child.getHeight()) {
                int oldX = child.x;
                int oldY = child.y;
                child.x = childX;
                child.y = childY;
                boolean result = child.mouseClicked(mouseX, mouseY, button);
                child.x = oldX;
                child.y = oldY;
                return result;
            }

            currentY += child.getHeight() + CHILD_SPACING;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingScrollBar && needsScrollBar()) {
            int trackTop = y + CONTENT_TOP_PADDING;
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

        int currentY = y + CONTENT_TOP_PADDING - scrollOffset;
        int contentX = getContentX();
        int contentWidth = getContentWidth();
        int clipTop = y + CONTENT_TOP_PADDING;
        int clipBottom = y + height - CONTENT_BOTTOM_PADDING;

        for (AbstractWidget child : children) {
            int childWidth = Math.max(1, child.getWidth());
            int childX = contentX + Math.max(0, (contentWidth - childWidth) / 2);
            int childY = currentY;
            int childBottom = childY + child.getHeight();

            boolean isVisible = (childBottom >= clipTop && childY <= clipBottom);
            if (isVisible && mouseX >= childX && mouseX < childX + childWidth && mouseY >= childY && mouseY < childY + child.getHeight()) {
                int oldX = child.x;
                int oldY = child.y;
                child.x = childX;
                child.y = childY;
                boolean result = child.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
                child.x = oldX;
                child.y = oldY;
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

        int currentY = y + CONTENT_TOP_PADDING - scrollOffset;
        int contentX = getContentX();
        int contentWidth = getContentWidth();
        int clipTop = y + CONTENT_TOP_PADDING;
        int clipBottom = y + height - CONTENT_BOTTOM_PADDING;

        for (AbstractWidget child : children) {
            int childWidth = Math.max(1, child.getWidth());
            int childX = contentX + Math.max(0, (contentWidth - childWidth) / 2);
            int childY = currentY;
            int childBottom = childY + child.getHeight();

            boolean isVisible = (childBottom >= clipTop && childY <= clipBottom);
            if (isVisible && mouseX >= childX && mouseX < childX + childWidth && mouseY >= childY && mouseY < childY + child.getHeight()) {
                int oldX = child.x;
                int oldY = child.y;
                child.x = childX;
                child.y = childY;
                boolean result = child.mouseReleased(mouseX, mouseY, button);
                child.x = oldX;
                child.y = oldY;
                return result;
            }

            currentY += child.getHeight() + CHILD_SPACING;
        }

        return false;
    }

    // 1.19.4 的 GuiEventListener.mouseScrolled 为 3 参
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (this.isMouseOver(mouseX, mouseY) && needsScrollBar()) {
            this.scrollOffset -= (int) (amount * SCROLL_SPEED);
            clampScrollOffset();
            return true;
        }
        return false;
    }

    // 确保鼠标悬停检测正确
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width &&
                mouseY >= this.y && mouseY < this.y + this.height;
    }

    // ===== 滚动条检测 =====
    private boolean isPointOverScrollBarTrack(double x, double y) {
        if (x < getScrollBarX() || x > getScrollBarX() + SCROLL_BAR_WIDTH) {
            return false;
        }
        // 参数 y 为鼠标坐标（double），字段 y 需 this. 限定
        return y >= this.y + CONTENT_TOP_PADDING &&
                y <= this.y + height - CONTENT_BOTTOM_PADDING;
    }

    private boolean isPointOverScrollBarHandle(double x, double y) {
        if (!needsScrollBar()) return false;
        if (y < this.y + CONTENT_TOP_PADDING ||
                y > this.y + height - CONTENT_BOTTOM_PADDING) {
            return false;
        }

        int scrollBarX = getScrollBarX();
        int trackTop = (int) this.y + CONTENT_TOP_PADDING;
        int trackHeight = getVisibleContentHeight();
        int sliderHeight = calculateSliderHeight(trackHeight);

        float scrollRatio = (float) scrollOffset / Math.max(1, contentHeight - trackHeight);
        int sliderY = trackTop + (int) ((trackHeight - sliderHeight) * scrollRatio);

        return x >= scrollBarX && x <= scrollBarX + SCROLL_BAR_WIDTH
                && y >= sliderY && y <= sliderY + sliderHeight;
    }

    // ===== Narration =====
    @Override
    public void updateNarration(@NotNull NarrationElementOutput output) {}

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }
}