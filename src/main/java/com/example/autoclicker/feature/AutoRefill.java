package com.example.autoclicker.feature;

import com.example.autoclicker.Main;
import com.example.autoclicker.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;import java.lang.reflect.Field;import java.lang.reflect.Method;

public class AutoRefill {
    // 记录主手和副手的记忆物品
    private Item mainHandMemory = null;
    private Item offHandMemory = null;

    // 是否启用自动补充
    private boolean enabled = false;

    // 最后一次补充时间
    private long lastRefillTime = 0;
    private static final long REFILL_COOLDOWN_TICKS = 5;

    public void tick(Minecraft client) {
        if (!enabled || client.player == null) return;
        if (!ConfigManager.getConfig().autoRefillEnabled) return;

        // 冷却检查
        long currentTime = client.level != null ? client.level.getGameTime() : 0;
        if (currentTime - lastRefillTime < REFILL_COOLDOWN_TICKS) return;

        Player player = client.player;
        Inventory inventory = player.getInventory();

        boolean refilled = false;

        // 检查并补充主手
        if (mainHandMemory != null) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.isEmpty() || mainHand.getItem() != mainHandMemory) {
                if (tryRefillFromInventory(inventory, player, true)) {
                    refilled = true;
                } else {
                    mainHandMemory = null;
                }
            }
        }

        // 检查并补充副手
        if (offHandMemory != null) {
            ItemStack offHand = player.getOffhandItem();
            if (offHand.isEmpty() || offHand.getItem() != offHandMemory) {
                if (tryRefillFromInventory(inventory, player, false)) {
                    refilled = true;
                } else {
                    offHandMemory = null;
                }
            }
        }

        if (refilled) {
            lastRefillTime = currentTime;
        }
    }

    // 不同版本获取主手的方法
    private int getSelectedSlot(Inventory inventory) {
        try {
            // 方法1: 尝试直接访问 selected 字段 (1.21.1-1.21.4)
            Field selectedField = Inventory.class.getDeclaredField("selected");
            selectedField.setAccessible(true);
            return (int) selectedField.get(inventory);
        } catch (Exception e1) {
            try {
                // 方法2: 尝试 getSelected() 方法 (1.21.5+)
                Method getSelectedMethod = Inventory.class.getMethod("getSelected");
                return (int) getSelectedMethod.invoke(inventory);
            } catch (Exception e2) {
                // 方法3: 尝试 getSelectedSlot() 方法
                try {
                    Method getSelectedSlotMethod = Inventory.class.getMethod("getSelectedSlot");
                    return (int) getSelectedSlotMethod.invoke(inventory);
                } catch (Exception e3) {
                    // 默认返回 0
                    return 0;
                }
            }
        }
    }

    private boolean tryRefillFromInventory(Inventory inventory, Player player, boolean isMainHand) {
        Item targetItem = isMainHand ? mainHandMemory : offHandMemory;
        if (targetItem == null) return false;

        // 从背包查找物品（跳过快捷栏当前选中的槽位）
        int startSlot = 9; // 背包槽位从9开始
        int hotbarSlot = getSelectedSlot(player.getInventory());  // 当前选中的快捷栏槽位

        for (int i = startSlot; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == targetItem) {
                // 找到物品，交换到手中
                if (isMainHand) {
                    // 交换背包物品和主手物品
                    ItemStack currentMainHand = player.getMainHandItem().copy();
                    inventory.setItem(hotbarSlot, stack.copy());
                    inventory.setItem(i, currentMainHand);
                } else {
                    // 副手处理
                    ItemStack currentOffHand = player.getOffhandItem().copy();

                    // 设置副手为新物品 - 使用正确的setItem方法
                    player.getInventory().setItem(Inventory.SLOT_OFFHAND, stack.copy());

                    // 原副手物品放入背包空位
                    if (!currentOffHand.isEmpty()) {
                        for (int j = startSlot; j < inventory.getContainerSize(); j++) {
                            if (inventory.getItem(j).isEmpty()) {
                                inventory.setItem(j, currentOffHand);
                                break;
                            }
                        }
                    }

                    // 清空原背包槽位
                    inventory.setItem(i, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }

    public void onAutoPlaceEnabled() {
        enabled = true;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            ItemStack mainHand = client.player.getMainHandItem();
            mainHandMemory = mainHand.isEmpty() ? null : mainHand.getItem();

            ItemStack offHand = client.player.getOffhandItem();
            offHandMemory = offHand.isEmpty() ? null : offHand.getItem();

            Main.LOGGER.info("自动补充已启用");
        }
    }

    public void onRefillSuccess() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            ItemStack mainHand = client.player.getMainHandItem();
            mainHandMemory = mainHand.isEmpty() ? null : mainHand.getItem();

            ItemStack offHand = client.player.getOffhandItem();
            offHandMemory = offHand.isEmpty() ? null : offHand.getItem();
        }
    }

    public void onAutoPlaceDisabled() {
        enabled = false;
        clearMemory();
    }

    public void onDisconnect() {
        enabled = false;
        clearMemory();
        lastRefillTime = 0;
    }

    private void clearMemory() {
        mainHandMemory = null;
        offHandMemory = null;
    }
}