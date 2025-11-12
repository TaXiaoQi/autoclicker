package com.example.autoclicker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoClicker implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("autoclicker");
    private boolean enabled = false;
    private int tickCounter = 0;
    private static final int ATTACK_INTERVAL = 20;
    private KeyMapping toggleKey;


    @Override
    public void onInitializeClient() {
        LOGGER.info("自动点击器初始化");

        toggleKey = new KeyMapping(
                "key.autoclicker.toggle",
                GLFW.GLFW_KEY_F8,
                net.minecraft.client.KeyMapping.Category.MISC
        );
        KeyBindingHelper.registerKeyBinding(toggleKey);
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }


    private void onClientTick(Minecraft client) {
        if (toggleKey.consumeClick()) {
            toggleEnabled();
            if (client.player != null) {
                String status = enabled ? "§a开启" : "§c关闭";
                client.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("自动攻击: " + status),
                        true
                );
            }
        }

        if (enabled && client.player != null) {
            tickCounter++;
            if (tickCounter >= ATTACK_INTERVAL) {
                performAttack(client);
                tickCounter = 0;
            }
        }
    }

    private void performAttack(Minecraft client) {
        try {
            if (client.gameMode == null || client.player == null) return;
            if (client.player.getAttackStrengthScale(0.0F) < 1.0F) return;

            var hitResult = client.hitResult;
            if (hitResult instanceof net.minecraft.world.phys.EntityHitResult entityHit) {
                var entity = entityHit.getEntity();

                if (entity instanceof net.minecraft.world.entity.decoration.ArmorStand ||
                        entity instanceof net.minecraft.world.entity.Mob) {

                    client.gameMode.attack(client.player, entity);
                    client.player.swing(InteractionHand.MAIN_HAND);
                }
            }
        } catch (Exception e) {
            LOGGER.error("自动攻击出错", e);
        }
    }


    private void toggleEnabled() {
        enabled = !enabled;
        tickCounter = 0;
        LOGGER.info("自动攻击已 {}", enabled ? "开启" : "关闭");
    }
}