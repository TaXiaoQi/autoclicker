package com.example.autoclicker.gui.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

// 绘图工具类
public class DrawingUtils {
    private static final Minecraft MC = Minecraft.getInstance();

    // 绘制矩形（带圆角）
    public static void drawRoundedRect(GuiGraphics graphics, int x, int y, int width, int height, int color, int radius) {
        // 简单实现：先绘制矩形
        graphics.fill(x, y, x + width, y + height, color);
    }

    // 绘制滑块轨道
    public static void drawSliderTrack(GuiGraphics graphics, int x, int y, int width, int height, int trackColor, int progress, int handleColor) {
        // 绘制轨道
        graphics.fill(x, y, x + width, y + height, trackColor);

        // 绘制进度
        int progressWidth = (int) (width * (progress / 100.0));
        graphics.fill(x, y, x + progressWidth, y + height, 0xFF00AA00);

        // 绘制滑块手柄
        int handleX = x + progressWidth - 4;
        graphics.fill(handleX, y - 2, handleX + 8, y + height + 2, handleColor);
    }

    // 启用剪裁区域 - 使用 GuiGraphics 的方法
    public static void enableScissor(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.enableScissor(x, y, x + width, y + height);
    }

    // 禁用剪裁 - 使用 GuiGraphics 的方法
    public static void disableScissor(GuiGraphics graphics) {
        graphics.disableScissor();
    }
}