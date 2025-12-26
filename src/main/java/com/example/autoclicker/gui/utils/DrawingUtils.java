package com.example.autoclicker.gui.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

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

    // 启用剪裁区域
    public static void enableScissor(int x, int y, int width, int height) {
        double scale = MC.getWindow().getGuiScale();
        RenderSystem.enableScissor(
                (int)(x * scale),
                (int)(MC.getWindow().getHeight() - (y + height) * scale),
                (int)(width * scale),
                (int)(height * scale)
        );
    }

    // 禁用剪裁
    public static void disableScissor() {
        RenderSystem.disableScissor();
    }
}