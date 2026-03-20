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
    private static final int HOTBAR_START = 0;
    private static final int HOTBAR_END = 8;

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
            // 配置开启，进入记忆管理
            manageAttackMainHandMemory(client.player);
        } else {
            // 配置关闭，检查并清除记忆
            if (attackMainHandMem != null) {
                Main.LOGGER.debug("攻击主手配置关闭，清除记忆");
                attackMainHandMem = null;
            }
        }

        // 处理副手
        if (config.autoRefillOffHand) {
            // 配置开启，进入记忆管理
            manageAttackOffHandMemory(client.player);
        } else {
            // 配置关闭，检查并清除记忆
            if (attackOffHandMem != null) {
                Main.LOGGER.debug("攻击副手配置关闭，清除记忆");
                attackOffHandMem = null;
            }
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
            if (placeMainHandMem != null) {
                Main.LOGGER.debug("放置主手配置关闭，清除记忆");
                placeMainHandMem = null;
            }
        }

        // 处理副手
        if (config.autoRefillOffHand) {
            managePlaceOffHandMemory(client.player);
        } else {
            if (placeOffHandMem != null) {
                Main.LOGGER.debug("放置副手配置关闭，清除记忆");
                placeOffHandMem = null;
            }
        }
    }

    /**
     * 第二步：管理攻击主手记忆
     */
    private void manageAttackMainHandMemory(LocalPlayer player) {
        int currentSlot = SelectedSlotHelper.getSelectedSlot();
        ItemStack currentItem = SelectedSlotHelper.getMainHandItem();

        if (attackMainHandMem == null) {
            // 没有记忆，写入新记忆
            if (!currentItem.isEmpty()) {
                attackMainHandMem = new Memory(currentItem, currentSlot);
                Main.LOGGER.debug("攻击主手写入新记忆: {} 槽位: {}",
                        currentItem.getItem(), currentSlot);
            }
        } else {
            // 有记忆，检查主手是否为空
            if (currentItem.isEmpty()) {
                // 主手为空，尝试补充
                Main.LOGGER.debug("攻击主手为空，尝试补充");
                boolean success = refillSlot(player, attackMainHandMem, currentSlot);

                if (success) {
                    // 补充成功，重新写入记忆
                    ItemStack newItem = SelectedSlotHelper.getMainHandItem();
                    attackMainHandMem.update(newItem);
                    attackMainHandMem.slot = currentSlot; // 更新槽位（可能切换了快捷栏）
                    Main.LOGGER.debug("攻击主手补充成功，更新记忆: {}", newItem.getItem());
                }
            } else {
                // 主手不为空，检查物品类型是否变化
                if (!attackMainHandMem.isSameType(currentItem)) {
                    // 物品已手动更换，更新记忆
                    attackMainHandMem = new Memory(currentItem, currentSlot);
                    Main.LOGGER.debug("攻击主手物品更换，更新记忆: {}", currentItem.getItem());
                }
            }
        }
    }

    /**
     * 第二步：管理攻击副手记忆
     */
    private void manageAttackOffHandMemory(LocalPlayer player) {
        ItemStack currentItem = SelectedSlotHelper.getOffHandItem();

        if (attackOffHandMem == null) {
            // 没有记忆，写入新记忆
            if (!currentItem.isEmpty()) {
                attackOffHandMem = new Memory(currentItem, OFFHAND_SLOT);
                Main.LOGGER.debug("攻击副手写入新记忆: {}", currentItem.getItem());
            }
        } else {
            // 有记忆，检查副手是否为空
            if (currentItem.isEmpty()) {
                // 副手为空，尝试补充
                Main.LOGGER.debug("攻击副手为空，尝试补充");
                boolean success = refillSlot(player, attackOffHandMem, OFFHAND_SLOT);

                if (success) {
                    // 补充成功，重新写入记忆
                    ItemStack newItem = SelectedSlotHelper.getOffHandItem();
                    attackOffHandMem.update(newItem);
                    Main.LOGGER.debug("攻击副手补充成功，更新记忆: {}", newItem.getItem());
                }
            } else {
                // 副手不为空，检查物品类型是否变化
                if (!attackOffHandMem.isSameType(currentItem)) {
                    // 物品已手动更换，更新记忆
                    attackOffHandMem = new Memory(currentItem, OFFHAND_SLOT);
                    Main.LOGGER.debug("攻击副手物品更换，更新记忆: {}", currentItem.getItem());
                }
            }
        }
    }

    /**
     * 第二步：管理放置主手记忆
     */
    private void managePlaceMainHandMemory(LocalPlayer player) {
        int currentSlot = SelectedSlotHelper.getSelectedSlot();
        ItemStack currentItem = SelectedSlotHelper.getMainHandItem();

        if (placeMainHandMem == null) {
            if (!currentItem.isEmpty()) {
                placeMainHandMem = new Memory(currentItem, currentSlot);
                Main.LOGGER.debug("放置主手写入新记忆: {} 槽位: {}",
                        currentItem.getItem(), currentSlot);
            }
        } else {
            if (currentItem.isEmpty()) {
                Main.LOGGER.debug("放置主手为空，尝试补充");
                boolean success = refillSlot(player, placeMainHandMem, currentSlot);

                if (success) {
                    ItemStack newItem = SelectedSlotHelper.getMainHandItem();
                    placeMainHandMem.update(newItem);
                    placeMainHandMem.slot = currentSlot;
                    Main.LOGGER.debug("放置主手补充成功，更新记忆: {}", newItem.getItem());
                }
            } else {
                if (!placeMainHandMem.isSameType(currentItem)) {
                    placeMainHandMem = new Memory(currentItem, currentSlot);
                    Main.LOGGER.debug("放置主手物品更换，更新记忆: {}", currentItem.getItem());
                }
            }
        }
    }

    /**
     * 第二步：管理放置副手记忆
     */
    private void managePlaceOffHandMemory(LocalPlayer player) {
        ItemStack currentItem = SelectedSlotHelper.getOffHandItem();

        if (placeOffHandMem == null) {
            if (!currentItem.isEmpty()) {
                placeOffHandMem = new Memory(currentItem, OFFHAND_SLOT);
                Main.LOGGER.debug("放置副手写入新记忆: {}", currentItem.getItem());
            }
        } else {
            if (currentItem.isEmpty()) {
                Main.LOGGER.debug("放置副手为空，尝试补充");
                boolean success = refillSlot(player, placeOffHandMem, OFFHAND_SLOT);

                if (success) {
                    ItemStack newItem = SelectedSlotHelper.getOffHandItem();
                    placeOffHandMem.update(newItem);
                    Main.LOGGER.debug("放置副手补充成功，更新记忆: {}", newItem.getItem());
                }
            } else {
                if (!placeOffHandMem.isSameType(currentItem)) {
                    placeOffHandMem = new Memory(currentItem, OFFHAND_SLOT);
                    Main.LOGGER.debug("放置副手物品更换，更新记忆: {}", currentItem.getItem());
                }
            }
        }
    }

    /**
     * 第三步：补充物品到指定槽位
     * @return true=补充成功, false=补充失败
     */
    private boolean refillSlot(LocalPlayer player, Memory memory, int targetSlot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return false;

        Inventory inv = player.getInventory();
        int windowId = player.containerMenu.containerId;

        // 优先从快捷栏找（0-8）
        int sourceSlot = findItemInHotbar(inv, memory.item);

        // 如果快捷栏没有，从背包找（9-35）
        if (sourceSlot == -1) {
            sourceSlot = findItemInInventory(inv, memory.item);
        }

        if (sourceSlot == -1) {
            // 没有找到可补充的物品，发送提示
            String itemName = memory.item.getHoverName().getString();
            Main.sendMessage("autoclicker.message.no_items_left",
                    Component.literal(itemName));
            Main.LOGGER.warn("背包中没有可补充的物品: {}", itemName);
            return false;
        }

        // 执行交换
        try {
            // 左键点击源槽位（拿起物品）
            mc.gameMode.handleInventoryMouseClick(windowId, sourceSlot, 0, ClickType.PICKUP, player);

            // 左键点击目标槽位（放下物品）
            mc.gameMode.handleInventoryMouseClick(windowId, targetSlot, 0, ClickType.PICKUP, player);

            // 如果手上还有物品（可能是堆叠物品），放回源槽位
            if (!player.containerMenu.getCarried().isEmpty()) {
                mc.gameMode.handleInventoryMouseClick(windowId, sourceSlot, 0, ClickType.PICKUP, player);
            }

            Main.LOGGER.info("成功补充物品到槽位: {}", targetSlot);
            return true;

        } catch (Exception e) {
            Main.LOGGER.error("补充物品失败", e);
            return false;
        }
    }

    /**
     * 在快捷栏中查找指定物品
     */
    private int findItemInHotbar(Inventory inv, ItemStack target) {
        for (int i = HOTBAR_START; i <= HOTBAR_END; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == target.getItem()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 在背包中查找指定物品（不包括快捷栏和盔甲槽）
     */
    private int findItemInInventory(Inventory inv, ItemStack target) {
        // 背包槽位 9-35
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == target.getItem()) {
                return i;
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
        Main.LOGGER.debug("清除攻击记忆");
    }

    /**
     * 清除放置记忆
     */
    public void clearPlaceMemory() {
        placeMainHandMem = null;
        placeOffHandMem = null;
        Main.LOGGER.debug("清除放置记忆");
    }

    /**
     * 清除所有记忆
     */
    public void clearAllMemory() {
        clearAttackMemory();
        clearPlaceMemory();
    }
}