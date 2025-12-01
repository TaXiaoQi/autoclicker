package com.example.autoclicker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.*;

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
    private Checkbox humanizeClicksCheckbox;

    protected ConfigScreen(Screen parent) {
        super(Component.literal("自动点击器配置"));
        this.parent = parent;
        this.config = loadConfig();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        init();
    }

    @Override
    protected void init() {
        super.init();

        // === 创建垂直列表容器 ===
        int leftColumnWidth = 150; // 左侧标签宽度
        int rightColumnWidth = 100; // 右侧输入框宽度
        int elementHeight = 20; // 元素高度
        int spacing = 10; // 元素间距
        int padding = 20; // 边距

        int x = this.width / 2 - (leftColumnWidth + spacing + rightColumnWidth) / 2;
        int y = padding;

        // === 自动攻击配置 ===
        StringWidget attackTitle = new StringWidget(
                Component.literal("=== 自动攻击配置 ==="),
                this.font
        );
        attackTitle.setPosition(x, y);
        attackTitle.setWidth(leftColumnWidth + spacing + rightColumnWidth);
        this.addRenderableWidget(attackTitle);
        y += elementHeight + spacing;

        // 攻击间隔设置
        StringWidget attackIntervalLabel = new StringWidget(
                Component.literal("攻击间隔 (ticks):"),
                this.font
        );
        attackIntervalLabel.setPosition(x, y);
        attackIntervalLabel.setWidth(leftColumnWidth);
        this.addRenderableWidget(attackIntervalLabel);

        attackIntervalField = new EditBox(this.font, x + leftColumnWidth + spacing, y, rightColumnWidth, elementHeight, Component.literal("攻击间隔"));
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
        this.addRenderableWidget(attackIntervalField);
        y += elementHeight + spacing;

        // 攻击随机性设置
        StringWidget attackRandomnessLabel = new StringWidget(
                Component.literal("攻击随机性 (±):"),
                this.font
        );
        attackRandomnessLabel.setPosition(x, y);
        attackRandomnessLabel.setWidth(leftColumnWidth);
        this.addRenderableWidget(attackRandomnessLabel);

        attackRandomnessField = new EditBox(this.font, x + leftColumnWidth + spacing, y, rightColumnWidth, elementHeight, Component.literal("攻击随机性"));
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
        this.addRenderableWidget(attackRandomnessField);
        y += elementHeight + spacing;

        attackRandomnessCheckbox = Checkbox.builder(
                Component.literal("启用攻击随机性"),
                this.font
        ).selected(config.attackRandomnessEnabled).pos(x, y).build();
        this.addRenderableWidget(attackRandomnessCheckbox);
        y += elementHeight + spacing;

        // 攻击目标类型
        attackArmorStandCheckbox = Checkbox.builder(
                Component.literal("攻击盔甲架"),
                this.font
        ).selected(config.attackArmorStands).pos(x, y).build();
        this.addRenderableWidget(attackArmorStandCheckbox);
        y += elementHeight + spacing/2;

        attackHostileMobsCheckbox = Checkbox.builder(
                Component.literal("攻击敌对生物"),
                this.font
        ).selected(config.attackHostileMobs).pos(x, y).build();
        this.addRenderableWidget(attackHostileMobsCheckbox);
        y += elementHeight + spacing/2;

        attackNeutralMobsCheckbox = Checkbox.builder(
                Component.literal("攻击中立生物"),
                this.font
        ).selected(config.attackNeutralMobs).pos(x, y).build();
        this.addRenderableWidget(attackNeutralMobsCheckbox);
        y += elementHeight + spacing/2;

        attackPassiveMobsCheckbox = Checkbox.builder(
                Component.literal("攻击被动生物"),
                this.font
        ).selected(config.attackPassiveMobs).pos(x, y).build();
        this.addRenderableWidget(attackPassiveMobsCheckbox);
        y += elementHeight + spacing;

        // === 自动放置配置 ===
        StringWidget placeTitle = new StringWidget(
                Component.literal("=== 自动放置配置 ==="),
                this.font
        );
        placeTitle.setPosition(x, y);
        placeTitle.setWidth(leftColumnWidth + spacing + rightColumnWidth);
        this.addRenderableWidget(placeTitle);
        y += elementHeight + spacing;

        // 放置间隔设置
        StringWidget placeIntervalLabel = new StringWidget(
                Component.literal("放置间隔 (ticks):"),
                this.font
        );
        placeIntervalLabel.setPosition(x, y);
        placeIntervalLabel.setWidth(leftColumnWidth);
        this.addRenderableWidget(placeIntervalLabel);

        placeIntervalField = new EditBox(this.font, x + leftColumnWidth + spacing, y, rightColumnWidth, elementHeight, Component.literal("放置间隔"));
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
        this.addRenderableWidget(placeIntervalField);
        y += elementHeight + spacing;

        // 放置随机性设置
        StringWidget placeRandomnessLabel = new StringWidget(
                Component.literal("放置随机性 (±ticks):"),
                this.font
        );
        placeRandomnessLabel.setPosition(x, y);
        placeRandomnessLabel.setWidth(leftColumnWidth);
        this.addRenderableWidget(placeRandomnessLabel);

        placeRandomnessField = new EditBox(this.font, x + leftColumnWidth + spacing, y, rightColumnWidth, elementHeight, Component.literal("放置随机性"));
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
        this.addRenderableWidget(placeRandomnessField);
        y += elementHeight + spacing;

        placeRandomnessCheckbox = Checkbox.builder(
                Component.literal("启用放置随机性"),
                this.font
        ).selected(config.placeRandomnessEnabled).pos(x, y).build();
        this.addRenderableWidget(placeRandomnessCheckbox);
        y += elementHeight + spacing;

        // 骨粉开关
        placeUseBoneMealCheckbox = Checkbox.builder(
                Component.literal("自动使用骨粉"),
                this.font
        ).selected(config.useBoneMeal).pos(x, y).build();
        this.addRenderableWidget(placeUseBoneMealCheckbox);
        y += elementHeight + spacing;

        // === 反检测设置 ===
        StringWidget antiDetectionTitle = new StringWidget(
                Component.literal("=== 反检测设置 ==="),
                this.font
        );
        antiDetectionTitle.setPosition(x, y);
        antiDetectionTitle.setWidth(leftColumnWidth + spacing + rightColumnWidth);
        this.addRenderableWidget(antiDetectionTitle);
        y += elementHeight + spacing;

        humanizeClicksCheckbox = Checkbox.builder(
                Component.literal("人性化点击 (随机跳过)"),
                this.font
        ).selected(config.humanizeClicks).pos(x, y).build();
        this.addRenderableWidget(humanizeClicksCheckbox);
        y += elementHeight + spacing * 2;

        // === 按钮行 ===
        int buttonWidth = 80;
        int totalButtonsWidth = buttonWidth * 2 + spacing;
        int buttonX = this.width / 2 - totalButtonsWidth / 2;

        // 保存按钮
        Button saveButton = Button.builder(Component.literal("保存"), button -> {
            saveConfig();
            if (this.minecraft != null) {
                this.minecraft.setScreen(parent);
            }
        }).bounds(buttonX, y, buttonWidth, elementHeight).build();
        this.addRenderableWidget(saveButton);

        // 取消按钮
        Button cancelButton = Button.builder(Component.literal("取消"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(parent);
            }
        }).bounds(buttonX + buttonWidth + spacing, y, buttonWidth, elementHeight).build();
        this.addRenderableWidget(cancelButton);
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