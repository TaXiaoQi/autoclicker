package com.example.autoclicker.feature;

import com.example.autoclicker.Main;
import com.example.autoclicker.config.Config;
import com.example.autoclicker.config.ConfigManager;
import com.example.autoclicker.tool.SwingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.network.chat.Component;

public class AutoClicker {
    private int attackTickCounter = 0;
    private int placeTickCounter = 0;
    private int attackCooldown = 0;
    private int placeCooldown = 0;

    private boolean wasAttackEnabled = false;
    private boolean wasPlaceEnabled = false;

    private final AutoRefill autoRefill = new AutoRefill();

    private long lastSuccessfulAttackTime = -1;
    private long lastSuccessfulPlaceTime = -1;

    // 记录功能启动时间
    private long attackStartTime = -1;
    private long placeStartTime = -1;

    private boolean memoryInitialized = false;

    public void tick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        if (!memoryInitialized) {
            Config config = ConfigManager.getConfig();

            if (config.autoRefillMainHand || config.autoRefillOffHand) {
                if (config.autoAttackEnabled) {
                    autoRefill.onAttackUsed();
                }
                if (config.autoPlaceEnabled) {
                    autoRefill.onPlaceUsed();
                }
            }
            memoryInitialized = true;
        }


        long levelTime = client.level.getGameTime();
        if (attackCooldown > 0) attackCooldown--;
        if (placeCooldown > 0) placeCooldown--;

        boolean attackNowEnabled = isAutoAttackEnabled();
        boolean placeNowEnabled = isAutoPlaceEnabled();

        // ===== 检测攻击开关变化 =====
        if (attackNowEnabled && !wasAttackEnabled) {
            // 刚开启：记录启动时间
            attackStartTime = levelTime;
            Main.LOGGER.debug("攻击启动时间记录：{}", attackStartTime);
        } else if (!attackNowEnabled && wasAttackEnabled) {
            // 刚关闭（手动）：重置计时器
            attackStartTime = -1;
            lastSuccessfulAttackTime = -1;
            Main.LOGGER.debug("攻击已手动关闭，重置超时计时器");
        }

        // ===== 检测放置开关变化 =====
        if (placeNowEnabled && !wasPlaceEnabled) {
            placeStartTime = levelTime;
            Main.LOGGER.debug("放置启动时间记录：{}", placeStartTime);
        } else if (!placeNowEnabled && wasPlaceEnabled) {
            placeStartTime = -1;
            lastSuccessfulPlaceTime = -1;
            Main.LOGGER.debug("放置已手动关闭，重置超时计时器");
        }

        // 更新状态追踪
        wasAttackEnabled = attackNowEnabled;
        wasPlaceEnabled = placeNowEnabled;

        // ===== 自动攻击逻辑 =====
        if (attackNowEnabled) {
            attackTickCounter++;
            if (attackTickCounter >= getAttackInterval() && attackCooldown <= 0) {
                if (performAttack(client)) {
                    attackTickCounter = 0;
                    attackCooldown = getAttackCooldown();
                    lastSuccessfulAttackTime = levelTime;

                    autoRefill.onAttackUsed();
                } else {
                    attackTickCounter = 0;
                }
            }
        }

        // ===== 自动放置逻辑 =====
        if (placeNowEnabled) {
            placeTickCounter++;
            if (placeTickCounter >= getPlaceInterval() && placeCooldown <= 0) {
                if (performAutoPlace(client)) {
                    placeTickCounter = 0;
                    placeCooldown = getPlaceCooldown();
                    lastSuccessfulPlaceTime = levelTime;
                    autoRefill.onPlaceUsed();
                } else {
                    placeTickCounter = 0;
                }
            }
        }

