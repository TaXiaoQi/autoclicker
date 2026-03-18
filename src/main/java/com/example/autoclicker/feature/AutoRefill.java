package com.example.autoclicker.feature;

import com.example.autoclicker.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class AutoRefill {
    // 记录主手和副手的记忆物品
    private Item mainHandMemory = null;
    private Item offHandMemory = null;


    public void checkAndRefill(Minecraft client) {
        var config = ConfigManager.getConfig();

        LocalPlayer player = client.player;
        Inventory inventory = null;
        if (player != null) {
            inventory = player.getInventory();
        }

        // 检查并补充主手（如果主手补充开启）
        if (config.autoRefillMainHand && mainHandMemory != null) {
            ItemStack mainHand = null;
            if (player != null) {
                mainHand = player.getMainHandItem();
            }
            if (mainHand != null && (mainHand.isEmpty() || mainHand.getItem() != mainHandMemory)) {
                if (!tryRefillMainHand(player, inventory)) {
                    mainHandMemory = null;
                }
            }
        }

        // 检查并补充副手（如果副手补充开启）
        if (config.autoRefillOffHand && offHandMemory != null) {
            ItemStack offHand = null;
            if (player != null) {
                offHand = player.getOffhandItem();
            }
            if (offHand != null && (offHand.isEmpty() || offHand.getItem() != offHandMemory)) {
                if (!tryRefillOffHand(player, inventory)) {
                    offHandMemory = null;
                }
            }
        }
    }

    private boolean tryRefillMainHand(LocalPlayer player, Inventory inventory) {
        if (mainHandMemory == null) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return false;

        // 查找所有槽位中匹配的物品（包括快捷栏 0-8）
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            // 跳过盔甲槽位 (36-39) 和副手槽位 (40)
            if (i >= 36 && i <= 40) continue;

            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == mainHandMemory) {
                if (i == inventory.selected) {
                    return true;
                }

                // 使用原版点击事件：交换找到的物品和快捷栏选中物品
                mc.gameMode.handleInventoryMouseClick(
                        0,                    // 容器ID (0=玩家背包)
                        i,                    // 找到的物品槽位
                        inventory.selected,   // 目标快捷栏槽位 (作为按钮参数)
                        ClickType.SWAP,       // 交换类型
                        player
                );
                return true;
            }
        }
        return false;
    }

    private boolean tryRefillOffHand(LocalPlayer player, Inventory inventory) {
        if (offHandMemory == null) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return false;

        // 查找所有槽位中匹配的物品（包括快捷栏 0-8）
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            // 跳过盔甲槽位 (36-39) 和副手槽位 (40)
            if (i >= 36 && i <= 40) continue;

            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == offHandMemory) {
                // 使用原版点击事件：交换找到的物品和副手物品
                mc.gameMode.handleInventoryMouseClick(
                        0,                    // 容器ID (0=玩家背包)
                        i,                    // 找到的物品槽位
                        40,                   // 副手槽位
                        ClickType.SWAP,       // 交换类型
                        player
                );
                return true;
            }
        }
        return false;
    }

    public void onRefillSuccess() {
        // 成功使用物品后，重新记忆当前手中的物品
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            var config = ConfigManager.getConfig();

            // 只在对应开关开启时更新记忆
            if (config.autoRefillMainHand) {
                ItemStack mainHand = client.player.getMainHandItem();
                mainHandMemory = mainHand.isEmpty() ? null : mainHand.getItem();
            }

            if (config.autoRefillOffHand) {
                ItemStack offHand = client.player.getOffhandItem();
                offHandMemory = offHand.isEmpty() ? null : offHand.getItem();
            }
        }
    }

    public void clearAllMemory() {
        mainHandMemory = null;
        offHandMemory = null;
    }

    }