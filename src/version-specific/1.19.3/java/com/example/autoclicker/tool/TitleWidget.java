package com.example.autoclicker.tool;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * 标题文本组件：1.19.4 才有 StringWidget，1.19.3 用本类替代（渲染效果与 StringWidget 左对齐一致）。
 * 1.19.3 起 x/y 为私有字段，使用 getX()/getY() 访问。
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
        int textY = this.getY() + (this.height - 8) / 2;
        this.font.drawShadow(poseStack, this.message, this.getX(), textY, 0xFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
