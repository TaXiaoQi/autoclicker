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
     * 自动从背包或快捷栏查找，根据源位置选择不同的交换方式
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
            doSwap(player, sourceSlot, targetSlot);
            return true;
        } catch (Exception e) {
            Main.sendMessage("autoclicker.message.refill_failed");
            return false;
        }
    }

    /**
     * 通用查找方法：从除 targetSlot 外的所有槽位（0-40）查找目标物品
     * 查找顺序：背包(9-35) → 快捷栏(0-8) → 副手(40)
     */
    private int findSourceSlot(Inventory inv, ItemStack target, int targetSlot) {
        // 1. 背包（9-35）
        for (int i = 9; i < 36; i++) {
            if (i == targetSlot) continue;
            if (matches(inv.getItem(i), target)) {
                return i;
            }
        }

        // 2. 快捷栏（0-8）
        for (int i = 0; i <= 8; i++) {
            if (i == targetSlot) continue;
            if (matches(inv.getItem(i), target)) {
                return i;
            }
        }

        // 3. 副手（40）
        if (targetSlot != OFFHAND_SLOT && matches(inv.getItem(OFFHAND_SLOT), target)) {
            return OFFHAND_SLOT;
        }

        return -1;
    }

    private boolean matches(ItemStack stack, ItemStack target) {
        return !stack.isEmpty() && stack.getItem() == target.getItem();
    }

    /**
     * 根据源位置和目标位置选择交换方式
     */
    private void doSwap(LocalPlayer player, int source, int target) {
        // 情况1：两个都是快捷栏 → 模拟鼠标拾取交换
        if (isHotbar(source) && isHotbar(target)) {
            swapHotbarByPickup(player, source, target);
        }
        // 情况2：源在背包，目标在快捷栏 → SWAP 按键交换
        else if (!isHotbar(source) && source != OFFHAND_SLOT && isHotbar(target)) {
            swapInventoryToHotbar(player, source, target);
        }
        // 情况3：目标为副手 → SWAP 交换
        else if (target == OFFHAND_SLOT) {
            swapToOffhand(player, source);
        }
        // 情况4：源为副手 → SWAP 交换
        else if (source == OFFHAND_SLOT) {
            swapFromOffhand(player, target);
        }
        // 情况5：其他（如两个都在背包，一般不会出现）
        else {
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryMouseClick(
                        player.containerMenu.containerId, source, target, ClickType.SWAP, player);
            }
        }
    }

    private boolean isHotbar(int slot) {
        return slot >= 0 && slot <= 8;
    }

    // ========== 交换方式实现 ==========

    /** 快捷栏之间：模拟鼠标拾取交换 */
    private void swapHotbarByPickup(LocalPlayer player, int slot1, int slot2) {
        ItemStack stack2 = player.getInventory().getItem(slot2).copy();

        // 拿起 slot1
        click(player, slot1, 0, ClickType.PICKUP);
        // 放到 slot2
        click(player, slot2, 0, ClickType.PICKUP);
        // 如果 slot2 原来有物品，放回 slot1
        if (!stack2.isEmpty()) {
            click(player, slot1, 0, ClickType.PICKUP);
        }
    }

    /** 背包 → 快捷栏：数字键 SWAP */
    private void swapInventoryToHotbar(LocalPlayer player, int source, int target) {
        click(player, source, target, ClickType.SWAP);
    }

    /** → 副手：F 键 SWAP */
    private void swapToOffhand(LocalPlayer player, int source) {
        click(player, source, 0, ClickType.SWAP);
    }

    /** 副手 → 快捷栏：F 键 SWAP */
    private void swapFromOffhand(LocalPlayer player, int target) {
        click(player, OFFHAND_SLOT, target, ClickType.SWAP);
    }

    /** 统一点击方法 */
    private void click(LocalPlayer player, int slot, int button, ClickType type) {
        if (mc.gameMode != null) {
            mc.gameMode.handleInventoryMouseClick(
                    player.containerMenu.containerId, slot, button, type, player);
        }
    }
}