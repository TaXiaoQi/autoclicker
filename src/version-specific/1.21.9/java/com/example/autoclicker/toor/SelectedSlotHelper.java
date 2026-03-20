package com.example.autoclicker.toor;


import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 辅助类：获取玩家当前选中的快捷栏槽位
 * 不同MC版本可能需要修改此类的实现
 */
public class SelectedSlotHelper {

    /**
     * 获取当前选中的快捷栏槽位索引
     * @return 槽位索引 (0-8)
     */
    public static int getSelectedSlot() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return -1;
        return player.getInventory().getSelectedSlot();
    }

    /**
     * 获取主手物品
     */
    public static ItemStack getMainHandItem() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return ItemStack.EMPTY;

        return player.getMainHandItem();
    }

    /**
     * 获取副手物品
     */
    public static ItemStack getOffHandItem() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return ItemStack.EMPTY;

        return player.getOffhandItem();
    }

}