package com.example.autoclicker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
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

    // UI 组件引用（用于保存时读取值）
    private EditBox attackIntervalField;
    private EditBox attackRandomnessField;
    private Checkbox attackRandomnessCheckbox;
    private Checkbox attackArmorStandCheckbox;
    private Checkbox attackHostileMobsCheckbox;
    private Checkbox attackNeutralMobsCheckbox;
    private Checkbox attackPassiveMobsCheckbox;
    private EditBox placeIntervalField;
    private EditBox placeRandomnessField;
    private Checkbox placeRandomnessCheckbox;
    private Checkbox useBoneMealCheckbox;
    private Checkbox humanizeClicksCheckbox;

    protected ConfigScreen(Screen parent) {
        super(Component.translatable("screen.autoclicker.config.title"));
        this.parent = parent;
        this.config = loadConfig();
    }

    @Override
    protected void init() {
        super.init();

        LinearLayout layout = LinearLayout.vertical().spacing(10);

        // === 自动攻击配置 ===
        StringWidget attackTitle = new StringWidget(
                Component.translatable("screen.autoclicker.section.attack"),
                this.font
        );
        layout.addChild(attackTitle);

        // 攻击间隔
        LinearLayout attackIntervalLayout = LinearLayout.horizontal().spacing(5);
        attackIntervalLayout.addChild(new StringWidget(
                Component.translatable("config.autoclicker.attack_interval"),
                this.font
        ));
        attackIntervalField = new EditBox(this.font, 80, 20, Component.empty());
        attackIntervalField.setValue(String.valueOf(config.attackInterval));
        attackIntervalField.setFilter(s -> s.isEmpty() || (isPositiveInt(s)));
        attackIntervalLayout.addChild(attackIntervalField);
        layout.addChild(attackIntervalLayout);

        // 攻击随机性数值
        LinearLayout attackRandomnessLayout = LinearLayout.horizontal().spacing(5);
        attackRandomnessLayout.addChild(new StringWidget(
                Component.translatable("config.autoclicker.attack_randomness"),
                this.font
        ));
        attackRandomnessField = new EditBox(this.font, 80, 20, Component.empty());
        attackRandomnessField.setValue(String.valueOf(config.attackRandomness));
        attackRandomnessField.setFilter(s -> s.isEmpty() || (isNonNegativeInt(s)));
        attackRandomnessLayout.addChild(attackRandomnessField);
        layout.addChild(attackRandomnessLayout);

        // 启用攻击随机性
        attackRandomnessCheckbox = Checkbox.builder(
                Component.translatable("config.autoclicker.enable_attack_randomness"),
                this.font
        ).selected(config.attackRandomnessEnabled).build();
        layout.addChild(attackRandomnessCheckbox);

        // 攻击目标类型
        attackArmorStandCheckbox = Checkbox.builder(
                Component.translatable("config.autoclicker.attack_armor_stands"),
                this.font
        ).selected(config.attackArmorStands).build();
        layout.addChild(attackArmorStandCheckbox);

        attackHostileMobsCheckbox = Checkbox.builder(
                Component.translatable("config.autoclicker.attack_hostile_mobs"),
                this.font
        ).selected(config.attackHostileMobs).build();
        layout.addChild(attackHostileMobsCheckbox);

        attackNeutralMobsCheckbox = Checkbox.builder(
                Component.translatable("config.autoclicker.attack_neutral_mobs"),
                this.font
        ).selected(config.attackNeutralMobs).build();
        layout.addChild(attackNeutralMobsCheckbox);

        attackPassiveMobsCheckbox = Checkbox.builder(
                Component.translatable("config.autoclicker.attack_passive_mobs"),
                this.font
        ).selected(config.attackPassiveMobs).build();
        layout.addChild(attackPassiveMobsCheckbox);

        // === 自动放置配置 ===
        StringWidget placeTitle = new StringWidget(
                Component.translatable("screen.autoclicker.section.place"),
                this.font
        );
        layout.addChild(placeTitle);

        // 放置间隔
        LinearLayout placeIntervalLayout = LinearLayout.horizontal().spacing(5);
        placeIntervalLayout.addChild(new StringWidget(
                Component.translatable("config.autoclicker.place_interval"),
                this.font
        ));
        placeIntervalField = new EditBox(this.font, 80, 20, Component.empty());
        placeIntervalField.setValue(String.valueOf(config.placeInterval));
        placeIntervalField.setFilter(s -> s.isEmpty() || (isPositiveInt(s)));
        placeIntervalLayout.addChild(placeIntervalField);
        layout.addChild(placeIntervalLayout);

        // 放置随机性数值
        LinearLayout placeRandomnessLayout = LinearLayout.horizontal().spacing(5);
        placeRandomnessLayout.addChild(new StringWidget(
                Component.translatable("config.autoclicker.place_randomness"),
                this.font
        ));
        placeRandomnessField = new EditBox(this.font, 80, 20, Component.empty());
        placeRandomnessField.setValue(String.valueOf(config.placeRandomness));
        placeRandomnessField.setFilter(s -> s.isEmpty() || (isNonNegativeInt(s)));
        placeRandomnessLayout.addChild(placeRandomnessField);
        layout.addChild(placeRandomnessLayout);

        // 启用放置随机性
        placeRandomnessCheckbox = Checkbox.builder(
                Component.translatable("config.autoclicker.enable_place_randomness"),
                this.font
        ).selected(config.placeRandomnessEnabled).build();
        layout.addChild(placeRandomnessCheckbox);

        // 自动使用骨粉
        useBoneMealCheckbox = Checkbox.builder(
                Component.translatable("config.autoclicker.use_bone_meal"),
                this.font
        ).selected(config.useBoneMeal).build();
        layout.addChild(useBoneMealCheckbox);

        // === 反检测设置 ===
        StringWidget antiDetectionTitle = new StringWidget(
                Component.translatable("screen.autoclicker.section.anti_detection"),
                this.font
        );
        layout.addChild(antiDetectionTitle);

        humanizeClicksCheckbox = Checkbox.builder(
                Component.translatable("config.autoclicker.humanize_clicks"),
                this.font
        ).selected(config.humanizeClicks).build();
        layout.addChild(humanizeClicksCheckbox);

        // === 按钮 ===
        LinearLayout buttonLayout = LinearLayout.horizontal().spacing(10);

        Button saveButton = Button.builder(
                Component.translatable("gui.autoclicker.save"),
                button -> saveConfig()
        ).build();
        buttonLayout.addChild(saveButton);

        Button cancelButton = Button.builder(
                Component.translatable("gui.autoclicker.cancel"),
                button -> onClose()
        ).build();
        buttonLayout.addChild(cancelButton);

        layout.addChild(buttonLayout);

        // 布局定位居中
        layout.arrangeElements();
        int x = (this.width - layout.getWidth()) / 2;
        int y = Math.max(20, (this.height - layout.getHeight()) / 2);
        layout.setPosition(x, y);
        layout.visitWidgets(this::addRenderableWidget);
    }

    private boolean isPositiveInt(String s) {
        try {
            return Integer.parseInt(s) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isNonNegativeInt(String s) {
        try {
            return Integer.parseInt(s) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Config loadConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                return GSON.fromJson(json, Config.class);
            }
        } catch (IOException e) {
            AutoClicker.LOGGER.error("Failed to load config", e);
        }
        return new Config();
    }

    private void saveConfig() {
        // 验证并解析输入
        int attackInterval, attackRandomness, placeInterval, placeRandomness;

        try {
            attackInterval = Integer.parseInt(attackIntervalField.getValue());
            if (attackInterval < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError(Component.translatable("autoclicker.error.attack_interval_positive"));
            return;
        }

        try {
            attackRandomness = Integer.parseInt(attackRandomnessField.getValue());
            if (attackRandomness < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError(Component.translatable("autoclicker.error.attack_randomness_non_negative"));
            return;
        }

        try {
            placeInterval = Integer.parseInt(placeIntervalField.getValue());
            if (placeInterval < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError(Component.translatable("autoclicker.error.place_interval_positive"));
            return;
        }

        try {
            placeRandomness = Integer.parseInt(placeRandomnessField.getValue());
            if (placeRandomness < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError(Component.translatable("autoclicker.error.place_randomness_non_negative"));
            return;
        }

        // 更新配置对象
        config.attackInterval = attackInterval;
        config.attackRandomness = attackRandomness;
        config.attackRandomnessEnabled = attackRandomnessCheckbox.selected();
        config.attackArmorStands = attackArmorStandCheckbox.selected();
        config.attackHostileMobs = attackHostileMobsCheckbox.selected();
        config.attackNeutralMobs = attackNeutralMobsCheckbox.selected();
        config.attackPassiveMobs = attackPassiveMobsCheckbox.selected();
        config.placeInterval = placeInterval;
        config.placeRandomness = placeRandomness;
        config.placeRandomnessEnabled = placeRandomnessCheckbox.selected();
        config.useBoneMeal = useBoneMealCheckbox.selected();
        config.humanizeClicks = humanizeClicksCheckbox.selected();

        // 保存到磁盘
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(config));

            AutoClicker.getInstance().reloadConfig();

            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.translatable("autoclicker.message.config_saved").withStyle(ChatFormatting.GREEN),
                        true
                );
            }
            onClose(); // 返回上一屏幕
        } catch (IOException e) {
            AutoClicker.LOGGER.error("Failed to save config", e);
            showError(Component.translatable("autoclicker.error.save_failed"));
        }
    }

    private void showError(Component message) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(message.withStyle(ChatFormatting.RED), true);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}