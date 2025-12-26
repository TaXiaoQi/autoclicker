package com.example.autoclicker.gui;

import com.example.autoclicker.AutoClicker;
import com.example.autoclicker.Config;
import com.example.autoclicker.gui.elements.BooleanElement;
import com.example.autoclicker.gui.elements.ConfigElement;
import com.example.autoclicker.gui.elements.IntSliderElement;
import com.example.autoclicker.gui.widgets.ScrollableList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private final Config config;
    private ScrollableList scrollList;
    private final List<ConfigElement<?>> elements = new ArrayList<>();

    private Button saveButton;
    private Button cancelButton;
    private Button resetButton;

    public ConfigScreen(Screen parent) {
        super(Component.literal("自动点击器配置"));
        this.parent = parent;
        this.config = Config.load();
    }

    @Override
    protected void init() {
        super.init();

        // 创建滑动列表（占据屏幕大部分区域）
        int listHeight = height - 60; // 留出底部按钮空间
        scrollList = new ScrollableList(20, 20, width - 40, listHeight,
                Component.literal("配置选项"));
        addRenderableWidget(scrollList);

        // 清空列表
        scrollList.clearChildren();
        elements.clear();

        // === 添加配置元素 ===
        int yOffset = 0;
        int elementWidth = width - 80;

        // 标题
        scrollList.addChild(new net.minecraft.client.gui.components.StringWidget(
                0, yOffset, elementWidth, 20,
                Component.literal("自动攻击设置").withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE),
                font
        ));
        yOffset += 25;

        // 自动攻击开关
        elements.add(new BooleanElement(0, yOffset, elementWidth, 20,
                Component.literal("自动攻击"),
                config.autoAttackEnabled,
                value -> {
                    config.autoAttackEnabled = value;
                    updateSaveButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 25;

        // 攻击间隔滑块
        elements.add(new IntSliderElement(0, yOffset, elementWidth, 20,
                Component.literal("攻击间隔 (tick)"),
                1, 100, config.attackInterval,
                value -> {
                    config.attackInterval = value;
                    updateSaveButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 30;

        // 攻击随机性开关
        elements.add(new BooleanElement(0, yOffset, elementWidth, 20,
                Component.literal("启用攻击随机性"),
                config.attackRandomnessEnabled,
                value -> {
                    config.attackRandomnessEnabled = value;
                    updateSaveButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 25;

        // 攻击随机范围滑块
        elements.add(new IntSliderElement(0, yOffset, elementWidth, 20,
                Component.literal("攻击随机范围 (±)"),
                0, 20, config.attackRandomness,
                value -> {
                    config.attackRandomness = value;
                    updateSaveButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 30;

        // 攻击目标类型
        scrollList.addChild(new net.minecraft.client.gui.components.StringWidget(
                0, yOffset, elementWidth, 20,
                Component.literal("攻击目标:"),
                font
        ));
        yOffset += 25;

        // 盔甲架开关
        elements.add(new BooleanElement(0, yOffset, elementWidth/2-5, 20,
                Component.literal("攻击盔甲架"),
                config.attackArmorStands,
                value -> {
                    config.attackArmorStands = value;
                    updateSaveButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());

        // 敌对生物开关
        elements.add(new BooleanElement(elementWidth/2+5, yOffset, elementWidth/2-5, 20,
                Component.literal("攻击敌对生物"),
                config.attackHostileMobs,
                value -> {
                    config.attackHostileMobs = value;
                    updateSaveButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 25;

        // 中立生物开关
        elements.add(new BooleanElement(0, yOffset, elementWidth/2-5, 20,
                Component.literal("攻击中立生物"),
                config.attackNeutralMobs,
                value -> {
                    config.attackNeutralMobs = value;
                    updateSaveButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 30;

        // === 自动放置设置 ===
        scrollList.addChild(new net.minecraft.client.gui.components.StringWidget(
                0, yOffset, elementWidth, 20,
                Component.literal("自动放置设置").withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE),
                font
        ));
        yOffset += 25;

        // 自动放置开关
        elements.add(new BooleanElement(0, yOffset, elementWidth, 20,
                Component.literal("自动放置"),
                config.autoPlaceEnabled,
                value -> {
                    config.autoPlaceEnabled = value;
                    updateSaveButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 25;

        // 放置间隔滑块
        elements.add(new IntSliderElement(0, yOffset, elementWidth, 20,
                Component.literal("放置间隔 (tick)"),
                1, 40, config.placeInterval,
                value -> {
                    config.placeInterval = value;
                    updateSaveButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 30;

        // 使用骨粉开关
        elements.add(new BooleanElement(0, yOffset, elementWidth, 20,
                Component.literal("自动使用骨粉"),
                config.useBoneMeal,
                value -> {
                    config.useBoneMeal = value;
                    updateSaveButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 30;

        // === 反检测设置 ===
        scrollList.addChild(new net.minecraft.client.gui.components.StringWidget(
                0, yOffset, elementWidth, 20,
                Component.literal("反检测设置").withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE),
                font
        ));
        yOffset += 25;

        // 人性化点击开关
        elements.add(new BooleanElement(0, yOffset, elementWidth, 20,
                Component.literal("人性化点击 (随机跳过)"),
                config.humanizeClicks,
                value -> {
                    config.humanizeClicks = value;
                    updateSaveButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());

        // === 底部按钮 ===
        int buttonY = height - 35;
        int buttonWidth = 100;

        // 保存按钮
        saveButton = Button.builder(
                        Component.literal("保存").withStyle(ChatFormatting.GREEN),
                        button -> saveAndClose()
                ).pos(width / 2 - buttonWidth - 10, buttonY)
                .size(buttonWidth, 20)
                .build();
        addRenderableWidget(saveButton);

        // 取消按钮
        cancelButton = Button.builder(
                        Component.literal("取消").withStyle(ChatFormatting.RED),
                        button -> onClose()
                ).pos(20, buttonY)
                .size(buttonWidth, 20)
                .build();
        addRenderableWidget(cancelButton);

        // 重置按钮
        resetButton = Button.builder(
                        Component.literal("重置").withStyle(ChatFormatting.YELLOW),
                        button -> resetAll()
                ).pos(width - buttonWidth - 20, buttonY)
                .size(buttonWidth, 20)
                .build();
        addRenderableWidget(resetButton);

        // 初始更新保存按钮状态
        updateSaveButton();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // 绘制半透明背景
        graphics.fill(0, 0, width, height, 0x80000000);

        // 绘制标题
        graphics.drawCenteredString(font, title, width / 2, 5, 0xFFFFFF);

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private void updateSaveButton() {
        saveButton.active = elements.stream().anyMatch(ConfigElement::isChanged);
    }

    private void saveAndClose() {
        // 保存所有修改的配置
        for (ConfigElement<?> element : elements) {
            if (element.isChanged()) {
                element.save();
            }
        }

        // 保存到文件
        config.save();

        // 通知主类重新加载配置
        AutoClicker.getInstance().reloadConfig();

        // 关闭屏幕
        onClose();
    }

    private void resetAll() {
        for (ConfigElement<?> element : elements) {
            element.reset();
        }
        updateSaveButton();
    }
}