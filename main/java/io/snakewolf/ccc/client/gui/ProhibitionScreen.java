// src/main/java/io/snakewolf/ccc/client/gui/ProhibitionScreen.java
package io.snakewolf.ccc.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;

public class ProhibitionScreen extends Screen {

    public ProhibitionScreen() {
        super(Component.literal("Operation Prohibited"));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(poseStack);

        Component message = Component.literal("操作禁止中");
        Component subMessage = Component.literal("Mod管理ツールによって操作が制限されています。");

        this.drawCenteredString(poseStack, this.font, message, this.width / 2, this.height / 2 - 20, 0xFFFFFFFF);
        this.drawCenteredString(poseStack, this.font, subMessage, this.width / 2, this.height / 2 + 10, 0xFFAAAAAA);

        super.render(poseStack, mouseX, mouseY, partialTicks);
    }

    /**
     * エスケープキーを押しても画面が閉じないようにする
     */
    // ★修正: @Override アノテーションを削除しました。
    public boolean isPauseScreen() {
        return true;
    }

    /**
     * キー入力イベントをすべて無効化 (Trueを返してイベントを消費)
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return true;
    }

    /**
     * マウスボタンクリックイベントをすべて無効化
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return true;
    }

    /**
     * マウスドラッグイベントをすべて無効化
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return true;
    }

    /**
     * キャラクター入力イベントをすべて無効化
     */
    @Override
    public boolean charTyped(char chr, int modifiers) {
        return true;
    }
}