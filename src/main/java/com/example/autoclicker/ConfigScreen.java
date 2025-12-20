package com.example.autoclicker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 自动点击器的配置界面（GUI）
 * 使用 Minecraft 客户端 GUI 系统构建，支持滚动、输入验证、保存/加载 JSON 配置。
 */
public class ConfigScreen extends Screen {

    // Gson 实例：用于将 Config 对象序列化/反序列化为 JSON
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 配置文件路径：位于 .minecraft/config/autoclicker.json
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("autoclicker.json");

    // 上一级屏幕（返回时使用）
    private final Screen parent;

    // 当前加载的配置对象（从文件或默认值）
    private final Config config;

    // 滚动容器：使内容超出屏幕高度时可滚动
    private ScrollContainer scrollContainer;

    // 所有 UI 控件的引用（用于读取用户输入）
    private EditBox attackIntervalField;           // 攻击间隔（毫秒）
    private EditBox attackRandomnessField;         // 攻击随机偏移量
    private Checkbox attackRandomnessCheckbox;     // 是否启用攻击随机性
    private Checkbox attackArmorStandCheckbox;     // 是否攻击盔甲架
    private Checkbox attackHostileMobsCheckbox;    // 是否攻击敌对生物
    private Checkbox attackNeutralMobsCheckbox;    // 是否攻击中立生物
    private Checkbox attackPassiveMobsCheckbox;    // 是否攻击被动生物
    private EditBox placeIntervalField;            // 放置方块间隔
    private EditBox placeRandomnessField;          // 放置随机偏移量
    private Checkbox placeRandomnessCheckbox;      // 是否启用放置随机性
    private Checkbox useBoneMealCheckbox;          // 是否自动使用骨粉
    private Checkbox humanizeClicksCheckbox;       // 是否启用“拟人化”点击（反检测）

    /**
     * 构造函数：初始化配置界面
     * @param parent 返回时跳转的上一级屏幕
     */
    protected ConfigScreen(Screen parent) {
        super(Component.translatable("screen.autoclicker.config.title")); // 设置窗口标题（支持多语言）
        this.parent = parent;
        this.config = loadConfig(); // 从磁盘加载配置，若无则用默认值
    }

