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
        int minCount;              // 初始数量（用于判断是否用完）
        int currentDurability;     // 初始耐久度（如果是可损坏物品）
        boolean isDamageable;      // 是否是可损坏物品

        ItemMemory(ItemStack stack, int slot) {
            this.item = stack.getItem();
            this.slot = slot;
            this.minCount = stack.getCount();

            // 检查物品是否有耐久度
            this.isDamageable = stack.isDamageableItem();
            if (this.isDamageable) {
                this.currentDurability = stack.getDamageValue();
            } else {
                this.currentDurability = -1;
            }
        }

        // 检查物品是否匹配（只对比物品类型）
        boolean matches(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;
            return stack.getItem() == this.item;
        }

        // 检查是否需要补充
        boolean needsRefill(ItemStack stack) {
            if (stack.isEmpty()) return true;  // 物品用完了

            // 可损坏物品（工具/武器）
            if (this.isDamageable && stack.isDamageableItem()) {
                int remainingUses = stack.getMaxDamage() - stack.getDamageValue();
                return remainingUses <= 1;  // 耐久剩1次
            }

            return false;
        }
    }

    private MemoryState currentState = MemoryState.UNINITIALIZED;

    // 初始记录的所有物品（槽位 -> 物品记忆）
    private final Map<Integer, ItemMemory> initialMemory = new HashMap<>();

    // 锁定的物品记忆（只记录被使用的物品）
    private ItemMemory lockedMainHand = null;
    private ItemMemory lockedOffHand = null;

    // 快捷栏槽位范围（0-8是快捷栏，40是副手）
    private static final int HOTBAR_START = 0;
    private static final int HOTBAR_END = 8;
    private static final int OFFHAND_SLOT = 40;

    public void checkAndRefill(Minecraft client) {
        var config = ConfigManager.getConfig();

        // 主手和副手补充都关闭，直接返回
        if (!config.autoRefillMainHand && !config.autoRefillOffHand) {
            return;
        }

        LocalPlayer player = client.player;
        if (player == null) return;

        Inventory inventory = player.getInventory();

        // 根据当前状态执行不同逻辑
        switch (currentState) {
            case UNINITIALIZED:
                initializeMemory( inventory);
                break;

            case INITIALIZED:
                detectUsedItems(player );
                break;

            case LOCKED:
                // 分别检查主手和副手是否开启
                checkAndRefillLockedItems(player, inventory);
                break;
        }
    }

    /**
     * 初始化阶段：记录所有快捷栏和副手的物品
     */
    private void initializeMemory(Inventory inventory) {
        initialMemory.clear();

        // 记录快捷栏物品（0-8）
        for (int i = HOTBAR_START; i <= HOTBAR_END; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                initialMemory.put(i, new ItemMemory(stack, i));
                Main.LOGGER.debug("初始化记录快捷栏[{}]: {} x{}", i,
                        stack.getItem().getDescriptionId(), stack.getCount());
            }
        }

        // 记录副手物品
        ItemStack offHand = inventory.getItem(OFFHAND_SLOT);
        if (!offHand.isEmpty()) {
            initialMemory.put(OFFHAND_SLOT, new ItemMemory(offHand, OFFHAND_SLOT));
            Main.LOGGER.debug("初始化记录副手: {} x{}",
                    offHand.getItem().getDescriptionId(), offHand.getCount());
        }

        currentState = MemoryState.INITIALIZED;
        Main.LOGGER.info("自动补货记忆初始化完成，记录了 {} 个物品", initialMemory.size());
    }

    /**
     * 检测阶段：通过对比物品变化找出正在使用的物品
     */
    private void detectUsedItems(LocalPlayer player) {
        var config = ConfigManager.getConfig();
        boolean mainHandLocked = false;
        boolean offHandLocked = false;

        // 原只有开启主手补充时才检测主手
        if (config.autoRefillMainHand) {
            ItemStack currentMainHand = player.getMainHandItem();
            if (!currentMainHand.isEmpty()) {
                // 查找哪个初始记录物品发生了变化（数量减少或耐久减少）
                ItemMemory changedItem = findChangedItem( currentMainHand);
                if (changedItem != null) {
                    lockedMainHand = new ItemMemory(currentMainHand, changedItem.slot);
                    mainHandLocked = true;
                    Main.LOGGER.debug("检测到主手物品变化并锁定: {} 来自槽位 {}, 当前数量: {}, 耐久: {}",
                            currentMainHand.getItem().getDescriptionId(),
                            changedItem.slot,
                            currentMainHand.getCount(),
                            currentMainHand.isDamageableItem() ? currentMainHand.getDamageValue() : "N/A");
                }
            }
        }

        // 原只有开启副手补充时才检测副手
        if (config.autoRefillOffHand) {
            ItemStack currentOffHand = player.getOffhandItem();
            if (!currentOffHand.isEmpty()) {
                // 查找副手的变化
                ItemMemory offHandMemory = initialMemory.get(OFFHAND_SLOT);
                if (offHandMemory != null && hasItemChanged(offHandMemory, currentOffHand)) {
                    lockedOffHand = new ItemMemory(currentOffHand, OFFHAND_SLOT);
                    offHandLocked = true;
                    Main.LOGGER.debug("检测到副手物品变化并锁定: {} 数量: {}, 耐久: {}",
                            currentOffHand.getItem().getDescriptionId(),
                            currentOffHand.getCount(),
                            currentOffHand.isDamageableItem() ? currentOffHand.getDamageValue() : "N/A");
                }
            }
        }

        // 如果两个都锁定成功（或不需要锁定的已完成），进入锁定状态
        boolean mainHandDone = !config.autoRefillMainHand || mainHandLocked;
        boolean offHandDone = !config.autoRefillOffHand || offHandLocked;

        if (mainHandDone && offHandDone) {
            currentState = MemoryState.LOCKED;
            // 清理初始记忆释放内存
            initialMemory.clear();
            Main.LOGGER.info("自动补货已锁定监控物品");
        }
    }

    /**
     * 查找哪个初始记录物品发生了变化
     */
    private ItemMemory findChangedItem(ItemStack currentItem) {
        // 先检查当前手持物品是否与任何初始记录匹配（同类型）
        for (Map.Entry<Integer, ItemMemory> entry : initialMemory.entrySet()) {
            ItemMemory memory = entry.getValue();

            // 跳过副手槽位
            if (entry.getKey() == OFFHAND_SLOT) continue;

            // 如果是同类型物品
            if (memory.item == currentItem.getItem()) {
                // 检查是否发生了变化
                if (hasItemChanged(memory, currentItem)) {
                    return memory;
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
        if (currentStack.getCount() < memory.minCount) {
            Main.LOGGER.debug("物品数量从 {} 减少到 {}", memory.minCount, currentStack.getCount());
            return true;
        }

        // 如果是可损坏物品，检查耐久是否减少
        if (memory.isDamageable && currentStack.isDamageableItem()) {
            int currentDurability = currentStack.getDamageValue();
            if (currentDurability > memory.currentDurability) {
                Main.LOGGER.debug("物品耐久从 {} 增加到 {}", memory.currentDurability, currentDurability);
                return true;
            }
        }

        return false;
    }

    /**
     * 锁定阶段：监控并补充锁定的物品
     */
    private void checkAndRefillLockedItems(LocalPlayer player, Inventory inventory) {
        var config = ConfigManager.getConfig();

        // 原版开关判定：只有开启主手补充时才检查主手
        if (config.autoRefillMainHand && lockedMainHand != null) {
            ItemStack currentMainHand = player.getMainHandItem();

            // 如果当前主手物品不是锁定的物品，或者需要补充
            if (!lockedMainHand.matches(currentMainHand) ||
                    lockedMainHand.needsRefill(currentMainHand)) {

                if (!tryRefillFromInventory(player, inventory, lockedMainHand, true)) {
                    // 如果无法补充，清除记忆（原版逻辑）
                    lockedMainHand = null;
                    Main.LOGGER.warn("主手物品无法补充，已清除记忆");
                }
            }
        }

        // 只有开启副手补充时才检查副手
        if (config.autoRefillOffHand && lockedOffHand != null) {
            ItemStack currentOffHand = player.getOffhandItem();

            if (!lockedOffHand.matches(currentOffHand) ||
                    lockedOffHand.needsRefill(currentOffHand)) {

                if (!tryRefillFromInventory(player, inventory, lockedOffHand, false)) {
                    lockedOffHand = null;
                    Main.LOGGER.warn("副手物品无法补充，已清除记忆");
                }
            }
        }
    }

    /**
     * 从背包中尝试补充物品到指定位置
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean tryRefillFromInventory(LocalPlayer player, Inventory inventory,
                                           ItemMemory memory, boolean isMainHand) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return false;

        int targetSlot = isMainHand ? memory.slot : OFFHAND_SLOT;

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
     * 重置所有记忆
     */
    public void clearAllMemory() {
        currentState = MemoryState.UNINITIALIZED;
        initialMemory.clear();
        lockedMainHand = null;
        lockedOffHand = null;
        Main.LOGGER.info("自动补货记忆已清除");
    }
    }