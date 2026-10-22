package com.example.autoclicker.tool;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/**
 * 可种植/可骨粉物品的版本差异适配：main 只保留各 era 通用常量，
 * 高版本新增物品由各 era 的 Plantables 提供判断。
 */
public final class Plantables {

    private Plantables() {
    }

    /** 1.19 系新增的可种植物品 */
    public static boolean isExtraPlantable(Item item) {
        return item == Items.MANGROVE_PROPAGULE ||  // 红树胎生苗（1.19）
                item == Items.GLOW_BERRIES ||       // 发光浆果（1.17）
                item == Items.AZALEA ||             // 杜鹃花丛（1.17）
                item == Items.FLOWERING_AZALEA;     // 盛开的杜鹃花丛（1.17）
    }

    /** 1.19.3~1.19.4 无额外骨粉目标（1.17+ 方块 main 已直接引用） */
    public static boolean isExtraBonemealTarget(Block block) {
        return false;
    }
}
