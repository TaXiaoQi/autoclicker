package com.example.autoclicker;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义滚动容器（Scroll Container）
 * 用于在 Minecraft GUI 中显示超出屏幕高度的内容，并支持鼠标滚轮/拖拽滚动。
 * 注意：Minecraft 原生在 1.20.5+ 提供了 ScrollPanel，但本实现为自定义简化版，
 * 适用于需要完全控制布局和交互的场景。
 */
public class ScrollContainer extends AbstractWidget implements ContainerEventHandler {
    // 存储所有可交互的子组件（用于事件分发）
    private final List<GuiEventListener> children = new ArrayList<>();
    // 存储所有可渲染的子组件（用于绘制）
    private final List<Renderable> renderables = new ArrayList<>();
    // 内容布局（由外部传入，如 LinearLayout）
    private LinearLayout content;
    // 内容区域的固定宽度（用于水平居中）
    private int contentWidth;
    // 当前滚动偏移量（向上滚动时为正值）
    private double scrollAmount = 0;
    // 是否正在拖拽滚动条
    private boolean scrolling = false;
    // 拖拽开始时的鼠标Y坐标
    private double dragStartMouseY;
    // 拖拽开始时的滚动位置
    private double dragStartScrollAmount;

    /**
     * 构造函数：初始化一个空的滚动容器
     */
    public ScrollContainer() {
        super(0, 0, 0, 0, Component.empty()); // 位置和尺寸由外部设置
    }

    /**
     * 设置滚动容器的内容布局
     * @param content 线性垂直布局（通常包含多个配置项）
     */
    public void setContent(LinearLayout content) {
        this.content = content;
        this.children.clear();
        this.renderables.clear();

        // 遍历内容中的所有子组件，收集事件监听器和可渲染对象
        content.visitWidgets(widget -> {
            if (widget instanceof GuiEventListener listener) {
                children.add(listener);
            }
            if (widget instanceof Renderable renderable) {
                renderables.add(renderable);
            }
        });
    }

    /**
     * 渲染滚动容器及其内容
     */
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        // 启用裁剪区域（只在容器可视区域内绘制）
        guiGraphics.enableScissor(
                this.getX(), this.getY(),
                this.getX() + this.width,
                this.getY() + this.height
        );

        // 保存当前变换矩阵（用于后续恢复）
        guiGraphics.pose().pushPose();

        // 应用滚动偏移 - 将内容向上移动
        guiGraphics.pose().translate(0, 0);

        // 绘制背景（半透明黑色，增强可读性）
        guiGraphics.fill(
                this.getX(), this.getY(),
                this.getX() + this.width,
                this.getY() + this.height + (int) scrollAmount,
                0x80000000
        );

        // 渲染内容（应用滚动偏移）
        if (content != null) {
            // 水平居中内容
            int contentX = this.getX() + (this.width - this.contentWidth) / 2;
            int contentY = this.getY() + 30;

            content.setPosition(contentX, contentY);

            // 渲染所有子组件（注意：mouseY已经通过矩阵变换自动处理）
            content.visitWidgets(widget -> {
                if (widget instanceof Renderable renderable) {
                    renderable.render(guiGraphics, mouseX, mouseY, delta);
                }
            });
        }

        // 恢复变换矩阵
        guiGraphics.pose().popPose();

        // 关闭裁剪区域
        guiGraphics.disableScissor();

