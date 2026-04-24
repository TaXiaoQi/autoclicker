package com.example.autoclicker.toor;

import com.example.autoclicker.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

public class Replace {

    private static final int OFFHAND_SLOT = 40;
    private final Minecraft mc;

    public Replace() {
        this.mc = Minecraft.getInstance();
    }

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

    private int findSourceSlot(Inventory inv, ItemStack target, int targetSlot) {
        // 1. 背包（9-35）
        for (int i = 9; i < 36; i++) {
            if (i == targetSlot) continue;
            if (matches(inv.getItem(i), target)) return i;
        }
        // 2. 快捷栏（0-8）
        for (int i = 0; i <= 8; i++) {
            if (i == targetSlot) continue;
            if (matches(inv.getItem(i), target)) return i;
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

    private void doSwap(LocalPlayer player, int source, int target) {
        if (isHotbar(source) && isHotbar(target)) {
            swapHotbarByPickup(player, source, target);
        } else if (!isHotbar(source) && source != OFFHAND_SLOT && isHotbar(target)) {
            swapInventoryToHotbar(player, source, target);
        } else if (target == OFFHAND_SLOT) {
            swapToOffhand(player, source);
        } else if (source == OFFHAND_SLOT) {
            swapFromOffhand(player, target);
        } else {
            // 兜底
            handleInput(player, source, target, ContainerInput.SWAP);
        }
    }

    private boolean isHotbar(int slot) {
        return slot >= 0 && slot <= 8;
    }

    // ========== 26.1 使用 ContainerInput + handleContainerInput ==========

    /** 快捷栏之间：模拟鼠标拾取交换 */
    private void swapHotbarByPickup(LocalPlayer player, int slot1, int slot2) {
        ItemStack stack2 = player.getInventory().getItem(slot2).copy();

        handleInput(player, slot1, 0, ContainerInput.PICKUP);
        handleInput(player, slot2, 0, ContainerInput.PICKUP);
        if (!stack2.isEmpty()) {
            handleInput(player, slot1, 0, ContainerInput.PICKUP);
        }
    }

    /** 背包 → 快捷栏：SWAP */
    private void swapInventoryToHotbar(LocalPlayer player, int source, int target) {
        handleInput(player, source, target, ContainerInput.SWAP);
    }

    /** → 副手 */
    private void swapToOffhand(LocalPlayer player, int source) {
        handleInput(player, source, 0, ContainerInput.SWAP);
    }

    /** 副手 → */
    private void swapFromOffhand(LocalPlayer player, int target) {
        handleInput(player, OFFHAND_SLOT, target, ContainerInput.SWAP);
    }

    /** 统一操作 */
    private void handleInput(LocalPlayer player, int slot, int button, ContainerInput input) {
        if (mc.gameMode != null) {
            mc.gameMode.handleContainerInput(
                    player.containerMenu.containerId, slot, button, input, player);
        }
    }
}