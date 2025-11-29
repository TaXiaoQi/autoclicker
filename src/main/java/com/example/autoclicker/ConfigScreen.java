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

    protected ConfigScreen(Screen parent) {
        super(Component.literal("自动点击器配置"));
        this.parent = parent;
        this.config = loadConfig();
    }

    @Override
    protected void init() {
        super.init();

        // 使用 LinearLayout
        LinearLayout layout = LinearLayout.vertical().spacing(10);

        // 标题 - 使用 StringWidget 替代 Label
        StringWidget titleWidget = new StringWidget(
                Component.literal("自动点击器配置"),
                this.font
        );
        layout.addChild(titleWidget);

        // 攻击间隔设置
        LinearLayout intervalLayout = LinearLayout.horizontal().spacing(5);

        // 间隔标签
        StringWidget intervalLabel = new StringWidget(
                Component.literal("攻击间隔 (ticks):"),
                this.font
        );
        intervalLayout.addChild(intervalLabel);

        // 间隔输入框
        EditBox intervalField = new EditBox(this.font, 100, 20, Component.literal("攻击间隔"));
        intervalField.setValue(String.valueOf(config.attackInterval));
        intervalField.setFilter(s -> {
            if (s.isEmpty()) return true;
            try {
                int value = Integer.parseInt(s);
                return value > 0;
            } catch (NumberFormatException e) {
                return false;
            }
        });
        intervalLayout.addChild(intervalField);
        layout.addChild(intervalLayout);

        // 攻击目标类型 - 使用 Checkbox
        Checkbox armorStandCheckbox = Checkbox.builder(
                Component.literal("攻击盔甲架"),
                this.font
        ).selected(config.attackArmorStands).build();
        layout.addChild(armorStandCheckbox);

        Checkbox hostileMobsCheckbox = Checkbox.builder(
                Component.literal("攻击敌对生物"),
                this.font
        ).selected(config.attackHostileMobs).build();
        layout.addChild(hostileMobsCheckbox);

        Checkbox neutralMobsCheckbox = Checkbox.builder(
                Component.literal("攻击中立生物"),
                this.font
        ).selected(config.attackNeutralMobs).build();
        layout.addChild(neutralMobsCheckbox);

        Checkbox passiveMobsCheckbox = Checkbox.builder(
                Component.literal("攻击被动生物"),
                this.font
        ).selected(config.attackPassiveMobs).build();
        layout.addChild(passiveMobsCheckbox);

        // 按钮行
        LinearLayout buttonLayout = LinearLayout.horizontal().spacing(10);

        // 保存按钮
        Button saveButton = Button.builder(
                Component.literal("保存"),
                button -> saveConfig(
                        intervalField.getValue(),
                        armorStandCheckbox.selected(),
                        hostileMobsCheckbox.selected(),
                        neutralMobsCheckbox.selected(),
                        passiveMobsCheckbox.selected()
                )
        ).build();
        buttonLayout.addChild(saveButton);

        // 取消按钮
        Button cancelButton = Button.builder(
                Component.literal("取消"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(parent);
                    }
                }
        ).build();
        buttonLayout.addChild(cancelButton);

        layout.addChild(buttonLayout);

        // 应用布局
        layout.arrangeElements();
        layout.setPosition(
                this.width / 2 - layout.getWidth() / 2,
                this.height / 2 - layout.getHeight() / 2
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

    private void saveConfig(String intervalValue, boolean attackArmorStands,
                            boolean attackHostileMobs, boolean attackNeutralMobs,
                            boolean attackPassiveMobs) {
        try {
            // 验证攻击间隔
            int interval;
            try {
                interval = Integer.parseInt(intervalValue);
                if (interval < 1) {
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

            // 更新配置
            config.attackInterval = interval;
            config.attackArmorStands = attackArmorStands;
            config.attackHostileMobs = attackHostileMobs;
            config.attackNeutralMobs = attackNeutralMobs;
            config.attackPassiveMobs = attackPassiveMobs;

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