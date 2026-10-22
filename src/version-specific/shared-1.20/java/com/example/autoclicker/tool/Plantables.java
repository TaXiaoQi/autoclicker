package com.example.autoclicker.tool;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * 可种植/可骨粉物品的版本差异适配：main 只保留各 era 通用常量，
 * 高版本新增物品由各 era 的 Plantables 提供判断。
 */
public final class Plantables {

    private Plantables() {
    }

    /** 1.20+ 新增的可种植物品 */
    public static boolean isExtraPlantable(Item item) {
        return item == Items.TORCHFLOWER_SEEDS ||   // 火炬花种子（1.20）
                item == Items.PITCHER_POD ||        // 瓶子草荚（1.20）
                item == Items.CHERRY_SAPLING;       // 樱花树苗（1.20）
    }

    /** 1.20+ 新增骨粉目标 */
    public static boolean isExtraBonemealTarget(Block block) {
        return block == Blocks.PITCHER_CROP;   // 瓶子草作物（1.20，两格高花，骨粉催熟 3 阶段）
    }
}
