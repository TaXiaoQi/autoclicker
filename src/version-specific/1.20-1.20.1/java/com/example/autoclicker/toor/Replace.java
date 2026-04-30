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

    public boolean refillSlot(LocalPlayer player, ItemStack targetItem, int targetSlot) {
        if (mc.gameMode == null) return false;

        Inventory inv = player.getInventory();
        int sourceSlot = findSourceSlot(inv, targetItem, targetSlot);

        if (sourceSlot == -1) {
            Main.sendMessage("autoclicker.message.no_items_left",
                    Component.literal(targetItem.getHoverName().getString()));
            return false;
        }

        Main.LOGGER.info("[AutoRefill] Refilling: slot {} -> slot {}", sourceSlot, targetSlot);

        try {
            if (sourceSlot >= 9 && sourceSlot <= 35) {
                // 源在背包：SWAP
                mc.gameMode.handleInventoryMouseClick(
                        player.containerMenu.containerId,
                        sourceSlot,
                        targetSlot,
                        ClickType.SWAP,
                        player
                );
            } else {
                // 源在快捷栏：PICKUP 两次
                mc.gameMode.handleInventoryMouseClick(
                        player.containerMenu.containerId,
                        sourceSlot,
                        0,
                        ClickType.PICKUP,
                        player
                );
                mc.gameMode.handleInventoryMouseClick(
                        player.containerMenu.containerId,
                        targetSlot,
                        0,
                        ClickType.PICKUP,
                        player
                );
            }

            return true;
        } catch (Exception e) {
            Main.LOGGER.error("补货失败", e);
            Main.sendMessage("autoclicker.message.refill_failed");
            return false;
        }
    }

    private int findSourceSlot(Inventory inv, ItemStack target, int targetSlot) {
        for (int i = 9; i < 36; i++) {
            if (i != targetSlot && matches(inv.getItem(i), target)) {
                return i;
            }
        }
        for (int i = 0; i <= 8; i++) {
            if (i != targetSlot && matches(inv.getItem(i), target)) {
                return i;
            }
        }
        if (targetSlot != OFFHAND_SLOT && matches(inv.getItem(OFFHAND_SLOT), target)) {
            return OFFHAND_SLOT;
        }
        return -1;
    }

    private boolean matches(ItemStack stack, ItemStack target) {
        return !stack.isEmpty() && stack.getItem() == target.getItem();
    }
}