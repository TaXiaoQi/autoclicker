package com.example.autoclicker.tool;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * 可种植/可骨粉物品的版本差异适配：main 只保留各 era 通用常量，
 * 高版本新增物品由各 era 的 Plantables 提供判断。
 */
public final class Plantables {

    private Plantables() {
    }

    /** 1.16.5 无版本差异新增物品 */
    public static boolean isExtraPlantable(Item item) {
        return false;
    }

    /** 1.16.5 无版本差异骨粉目标（洞穴藤蔓/垂滴叶/杜鹃为 1.17 新增） */
    public static boolean isExtraBonemealTarget(Block block) {
        return false;
    }
}
