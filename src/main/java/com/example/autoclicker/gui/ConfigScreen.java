package com.example.autoclicker.gui;

import com.example.autoclicker.config.Config;
import com.example.autoclicker.config.ConfigManager;
import com.example.autoclicker.gui.elements.ConfigElement;
import com.example.autoclicker.gui.elements.IntSliderElement;
import com.example.autoclicker.toor.ScrollableList;import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public abstract class ConfigScreen extends Screen {
    protected final Screen parent;
    protected final Config config;
    private final List<ConfigElement<?>> elements = new ArrayList<>();
    protected ScrollableList scrollList;

    private Button doneButton;

    public ConfigScreen(Object parent) {
        super(Component.translatable("screen.autoclicker.title"));
        this.parent = (Screen) parent;  // 强制转换
        this.config = ConfigManager.getConfig();
    }

    protected abstract boolean handleMouseScroll(double mouseX, double mouseY, double... scrollAmounts);

    @Override
    protected void init() {
        super.init();

        int margin = 20;
        int elementWidth = Math.min(240, width - 2 * margin - 20);

        // === 主标题 ===
        Component mainTitle = Component.literal("Auto Clicker");
        int titleWidth = font.width(mainTitle.getString());
        int titleX = (width - titleWidth) / 2;
        int titleY = 10;
        addRenderableOnly(new StringWidget(titleX, titleY, titleWidth, 20, mainTitle, font));

        // === 滚动列表 ===
        int listTopMargin = 30;
        int buttonHeight = 40;
        int listX = 0;
        int listWidth = width;
        int listHeight = height - listTopMargin - buttonHeight;
        this.scrollList = new ScrollableList(
                listX,
                listTopMargin,
                listWidth,
                listHeight,
                Component.translatable("screen.autoclicker.options")
        );
        addRenderableWidget(this.scrollList);

        scrollList.clearChildren();
        elements.clear();

        int yOffset = 0;

        // === 自动攻击设置 ===
        addSectionTitle(scrollList, elementWidth, yOffset, Component.translatable("config.section.attack"));
        yOffset += 25;

        // ✅ 使用工厂方法创建 BooleanElement
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.auto_attack_enabled"),
                config.autoAttackEnabled,
                value -> {
                    config.autoAttackEnabled = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 25;

        elements.add(new IntSliderElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.attack_interval"),
                1, 100, config.attackInterval,
                value -> {
                    config.attackInterval = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 30;

        // ✅ 使用工厂方法
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.attack_randomness_enabled"),
                config.attackRandomnessEnabled,
                value -> {
                    config.attackRandomnessEnabled = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 25;

        elements.add(new IntSliderElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.attack_randomness_range"),
                0, 20, config.attackRandomness,
                value -> {
                    config.attackRandomness = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 30;

        // === 攻击目标 ===
        addSectionTitle(scrollList, elementWidth, yOffset, Component.translatable("config.attack_targets"));
        yOffset += 25;

        // ✅ 使用工厂方法
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.attack_armor_stands"),
                config.attackArmorStands,
                value -> {
                    config.attackArmorStands = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 25;

        // ✅ 使用工厂方法
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.attack_hostile_mobs"),
                config.attackHostileMobs,
                value -> {
                    config.attackHostileMobs = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 25;

        // ✅ 使用工厂方法
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.attack_neutral_mobs"),
                config.attackNeutralMobs,
                value -> {
                    config.attackNeutralMobs = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 30;

        // === 自动放置设置 ===
        addSectionTitle(scrollList, elementWidth, yOffset, Component.translatable("config.section.place"));
        yOffset += 25;

        // ✅ 使用工厂方法
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.auto_place_enabled"),
                config.autoPlaceEnabled,
                value -> {
                    config.autoPlaceEnabled = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 25;

        elements.add(new IntSliderElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.place_interval"),
                1, 40, config.placeInterval,
                value -> {
                    config.placeInterval = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 30;

        // ✅ 使用工厂方法
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.use_bone_meal"),
                config.useBoneMeal,
                value -> {
                    config.useBoneMeal = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 30;

        // 随机化启用
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.place_randomness_enabled"),
                config.placeRandomnessEnabled,
                value -> {
                    config.placeRandomnessEnabled = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 25;

        // 随机化范围
        elements.add(new IntSliderElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.place_randomness_range"),
                0, 10, config.placeRandomness,
                value -> {
                    config.placeRandomness = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 30;

        // === 自动补充设置 ===
        addSectionTitle(scrollList, elementWidth, yOffset, Component.translatable("config.section.auto_refill"));
        yOffset += 25;

        // 主手补充开关
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.auto_refill_main_hand"),
                config.autoRefillMainHand,
                value -> {
                    config.autoRefillMainHand = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 25;

        // 副手补充开关
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.auto_refill_off_hand"),
                config.autoRefillOffHand,
                value -> {
                    config.autoRefillOffHand = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 25;

        // 数量触发阈值（0-63）
        elements.add(new IntSliderElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.refill_count_threshold"),
                0, 63, config.refillCountThreshold,
                value -> {
                    config.refillCountThreshold = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 30;  // 需要这行！

        // 耐久触发阈值（0-100%）
        elements.add(new IntSliderElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.refill_durability_threshold"),
                0, 100, config.refillDurabilityThreshold,
                value -> {
                    config.refillDurabilityThreshold = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 30;  // 也需要这行！

        // === 自动关闭设置 ===
        addSectionTitle(scrollList, elementWidth, yOffset, Component.translatable("config.section.auto_disable"));
        yOffset += 25;

        // 自动攻击超时关闭开关
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.auto_disable_attack"),
                config.autoDisableAttack,
                value -> {
                    config.autoDisableAttack = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 25;

        // 自动放置超时关闭开关
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.auto_disable_place"),
                config.autoDisablePlace,
                value -> {
                    config.autoDisablePlace = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 25;

        // 超时时间滑块（1-240秒）
        elements.add(new IntSliderElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.auto_disable_timeout"),
                1, 240, config.autoDisableTimeout,
                value -> {
                    config.autoDisableTimeout = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 30;

        // === 自动静音设置 ===
        addSectionTitle(scrollList, elementWidth, yOffset, Component.translatable("config.section.auto_mute"));
        yOffset += 25;

        // ✅ 使用工厂方法：最小化时静音
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.mute_when_minimized"),
                config.muteWhenMinimized,
                value -> {
                    config.muteWhenMinimized = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 25;

        // ✅ 使用工厂方法：自动攻击时静音
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.mute_on_auto_attack"),
                config.muteOnAutoAttack,
                value -> {
                    config.muteOnAutoAttack = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 25;

        // ✅ 使用工厂方法：自动放置时静音
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.mute_on_auto_place"),
                config.muteOnAutoPlace,
                value -> {
                    config.muteOnAutoPlace = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());
        yOffset += 30;

        // === 反检测设置 ===
        addSectionTitle(scrollList, elementWidth, yOffset, Component.translatable("config.section.anti_detection"));
        yOffset += 25;

        // ✅ 使用工厂方法
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                Component.translatable("config.humanize_clicks"),
                config.humanizeClicks,
                value -> {
                    config.humanizeClicks = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.getLast().getWidget());

        // === 底部按钮 ===
        int buttonY = height - 30;
        int buttonWidth = 150;
        int spacing = 10;

        Button cancelButton = Button.builder(
                        Component.translatable("gui.cancel"),
                        btn -> onClose()  // 改用 btn 代替 _
                )
                .pos(width / 2 - buttonWidth - spacing / 2, buttonY)
                .size(buttonWidth, 20)
                .build();
        addRenderableWidget(cancelButton);

        doneButton = Button.builder(
                        Component.translatable("gui.done"),
                        btn -> saveAndClose()  // 改用 btn 代替 _
                )
                .pos(width / 2 + spacing / 2, buttonY)
                .size(buttonWidth, 20)
                .build();
        addRenderableWidget(doneButton);

        updateDoneButton();
    }

    // 辅助方法：添加居中标题
    private void addSectionTitle(ScrollableList list, int width, int y, Component text) {
        int textW = font.width(text.getString());
        int x = Math.max(0, (width - textW) / 2);
        list.addChild(new StringWidget(x, y, width, 20, text, font));
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private void updateDoneButton() {
        doneButton.active = elements.stream().anyMatch(ConfigElement::isChanged);
    }

    private void saveAndClose() {
        for (ConfigElement<?> element : elements) {
            if (element.isChanged()) {
                element.save();
            }
        }
        ConfigManager.save();
        onClose();
    }
}