package com.example.autoclicker.feature;

import com.example.autoclicker.Main;
import com.example.autoclicker.config.ConfigManager;
import com.example.autoclicker.toor.SelectedSlotHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

public class AutoRefill {

    // 记忆数据结构
    private static class Memory {
        ItemStack item;     // 物品
        int slot;           // 槽位索引
        boolean enabled;    // 是否启用

        Memory(ItemStack item, int slot) {
            this.item = item.copy();
            this.slot = slot;
            this.enabled = true;
        }

        boolean isSameType(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() == item.getItem();
        }

        void update(ItemStack newStack) {
            this.item = newStack.copy();
        }
    }

    // 攻击功能记忆
    private Memory attackMainHandMem = null;
    private Memory attackOffHandMem = null;

    // 放置功能记忆
    private Memory placeMainHandMem = null;
    private Memory placeOffHandMem = null;

    private static final int OFFHAND_SLOT = 40;

    /**
     * 第一步：主入口方法 - 攻击功能使用后调用
     * 检查配置，管理记忆
     */
    public void onAttackUsed() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        var config = ConfigManager.getConfig();

        // 处理主手
        if (config.autoRefillMainHand) {
            manageAttackMainHandMemory(client.player);
        } else {
            attackMainHandMem = null;
        }

        // 处理副手
        if (config.autoRefillOffHand) {
            manageAttackOffHandMemory(client.player);
        } else {
            attackOffHandMem = null;
        }
    }

    /**
     * 第一步：主入口方法 - 放置功能使用后调用
     */
    public void onPlaceUsed() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        var config = ConfigManager.getConfig();

        // 处理主手
        if (config.autoRefillMainHand) {
            managePlaceMainHandMemory(client.player);
        } else {
            placeMainHandMem = null;
        }

        // 处理副手
        if (config.autoRefillOffHand) {
            managePlaceOffHandMemory(client.player);
        } else {
            placeOffHandMem = null;
        }
    }

    /**
     * 第二步：管理攻击主手记忆
     */
    private void manageAttackMainHandMemory(LocalPlayer player) {
        int currentSlot = SelectedSlotHelper.getSelectedSlot();
        ItemStack currentItem = SelectedSlotHelper.getMainHandItem();
        var config = ConfigManager.getConfig();

        if (attackMainHandMem == null) {
            // 没有记忆，写入新记忆
            if (!currentItem.isEmpty()) {
                attackMainHandMem = new Memory(currentItem, currentSlot);
            }
        } else {
            // 有记忆，检查是否需要补充
            if (shouldRefill(currentItem, attackMainHandMem.item, config)) {
                boolean success = refillSlot(player, attackMainHandMem, currentSlot);
                if (success) {
                    ItemStack newItem = SelectedSlotHelper.getMainHandItem();
                    attackMainHandMem.update(newItem);
                    attackMainHandMem.slot = currentSlot;
                }
            } else if (!currentItem.isEmpty() && !attackMainHandMem.isSameType(currentItem)) {
                // 物品已手动更换，更新记忆
                attackMainHandMem = new Memory(currentItem, currentSlot);
            }
        }
    }

    /**
     * 第二步：管理攻击副手记忆
     */
    private void manageAttackOffHandMemory(LocalPlayer player) {
        ItemStack currentItem = SelectedSlotHelper.getOffHandItem();
        var config = ConfigManager.getConfig();

        if (attackOffHandMem == null) {
            // 没有记忆，写入新记忆
            if (!currentItem.isEmpty()) {
                attackOffHandMem = new Memory(currentItem, OFFHAND_SLOT);
            }
        } else {
            // 有记忆，检查是否需要补充
            if (shouldRefill(currentItem, attackOffHandMem.item, config)) {
                boolean success = refillSlot(player, attackOffHandMem, OFFHAND_SLOT);
                if (success) {
                    ItemStack newItem = SelectedSlotHelper.getOffHandItem();
                    attackOffHandMem.update(newItem);
                }
            } else if (!currentItem.isEmpty() && !attackOffHandMem.isSameType(currentItem)) {
                // 物品已手动更换，更新记忆
                attackOffHandMem = new Memory(currentItem, OFFHAND_SLOT);
            }
        }
    }

    /**
     * 第二步：管理放置主手记忆
     */
    private void managePlaceMainHandMemory(LocalPlayer player) {
        int currentSlot = SelectedSlotHelper.getSelectedSlot();
        ItemStack currentItem = SelectedSlotHelper.getMainHandItem();
        var config = ConfigManager.getConfig();

        if (placeMainHandMem == null) {
            // 没有记忆，写入新记忆
            if (!currentItem.isEmpty()) {
                placeMainHandMem = new Memory(currentItem, currentSlot);
            }
        } else {
            // 有记忆，检查是否需要补充
            if (shouldRefill(currentItem, placeMainHandMem.item, config)) {
                boolean success = refillSlot(player, placeMainHandMem, currentSlot);
                if (success) {
                    ItemStack newItem = SelectedSlotHelper.getMainHandItem();
                    placeMainHandMem.update(newItem);
                    placeMainHandMem.slot = currentSlot;
                }
            } else if (!currentItem.isEmpty() && !placeMainHandMem.isSameType(currentItem)) {
                // 物品已手动更换，更新记忆
                placeMainHandMem = new Memory(currentItem, currentSlot);
            }
        }
    }

    /**
     * 第二步：管理放置副手记忆
     */
    private void managePlaceOffHandMemory(LocalPlayer player) {
        ItemStack currentItem = SelectedSlotHelper.getOffHandItem();
        var config = ConfigManager.getConfig();

        if (placeOffHandMem == null) {
            // 没有记忆，写入新记忆
            if (!currentItem.isEmpty()) {
                placeOffHandMem = new Memory(currentItem, OFFHAND_SLOT);
            }
        } else {
            // 有记忆，检查是否需要补充
            if (shouldRefill(currentItem, placeOffHandMem.item, config)) {
                boolean success = refillSlot(player, placeOffHandMem, OFFHAND_SLOT);
                if (success) {
                    ItemStack newItem = SelectedSlotHelper.getOffHandItem();
                    placeOffHandMem.update(newItem);
                }
            } else if (!currentItem.isEmpty() && !placeOffHandMem.isSameType(currentItem)) {
                // 物品已手动更换，更新记忆
                placeOffHandMem = new Memory(currentItem, OFFHAND_SLOT);
            }
        }
    }

    /**
     * 判断是否需要补充物品
     */
    private boolean shouldRefill(ItemStack currentItem, ItemStack targetItem, com.example.autoclicker.config.Config config) {
        // 当前槽位为空，需要补充
        if (currentItem.isEmpty()) {
            return true;
        }

        // 当前物品类型与记忆中的不同，不需要补充（等待物品更换逻辑处理）
        if (currentItem.getItem() != targetItem.getItem()) {
            return false;
        }

        // 检查数量阈值
        if (config.refillCountThreshold > 0) {
            if (currentItem.getCount() <= config.refillCountThreshold) {
                return true;
            }
        }

        // 检查耐久阈值（仅对可损坏物品）
        if (config.refillDurabilityThreshold > 0 && currentItem.isDamageableItem()) {
            int maxDamage = currentItem.getMaxDamage();
            int currentDamage = currentItem.getDamageValue();
            int remainingPercent = (int) ((maxDamage - currentDamage) * 100.0 / maxDamage);

            return remainingPercent <= config.refillDurabilityThreshold;
        }

        return false;
    }

    /**
     * 第三步：补充物品到指定槽位
     * @return true=补充成功, false=补充失败
     */
    private boolean refillSlot(LocalPlayer player, Memory memory, int targetSlot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return false;

        Inventory inv = player.getInventory();

        // 查找可补充的物品（排除目标槽位本身）
        int sourceSlot = findSourceSlot(inv, memory.item, targetSlot);

        if (sourceSlot == -1) {
            String itemName = memory.item.getHoverName().getString();
            Main.sendMessage("autoclicker.message.no_items_left",
                    Component.literal(itemName));
            return false;
        }

        try {
            mc.gameMode.handleInventoryMouseClick(
                    player.containerMenu.containerId,
                    sourceSlot,
                    targetSlot,
                    ClickType.SWAP,
                    player
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 在背包中查找指定物品
     */
    private int findSourceSlot(Inventory inv, ItemStack target, int excludeSlot) {
        // 1. 优先从背包找（9-35）
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == target.getItem()) {
                return i;
            }
        }

        // 2. 如果目标是主手（快捷栏），可以从其他快捷栏找，但不能从副手拿
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
     * 清除攻击记忆
     */
    public void clearAttackMemory() {
        attackMainHandMem = null;
        attackOffHandMem = null;
    }

    /**
     * 清除放置记忆
     */
    public void clearPlaceMemory() {
        placeMainHandMem = null;
        placeOffHandMem = null;
    }
}