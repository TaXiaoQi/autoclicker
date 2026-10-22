package com.example.autoclicker.tool;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * 标题文本组件：1.19.4 才有 StringWidget，旧版本用本类替代（渲染效果与 StringWidget 左对齐一致）。
 * 1.16.5 使用 render + 公共 x/y 字段，无 narration 体系。
 */
public class TitleWidget extends AbstractWidget {
    private final Component message;
    private final Font font;

    public TitleWidget(int x, int y, int width, int height, Component message, Font font) {
        super(x, y, width, height, message);
        this.message = message;
        this.font = font;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        // 垂直居中，白色带阴影，与 1.20 StringWidget 默认渲染一致
        int textY = this.y + (this.height - 8) / 2;
        this.font.drawShadow(poseStack, this.message, this.x, textY, 0xFFFFFF);
    }
}
