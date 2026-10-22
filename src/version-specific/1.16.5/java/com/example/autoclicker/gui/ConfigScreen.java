package com.example.autoclicker.gui;

import com.example.autoclicker.config.Config;
import com.example.autoclicker.config.ConfigManager;
import com.example.autoclicker.gui.elements.ConfigElement;
import com.example.autoclicker.gui.elements.IntSliderElement;
import com.example.autoclicker.tool.ScreenHelper;
import com.example.autoclicker.tool.ScrollableList;
import com.example.autoclicker.tool.TitleWidget;import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import java.util.ArrayList;
import java.util.List;

public abstract class ConfigScreen extends Screen {
    protected final Screen parent;
    protected final Config config;
    private final List<ConfigElement<?>> elements = new ArrayList<>();
    protected ScrollableList scrollList;

    private Button doneButton;

    // 打开界面时的配置快照：「取消/ESC」时回滚用
    private Config snapshot;
    private boolean saved;

    public ConfigScreen(Object parent) {
        super(new TranslatableComponent("screen.autoclicker.title"));
        this.parent = (Screen) parent;  // 强制转换
        this.config = ConfigManager.getConfig();
    }

    protected abstract boolean handleMouseScroll(double mouseX, double mouseY, double... scrollAmounts);

    @Override
    protected void init() {
        super.init();

        // 记录打开界面时的配置快照（resize 会重走 init，只取首次快照）
        if (snapshot == null) {
            snapshot = ConfigManager.snapshot();
        }

        int margin = 20;
        int elementWidth = Math.min(240, width - 2 * margin - 20);

        // === 主标题 ===
        Component mainTitle = new TextComponent("Auto Clicker");
        int titleWidth = font.width(mainTitle.getString());
        int titleX = (width - titleWidth) / 2;
        int titleY = 10;
        addButton(new TitleWidget(titleX, titleY, titleWidth, 20, mainTitle, font));

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
                new TranslatableComponent("screen.autoclicker.options")
        );
        addButton(this.scrollList);

        scrollList.clearChildren();
        elements.clear();

        int yOffset = 0;

        // === 自动攻击设置 ===
        addSectionTitle(scrollList, elementWidth, yOffset, new TranslatableComponent("config.section.attack"));
        yOffset += 25;

