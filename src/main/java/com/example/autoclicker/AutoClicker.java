package com.example.autoclicker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoClicker implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("autoclicker");
    private static AutoClicker INSTANCE;

    private int attackTickCounter = 0;
    private int placeTickCounter = 0;
    private Config config;
    private KeyMapping attackToggleKey;
    private KeyMapping placeToggleKey;
    private KeyMapping configScreenKey;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        LOGGER.info("自动点击器初始化");

        // 加载配置
        config = Config.load();

        // 自动攻击开关
        attackToggleKey = new KeyMapping(
                "key.autoclicker.attack_toggle",
                GLFW.GLFW_KEY_F8,
                net.minecraft.client.KeyMapping.Category.MISC
        );

        // 自动放置开关
        placeToggleKey = new KeyMapping(
                "key.autoclicker.place_toggle",
                GLFW.GLFW_KEY_F9,
                net.minecraft.client.KeyMapping.Category.MISC
        );

        // 配置菜单按键
        configScreenKey = new KeyMapping(
                "key.autoclicker.config",
                GLFW.GLFW_KEY_F10,
                net.minecraft.client.KeyMapping.Category.MISC
        );

        KeyBindingHelper.registerKeyBinding(attackToggleKey);
        KeyBindingHelper.registerKeyBinding(placeToggleKey);
        KeyBindingHelper.registerKeyBinding(configScreenKey);
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(Minecraft client) {
        // 切换自动攻击开关
        if (attackToggleKey.consumeClick()) {
            config.autoAttackEnabled = !config.autoAttackEnabled;
            attackTickCounter = 0;
            if (client.player != null) {
                String status = config.autoAttackEnabled ? "§a开启" : "§c关闭";
                client.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("自动攻击: " + status + " | 间隔: " + config.attackInterval + "ticks"),
                        true
                );
            }
            LOGGER.info("自动攻击已 {}", config.autoAttackEnabled ? "开启" : "关闭");
        }

        // 切换自动放置开关
        if (placeToggleKey.consumeClick()) {
            config.autoPlaceEnabled = !config.autoPlaceEnabled;
            placeTickCounter = 0;
            if (client.player != null) {
                String status = config.autoPlaceEnabled ? "§a开启" : "§c关闭";
                String boneMealStatus = config.useBoneMeal ? "含骨粉" : "不含骨粉";
                client.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("自动放置: " + status + " | 间隔: " + config.placeInterval + "ticks | " + boneMealStatus),
                        true
                );
            }
            LOGGER.info("自动放置已 {}", config.autoPlaceEnabled ? "开启" : "关闭");
        }

        // 打开配置屏幕
        if (configScreenKey.consumeClick() && client.player != null) {
            client.setScreen(new ConfigScreen(null));
        }

        // 自动攻击
        if (config.autoAttackEnabled && client.player != null) {
            attackTickCounter++;
            if (attackTickCounter >= config.attackInterval) {
                performAttack(client);
                attackTickCounter = 0;
            }
        }

        // 自动放置逻
        if (config.autoPlaceEnabled && client.player != null) {
            placeTickCounter++;
            if (placeTickCounter >= config.placeInterval) {
                performAutoPlace(client);
                placeTickCounter = 0;
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
                    case net.minecraft.world.entity.decoration.ArmorStand ignored when config.attackArmorStands -> {
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

    private void performAutoPlace(Minecraft client) {
        try {
            if (client.gameMode == null || client.player == null) return;

            HitResult hitResult = client.hitResult;
            if (!(hitResult instanceof BlockHitResult blockHit)) {
                return;
            }

            ItemStack mainHandItem = client.player.getMainHandItem();
            ItemStack offHandItem = client.player.getOffhandItem();

            boolean mainHandPlaceable = isPlaceableItem(mainHandItem);
            boolean offHandPlaceable = isPlaceableItem(offHandItem);
            boolean offHandBoneMeal = isPlaceableItem(offHandItem);

            // 优先使用主手放置
            if (mainHandPlaceable) {
                client.gameMode.useItemOn(
                        client.player,
                        InteractionHand.MAIN_HAND,
                        blockHit
                );
                client.player.swing(InteractionHand.MAIN_HAND);
                LOGGER.debug("主手自动放置: {}", mainHandItem.getItem().getDescriptionId());
            }
            // 如果主手没有可放置物品，但副手有可放置物品
            else if (offHandPlaceable) {
                client.gameMode.useItemOn(
                        client.player,
                        InteractionHand.OFF_HAND,
                        blockHit
                );
                client.player.swing(InteractionHand.OFF_HAND);
                LOGGER.debug("副手自动放置: {}", offHandItem.getItem().getDescriptionId());
            }

            // 副手骨粉（如果启用且副手是骨粉）
            if (offHandBoneMeal) {
                client.gameMode.useItemOn(
                        client.player,
                        InteractionHand.OFF_HAND,
                        blockHit
                );
                client.player.swing(InteractionHand.OFF_HAND);
                LOGGER.debug("副手自动使用骨粉");
            }

        } catch (Exception e) {
            LOGGER.error("自动放置出错", e);
        }
    }

    // 检查物品
    // 检查物品
    private boolean isPlaceableItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) return false;
        var item = itemStack.getItem();

        // 骨粉检测
        if (config.useBoneMeal && item == Items.BONE_MEAL) {
            return true;
        }

        // 种植物品检测
        if (item instanceof net.minecraft.world.item.BlockItem blockItem) {
            var block = blockItem.getBlock();

            // 检查是否是植物方块
            switch (block) {
                case CropBlock ignored -> {
                    return true;                    // 作物
                }
                case SaplingBlock ignored -> {
                    return true;                // 树苗
                }
                case FlowerBlock ignored1 -> {
                    return true;                 // 花
                }
                case TallFlowerBlock ignored -> {
                    return true;             // 高花
                }
                case MushroomBlock ignored -> {
                    return true;               // 蘑菇
                }
                case NetherWartBlock ignored -> {
                    return true;             // 地狱疣
                }
                case SugarCaneBlock ignored -> {
                    return true;              // 甘蔗
                }
                case CactusBlock ignored -> {
                    return true;                 // 仙人掌
                }
                case KelpBlock ignored -> {
                    return true;                   // 海带
                }
                case KelpPlantBlock ignored -> {
                    return true;              // 海带植物
                }
                case SeagrassBlock ignored -> {
                    return true;               // 海草
                }
                case SeaPickleBlock ignored -> {
                    return true;              // 海泡菜
                }
                case StemBlock ignored -> {
                    return true;                   // 南瓜/西瓜茎
                }
                case AttachedStemBlock ignored -> {
                    return true;           // 连接的茎
                }
                case BambooSaplingBlock ignored -> {
                    return true;          // 竹笋
                }
                case BambooStalkBlock ignored -> {
                    return true;            // 竹子
                }
                case ChorusFlowerBlock ignored -> {
                    return true;           // 紫颂花
                }
                case ChorusPlantBlock ignored -> {
                    return true;            // 紫颂植物
                }
                case TwistingVinesBlock ignored -> {
                    return true;          // 扭曲藤
                }
                case TwistingVinesPlantBlock ignored -> {
                    return true;     // 扭曲藤植物
                }
                case WeepingVinesBlock ignored -> {
                    return true;           // 垂泪藤
                }
                case WeepingVinesPlantBlock ignored -> {
                    return true;      // 垂泪藤植物
                }
                case CaveVinesBlock ignored -> {
                    return true;              // 洞穴藤蔓
                }
                case CaveVinesPlantBlock ignored -> {
                    return true;         // 洞穴藤蔓植物
                }
                case GlowLichenBlock ignored -> {
                    return true;             // 发光地衣
                }
                default -> {
                }
            }
        }
        if (item == Items.WHEAT_SEEDS) return true;
        if (item == Items.BEETROOT_SEEDS) return true;
        if (item == Items.MELON_SEEDS) return true;
        if (item == Items.PUMPKIN_SEEDS) return true;
        if (item == Items.TORCHFLOWER_SEEDS) return true;
        return item == Items.PITCHER_POD;
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