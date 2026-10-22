package com.example.autoclicker.tool;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;

/**
 * 挥手动画 API 适配（MC 26.3+）：
 * LivingEntity.swing 需要 (InteractionHand, SwingAnimation, boolean) 三参签名。
 */
public final class SwingHelper {

    private SwingHelper() {
    }

    /** 播放手臂挥动动画（等价旧版 swing(hand) 的默认行为） */
    public static void swingArm(LocalPlayer player, InteractionHand hand) {
        player.swing(hand, SwingAnimation.DEFAULT, true);
    }
}
