package com.example.autoclicker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class AutoClicker implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("autoclicker");
    private static AutoClicker INSTANCE;
    private final Random random = new Random();
    private int attackTickCounter = 0;
    private int placeTickCounter = 0;
    Config config;
    private KeyMapping attackToggleKey;
    private KeyMapping placeToggleKey;
    private KeyMapping configScreenKey;

    // ✅ 自动关闭计时器（使用 gameTime）
    private long lastAttackGameTime = 0;
    private long lastPlaceGameTime = 0;
    private static final long AUTO_DISABLE_DELAY_TICKS = 600L; // 30秒 = 600 tick

    // 人性化点击变量
    private int currentAttackInterval;
    private int currentPlaceInterval;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        LOGGER.info("自动点击器初始化");

        // 加载配置
        config = Config.load();

        // 初始化随机间隔
        updateAttackInterval();
        updatePlaceInterval();

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
        if (client.level == null) return; // 防止主菜单崩溃
        long currentGameTime = client.level.getGameTime();

        if (attackToggleKey.consumeClick()) {
            config.autoAttackEnabled = !config.autoAttackEnabled;
            attackTickCounter = 0;
            updateAttackInterval();
            if (config.autoAttackEnabled) {
                // ✅ 开启时重置最后活跃时间为现在，防止立即超时
                lastAttackGameTime = currentGameTime;
            }
            if (client.player != null) {
                String status = config.autoAttackEnabled ? "§a开启" : "§c关闭";
                client.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("自动攻击: " + status + " | 间隔: " +
                                config.attackInterval + "-" + (config.attackInterval + config.attackRandomness) + "ticks"),
                        true
                );
            }
            LOGGER.info("自动攻击已 {}", config.autoAttackEnabled ? "开启" : "关闭");
            config.save();
        }

        if (placeToggleKey.consumeClick()) {
            config.autoPlaceEnabled = !config.autoPlaceEnabled;
            placeTickCounter = 0;
            updatePlaceInterval();
            if (config.autoPlaceEnabled) {
                // ✅ 开启时重置最后活跃时间为现在
                lastPlaceGameTime = currentGameTime;
            }
            if (client.player != null) {
                String status = config.autoPlaceEnabled ? "§a开启" : "§c关闭";
                String boneMealStatus = config.useBoneMeal ? "含骨粉" : "不含骨粉";
                client.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("自动放置: " + status + " | 间隔: " +
                                config.placeInterval + "-" + (config.placeInterval + config.placeRandomness) + "ticks | " + boneMealStatus),
                        true
                );
            }
            LOGGER.info("自动放置已 {}", config.autoPlaceEnabled ? "开启" : "关闭");
            config.save();
        }

        if (configScreenKey.consumeClick() && client.player != null) {
            client.setScreen(new ConfigScreen(null));
        }

        // 自动攻击
        if (config.autoAttackEnabled && client.player != null) {
            attackTickCounter++;
            if (attackTickCounter >= currentAttackInterval) {
                performAttack(client);
                attackTickCounter = 0;
                updateAttackInterval();
            }
        }

        // 自动放置
        if (config.autoPlaceEnabled && client.player != null) {
            placeTickCounter++;
            if (placeTickCounter >= currentPlaceInterval) {
                performAutoPlace(client);
                placeTickCounter = 0;
                updatePlaceInterval();
            }
        }
        // 检查自动攻击超时
        if (config.autoAttackEnabled && currentGameTime - lastAttackGameTime >= AUTO_DISABLE_DELAY_TICKS) {
            config.autoAttackEnabled = false;
            attackTickCounter = 0;
            if (client.player != null) {
                client.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§c自动攻击已因长时间未触发而关闭"),
                        true
                );
            }
            LOGGER.info("自动攻击因超时自动关闭");
            config.save();
        }

        // 检查自动放置超时
        if (config.autoPlaceEnabled && currentGameTime - lastPlaceGameTime >= AUTO_DISABLE_DELAY_TICKS) {
            config.autoPlaceEnabled = false;
            placeTickCounter = 0;
            if (client.player != null) {
                client.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§c自动放置已因长时间未触发而关闭"),
                        true
                );
            }
            LOGGER.info("自动放置因超时自动关闭");
            config.save();
        }
    }

    // 自动攻击
    private void performAttack(Minecraft client) {
        try {
            if (client.gameMode == null || client.player == null) return;
            if (client.player.getAttackStrengthScale(0.0F) < 1.0F) return;
            if (!(client.hitResult instanceof EntityHitResult entityHit)) return;

            var entity = entityHit.getEntity();
            boolean shouldAttack = isShouldAttack(entity);

            if (shouldAttack) {
                if (config.humanizeClicks && random.nextFloat() < 0.1f) {
                    return;
                }
                client.gameMode.attack(client.player, entity);
                client.player.swing(InteractionHand.MAIN_HAND);

                // ✅ 记录成功攻击时间
                if (client.level != null) {
                    lastAttackGameTime = client.level.getGameTime();
                }

                LOGGER.debug("自动攻击: {}", entity.getType().getDescriptionId());
            }
        } catch (Exception e) {
            LOGGER.error("自动攻击出错", e);
        }
    }

    private boolean isShouldAttack(Entity entity) {
        boolean shouldAttack = false;
        if (entity instanceof net.minecraft.world.entity.decoration.ArmorStand && config.attackArmorStands) {
            shouldAttack = true;
        } else if (entity instanceof net.minecraft.world.entity.Mob mob) {
            var category = mob.getType().getCategory();
            if (category == net.minecraft.world.entity.MobCategory.MONSTER && config.attackHostileMobs) {
                shouldAttack = true;
            } else if (category == net.minecraft.world.entity.MobCategory.CREATURE && config.attackNeutralMobs) {
                shouldAttack = true;
            }
        }
        return shouldAttack;
    }

    // 放置逻辑
    private void performAutoPlace(Minecraft client) {
        try {
            if (client.gameMode == null || client.player == null || client.level == null) {
                return;
            }

            HitResult hitResult = client.hitResult;
            if (!(hitResult instanceof BlockHitResult blockHit)) {
                return;
            }

            BlockPos targetPos = blockHit.getBlockPos();
            BlockState targetState = client.level.getBlockState(targetPos);
            Block targetBlock = targetState.getBlock();

            ItemStack mainHand = client.player.getMainHandItem();
            ItemStack offHand = client.player.getOffhandItem();

            // 检查手中是否有植物（任意手）
            InteractionHand plantHand = null;
            if (isPlaceableItem(mainHand)) {
                plantHand = InteractionHand.MAIN_HAND;
            } else if (isPlaceableItem(offHand)) {
                plantHand = InteractionHand.OFF_HAND;
            }

            // 检查手中是否有骨粉（任意手，副手优先）
            InteractionHand boneMealHand = null;
            if (offHand.getItem() == Items.BONE_MEAL) {
                boneMealHand = InteractionHand.OFF_HAND;
            } else if (mainHand.getItem() == Items.BONE_MEAL) {
                boneMealHand = InteractionHand.MAIN_HAND;
            }

            // 人性化跳过
            if (config.humanizeClicks && random.nextFloat() < 0.1f) {
                return;
            }

            // ✅ 核心逻辑：根据“看到的方块”决定行为
            if (isBonemealTarget(targetBlock, targetState)) {
                // 看着的是可催熟植物 → 尝试用骨粉
                if (config.useBoneMeal && boneMealHand != null) {
                    if (!config.humanizeClicks || random.nextFloat() > 0.5f) {
                        client.gameMode.useItemOn(client.player, boneMealHand, blockHit);
                        client.player.swing(boneMealHand);
                        LOGGER.debug("使用骨粉催熟: {} ({})",
                                targetBlock.getDescriptionId(),
                                boneMealHand == InteractionHand.MAIN_HAND ? "主手" : "副手");
                        lastPlaceGameTime = client.level.getGameTime();
                    }
                }
            } else if (isPlaceableOn(targetBlock)) {
                // 看着的是可放置基底 → 尝试放植物
                if (plantHand != null) {
                    client.gameMode.useItemOn(client.player, plantHand, blockHit);
                    client.player.swing(plantHand);
                    LOGGER.debug("放置植物: {} ({})",
                            client.player.getItemInHand(plantHand).getItem().getDescriptionId(),
                            plantHand == InteractionHand.MAIN_HAND ? "主手" : "副手");
                    lastPlaceGameTime = client.level.getGameTime();
                }
            }

        } catch (Exception e) {
            LOGGER.error("自动放置出错", e);
        }
    }

    // 判断该方块是否能被骨粉催熟
    private boolean isBonemealTarget(Block block, BlockState state) {
        // 农作物（小麦、胡萝卜等）
        if (block instanceof CropBlock) {
            return !((CropBlock) block).isMaxAge(state); // 未成熟才催
        }
        // 南瓜/西瓜茎
        if (block instanceof StemBlock) {
            return state.getValue(StemBlock.AGE) < 7;
        }
        // 甘蔗、竹子、海带等
        if (block instanceof SugarCaneBlock || block instanceof BambooStalkBlock ||
                block instanceof KelpBlock || block instanceof KelpPlantBlock) {
            return true;
        }
        // 蘑菇（在菌丝上）
        if (block instanceof MushroomBlock) {
            return true;
        }
        // 下界苗（绯红/诡异）
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) < 3;
        }
        // 花、树苗、藤蔓、缠怨藤、垂泪藤、洞穴藤蔓等
        if (block instanceof SaplingBlock || block instanceof FlowerBlock ||
                block instanceof TallFlowerBlock || block instanceof WeepingVinesBlock ||
                block instanceof TwistingVinesBlock || block instanceof CaveVinesBlock ||
                block instanceof GlowLichenBlock) {
            return true;
        }
        // 海草、海泡菜
        if (block instanceof SeagrassBlock || block instanceof SeaPickleBlock) {
            return true;
        }
        // 甜莓丛（1.14+）
        if (block instanceof SweetBerryBushBlock) {
            return state.getValue(SweetBerryBushBlock.AGE) < 3;
        }
        // 火把花（1.20+）- 使用反射安全检查
        if (isClassAvailable("net.minecraft.world.level.block.Torch flowerBlock") &&
                block.getClass().getName().equals("net.minecraft.world.level.block.Torch flowerBlock")) {
            return true;
        }
        // 瓶子草（1.20+）- 使用反射安全检查
        return isClassAvailable("net.minecraft.world.level.block.PitcherPlantBlock") &&
                block.getClass().getName().equals("net.minecraft.world.level.block.PitcherPlantBlock");
    }

    // 辅助方法：检查类是否存在
    private boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // 可放置的方块
    private boolean isPlaceableOn(Block block) {
        return block == Blocks.FARMLAND || block == Blocks.GRASS_BLOCK || block == Blocks.DIRT ||
                block == Blocks.COARSE_DIRT || block == Blocks.PODZOL || block == Blocks.MYCELIUM ||
                block == Blocks.CRIMSON_NYLIUM || block == Blocks.WARPED_NYLIUM || block == Blocks.SAND ||
                block == Blocks.RED_SAND || block == Blocks.SNOW || block == Blocks.SNOW_BLOCK ||
                block == Blocks.GRAVEL || block == Blocks.SOUL_SAND || block == Blocks.SOUL_SOIL ||
                block == Blocks.WATER || block == Blocks.LAVA || block instanceof LeavesBlock;
    }

    // 可放置的物品（✅ 不包含骨粉）
    private boolean isPlaceableItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) return false;
        var item = itemStack.getItem();

        // 种子类
        if (item == Items.WHEAT_SEEDS || item == Items.BEETROOT_SEEDS ||
                item == Items.MELON_SEEDS || item == Items.PUMPKIN_SEEDS ||
                item == Items.TORCHFLOWER_SEEDS || item == Items.PITCHER_POD) {
            return true;
        }

        // 植物方块类
        if (item instanceof net.minecraft.world.item.BlockItem blockItem) {
            Block block = blockItem.getBlock();
            return block instanceof CropBlock || block instanceof SaplingBlock ||
                    block instanceof FlowerBlock || block instanceof TallFlowerBlock ||
                    block instanceof MushroomBlock || block instanceof NetherWartBlock ||
                    block instanceof SugarCaneBlock || block instanceof CactusBlock ||
                    block instanceof KelpBlock || block instanceof KelpPlantBlock ||
                    block instanceof SeagrassBlock || block instanceof SeaPickleBlock ||
                    block instanceof StemBlock || block instanceof AttachedStemBlock ||
                    block instanceof BambooSaplingBlock || block instanceof BambooStalkBlock ||
                    block instanceof ChorusFlowerBlock || block instanceof ChorusPlantBlock ||
                    block instanceof TwistingVinesBlock || block instanceof TwistingVinesPlantBlock ||
                    block instanceof WeepingVinesBlock || block instanceof WeepingVinesPlantBlock ||
                    block instanceof CaveVinesBlock || block instanceof CaveVinesPlantBlock ||
                    block instanceof GlowLichenBlock;
        }
        return false;
    }

    // 更新攻击间隔
    private void updateAttackInterval() {
        int baseInterval = config.attackInterval;
        int randomness = 0;
        if (config.attackRandomnessEnabled && config.humanizeClicks && config.attackRandomness > 0) {
            randomness = random.nextInt(config.attackRandomness + 1);
        }
        currentAttackInterval = Math.max(1, baseInterval + randomness);
    }

    // 更新放置间隔
    private void updatePlaceInterval() {
        int baseInterval = config.placeInterval;
        int randomness = 0;
        if (config.placeRandomnessEnabled && config.humanizeClicks && config.placeRandomness > 0) {
            randomness = random.nextInt(config.placeRandomness + 1);
        }
        currentPlaceInterval = Math.max(1, baseInterval + randomness);
    }

    // 重新加载配置的方法
    public void reloadConfig() {
        config = Config.load();
        updateAttackInterval();
        updatePlaceInterval();
        LOGGER.info("配置已重新加载");
    }

    // 获取实例的静态方法
    public static AutoClicker getInstance() {
        return INSTANCE;
    }
}