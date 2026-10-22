package com.example.autoclicker.tool;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.PiercingWeapon;

/**
 * 攻击方式适配：26.1~26.2 突刺类武器（长矛等）必须走 piercingAttack，
 * 普通 gameMode.attack 对它们只会播放突刺动画（反复伸缩）而无伤害。
 */
public final class AttackCompat {

    private AttackCompat() {
    }

    public static boolean attack(Minecraft client, Entity entity) {
        ItemStack stack = client.player.getMainHandItem();
        PiercingWeapon piercing = stack.get(DataComponents.PIERCING_WEAPON);
        if (piercing == null) {
            return false; // 非突刺武器走普通攻击
        }
        client.gameMode.piercingAttack(piercing);
        return true;
    }
}
