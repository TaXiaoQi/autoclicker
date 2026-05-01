package com.example.autoclicker.toor;

import com.example.autoclicker.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
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
                // 源在快捷栏(0-8)或副手(40)
                int originalSlot = inv.getSelectedSlot();

                if (targetSlot == OFFHAND_SLOT) {
                    // 目标副手：切到源 → F → 切回
                    inv.setSelectedSlot(sourceSlot);
                    if (mc.player != null) {
                        mc.player.connection.send(new ServerboundSetCarriedItemPacket(sourceSlot));
                    }
                    if (mc.player != null) {
                        mc.player.connection.send(new ServerboundPlayerActionPacket(
                                ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                                BlockPos.ZERO,
                                Direction.DOWN
                        ));
                    }
                    inv.setSelectedSlot(originalSlot);
                    if (mc.player != null) {
                        mc.player.connection.send(new ServerboundSetCarriedItemPacket(originalSlot));
                    }
                } else if (sourceSlot == OFFHAND_SLOT) {
                    // 源在副手 → 目标快捷栏：F 交换
                    if (mc.player != null) {
                        mc.player.connection.send(new ServerboundPlayerActionPacket(
                                ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                                BlockPos.ZERO,
                                Direction.DOWN
                        ));
                    }
                } else {
                    // 源快捷栏 → 目标快捷栏：切到源
                    inv.setSelectedSlot(sourceSlot);
                    if (mc.player != null) {
                        mc.player.connection.send(new ServerboundSetCarriedItemPacket(sourceSlot));
                    }
                }
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