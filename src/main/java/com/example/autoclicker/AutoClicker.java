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
    private static AutoClicker INSTANCE;

    private boolean enabled = false;
    private int tickCounter = 0;
    private Config config;
    private KeyMapping toggleKey;
    private KeyMapping configScreenKey;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        LOGGER.info("自动点击器初始化");

        // 加载配置
        config = Config.load();

        // 切换开关按键
        toggleKey = new KeyMapping(
                "key.autoclicker.toggle",
                GLFW.GLFW_KEY_F8,
                net.minecraft.client.KeyMapping.Category.MISC
        );

        // 配置菜单按键
        configScreenKey = new KeyMapping(
                "key.autoclicker.config",
                GLFW.GLFW_KEY_F9,
                net.minecraft.client.KeyMapping.Category.MISC
        );

        KeyBindingHelper.registerKeyBinding(toggleKey);
        KeyBindingHelper.registerKeyBinding(configScreenKey);
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(Minecraft client) {
        // 切换开关
        if (toggleKey.consumeClick()) {
            toggleEnabled();
            if (client.player != null) {
                String status = enabled ? "§a开启" : "§c关闭";
                client.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("自动攻击: " + status + " | 间隔: " + config.attackInterval + "ticks"),
                        true
                );
            }
        }

        // 打开配置屏幕
        if (configScreenKey.consumeClick() && client.player != null) {
            client.setScreen(new ConfigScreen(null));
        }

        if (enabled && client.player != null) {
            tickCounter++;
            if (tickCounter >= config.attackInterval) {
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

                // 攻击盔甲架
                switch (entity) {
                    case net.minecraft.world.entity.decoration.ArmorStand armorStand when config.attackArmorStands -> {
                        client.gameMode.attack(client.player, entity);
                        client.player.swing(InteractionHand.MAIN_HAND);
                    }
                    // 攻击敌对生物
                    case net.minecraft.world.entity.Mob mob when config.attackHostileMobs -> {
                        if (mob.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) {
                            client.gameMode.attack(client.player, entity);
                            client.player.swing(InteractionHand.MAIN_HAND);
                        }
                    }
                    // 攻击中立生物（可选）
                    case net.minecraft.world.entity.Mob mob when config.attackNeutralMobs -> {
                        if (mob.getType().getCategory() == net.minecraft.world.entity.MobCategory.CREATURE) {
                            client.gameMode.attack(client.player, entity);
                            client.player.swing(InteractionHand.MAIN_HAND);
                        }
                    }
                    default -> {
                    }
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

    // 获取配置实例
    public Config getConfig() {
        return config;
    }

    // 重新加载配置的方法
    public void reloadConfig() {
        config = Config.load();
        LOGGER.info("配置已重新加载");
    }

    // 获取实例的静态方法
    public static AutoClicker getInstance() {
        return INSTANCE;
    }
}