    /**
     * 初始化 GUI 组件（每次打开界面时调用）
     */
    @Override
    protected void init() {
        super.init();

        // 创建滚动容器并设置尺寸为全屏
        scrollContainer = new ScrollContainer();
        scrollContainer.setWidth(this.width);
        scrollContainer.setHeight(this.height);

        // 垂直布局容器：用于组织所有配置项
        LinearLayout contentLayout = LinearLayout.vertical().spacing(10); // 元素间距 10px

        // === 自动攻击配置区域 ===
        StringWidget attackTitle = new StringWidget(
                Component.translatable("screen.autoclicker.section.attack").withStyle(ChatFormatting.BOLD),
                this.font
        );
        contentLayout.addChild(attackTitle);

        // 攻击间隔输入框（必须为正整数）
        LinearLayout attackIntervalLayout = LinearLayout.horizontal().spacing(5);
        attackIntervalLayout.addChild(new StringWidget(Component.translatable("config.autoclicker.attack_interval"), this.font));
        attackIntervalField = new EditBox(this.font, 80, 20, Component.empty());
        attackIntervalField.setValue(String.valueOf(config.attackInterval));
        attackIntervalField.setFilter(s -> s.isEmpty() || isPositiveInt(s)); // 输入过滤器：只允许空或正整数
        attackIntervalLayout.addChild(attackIntervalField);
        contentLayout.addChild(attackIntervalLayout);

        // 攻击随机性数值（非负整数）
        LinearLayout attackRandomnessLayout = LinearLayout.horizontal().spacing(5);
        attackRandomnessLayout.addChild(new StringWidget(Component.translatable("config.autoclicker.attack_randomness"), this.font));
        attackRandomnessField = new EditBox(this.font, 80, 20, Component.empty());
        attackRandomnessField.setValue(String.valueOf(config.attackRandomness));
        attackRandomnessField.setFilter(s -> s.isEmpty() || isNonNegativeInt(s));
        attackRandomnessLayout.addChild(attackRandomnessField);
        contentLayout.addChild(attackRandomnessLayout);

        // 启用攻击随机性复选框
        attackRandomnessCheckbox = Checkbox.builder(
                Component.translatable("config.autoclicker.enable_attack_randomness"),
                this.font
        ).selected(config.attackRandomnessEnabled).build();
        contentLayout.addChild(attackRandomnessCheckbox);

        // 攻击目标类型（使用网格布局 2x2）
        GridLayout targetGrid = new GridLayout().spacing(4);
        targetGrid.defaultCellSetting().alignHorizontallyLeft();

        attackArmorStandCheckbox = Checkbox.builder(Component.translatable("config.autoclicker.attack_armor_stands"), this.font)
                .selected(config.attackArmorStands).build();
        targetGrid.addChild(attackArmorStandCheckbox, 0, 0);

        attackHostileMobsCheckbox = Checkbox.builder(Component.translatable("config.autoclicker.attack_hostile_mobs"), this.font)
                .selected(config.attackHostileMobs).build();
        targetGrid.addChild(attackHostileMobsCheckbox, 1, 0);

        attackNeutralMobsCheckbox = Checkbox.builder(Component.translatable("config.autoclicker.attack_neutral_mobs"), this.font)
                .selected(config.attackNeutralMobs).build();
        targetGrid.addChild(attackNeutralMobsCheckbox, 0, 1);

        attackPassiveMobsCheckbox = Checkbox.builder(Component.translatable("config.autoclicker.attack_passive_mobs"), this.font)
                .selected(config.attackPassiveMobs).build();
        targetGrid.addChild(attackPassiveMobsCheckbox, 1, 1);

        contentLayout.addChild(targetGrid);

        // === 自动放置配置区域 ===
        StringWidget placeTitle = new StringWidget(
                Component.translatable("screen.autoclicker.section.place").withStyle(ChatFormatting.BOLD),
                this.font
        );
        contentLayout.addChild(placeTitle);

        // 放置间隔
        LinearLayout placeIntervalLayout = LinearLayout.horizontal().spacing(5);
        placeIntervalLayout.addChild(new StringWidget(Component.translatable("config.autoclicker.place_interval"), this.font));
        placeIntervalField = new EditBox(this.font, 80, 20, Component.empty());
        placeIntervalField.setValue(String.valueOf(config.placeInterval));
        placeIntervalField.setFilter(s -> s.isEmpty() || isPositiveInt(s));
        placeIntervalLayout.addChild(placeIntervalField);
        contentLayout.addChild(placeIntervalLayout);

        // 放置随机性数值
        LinearLayout placeRandomnessLayout = LinearLayout.horizontal().spacing(5);
        placeRandomnessLayout.addChild(new StringWidget(Component.translatable("config.autoclicker.place_randomness"), this.font));
        placeRandomnessField = new EditBox(this.font, 80, 20, Component.empty());
        placeRandomnessField.setValue(String.valueOf(config.placeRandomness));
        placeRandomnessField.setFilter(s -> s.isEmpty() || isNonNegativeInt(s));
        placeRandomnessLayout.addChild(placeRandomnessField);
        contentLayout.addChild(placeRandomnessLayout);

        // 启用放置随机性
        placeRandomnessCheckbox = Checkbox.builder(
                Component.translatable("config.autoclicker.enable_place_randomness"),
                this.font
        ).selected(config.placeRandomnessEnabled).build();
        contentLayout.addChild(placeRandomnessCheckbox);

        // 自动使用骨粉
        useBoneMealCheckbox = Checkbox.builder(
                Component.translatable("config.autoclicker.use_bone_meal"),
                this.font
        ).selected(config.useBoneMeal).build();
        contentLayout.addChild(useBoneMealCheckbox);

        // === 反检测设置 ===
        StringWidget antiDetectionTitle = new StringWidget(
                Component.translatable("screen.autoclicker.section.anti_detection").withStyle(ChatFormatting.BOLD),
                this.font
        );
        contentLayout.addChild(antiDetectionTitle);

        humanizeClicksCheckbox = Checkbox.builder(
                Component.translatable("config.autoclicker.humanize_clicks"),
                this.font
        ).selected(config.humanizeClicks).build();
        contentLayout.addChild(humanizeClicksCheckbox);

        // === 底部按钮：保存 / 取消 ===
        LinearLayout buttonLayout = LinearLayout.horizontal().spacing(10);
        Button saveButton = Button.builder(Component.translatable("gui.autoclicker.save"), button -> saveConfig()).build();
        Button cancelButton = Button.builder(Component.translatable("gui.autoclicker.cancel"), button -> onClose()).build();
        buttonLayout.addChild(saveButton);
        buttonLayout.addChild(cancelButton);
        contentLayout.addChild(buttonLayout);

        // 调整内容宽度（最大 400px），并排版
        int contentWidth = Math.min(400, this.width - 40);
        contentLayout.arrangeElements();

        // 将内容放入滚动容器，并添加到屏幕
        scrollContainer.setContent(contentLayout);
        scrollContainer.setX(0);
        scrollContainer.setY(0);

        this.addRenderableWidget(scrollContainer);
    }