        // ✅ 使用工厂方法创建 BooleanElement
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.auto_attack_enabled"),
                config.autoAttackEnabled,
                value -> {
                    config.autoAttackEnabled = value;
                    // 开关变化时同步联动静音
                    com.example.autoclicker.feature.MuteFeature.getInstance().syncFeatureMute();
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 25;

        elements.add(new IntSliderElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.attack_interval"),
                1, 100, config.attackInterval,
                value -> {
                    config.attackInterval = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 30;

        // ✅ 使用工厂方法
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.attack_randomness_enabled"),
                config.attackRandomnessEnabled,
                value -> {
                    config.attackRandomnessEnabled = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 25;

        elements.add(new IntSliderElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.attack_randomness_range"),
                0, 20, config.attackRandomness,
                value -> {
                    config.attackRandomness = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 30;

        // === 攻击目标 ===
        addSectionTitle(scrollList, elementWidth, yOffset, new TranslatableComponent("config.attack_targets"));
        yOffset += 25;

        // ✅ 使用工厂方法
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.attack_armor_stands"),
                config.attackArmorStands,
                value -> {
                    config.attackArmorStands = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 25;

        // ✅ 使用工厂方法
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.attack_hostile_mobs"),
                config.attackHostileMobs,
                value -> {
                    config.attackHostileMobs = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 25;

        // ✅ 使用工厂方法
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.attack_neutral_mobs"),
                config.attackNeutralMobs,
                value -> {
                    config.attackNeutralMobs = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 30;

        // === 自动放置设置 ===
        addSectionTitle(scrollList, elementWidth, yOffset, new TranslatableComponent("config.section.place"));
        yOffset += 25;

        // ✅ 使用工厂方法
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.auto_place_enabled"),
                config.autoPlaceEnabled,
                value -> {
                    config.autoPlaceEnabled = value;
                    // 开关变化时同步联动静音
                    com.example.autoclicker.feature.MuteFeature.getInstance().syncFeatureMute();
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 25;

        elements.add(new IntSliderElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.place_interval"),
                1, 40, config.placeInterval,
                value -> {
                    config.placeInterval = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 30;

        // ✅ 使用工厂方法
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.use_bone_meal"),
                config.useBoneMeal,
                value -> {
                    config.useBoneMeal = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 30;

        // 随机化启用
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.place_randomness_enabled"),
                config.placeRandomnessEnabled,
                value -> {
                    config.placeRandomnessEnabled = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 25;

        // 随机化范围
        elements.add(new IntSliderElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.place_randomness_range"),
                0, 10, config.placeRandomness,
                value -> {
                    config.placeRandomness = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 30;

        // === 自动补充设置 ===
        addSectionTitle(scrollList, elementWidth, yOffset, new TranslatableComponent("config.section.auto_refill"));
        yOffset += 25;

        // 主手补充开关
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.auto_refill_main_hand"),
                config.autoRefillMainHand,
                value -> {
                    config.autoRefillMainHand = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 25;

        // 副手补充开关
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.auto_refill_off_hand"),
                config.autoRefillOffHand,
                value -> {
                    config.autoRefillOffHand = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 25;

        // 数量触发阈值（0-63）
        elements.add(new IntSliderElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.refill_count_threshold"),
                0, 63, config.refillCountThreshold,
                value -> {
                    config.refillCountThreshold = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 30;  // 需要这行！

        // 耐久触发阈值（0-100%）
        elements.add(new IntSliderElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.refill_durability_threshold"),
                0, 100, config.refillDurabilityThreshold,
                value -> {
                    config.refillDurabilityThreshold = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 30;  // 也需要这行！

        // === 自动关闭设置 ===
        addSectionTitle(scrollList, elementWidth, yOffset, new TranslatableComponent("config.section.auto_disable"));
        yOffset += 25;

        // 自动攻击超时关闭开关
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.auto_disable_attack"),
                config.autoDisableAttack,
                value -> {
                    config.autoDisableAttack = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 25;

        // 自动放置超时关闭开关
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.auto_disable_place"),
                config.autoDisablePlace,
                value -> {
                    config.autoDisablePlace = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 25;

        // 超时时间滑块（1-240秒）
        elements.add(new IntSliderElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.auto_disable_timeout"),
                1, 240, config.autoDisableTimeout,
                value -> {
                    config.autoDisableTimeout = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 30;

        // === 自动静音设置 ===
        addSectionTitle(scrollList, elementWidth, yOffset, new TranslatableComponent("config.section.auto_mute"));
        yOffset += 25;

        // ✅ 使用工厂方法：最小化时静音
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.mute_when_minimized"),
                config.muteWhenMinimized,
                value -> {
                    config.muteWhenMinimized = value;
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 25;

        // ✅ 使用工厂方法：自动攻击时静音
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.mute_on_auto_attack"),
                config.muteOnAutoAttack,
                value -> {
                    config.muteOnAutoAttack = value;
                    // 开关变化时同步联动静音
                    com.example.autoclicker.feature.MuteFeature.getInstance().syncFeatureMute();
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 25;

        // ✅ 使用工厂方法：自动放置时静音
        elements.add(GuiFactory.createBooleanElement(0, yOffset, elementWidth, 20,
                new TranslatableComponent("config.mute_on_auto_place"),
                config.muteOnAutoPlace,
                value -> {
                    config.muteOnAutoPlace = value;
                    // 开关变化时同步联动静音
                    com.example.autoclicker.feature.MuteFeature.getInstance().syncFeatureMute();
                    updateDoneButton();
                }
        ));
        scrollList.addChild(elements.get(elements.size() - 1).getWidget());
        yOffset += 30;

        // === 底部按钮 ===
        int buttonY = height - 30;
        int buttonWidth = 150;
        int spacing = 10;

        // 1.19.3 之前无 Button.builder，直接使用构造器
        Button cancelButton = new Button(
                width / 2 - buttonWidth - spacing / 2,
                buttonY,
                buttonWidth,
                20,
                new TranslatableComponent("gui.cancel"),
                btn -> onClose()  // 改用 btn 代替 _
        );
        addButton(cancelButton);

        doneButton = new Button(
                width / 2 + spacing / 2,
                buttonY,
                buttonWidth,
                20,
                new TranslatableComponent("gui.done"),
                btn -> saveAndClose()  // 改用 btn 代替 _
        );
        addButton(doneButton);

        updateDoneButton();
    }

    // 辅助方法：添加居中标题
    private void addSectionTitle(ScrollableList list, int width, int y, Component text) {
        int textW = font.width(text.getString());
        int x = Math.max(0, (width - textW) / 2);
        list.addChild(new TitleWidget(x, y, width, 20, text, font));
    }

    @Override
    public void onClose() {
        if (!saved) {
            // 未保存即关闭（取消/ESC）：回滚到打开界面时的快照，并同步联动静音
            ConfigManager.restore(snapshot);
            com.example.autoclicker.feature.MuteFeature.getInstance().syncFeatureMute();
        }
        ScreenHelper.open(Minecraft.getInstance(), parent);
    }

    private void updateDoneButton() {
        doneButton.active = elements.stream().anyMatch(ConfigElement::isChanged);
    }

    private void saveAndClose() {
        saved = true;
        for (ConfigElement<?> element : elements) {
            if (element.isChanged()) {
                element.save();
            }
        }
        ConfigManager.save();
        onClose();
    }
}