        checkAndDisableTimeout(levelTime);
    }

    private void checkAndDisableTimeout(long currentTime) {
        Config config = ConfigManager.getConfig();

        // 计算超时刻数（将秒转换为游戏刻，20刻=1秒）
        long timeoutTicks = config.autoDisableTimeout * 20L;

        // 检查自动攻击超时（如果开启了自动关闭功能）
        if (config.autoAttackEnabled && config.autoDisableAttack) {
            long referenceTime = (lastSuccessfulAttackTime != -1) ? lastSuccessfulAttackTime : attackStartTime;

            if (referenceTime != -1 && currentTime - referenceTime >= timeoutTicks) {
                config.autoAttackEnabled = false;
                ConfigManager.save();
                // 超时自动关闭属于功能关闭，需要同步解除联动静音
                MuteFeature.getInstance().releaseAttackMute();
                Main.sendMessage("autoclicker.message.auto_disabled_timeout",
                        com.example.autoclicker.tool.TextCompat.translatable("autoclicker.feature.attack"),
                        config.autoDisableTimeout);
                // 重置状态
                attackStartTime = -1;
                lastSuccessfulAttackTime = -1;
                wasAttackEnabled = false;
                Main.LOGGER.info("自动攻击已超时关闭（{}秒无操作）", config.autoDisableTimeout);
            }
        }

        // 检查自动放置超时（如果开启了自动关闭功能）
        if (config.autoPlaceEnabled && config.autoDisablePlace) {
            long referenceTime = (lastSuccessfulPlaceTime != -1) ? lastSuccessfulPlaceTime : placeStartTime;

            if (referenceTime != -1 && currentTime - referenceTime >= timeoutTicks) {
                config.autoPlaceEnabled = false;
                ConfigManager.save();
                // 超时自动关闭属于功能关闭，需要同步解除联动静音
                MuteFeature.getInstance().releasePlaceMute();
                Main.sendMessage("autoclicker.message.auto_disabled_timeout",
                        com.example.autoclicker.tool.TextCompat.translatable("autoclicker.feature.place"),
                        config.autoDisableTimeout);
                // 重置状态
                placeStartTime = -1;
                lastSuccessfulPlaceTime = -1;
                wasPlaceEnabled = false;
                Main.LOGGER.info("自动放置已超时关闭（{}秒无操作）", config.autoDisableTimeout);
            }
        }
    }

    public void resetAutomationOnDisconnect() {
        Config config = ConfigManager.getConfig();

        // 重置计时器
        attackTickCounter = 0;
        placeTickCounter = 0;
        attackCooldown = 0;
        placeCooldown = 0;
        lastSuccessfulAttackTime = -1;
        lastSuccessfulPlaceTime = -1;

        // 重置启动时间
        attackStartTime = -1;
        placeStartTime = -1;

        // 重置开关追踪
        wasAttackEnabled = false;
        wasPlaceEnabled = false;

        // 重置初始化标记
        memoryInitialized = false;

        // 关闭自动化功能
        config.autoAttackEnabled = false;
        config.autoPlaceEnabled = false;

        // 同步释放联动静音请求，避免残留标志影响后续静音判定
        MuteFeature.getInstance().releaseAttackMute();
        MuteFeature.getInstance().releasePlaceMute();

        // 分别清除攻击和放置功能的记忆
        autoRefill.clearAttackMemory();
        autoRefill.clearPlaceMemory();

        // 保存配置
        ConfigManager.save();
    }

    // 攻击逻辑
    private boolean performAttack(Minecraft client) {
        try {
            if (client.gameMode == null || client.player == null) return false;
            if (client.player.getAttackStrengthScale(0.0F) < 1.0F) return false;

            HitResult hitResult = client.hitResult;
            // Java 8 兼容：不用 instanceof 模式匹配
            if (!(hitResult instanceof EntityHitResult)) return false;
            EntityHitResult entityHit = (EntityHitResult) hitResult;

            net.minecraft.world.entity.Entity entity = entityHit.getEntity();
            Config config = ConfigManager.getConfig();

            // 攻击盔甲架
            if (entity instanceof net.minecraft.world.entity.decoration.ArmorStand && config.attackArmorStands) {
                return performEntityAttack(client, entity);
            }

            // 攻击敌对生物
            // Java 8 兼容：不用 instanceof 模式匹配
            if (entity instanceof net.minecraft.world.entity.Mob) {
                net.minecraft.world.entity.Mob mob = (net.minecraft.world.entity.Mob) entity;
                net.minecraft.world.entity.MobCategory category = mob.getType().getCategory();
                if (category == net.minecraft.world.entity.MobCategory.MONSTER && config.attackHostileMobs) {
                    return performEntityAttack(client, entity);
                }
                // 攻击中立生物
                if (category == net.minecraft.world.entity.MobCategory.CREATURE && config.attackNeutralMobs) {
                    return performEntityAttack(client, entity);
                }
            }
        } catch (Exception e) {
            Main.LOGGER.error("自动攻击出错", e);
        }
        return false;
    }

    /**
     * 对实体执行一次攻击并挥臂。
     * 26.x 起突刺类武器（长矛等）必须走版本适配层的 piercingAttack，
     * 普通 gameMode.attack 对它们只会播放突刺动画（反复伸缩）而无伤害。
     */
    private boolean performEntityAttack(Minecraft client, net.minecraft.world.entity.Entity entity) {
        if (!com.example.autoclicker.tool.AttackCompat.attack(client, entity)) {
            client.gameMode.attack(client.player, entity);
        }
        SwingHelper.swingArm(client.player, InteractionHand.MAIN_HAND);
        return true;
    }

    // 放置逻辑
    private boolean performAutoPlace(Minecraft client) {
        try {
            if (client.gameMode == null || client.player == null) return false;

            HitResult hitResult = client.hitResult;
            // Java 8 兼容：不用 instanceof 模式匹配
            if (!(hitResult instanceof BlockHitResult)) {
                return false;
            }
            BlockHitResult blockHit = (BlockHitResult) hitResult;

            net.minecraft.client.multiplayer.ClientLevel level = client.level;
            net.minecraft.core.BlockPos pos = blockHit.getBlockPos();
            BlockState state = null;
            if (level != null) {
                state = level.getBlockState(pos);
            }
            Config config = ConfigManager.getConfig();

            // 检查主副手物品
            ItemStack mainHandItem = client.player.getMainHandItem();
            ItemStack offHandItem = client.player.getOffhandItem();

            // ===== 1. 骨粉逻辑 =====
            if (config.useBoneMeal && state != null && canUseBoneMealOn(state.getBlock())) {
                // 检查主副手是否有骨粉
                if (mainHandItem.getItem() == Items.BONE_MEAL) {
                    InteractionResult result = com.example.autoclicker.tool.ItemUseAdapter.useItemOn(client, InteractionHand.MAIN_HAND, blockHit);
                    if (result.consumesAction()) {
                        SwingHelper.swingArm(client.player, InteractionHand.MAIN_HAND);
                        return true;
                    }
                } else if (offHandItem.getItem() == Items.BONE_MEAL) {
                    InteractionResult result = com.example.autoclicker.tool.ItemUseAdapter.useItemOn(client, InteractionHand.OFF_HAND, blockHit);
                    if (result.consumesAction()) {
                        SwingHelper.swingArm(client.player, InteractionHand.OFF_HAND);
                        return true;
                    }
                }
            }

            // ===== 2. 种植逻辑 =====
// 检查手持物品是否为种子/可种植物品
            ItemStack seedItem = ItemStack.EMPTY;
            InteractionHand hand = null;

            if (isPlantableItem(mainHandItem)) {
                seedItem = mainHandItem;
                hand = InteractionHand.MAIN_HAND;
            } else if (isPlantableItem(offHandItem)) {
                seedItem = offHandItem;
                hand = InteractionHand.OFF_HAND;
            }

            if (seedItem.isEmpty()) return false;

// 检查瞄准的方块是否为种植土
            if (state != null && isPlantableSoil(state.getBlock())) {
                Block targetBlock = state.getBlock();
                Item seedItemType = seedItem.getItem();

                // 绯红菌只能种在绯红菌岩上
                if (seedItemType == Items.CRIMSON_FUNGUS && targetBlock != Blocks.CRIMSON_NYLIUM) {
                    return false;
                }
                // 诡异菌只能种在诡异菌岩上
                if (seedItemType == Items.WARPED_FUNGUS && targetBlock != Blocks.WARPED_NYLIUM) {
                    return false;
                }
                // 其他可种植物品（包括绯红菌和诡异菌以外的物品）正常检查种植土
                // 这里不需要额外条件，因为isPlantableSoil已经过滤了大部分方块

                if (hand != null) {
                    InteractionResult result = com.example.autoclicker.tool.ItemUseAdapter.useItemOn(client, hand, blockHit);
                    if (result.consumesAction()) {
                        SwingHelper.swingArm(client.player, hand);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Main.LOGGER.error("自动放置出错", e);
        }
        return false;
    }

    /**
     * 检查方块是否可以使用骨粉
     */
    private boolean canUseBoneMealOn(Block block) {
        return block instanceof CropBlock ||          // 作物类
                block instanceof SaplingBlock ||      // 树苗类
                block instanceof StemBlock ||         // 茎类
                block == Blocks.SEA_PICKLE ||         // 海泡菜
                block == Blocks.KELP ||               // 海带
                block == Blocks.KELP_PLANT ||         // 海带植物
                block == Blocks.WEEPING_VINES ||      // 垂泪藤
                block == Blocks.TWISTING_VINES ||     // 缠怨藤
                block == Blocks.CRIMSON_FUNGUS ||     // 绯红菌
                block == Blocks.WARPED_FUNGUS ||      // 诡异菌
                block == Blocks.BROWN_MUSHROOM ||     // 棕色蘑菇（骨粉催成巨型蘑菇）
                block == Blocks.RED_MUSHROOM ||       // 红色蘑菇
                block == Blocks.SUNFLOWER ||          // 向日葵（两格高花，骨粉复制）
                block == Blocks.LILAC ||              // 丁香
                block == Blocks.ROSE_BUSH ||          // 玫瑰丛
                block == Blocks.PEONY ||              // 牡丹
                // 洞穴藤蔓/垂滴叶/杜鹃为 1.17 新增，由版本适配层判断
                com.example.autoclicker.tool.Plantables.isExtraBonemealTarget(block);
    }

    private boolean isPlantableItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) return false;
        Item item = itemStack.getItem();

        // 种子类物品
        return item == Items.WHEAT_SEEDS ||
                item == Items.BEETROOT_SEEDS ||
                item == Items.MELON_SEEDS ||
                item == Items.PUMPKIN_SEEDS ||
                item == Items.COCOA_BEANS ||
                item == Items.SWEET_BERRIES ||
                // 作物物品
                item == Items.CARROT ||
                item == Items.POTATO ||
                item == Items.NETHER_WART ||
                // 树苗
                item == Items.OAK_SAPLING ||
                item == Items.SPRUCE_SAPLING ||
                item == Items.BIRCH_SAPLING ||
                item == Items.JUNGLE_SAPLING ||
                item == Items.ACACIA_SAPLING ||
                item == Items.DARK_OAK_SAPLING ||
                // 下界菌类
                item == Items.CRIMSON_FUNGUS ||      // 绯红菌（种在绯红菌岩上）
                item == Items.WARPED_FUNGUS ||       // 诡异菌（种在诡异菌岩上）
                // 其他植物
                item == Items.RED_MUSHROOM ||
                item == Items.BROWN_MUSHROOM ||
                item == Items.SUGAR_CANE ||
                item == Items.CACTUS ||
                item == Items.BAMBOO ||
                item == Items.KELP ||
                item == Items.SEA_PICKLE ||
                item == Items.TWISTING_VINES ||
                item == Items.WEEPING_VINES ||
                // 高版本新增物品（火炬花/瓶子草/发光浆果/杜鹃/红树苗等）由版本适配层判断
                com.example.autoclicker.tool.Plantables.isExtraPlantable(item);
    }

    private boolean isPlantableSoil(Block block) {
        return block == Blocks.FARMLAND ||        // 耕地
                block == Blocks.DIRT ||            // 泥土
                block == Blocks.GRASS_BLOCK ||     // 草方块
                block == Blocks.SOUL_SAND ||       // 灵魂沙（地狱疣）
                block == Blocks.SAND ||            // 沙子（仙人掌、甘蔗）
                block == Blocks.RED_SAND ||        // 红沙
                block == Blocks.PODZOL ||          // 灰化土（蘑菇、杜鹃花丛）
                block == Blocks.MYCELIUM ||        // 菌丝（蘑菇）
                block == Blocks.CLAY ||            // 黏土（海泡菜）
                block == Blocks.GRAVEL ||          // 沙砾（海草）
                // 下界菌岩（菌类种植）
                block == Blocks.CRIMSON_NYLIUM ||   // 绯红菌岩
                block == Blocks.WARPED_NYLIUM;      // 诡异菌岩
    }

    // 配置访问方法
    public boolean isAutoAttackEnabled() {
        return ConfigManager.getConfig().autoAttackEnabled;
    }

    public int getAttackInterval() {
        return ConfigManager.getConfig().attackInterval;
    }

    public int getAttackCooldown() {
        // 添加随机性
        int interval = getAttackInterval();
        if (ConfigManager.getConfig().attackRandomnessEnabled && ConfigManager.getConfig().attackRandomness > 0) {
            interval += (int) (Math.random() * ConfigManager.getConfig().attackRandomness * 2) - ConfigManager.getConfig().attackRandomness;
        }
        return Math.max(1, interval);
    }

    public boolean isAutoPlaceEnabled() {
        return ConfigManager.getConfig().autoPlaceEnabled;
    }

    public int getPlaceInterval() {
        return ConfigManager.getConfig().placeInterval;
    }

    public int getPlaceCooldown() {
        // 添加随机性
        int interval = getPlaceInterval();
        if (ConfigManager.getConfig().placeRandomnessEnabled && ConfigManager.getConfig().placeRandomness > 0) {
            interval += (int) (Math.random() * ConfigManager.getConfig().placeRandomness * 2) - ConfigManager.getConfig().placeRandomness;
        }
        return Math.max(1, interval);
    }

    public void resetAttackTimer() {
        attackTickCounter = 0;
        attackCooldown = 0;
    }

    public void resetPlaceTimer() {
        placeTickCounter = 0;
        placeCooldown = 0;
    }
}