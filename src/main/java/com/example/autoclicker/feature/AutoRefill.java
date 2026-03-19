package com.example.autoclicker.feature;

import com.example.autoclicker.Main;
import com.example.autoclicker.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class AutoRefill {
    // 记忆状态枚举
    private enum MemoryState {
        UNINITIALIZED,    // 未初始化
        INITIALIZED,      // 已初始化（记录所有物品）
        LOCKED           // 已锁定（只记录使用的物品）
    }

    // 物品记忆信息
    private static class ItemMemory {
        Item item;
        int slot;                  // 槽位索引
        int initialCount;          // 初始数量
        int initialDurability;     // 初始耐久度
        boolean isDamageable;      // 是否是可损坏物品

        // 触发阈值（从配置读取，不保存到记忆文件）
        int countThreshold;     // 数量低于此值触发补充，0表示用完才触发
        int durabilityThreshold; // 耐久剩余次数低于此值触发补充

        ItemMemory(ItemStack stack, int slot) {
            this.item = stack.getItem();
            this.slot = slot;
            this.initialCount = stack.getCount();

            // 检查物品是否有耐久度
            this.isDamageable = stack.isDamageableItem();
            if (this.isDamageable) {
                this.initialDurability = stack.getDamageValue();
            } else {
                this.initialDurability = -1;
            }

            // 从配置读取触发阈值
            var config = ConfigManager.getConfig();
            this.countThreshold = config.refillCountThreshold;
            this.durabilityThreshold = config.refillDurabilityThreshold;
        }

        // 检查物品是否匹配（只对比物品类型）
        boolean matches(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;
            return stack.getItem() == this.item;
        }

        // 检查是否需要补充（使用可配置阈值）
        boolean needsRefill(ItemStack stack) {
            if (stack.isEmpty()) return true;  // 物品用完了

            // 可损坏物品（工具/武器）
            if (this.isDamageable && stack.isDamageableItem()) {
                int maxDamage = stack.getMaxDamage();
                int currentDamage = stack.getDamageValue();
                int remainingUses = maxDamage - currentDamage;
                int remainingPercent = (remainingUses * 100) / maxDamage;

                // 当剩余耐久百分比 <= 阈值时触发补充
                // 阈值0表示用到坏才补充，阈值100表示一开始就补充
                return remainingPercent <= durabilityThreshold;
            }

            // 可堆叠物品：检查数量是否低于阈值
            // countThreshold = 0 表示用完才补充
            if (countThreshold > 0) {
                return stack.getCount() <= countThreshold;
            }

            return false;
        }
    }


    // 为攻击和放置分别维护状态
    private static class FeatureMemory {
        MemoryState currentState = MemoryState.UNINITIALIZED;
        Map<Integer, ItemMemory> initialMemory = new HashMap<>();  // 初始记录的所有物品
        ItemMemory lockedItem = null;  // 锁定的物品记忆
    }

    private final FeatureMemory attackMemory = new FeatureMemory();  // 攻击功能的记忆
    private final FeatureMemory placeMemory = new FeatureMemory();   // 放置功能的记忆

    // 快捷栏槽位范围（0-8是快捷栏，40是副手）
    private static final int HOTBAR_START = 0;
    private static final int HOTBAR_END = 8;
    private static final int OFFHAND_SLOT = 40;

    /**
     * 攻击功能调用 - 检查并补充主手物品
     */
    public void checkAndRefillForAttack(Minecraft client) {
        var config = ConfigManager.getConfig();
        if (!config.autoRefillMainHand) return;  // 只检查主手

        LocalPlayer player = client.player;
        if (player == null) return;

        Inventory inventory = player.getInventory();

        switch (attackMemory.currentState) {
            case UNINITIALIZED:
                initializeMemory(attackMemory, inventory, true);  // true表示只记录主手相关
                break;

            case INITIALIZED:
                detectUsedItemForAttack(attackMemory, player );
                break;

            case LOCKED:
                checkAndRefillLockedItem(attackMemory, player, inventory, true);
                break;
        }
    }

    /**
     * 放置功能调用 - 检查并补充主手或副手物品
     */
    public void checkAndRefillForPlace(Minecraft client) {
        var config = ConfigManager.getConfig();
        if (!config.autoRefillMainHand && !config.autoRefillOffHand) return;

        LocalPlayer player = client.player;
        if (player == null) return;

        Inventory inventory = player.getInventory();

        switch (placeMemory.currentState) {
            case UNINITIALIZED:
                initializeMemory(placeMemory, inventory, false);  // false表示记录主手和副手
                break;

            case INITIALIZED:
                detectUsedItemForPlace(placeMemory, player );
                break;

            case LOCKED:
                checkAndRefillLockedItem(placeMemory, player, inventory, false);
                break;
        }
    }

    /**
     * 初始化阶段：记录物品
     * @param forAttack true=只记录主手, false=记录主手和副手
     */
    private void initializeMemory(FeatureMemory memory, Inventory inventory, boolean forAttack) {
        memory.initialMemory.clear();

        // 记录快捷栏物品（0-8）
        for (int i = HOTBAR_START; i <= HOTBAR_END; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                memory.initialMemory.put(i, new ItemMemory(stack, i));
                Main.LOGGER.debug("初始化记录快捷栏[{}]: {} x{}", i,
                        stack.getItem().getDescriptionId(), stack.getCount());
            }
        }

        // 如果不只是攻击功能，才记录副手
        if (!forAttack) {
            ItemStack offHand = inventory.getItem(OFFHAND_SLOT);
            if (!offHand.isEmpty()) {
                memory.initialMemory.put(OFFHAND_SLOT, new ItemMemory(offHand, OFFHAND_SLOT));
                Main.LOGGER.debug("初始化记录副手: {} x{}",
                        offHand.getItem().getDescriptionId(), offHand.getCount());
            }
        }

        memory.currentState = MemoryState.INITIALIZED;
        Main.LOGGER.info("自动补货记忆初始化完成，记录了 {} 个物品", memory.initialMemory.size());
    }

    /**
     * 攻击功能：检测正在使用的物品
     */
    private void detectUsedItemForAttack(FeatureMemory memory, LocalPlayer player) {
        ItemStack currentMainHand = player.getMainHandItem();
        if (currentMainHand.isEmpty()) return;

        // 查找哪个初始记录物品发生了变化
        ItemMemory changedItem = findChangedItem(memory, currentMainHand);
        if (changedItem != null) {
            memory.lockedItem = new ItemMemory(currentMainHand, changedItem.slot);
            memory.currentState = MemoryState.LOCKED;
            memory.initialMemory.clear();
            Main.LOGGER.debug("攻击功能锁定物品: {} 来自槽位 {}, 当前数量: {}, 耐久: {}",
                    currentMainHand.getItem().getDescriptionId(),
                    changedItem.slot,
                    currentMainHand.getCount(),
                    currentMainHand.isDamageableItem() ? currentMainHand.getDamageValue() : "N/A");
        }
    }

    /**
     * 放置功能：检测正在使用的物品
     */
    private void detectUsedItemForPlace(FeatureMemory memory, LocalPlayer player) {
        var config = ConfigManager.getConfig();

        // 先检查主手
        if (config.autoRefillMainHand) {
            ItemStack currentMainHand = player.getMainHandItem();
            if (!currentMainHand.isEmpty()) {
                ItemMemory changedItem = findChangedItem(memory, currentMainHand);
                if (changedItem != null) {
                    memory.lockedItem = new ItemMemory(currentMainHand, changedItem.slot);
                    memory.currentState = MemoryState.LOCKED;
                    memory.initialMemory.clear();
                    Main.LOGGER.debug("放置功能锁定主手物品: {} 来自槽位 {}",
                            currentMainHand.getItem().getDescriptionId(), changedItem.slot);
                    return;
                }
            }
        }

        // 再检查副手
        if (config.autoRefillOffHand) {
            ItemStack currentOffHand = player.getOffhandItem();
            if (!currentOffHand.isEmpty()) {
                ItemMemory offHandMemory = memory.initialMemory.get(OFFHAND_SLOT);
                if (offHandMemory != null && hasItemChanged(offHandMemory, currentOffHand)) {
                    memory.lockedItem = new ItemMemory(currentOffHand, OFFHAND_SLOT);
                    memory.currentState = MemoryState.LOCKED;
                    memory.initialMemory.clear();
                    Main.LOGGER.debug("放置功能锁定副手物品: {}",
                            currentOffHand.getItem().getDescriptionId());
                }
            }
        }
    }

    /**
     * 查找哪个初始记录物品发生了变化
     */
    private ItemMemory findChangedItem(FeatureMemory memory, ItemStack currentItem) {
        for (Map.Entry<Integer, ItemMemory> entry : memory.initialMemory.entrySet()) {
            ItemMemory itemMemory = entry.getValue();

            // 跳过副手槽位（如果是在攻击功能中）
            if (entry.getKey() == OFFHAND_SLOT) continue;

            if (itemMemory.item == currentItem.getItem()) {
                if (hasItemChanged(itemMemory, currentItem)) {
                    return itemMemory;
                }
            }
        }
        return null;
    }

    /**
     * 检查物品是否发生了变化（数量减少或耐久减少）
     */
    private boolean hasItemChanged(ItemMemory memory, ItemStack currentStack) {
        if (currentStack.isEmpty()) return false;
        if (memory.item != currentStack.getItem()) return false;

        // 检查数量是否减少
        if (currentStack.getCount() < memory.initialCount) {
            Main.LOGGER.debug("物品数量从 {} 减少到 {}", memory.initialCount, currentStack.getCount());
            return true;  // 需要返回 true
        }

        // 如果是可损坏物品，检查耐久是否减少
        if (memory.isDamageable && currentStack.isDamageableItem()) {
            int currentDurability = currentStack.getDamageValue();
            if (currentDurability > memory.initialDurability) {
                Main.LOGGER.debug("物品耐久从 {} 增加到 {}", memory.initialDurability, currentDurability);
                return true;  // 需要返回 true
            }
        }

        return false;
    }

    /**
     * 锁定阶段：监控并补充锁定的物品
     */
    private void checkAndRefillLockedItem(FeatureMemory memory, LocalPlayer player,
                                          Inventory inventory, boolean isAttack) {
        if (memory.lockedItem == null) return;

        ItemStack currentItem = isAttack ? player.getMainHandItem() :
                (memory.lockedItem.slot == OFFHAND_SLOT ? player.getOffhandItem() : player.getMainHandItem());

        // 如果当前物品不是锁定的物品，或者需要补充
        if (!memory.lockedItem.matches(currentItem) || memory.lockedItem.needsRefill(currentItem)) {
            if (!tryRefillFromInventory(player, inventory, memory.lockedItem)) {
                memory.lockedItem = null;
                memory.currentState = MemoryState.UNINITIALIZED;
                Main.LOGGER.warn("物品无法补充，已清除记忆");
            }
        }
    }

    /**
     * 从背包中尝试补充物品到指定位置
     */
    private boolean tryRefillFromInventory(LocalPlayer player, Inventory inventory, ItemMemory memory) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return false;

        int targetSlot = memory.slot;

        // 先检查目标槽位是否就是记忆中的槽位且物品正确
        ItemStack targetStack = inventory.getItem(targetSlot);
        if (memory.matches(targetStack) && !memory.needsRefill(targetStack)) {
            return true; // 不需要补充
        }

        // 在背包中查找匹配的物品
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            // 跳过盔甲槽 (36-39)
            if (i >= 36 && i <= 39) continue;

            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !memory.matches(stack)) continue;

            // 如果物品在快捷栏但不是目标槽位，直接交换
            if (i <= HOTBAR_END) {
                if (i != targetSlot) {
                    mc.gameMode.handleInventoryMouseClick(
                            0, i, targetSlot, ClickType.SWAP, player
                    );
                    Main.LOGGER.debug("从槽位 {} 交换物品到槽位 {}", i, targetSlot);
                    return true;
                }
            }
            // 物品在主背包（9-35），需要两步操作
            else {
                // 第一步：将物品拿到光标上
                mc.gameMode.handleInventoryMouseClick(
                        0, i, 0, ClickType.PICKUP, player
                );

                // 第二步：放到目标槽位
                mc.gameMode.handleInventoryMouseClick(
                        0, targetSlot, 0, ClickType.PICKUP, player
                );

                Main.LOGGER.debug("从背包槽位 {} 移动物品到槽位 {}", i, targetSlot);
                return true;
            }
        }

        return false;
    }

    /**
     * 攻击功能关闭时调用
     */
    public void clearAttackMemory() {
        attackMemory.currentState = MemoryState.UNINITIALIZED;
        attackMemory.initialMemory.clear();
        attackMemory.lockedItem = null;
        Main.LOGGER.info("攻击功能自动补货记忆已清除");
    }

    /**
     * 放置功能关闭时调用
     */
    public void clearPlaceMemory() {
        placeMemory.currentState = MemoryState.UNINITIALIZED;
        placeMemory.initialMemory.clear();
        placeMemory.lockedItem = null;
        Main.LOGGER.info("放置功能自动补货记忆已清除");
    }

    /**
     * 当成功使用物品后调用（攻击成功时）
     */
    public void onAttackUsed() {
        if (attackMemory.currentState != MemoryState.LOCKED || attackMemory.lockedItem == null) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        ItemStack mainHand = client.player.getMainHandItem();
        if (attackMemory.lockedItem.matches(mainHand)) {
            // 只更新耐久度，不更新数量阈值
            if (attackMemory.lockedItem.isDamageable && mainHand.isDamageableItem()) {
                attackMemory.lockedItem.initialDurability = mainHand.getDamageValue();
            }
        }
    }

    /**
     * 当成功使用物品后调用（放置成功时）
     */
    public void onPlaceUsed() {
        if (placeMemory.currentState != MemoryState.LOCKED || placeMemory.lockedItem == null) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        var config = ConfigManager.getConfig();

        // 更新主手
        if (config.autoRefillMainHand && placeMemory.lockedItem.slot != OFFHAND_SLOT) {
            ItemStack mainHand = client.player.getMainHandItem();
            if (placeMemory.lockedItem.matches(mainHand)) {
                if (placeMemory.lockedItem.isDamageable && mainHand.isDamageableItem()) {
                    placeMemory.lockedItem.initialDurability = mainHand.getDamageValue();
                }
            }
        }

        // 更新副手
        if (config.autoRefillOffHand && placeMemory.lockedItem.slot == OFFHAND_SLOT) {
            ItemStack offHand = client.player.getOffhandItem();
            if (placeMemory.lockedItem.matches(offHand)) {
                if (placeMemory.lockedItem.isDamageable && offHand.isDamageableItem()) {
                    placeMemory.lockedItem.initialDurability = offHand.getDamageValue();
                }
            }
        }
    }
}