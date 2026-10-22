package com.example.autoclicker.tool;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 屏幕层 API 适配：main 源码只面向本类编程。
 * MC 26.2 起 screen 状态迁移到 Gui、消息迁移到 Hud，各版本组提供对应实现。
 */
public final class ScreenHelper {

    private ScreenHelper() {
    }

    /** 获取当前打开的屏幕（可为 null） */
    public static Screen current(Minecraft client) {
        return client.screen;
    }

    /** 打开屏幕 */
    public static void open(Minecraft client, Screen screen) {
        client.setScreen(screen);
    }

    /** 在快捷栏上方显示提示消息 */
    public static void showOverlayMessage(Minecraft client, Component message) {
        client.gui.setOverlayMessage(message, false);
    }
}
