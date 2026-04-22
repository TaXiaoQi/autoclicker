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
            if (isHotbarSlot(sourceSlot) && isHotbarSlot(targetSlot)) {
                swapBetweenHotbar(player, sourceSlot, targetSlot);
            } else if (sourceSlot == OFFHAND_SLOT || targetSlot == OFFHAND_SLOT) {
                swapWithOffhand(player, sourceSlot, targetSlot);
            } else {
                // 使用新方法名 handleContainerInput
                mc.gameMode.handleContainerInput(
                        player.containerMenu.containerId,
                        sourceSlot,
                        targetSlot,  // buttonNum
                        ContainerInput.SWAP,
                        player
                );
            }
            return true;
        } catch (Exception e) {
            Main.sendMessage("autoclicker.message.refill_failed");
            return false;
        }
    }

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

        // 3. 如果目标是副手，可以从快捷栏找
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

    private boolean isHotbarSlot(int slot) {
        return slot >= 0 && slot <= 8;
    }

    private void swapBetweenHotbar(LocalPlayer player, int slot1, int slot2) {
        ItemStack originalStack2 = player.getInventory().getItem(slot2).copy();

        if (mc.gameMode != null) {
            mc.gameMode.handleContainerInput(
                    player.containerMenu.containerId,
                    slot1,
                    0,
                    ContainerInput.PICKUP,
                    player
            );
        }

        if (mc.gameMode != null) {
            mc.gameMode.handleContainerInput(
                    player.containerMenu.containerId,
                    slot2,
                    0,
                    ContainerInput.PICKUP,
                    player
            );
        }

        if (!originalStack2.isEmpty()) {
            if (mc.gameMode != null) {
                mc.gameMode.handleContainerInput(
                        player.containerMenu.containerId,
                        slot1,
                        0,
                        ContainerInput.PICKUP,
                        player
                );
            }
        }
    }

    private void swapWithOffhand(LocalPlayer player, int sourceSlot, int targetSlot) {
        if (targetSlot == OFFHAND_SLOT) {
            if (mc.gameMode != null) {
                mc.gameMode.handleContainerInput(
                        player.containerMenu.containerId,
                        sourceSlot,
                        0,
                        ContainerInput.SWAP,
                        player
                );
            }
        } else {
            if (mc.gameMode != null) {
                mc.gameMode.handleContainerInput(
                        player.containerMenu.containerId,
                        OFFHAND_SLOT,
                        targetSlot,
                        ContainerInput.SWAP,
                        player
                );
            }
        }
    }
}