    /**
     * 渲染界面：背景 + 标题 + 滚动内容
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta); // 半透明背景
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF); // 居中标题
        scrollContainer.render(guiGraphics, mouseX, mouseY, delta); // 渲染滚动内容
    }

    // === 工具方法：输入验证 ===
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

    /**
     * 从磁盘加载配置，失败则返回默认配置
     */
    private Config loadConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                return GSON.fromJson(json, Config.class);
            }
        } catch (IOException e) {
            AutoClicker.LOGGER.error("Failed to load config", e);
        }
        return new Config(); // 默认配置
    }

    /**
     * 保存配置：验证输入 → 更新 config → 写入文件 → 通知主类重载 → 提示成功
     */
    private void saveConfig() {
        // 验证所有输入字段
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

        // 保存到文件
        try {
            Files.createDirectories(CONFIG_PATH.getParent()); // 确保目录存在
            Files.writeString(CONFIG_PATH, GSON.toJson(config));

            // 通知主类重新加载配置（使更改立即生效）
            AutoClicker.getInstance().reloadConfig();

            // 显示成功提示
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.translatable("autoclicker.message.config_saved").withStyle(ChatFormatting.GREEN),
                        true
                );
            }
            onClose(); // 关闭当前界面，返回上一级
        } catch (IOException e) {
            AutoClicker.LOGGER.error("Failed to save config", e);
            showError(Component.translatable("autoclicker.error.save_failed"));
        }
    }

    /**
     * 显示错误消息（在聊天栏）
     */
    private void showError(Component message) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(message, true);
        }
    }

    /**
     * 关闭界面时返回上一级屏幕
     */
    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    // === 鼠标事件转发给滚动容器（实现滚动/拖拽）===

    /**
     * 处理鼠标滚轮滚动事件
     * 当鼠标在滚动容器区域内时，将滚轮事件转发给容器处理
     * @param mouseX 鼠标X坐标（屏幕坐标）
     * @param mouseY 鼠标Y坐标（屏幕坐标）
     * @param deltaX 水平滚动量（通常为0）
     * @param deltaY 垂直滚动量（>0向上滚，<0向下滚）
     * @return true表示事件已处理，阻止进一步传递
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (scrollContainer.isMouseOver(mouseX, mouseY)) {
            return scrollContainer.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }
}