package com.example.autoclicker.tool;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 右键方块适配：1.19+ 为 useItemOn(player, hand, hit) 三参签名。
 */
public final class ItemUseAdapter {

    private ItemUseAdapter() {
    }

    public static InteractionResult useItemOn(Minecraft client, InteractionHand hand, BlockHitResult hit) {
        return client.gameMode.useItemOn(client.player, hand, hit);
    }
}
