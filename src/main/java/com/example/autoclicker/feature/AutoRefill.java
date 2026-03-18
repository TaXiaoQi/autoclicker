package com.example.autoclicker.feature;

import com.example.autoclicker.Main;import com.example.autoclicker.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;import java.lang.reflect.Field;import java.lang.reflect.Method;

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

    // 获取当前选中的快捷栏槽位（适配不同版本）
    private int getSelectedSlot(Inventory inventory) {
        // 先尝试通过反射获取（兼容1.21.1-1.21.4）
        try {
            Field selectedField = Inventory.class.getDeclaredField("selected");
            selectedField.setAccessible(true);
            return selectedField.getInt(inventory);
        } catch (Exception e) {
            Main.LOGGER.debug("Reflection failed, trying getter method", e);
        }

        try {
            // 使用反射调用getSelectedSlot方法
            Method getterMethod = Inventory.class.getMethod("getSelectedSlot");
            return (int) getterMethod.invoke(inventory);
        } catch (Exception e) {
            Main.LOGGER.error("All methods to get selected slot failed", e);
            return 0; // 默认返回第一个槽位
        }
    }

    private boolean tryRefillMainHand(LocalPlayer player, Inventory inventory) {
        if (mainHandMemory == null || player == null) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return false;

        // 使用适配方法获取选中的快捷栏槽位
        int selectedSlot = getSelectedSlot( inventory);

        // 查找所有槽位中匹配的物品
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            // 跳过盔甲槽位 (36-39) 和副手槽位 (40)
            if (i >= 36 && i <= 40) continue;

            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == mainHandMemory) {
                if (i == selectedSlot) {
                    return true;
                }

                mc.gameMode.handleInventoryMouseClick(
                        0,
                        i,
                        selectedSlot,
                        ClickType.SWAP,
                        player
                );
                return true;
            }
        }
        return false;
    }

    private boolean tryRefillOffHand(LocalPlayer player, Inventory inventory) {
        if (offHandMemory == null || player == null) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return false;

        // 查找所有槽位中匹配的物品
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (i >= 36 && i <= 40) continue;

            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == offHandMemory) {
                mc.gameMode.handleInventoryMouseClick(
                        0,
                        i,
                        40,
                        ClickType.SWAP,
                        player
                );
                return true;
            }
        }
        return false;
    }

    public void onRefillSuccess() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            var config = ConfigManager.getConfig();

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