        // 如果内容超出容器高度，则绘制滚动条
        if (getMaxScroll() > 0) {
            renderScrollBar(guiGraphics);
        }
    }

    /**
     * 绘制右侧滚动条
     */
    private void renderScrollBar(GuiGraphics guiGraphics) {
        int scrollBarWidth = 6;
        int scrollBarX = this.getX() + this.width - scrollBarWidth - 2; // 距离右边缘 2px
        int scrollBarY = this.getY() + 2;
        int scrollBarHeight = this.height - 4;

        // 滚动条背景（半透明白色）
        guiGraphics.fill(scrollBarX, scrollBarY,
                scrollBarX + scrollBarWidth,
                scrollBarY + scrollBarHeight,
                0x80FFFFFF);

        // 计算滑块高度（最小 20px）
        int sliderHeight = Math.max(20, (int) ((float) this.height / getContentHeight() * scrollBarHeight));
        // 计算滑块 Y 位置（根据 scrollAmount 比例）
        int sliderY = scrollBarY + (int) ((float) scrollAmount / getMaxScroll() * (scrollBarHeight - sliderHeight));

        // 滑块颜色（灰色）
        guiGraphics.fill(scrollBarX, sliderY,
                scrollBarX + scrollBarWidth,
                sliderY + sliderHeight,
                0xFF888888);
    }

    /**
     * 估算内容总高度（简化实现）
     * 实际应通过 layout.getHeight() 获取，但 LinearLayout 可能未暴露该方法。
     * 此处按每个组件约 25px + 额外 padding 估算。
     */
    private int getContentHeight() {
        if (content == null) return 0;
        return children.size() * 25 + 100; // 可根据实际调整
    }

    /**
     * 获取最大可滚动距离（内容高度 - 容器高度）
     */
    private int getMaxScroll() {
        return Math.max(0, getContentHeight() - this.height);
    }

    /**
     * 设置内容区域的宽度（用于水平居中）
     */
    public void setContentWidth(int contentWidth) {
        this.contentWidth = contentWidth;
    }

    /**
     * 处理鼠标滚轮事件（实现滚动）
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (isMouseOver(mouseX, mouseY)) {
            // deltaY > 0 表示向上滚动（内容向下移动），所以减去 deltaY * 速度
            scrollAmount = Math.max(0, Math.min(scrollAmount - deltaY * 20, getMaxScroll()));
            return true; // 消费事件
        }
        return false;
    }

    // === 以下为 ContainerEventHandler 接口必需方法 ===

    @Override
    public @Nullable GuiEventListener getFocused() {
        return null; // 本容器不管理焦点
    }

    @Override
    public void setFocused(@Nullable GuiEventListener guiEventListener) {
        // 忽略
    }

    /**
     * 处理鼠标拖拽（用于拖动滚动条）
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrolling && button == 0) {
            double scrollFactor = (double) getMaxScroll() / Math.max(1, this.height - 20);
            scrollAmount = Math.max(0, Math.min(scrollAmount + dragY * scrollFactor, getMaxScroll()));
            return true;
        }

        for (GuiEventListener child : children) {
            if (child.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 处理鼠标点击
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }

        // 滚动条区域计算
        if (getMaxScroll() > 0) {
            int scrollBarWidth = 6;
            int scrollBarX = this.getX() + this.width - scrollBarWidth - 2;
            int scrollBarY = this.getY() + 2;
            int scrollBarHeight = this.height - 4;

            // 滑块尺寸和位置
            int sliderHeight = Math.max(20, (int) ((float) this.height / getContentHeight() * scrollBarHeight));
            int sliderY = scrollBarY + (int) ((float) scrollAmount / getMaxScroll() * (scrollBarHeight - sliderHeight));

            // 判断是否点击了滑块
            if (button == 0 && mouseX >= scrollBarX && mouseY >= sliderY && mouseY <= sliderY + sliderHeight) {
                scrolling = true;
                dragStartMouseY = mouseY;          // 记录起始鼠标位置
                dragStartScrollAmount = scrollAmount; // 记录起始滚动位置
                return true;
            }
        }

        // 转发给子组件（注意：此处 mouseY 不偏移，如果你用了 pose().translate）
        for (GuiEventListener child : children) {
            if (child.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 处理鼠标释放
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        scrolling = false; // 结束拖拽

        if (isMouseOver(mouseX, mouseY)) {
            // 将释放事件传递给子组件（不再需要scrollAmount偏移）
            for (GuiEventListener child : children) {
                if (child.mouseReleased(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断鼠标是否在容器区域内
     */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.getX() &&
                mouseX <= this.getX() + this.width &&
                mouseY >= this.getY() &&
                mouseY <= this.getY() + this.height;
    }

    /**
     * 返回所有子事件监听器（用于事件分发）
     */
    @Override
    public List<? extends GuiEventListener> children() {
        return children;
    }

    // === 拖拽状态管理（用于兼容 Minecraft GUI 系统）===
    @Override
    public boolean isDragging() {
        return scrolling;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.scrolling = dragging;
    }

    // === 无障碍叙述（Narration）支持（此处禁用）===
    @Override
    public NarratableEntry.@NotNull NarrationPriority narrationPriority() {
        return NarratableEntry.NarrationPriority.NONE;
    }

    @Override
    public void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
        // 不提供叙述内容
    }
}