package com.example.autoclicker.feature;

import com.example.autoclicker.Main;
import com.example.autoclicker.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
            var config = ConfigManager.getConfig();

            if (config.autoRefillMainHand || config.autoRefillOffHand) {
                if (config.autoAttackEnabled) {
                    autoRefill.onAttackUsed();  // 初始化攻击记忆
                    Main.LOGGER.debug("攻击功能自动补货记忆已初始化");
                }
                if (config.autoPlaceEnabled) {
                    autoRefill.onPlaceUsed();   // 初始化放置记忆
                    Main.LOGGER.debug("放置功能自动补货记忆已初始化");
                }
            }

            memoryInitialized = true;
            Main.LOGGER.info("自动补货记忆初始化完成");
        }

        var levelTime = client.level.getGameTime();
        if (attackCooldown > 0) attackCooldown--;
        if (placeCooldown > 0) placeCooldown--;

        boolean attackNowEnabled = isAutoAttackEnabled();
        boolean placeNowEnabled = isAutoPlaceEnabled();

        // ===== 自动攻击逻辑 =====
        if (attackNowEnabled) {
            attackTickCounter++;
            if (attackTickCounter >= getAttackInterval() && attackCooldown <= 0) {
                if (performAttack(client)) {
                    attackTickCounter = 0;
                    attackCooldown = getAttackCooldown();
                    lastSuccessfulAttackTime = levelTime;
                    Main.LOGGER.debug("攻击成功，更新时间戳：{}", lastSuccessfulAttackTime);

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
                    Main.LOGGER.debug("放置成功，更新时间戳：{}", lastSuccessfulPlaceTime);

                    autoRefill.onAttackUsed();
                }
            }
        }

        checkAndDisableTimeout(levelTime);
    }

    private void checkAndDisableTimeout(long currentTime) {
        var config = ConfigManager.getConfig();

        // 计算超时刻数（将秒转换为游戏刻，20刻=1秒）
        long timeoutTicks = config.autoDisableTimeout * 20L;

        // 检查自动攻击超时（如果开启了自动关闭功能）
        if (config.autoAttackEnabled && config.autoDisableAttack) {
            // 修改：使用启动时间作为基准，如果成功攻击过则使用成功时间
            long referenceTime = (lastSuccessfulAttackTime != -1) ? lastSuccessfulAttackTime : attackStartTime;

            if (referenceTime != -1 && currentTime - referenceTime >= timeoutTicks) {
                config.autoAttackEnabled = false;
                ConfigManager.save();
                Main.sendMessage("autoclicker.message.auto_disabled_timeout",
                        Component.translatable("autoclicker.feature.attack"),
                        config.autoDisableTimeout);
                // 重置状态
                attackStartTime = -1;
                lastSuccessfulAttackTime = -1;
                Main.LOGGER.info("自动攻击已超时关闭（{}秒无操作）", config.autoDisableTimeout);
            }
        }

        // 检查自动放置超时（如果开启了自动关闭功能）
        if (config.autoPlaceEnabled && config.autoDisablePlace) {
            // 修改：使用启动时间作为基准，如果成功放置过则使用成功时间
            long referenceTime = (lastSuccessfulPlaceTime != -1) ? lastSuccessfulPlaceTime : placeStartTime;

            if (referenceTime != -1 && currentTime - referenceTime >= timeoutTicks) {
                config.autoPlaceEnabled = false;
                ConfigManager.save();
                Main.sendMessage("autoclicker.message.auto_disabled_timeout",
                        Component.translatable("autoclicker.feature.place"),
                        config.autoDisableTimeout);
                // 重置状态
                placeStartTime = -1;
                lastSuccessfulPlaceTime = -1;
                Main.LOGGER.info("自动放置已超时关闭（{}秒无操作）", config.autoDisableTimeout);
            }
        }
    }

    public void resetAutomationOnDisconnect() {
        var config = ConfigManager.getConfig();

        // 重置计时器
        attackTickCounter = 0;
        placeTickCounter = 0;
        attackCooldown = 0;
        placeCooldown = 0;
        lastSuccessfulAttackTime = -1;
        lastSuccessfulPlaceTime = -1;

        // 新增：重置启动时间
        attackStartTime = -1;
        placeStartTime = -1;

        // 重置初始化标记
        memoryInitialized = false;

        // 关闭自动化功能
        config.autoAttackEnabled = false;
        config.autoPlaceEnabled = false;

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

            var hitResult = client.hitResult;
            if (!(hitResult instanceof EntityHitResult entityHit)) return false;

            var entity = entityHit.getEntity();
            var config = ConfigManager.getConfig();

            // 攻击盔甲架
            if (entity instanceof net.minecraft.world.entity.decoration.ArmorStand && config.attackArmorStands) {
                client.gameMode.attack(client.player, entity);
                client.player.swing(InteractionHand.MAIN_HAND);
                autoRefill.onAttackUsed();  // ✅ 攻击成功，更新状态
                return true;
            }

            // 攻击敌对生物
            if (entity instanceof net.minecraft.world.entity.Mob mob) {
                var category = mob.getType().getCategory();
                if (category == net.minecraft.world.entity.MobCategory.MONSTER && config.attackHostileMobs) {
                    client.gameMode.attack(client.player, entity);
                    client.player.swing(InteractionHand.MAIN_HAND);
                    autoRefill.onAttackUsed();  // ✅ 攻击成功，更新状态
                    return true;
                }
                // 攻击中立生物
                if (category == net.minecraft.world.entity.MobCategory.CREATURE && config.attackNeutralMobs) {
                    client.gameMode.attack(client.player, entity);
                    client.player.swing(InteractionHand.MAIN_HAND);
                    autoRefill.onAttackUsed();  // ✅ 攻击成功，更新状态
                    return true;
                }
            }
        } catch (Exception e) {
            Main.LOGGER.error("自动攻击出错", e);
        }
        return false;
    }

    // 放置逻辑
    private boolean performAutoPlace(Minecraft client) {
        try {
            if (client.gameMode == null || client.player == null) return false;

            HitResult hitResult = client.hitResult;
            if (!(hitResult instanceof BlockHitResult blockHit)) {
                return false;
            }

            var level = client.level;
            var pos = blockHit.getBlockPos();
            BlockState state = null;
            if (level != null) {
                state = level.getBlockState(pos);
            }
            var config = ConfigManager.getConfig();

            // 检查主副手物品
            ItemStack mainHandItem = client.player.getMainHandItem();
            ItemStack offHandItem = client.player.getOffhandItem();

            // ===== 1. 骨粉逻辑 =====
            if (config.useBoneMeal) {
                // 检查瞄准的方块是否是可催熟的植物
                if (state != null && (state.getBlock() instanceof CropBlock ||
                        state.getBlock() instanceof SaplingBlock ||
                        state.getBlock() instanceof StemBlock)) {

                    // 检查主副手是否有骨粉
                    if (mainHandItem.getItem() == Items.BONE_MEAL) {
                        InteractionResult result = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, blockHit);
                        if (result.consumesAction()) {
                            client.player.swing(InteractionHand.MAIN_HAND);
                            autoRefill.onPlaceUsed();  // ✅ 放置成功，更新状态
                            return true;
                        }
                    } else if (offHandItem.getItem() == Items.BONE_MEAL) {
                        InteractionResult result = client.gameMode.useItemOn(client.player, InteractionHand.OFF_HAND, blockHit);
                        if (result.consumesAction()) {
                            client.player.swing(InteractionHand.OFF_HAND);
                            autoRefill.onPlaceUsed();  // ✅ 放置成功，更新状态
                            return true;
                        }
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
                if (hand != null) {
                    InteractionResult result = client.gameMode.useItemOn(client.player, hand, blockHit);
                    if (result.consumesAction()) {
                        client.player.swing(hand);
                        autoRefill.onPlaceUsed();  // ✅ 放置成功，更新状态
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Main.LOGGER.error("自动放置出错", e);
        }
        return false;
    }

    private boolean isPlantableItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) return false;
        var item = itemStack.getItem();

        // 种子类物品
        return item == Items.WHEAT_SEEDS ||
                item == Items.BEETROOT_SEEDS ||
                item == Items.MELON_SEEDS ||
                item == Items.PUMPKIN_SEEDS ||
                item == Items.TORCHFLOWER_SEEDS ||
                item == Items.PITCHER_POD ||
                item == Items.COCOA_BEANS ||
                item == Items.SWEET_BERRIES ||
                item == Items.GLOW_BERRIES ||
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
                item == Items.MANGROVE_PROPAGULE ||
                item == Items.CHERRY_SAPLING ||
                // 其他植物
                item == Items.RED_MUSHROOM ||
                item == Items.BROWN_MUSHROOM ||
                item == Items.SUGAR_CANE ||
                item == Items.CACTUS ||
                item == Items.BAMBOO ||
                item == Items.KELP ||
                item == Items.SEA_PICKLE ||
                item == Items.TWISTING_VINES ||
                item == Items.WEEPING_VINES;
    }

    private boolean isPlantableSoil(Block block) {
        return block == Blocks.FARMLAND ||        // 耕地
                block == Blocks.DIRT ||            // 泥土
                block == Blocks.GRASS_BLOCK ||     // 草方块
                block == Blocks.SOUL_SAND ||       // 灵魂沙（地狱疣）
                block == Blocks.SAND ||            // 沙子（仙人掌、甘蔗）
                block == Blocks.RED_SAND ||        // 红沙
                block == Blocks.PODZOL ||          // 灰化土（蘑菇）
                block == Blocks.MYCELIUM ||        // 菌丝（蘑菇）
                block == Blocks.CLAY ||            // 黏土（海泡菜）
                block == Blocks.GRAVEL;            // 沙砾（海草）
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