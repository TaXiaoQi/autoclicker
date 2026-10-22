package com.example.autoclicker.tool;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

/**
 * 挥手动画 API 适配：main 源码只面向本类编程。
 * MC 26.3 起 LivingEntity.swing 需要 SwingAnimation 参数，各版本组提供对应实现。
 */
public final class SwingHelper {

    private SwingHelper() {
    }

    /** 播放手臂挥动动画 */
    public static void swingArm(LocalPlayer player, InteractionHand hand) {
        player.swing(hand);
    }
}
