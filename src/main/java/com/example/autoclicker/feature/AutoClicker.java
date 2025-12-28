package com.example.autoclicker.feature;

import com.example.autoclicker.Main;
import com.example.autoclicker.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class AutoClicker {
    private int attackTickCounter = 0;
    private int placeTickCounter = 0;
    private int attackCooldown = 0;
    private int placeCooldown = 0;

    public void tick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        // 更新冷却计时器
        if (attackCooldown > 0) attackCooldown--;
        if (placeCooldown > 0) placeCooldown--;

        // 自动攻击逻辑
        if (isAutoAttackEnabled()) {
            attackTickCounter++;
            if (attackTickCounter >= getAttackInterval() && attackCooldown <= 0) {
                if (performAttack(client)) {
                    attackTickCounter = 0;
                    attackCooldown = getAttackCooldown();
                }
            }
        } else {
            attackTickCounter = 0;
        }

        // 自动放置逻辑
        if (isAutoPlaceEnabled()) {
            placeTickCounter++;
            if (placeTickCounter >= getPlaceInterval() && placeCooldown <= 0) {
                if (performAutoPlace(client)) {
                    placeTickCounter = 0;
                    placeCooldown = getPlaceCooldown();
                }
            }
        } else {
            placeTickCounter = 0;
        }
    }

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
                return true;
            }

            // 攻击敌对生物
            if (entity instanceof net.minecraft.world.entity.Mob mob) {
                var category = mob.getType().getCategory();
                if (category == net.minecraft.world.entity.MobCategory.MONSTER && config.attackHostileMobs) {
                    client.gameMode.attack(client.player, entity);
                    client.player.swing(InteractionHand.MAIN_HAND);
                    return true;
                }
                // 攻击中立生物（可选）
                if (category == net.minecraft.world.entity.MobCategory.CREATURE && config.attackNeutralMobs) {
                    client.gameMode.attack(client.player, entity);
                    client.player.swing(InteractionHand.MAIN_HAND);
                    return true;
                }
            }
        } catch (Exception e) {
            Main.LOGGER.error("自动攻击出错", e);
        }
        return false;
    }

    private boolean performAutoPlace(Minecraft client) {
        try {
            if (client.gameMode == null || client.player == null) return false;

            HitResult hitResult = client.hitResult;
            if (!(hitResult instanceof BlockHitResult blockHit)) {
                return false;
            }

            ItemStack mainHandItem = client.player.getMainHandItem();
            ItemStack offHandItem = client.player.getOffhandItem();

            boolean mainHandPlaceable = isPlaceableItem(mainHandItem);
            boolean offHandPlaceable = isPlaceableItem(offHandItem);
            boolean offHandBoneMeal = offHandItem.getItem() == Items.BONE_MEAL && ConfigManager.getConfig().useBoneMeal;

            // 优先使用主手放置
            if (mainHandPlaceable) {
                client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, blockHit);
                client.player.swing(InteractionHand.MAIN_HAND);
                return true;
            }
            // 如果主手没有可放置物品，但副手有可放置物品
            else if (offHandPlaceable) {
                client.gameMode.useItemOn(client.player, InteractionHand.OFF_HAND, blockHit);
                client.player.swing(InteractionHand.OFF_HAND);
                return true;
            }

            // 副手骨粉（如果启用且副手是骨粉）
            if (offHandBoneMeal) {
                client.gameMode.useItemOn(client.player, InteractionHand.OFF_HAND, blockHit);
                client.player.swing(InteractionHand.OFF_HAND);
                return true;
            }

        } catch (Exception e) {
            Main.LOGGER.error("自动放置出错", e);
        }
        return false;
    }

    private boolean isPlaceableItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) return false;
        var item = itemStack.getItem();
        var config = ConfigManager.getConfig();

        // 骨粉检测
        if (config.useBoneMeal && item == Items.BONE_MEAL) {
            return true;
        }

        // 种植物品检测
        if (item instanceof net.minecraft.world.item.BlockItem blockItem) {
            var block = blockItem.getBlock();

            // 使用更简洁的检查方式
            return block instanceof CropBlock ||
                    block instanceof SaplingBlock ||
                    block instanceof FlowerBlock ||
                    block instanceof TallFlowerBlock ||
                    block instanceof MushroomBlock ||
                    block instanceof NetherWartBlock ||
                    block instanceof SugarCaneBlock ||
                    block instanceof CactusBlock ||
                    block instanceof KelpBlock ||
                    block instanceof KelpPlantBlock ||
                    block instanceof SeagrassBlock ||
                    block instanceof SeaPickleBlock ||
                    block instanceof StemBlock ||
                    block instanceof AttachedStemBlock ||
                    block instanceof BambooSaplingBlock ||
                    block instanceof BambooStalkBlock ||
                    block instanceof ChorusFlowerBlock ||
                    block instanceof ChorusPlantBlock ||
                    block instanceof TwistingVinesBlock ||
                    block instanceof TwistingVinesPlantBlock ||
                    block instanceof WeepingVinesBlock ||
                    block instanceof WeepingVinesPlantBlock ||
                    block instanceof CaveVinesBlock ||
                    block instanceof CaveVinesPlantBlock ||
                    block instanceof GlowLichenBlock;
        }

        // 种子物品
        return item == Items.WHEAT_SEEDS ||
                item == Items.BEETROOT_SEEDS ||
                item == Items.MELON_SEEDS ||
                item == Items.PUMPKIN_SEEDS ||
                item == Items.TORCHFLOWER_SEEDS ||
                item == Items.PITCHER_POD;
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