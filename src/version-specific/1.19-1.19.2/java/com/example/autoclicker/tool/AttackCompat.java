package com.example.autoclicker.tool;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * 攻击方式适配：26.x 起突刺类武器（长矛等）需走 piercingAttack。
 * 26.x 之前的版本无突刺武器，恒返回 false，由调用方走普通 gameMode.attack。
 */
public final class AttackCompat {

    private AttackCompat() {
    }

    public static boolean attack(Minecraft client, Entity entity) {
        return false;
    }
}