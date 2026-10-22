package com.example.autoclicker.tool;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 屏幕层 API 适配（MC 26.2+）：
 * screen 状态由 Minecraft 迁移到 Gui（screen()/setScreen），
 * 快捷栏上方消息由 Gui 迁移到 Hud（hud.setOverlayMessage）。
 */
public final class ScreenHelper {

    private ScreenHelper() {
    }

    /** 获取当前打开的屏幕（可为 null） */
    public static Screen current(Minecraft client) {
        return client.gui.screen();
    }

    /** 打开屏幕 */
    public static void open(Minecraft client, Screen screen) {
        client.gui.setScreen(screen);
    }

    /** 在快捷栏上方显示提示消息 */
    public static void showOverlayMessage(Minecraft client, Component message) {
        client.gui.hud.setOverlayMessage(message, false);
    }
}
