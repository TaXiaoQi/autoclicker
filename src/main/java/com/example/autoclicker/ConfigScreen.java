package com.example.autoclicker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.LinearLayout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigScreen extends Screen {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("autoclicker.json");

    private final Screen parent;
    private final Config config;
    private EditBox attackIntervalField;
    private EditBox attackRandomnessField;
    private Checkbox attackRandomnessCheckbox;
    private Checkbox attackArmorStandCheckbox;
    private Checkbox attackHostileMobsCheckbox;
    private Checkbox attackNeutralMobsCheckbox;
    private Checkbox attackPassiveMobsCheckbox; // ✅ 新增
    private EditBox placeIntervalField;
    private EditBox placeRandomnessField;
    private Checkbox placeRandomnessCheckbox;
    private Checkbox placeUseBoneMealCheckbox;
    private Checkbox avoidInteractableBlocksCheckbox; // ✅ 新增
    private Checkbox humanizeClicksCheckbox;

    protected ConfigScreen(Screen parent) {
        super(Component.literal("自动点击器配置"));
        this.parent = parent;
        this.config = loadConfig();
    }

    @Override
    protected void init() {
        super.init();

        // 使用 LinearLayout
        LinearLayout layout = LinearLayout.vertical().spacing(8); // 减小间距以容纳更多选项

        // === 自动攻击配置 ===
        StringWidget attackTitle = new StringWidget(
                Component.literal("=== 自动攻击配置 ==="),
                this.font
        );
        layout.addChild(attackTitle);

        // 攻击间隔设置
        LinearLayout attackIntervalLayout = LinearLayout.horizontal().spacing(5);
        attackIntervalLayout.addChild(new StringWidget(
                Component.literal("攻击间隔 (ticks):"),
                this.font
        ));

        attackIntervalField = new EditBox(this.font, 80, 20, Component.literal("攻击间隔"));
        attackIntervalField.setValue(String.valueOf(config.attackInterval));
        attackIntervalField.setFilter(s -> {
            if (s.isEmpty()) return true;
            try {
                int value = Integer.parseInt(s);
                return value > 0;
            } catch (NumberFormatException e) {
                return false;
            }
        });
        attackIntervalLayout.addChild(attackIntervalField);
        layout.addChild(attackIntervalLayout);

        // 攻击随机性设置
        LinearLayout attackRandomnessLayout = LinearLayout.horizontal().spacing(5);
        attackRandomnessLayout.addChild(new StringWidget(
                Component.literal("攻击随机性 (±):"),
                this.font
        ));

        attackRandomnessField = new EditBox(this.font, 80, 20, Component.literal("攻击随机性"));
        attackRandomnessField.setValue(String.valueOf(config.attackRandomness));
        attackRandomnessField.setFilter(s -> {
            if (s.isEmpty()) return true;
            try {
                int value = Integer.parseInt(s);
                return value >= 0;
            } catch (NumberFormatException e) {
                return false;
            }
        });
        attackRandomnessLayout.addChild(attackRandomnessField);
        layout.addChild(attackRandomnessLayout);

        attackRandomnessCheckbox = Checkbox.builder(
                Component.literal("启用攻击随机性"),
                this.font
        ).selected(config.attackRandomnessEnabled).build();
        layout.addChild(attackRandomnessCheckbox);

        // 攻击目标类型
        attackArmorStandCheckbox = Checkbox.builder(
                Component.literal("攻击盔甲架"),
                this.font
        ).selected(config.attackArmorStands).build();
        layout.addChild(attackArmorStandCheckbox);

        attackHostileMobsCheckbox = Checkbox.builder(
                Component.literal("攻击敌对生物"),
                this.font
        ).selected(config.attackHostileMobs).build();
        layout.addChild(attackHostileMobsCheckbox);

        attackNeutralMobsCheckbox = Checkbox.builder(
                Component.literal("攻击中立生物"),
                this.font
        ).selected(config.attackNeutralMobs).build();
        layout.addChild(attackNeutralMobsCheckbox);

        attackPassiveMobsCheckbox = Checkbox.builder(
                Component.literal("攻击被动生物"),
                this.font
        ).selected(config.attackPassiveMobs).build();
        layout.addChild(attackPassiveMobsCheckbox);

        // === 自动放置配置 ===
        StringWidget placeTitle = new StringWidget(
                Component.literal("=== 自动放置配置 ==="),
                this.font
        );
        layout.addChild(placeTitle);

        // 放置间隔设置
        LinearLayout placeIntervalLayout = LinearLayout.horizontal().spacing(5);
        placeIntervalLayout.addChild(new StringWidget(
                Component.literal("放置间隔 (ticks):"),
                this.font
        ));

        placeIntervalField = new EditBox(this.font, 80, 20, Component.literal("放置间隔"));
        placeIntervalField.setValue(String.valueOf(config.placeInterval));
        placeIntervalField.setFilter(s -> {
            if (s.isEmpty()) return true;
            try {
                int value = Integer.parseInt(s);
                return value > 0;
            } catch (NumberFormatException e) {
                return false;
            }
        });
        placeIntervalLayout.addChild(placeIntervalField);
        layout.addChild(placeIntervalLayout);

        // 放置随机性设置
        LinearLayout placeRandomnessLayout = LinearLayout.horizontal().spacing(5);
        placeRandomnessLayout.addChild(new StringWidget(
                Component.literal("放置随机性 (±):"),
                this.font
        ));

        placeRandomnessField = new EditBox(this.font, 80, 20, Component.literal("放置随机性"));
        placeRandomnessField.setValue(String.valueOf(config.placeRandomness));
        placeRandomnessField.setFilter(s -> {
            if (s.isEmpty()) return true;
            try {
                int value = Integer.parseInt(s);
                return value >= 0;
            } catch (NumberFormatException e) {
                return false;
            }
        });
        placeRandomnessLayout.addChild(placeRandomnessField);
        layout.addChild(placeRandomnessLayout);

        placeRandomnessCheckbox = Checkbox.builder(
                Component.literal("启用放置随机性"),
                this.font
        ).selected(config.placeRandomnessEnabled).build();
        layout.addChild(placeRandomnessCheckbox);

        // 骨粉开关
        placeUseBoneMealCheckbox = Checkbox.builder(
                Component.literal("自动使用骨粉"),
                this.font
        ).selected(config.useBoneMeal).build();
        layout.addChild(placeUseBoneMealCheckbox);

        // 避开可交互方块
        avoidInteractableBlocksCheckbox = Checkbox.builder(
                Component.literal("避开可交互方块"),
                this.font
        ).selected(config.avoidInteractableBlocks).build();
        layout.addChild(avoidInteractableBlocksCheckbox);

        // === 反检测设置 ===
        StringWidget antiDetectionTitle = new StringWidget(
                Component.literal("=== 反检测设置 ==="),
                this.font
        );
        layout.addChild(antiDetectionTitle);

        humanizeClicksCheckbox = Checkbox.builder(
                Component.literal("人性化点击 (随机跳过)"),
                this.font
        ).selected(config.humanizeClicks).build();
        layout.addChild(humanizeClicksCheckbox);

        // === 按钮行 ===
        LinearLayout buttonLayout = LinearLayout.horizontal().spacing(10);

        // 保存按钮
        Button saveButton = Button.builder(Component.literal("保存"), button -> {
            saveConfig();
            if (this.minecraft != null) {
                this.minecraft.setScreen(parent);
            }
        }).build();
        buttonLayout.addChild(saveButton);

        // 取消按钮
        Button cancelButton = Button.builder(Component.literal("取消"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(parent);
            }
        }).build();
        buttonLayout.addChild(cancelButton);

        layout.addChild(buttonLayout);

        // 应用布局
        layout.arrangeElements();
        layout.setPosition(
                this.width / 2 - layout.getWidth() / 2,
                Math.max(20, this.height / 2 - layout.getHeight() / 2) // 确保不会太靠下
        );

        // 添加所有组件到屏幕
        layout.visitWidgets(this::addRenderableWidget);
    }

    private Config loadConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                return GSON.fromJson(json, Config.class);
            }
        } catch (IOException e) {
            AutoClicker.LOGGER.error("加载配置失败", e);
        }
        return new Config();
    }

    private void saveConfig() {
        try {
            // 验证攻击间隔
            int attackInterval;
            try {
                attackInterval = Integer.parseInt(attackIntervalField.getValue());
                if (attackInterval < 1) {
                    if (minecraft != null && minecraft.player != null) {
                        minecraft.player.displayClientMessage(
                                Component.literal("§c攻击间隔必须大于0"),
                                true
                        );
                    }
                    return;
                }
            } catch (NumberFormatException e) {
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.literal("§c攻击间隔必须是有效数字"),
                            true
                    );
                }
                return;
            }

            // 验证攻击随机性
            int attackRandomness;
            try {
                attackRandomness = Integer.parseInt(attackRandomnessField.getValue());
                if (attackRandomness < 0) {
                    if (minecraft != null && minecraft.player != null) {
                        minecraft.player.displayClientMessage(
                                Component.literal("§c攻击随机性不能为负数"),
                                true
                        );
                    }
                    return;
                }
            } catch (NumberFormatException e) {
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.literal("§c攻击随机性必须是有效数字"),
                            true
                    );
                }
                return;
            }

            // 验证放置间隔
            int placeInterval;
            try {
                placeInterval = Integer.parseInt(placeIntervalField.getValue());
                if (placeInterval < 1) {
                    if (minecraft != null && minecraft.player != null) {
                        minecraft.player.displayClientMessage(
                                Component.literal("§c放置间隔必须大于0"),
                                true
                        );
                    }
                    return;
                }
            } catch (NumberFormatException e) {
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.literal("§c放置间隔必须是有效数字"),
                            true
                    );
                }
                return;
            }

            // 验证放置随机性
            int placeRandomness;
            try {
                placeRandomness = Integer.parseInt(placeRandomnessField.getValue());
                if (placeRandomness < 0) {
                    if (minecraft != null && minecraft.player != null) {
                        minecraft.player.displayClientMessage(
                                Component.literal("§c放置随机性不能为负数"),
                                true
                        );
                    }
                    return;
                }
            } catch (NumberFormatException e) {
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.literal("§c放置随机性必须是有效数字"),
                            true
                    );
                }
                return;
            }

            // 更新配置
            config.attackInterval = attackInterval;
            config.attackRandomness = attackRandomness;
            config.attackRandomnessEnabled = attackRandomnessCheckbox.selected();
            config.attackArmorStands = attackArmorStandCheckbox.selected();
            config.attackHostileMobs = attackHostileMobsCheckbox.selected();
            config.attackNeutralMobs = attackNeutralMobsCheckbox.selected();
            config.attackPassiveMobs = attackPassiveMobsCheckbox.selected(); // ✅ 新增
            config.placeInterval = placeInterval;
            config.placeRandomness = placeRandomness;
            config.placeRandomnessEnabled = placeRandomnessCheckbox.selected();
            config.useBoneMeal = placeUseBoneMealCheckbox.selected();
            config.avoidInteractableBlocks = avoidInteractableBlocksCheckbox.selected(); // ✅ 新增
            config.humanizeClicks = humanizeClicksCheckbox.selected();

            // 保存到文件
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(config));

            // 重新加载配置到主模组
            if (minecraft != null) {
                AutoClicker.getInstance().reloadConfig();
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.literal("§a配置已保存并重载"),
                            true
                    );
                }
            }
        } catch (IOException e) {
            AutoClicker.LOGGER.error("保存配置失败", e);
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal("§c保存配置失败，请查看日志"),
                        true
                );
            }
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}