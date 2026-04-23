package com.example.autoclicker.toor;

import com.example.autoclicker.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

public class Replace {

    private static final int OFFHAND_SLOT = 40;
    private final Minecraft mc;

    public Replace() {
        this.mc = Minecraft.getInstance();
    }

    /**
     * 补充物品到指定槽位
     * @param player 玩家实例
     * @param targetItem 需要补充的目标物品
     * @param targetSlot 目标槽位
     * @return 是否成功补充
     */
    public boolean refillSlot(LocalPlayer player, ItemStack targetItem, int targetSlot) {
        if (mc.gameMode == null) return false;

        Inventory inv = player.getInventory();
        int sourceSlot = findSourceSlot(inv, targetItem, targetSlot);

        if (sourceSlot == -1) {
            Main.sendMessage("autoclicker.message.no_items_left",
                    Component.literal(targetItem.getHoverName().getString()));
            return false;
        }

        try {
            // 判断源槽位和目标槽位的类型
            if (isHotbarSlot(sourceSlot) && isHotbarSlot(targetSlot)) {
                // 两个都是快捷栏：使用普通的点击交换
                swapBetweenHotbar(player, sourceSlot, targetSlot);
            } else if (sourceSlot == OFFHAND_SLOT || targetSlot == OFFHAND_SLOT) {
                // 涉及副手的交换
                swapWithOffhand(player, sourceSlot, targetSlot);
            } else {
                // 一个背包一个快捷栏：使用 SWAP
                mc.gameMode.handleInventoryMouseClick(
                        player.containerMenu.containerId,
                        sourceSlot,
                        targetSlot,  // 快捷栏索引作为 button 参数
                        ClickType.SWAP,
                        player
                );
            }
            return true;
        } catch (Exception e) {
            Main.sendMessage("autoclicker.message.refill_failed");
            return false;
        }
    }

    /**
     * 在背包中查找指定物品
     * @param inv 玩家背包
     * @param target 目标物品
     * @param excludeSlot 排除的槽位（目标槽位本身）
     * @return 源槽位索引，-1 表示未找到
     */
    private int findSourceSlot(Inventory inv, ItemStack target, int excludeSlot) {
        // 1. 优先从背包找（9-35）
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == target.getItem()) {
                return i;
            }
        }

        // 2. 如果目标是主手（快捷栏），可以从其他快捷栏找
        if (excludeSlot >= 0 && excludeSlot <= 8) {
            for (int i = 0; i <= 8; i++) {
                if (i == excludeSlot) continue;
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && stack.getItem() == target.getItem()) {
                    return i;
                }
            }
        }

        // 3. 如果目标是副手，可以从快捷栏找（包括主手）
        if (excludeSlot == OFFHAND_SLOT) {
            for (int i = 0; i <= 8; i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && stack.getItem() == target.getItem()) {
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * 判断是否为快捷栏槽位
     */
    private boolean isHotbarSlot(int slot) {
        return slot >= 0 && slot <= 8;
    }

    /**
     * 快捷栏之间的物品交换
     */
    private void swapBetweenHotbar(LocalPlayer player, int slot1, int slot2) {
        // 删除未使用的 stack1
        // 保存 slot2 的原始物品（交换前）
        ItemStack originalStack2 = player.getInventory().getItem(slot2).copy();

        // 1. 点击 slot1 拿起物品
        if (mc.gameMode != null) {
            mc.gameMode.handleInventoryMouseClick(
                    player.containerMenu.containerId,
                    slot1,
                    0,
                    ClickType.PICKUP,
                    player
            );
        }

        // 2. 点击 slot2 放下物品
        if (mc.gameMode != null) {
            mc.gameMode.handleInventoryMouseClick(
                    player.containerMenu.containerId,
                    slot2,
                    0,
                    ClickType.PICKUP,
                    player
            );
        }

        // 3. 如果 slot2 原来有物品，放回 slot1
        if (!originalStack2.isEmpty()) {  // 改用 originalStack2
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryMouseClick(
                        player.containerMenu.containerId,
                        slot1,
                        0,
                        ClickType.PICKUP,
                        player
                );
            }
        }
    }

    /**
     * 涉及副手的物品交换
     */
    private void swapWithOffhand(LocalPlayer player, int sourceSlot, int targetSlot) {
        // 副手交换使用 F 键的功能
        // 注意：这里的实现可能需要根据版本调整
        if (targetSlot == OFFHAND_SLOT) {
            // 从 sourceSlot 移动到副手
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryMouseClick(
                        player.containerMenu.containerId,
                        sourceSlot,
                        0,  // button
                        ClickType.SWAP,
                        player
                );
            }
        } else {
            // 从副手移动到 targetSlot
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryMouseClick(
                        player.containerMenu.containerId,
                        OFFHAND_SLOT,
                        targetSlot,
                        ClickType.SWAP,
                        player
                );
            }
        }
    }
}