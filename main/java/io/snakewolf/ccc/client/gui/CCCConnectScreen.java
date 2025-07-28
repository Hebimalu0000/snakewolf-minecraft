package io.snakewolf.ccc.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import io.snakewolf.ccc.CentralControlCenter;
import io.snakewolf.ccc.config.CCCConfig;
import net.minecraft.client.Minecraft; // Minecraft をインポート
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.URISyntaxException;

public class CCCConnectScreen extends Screen implements CentralControlCenter.ClientWebSocketHandlerImpl.ConnectionStatusCallback {

    private final Screen parentScreen; // 親スクリーンを保持するためのフィールド
    private EditBox addressField;
    private Button connectButton;
    private Component statusMessage = Component.empty();

    // 親スクリーンを受け取るコンストラクタを追加
    public CCCConnectScreen(Screen parent) {
        super(Component.translatable("ccc.connect_screen.title"));
        this.parentScreen = parent; // 親スクリーンを保存
    }

    @Override
    protected void init() {
        super.init();

        // コールバックを登録
        // CentralControlCenter.wsHandler が null でないことを確認
        if (CentralControlCenter.wsHandler != null) {
            CentralControlCenter.ClientWebSocketHandlerImpl.setStatusCallback(this);
        } else {
            // wsHandler がまだ初期化されていない場合（Modのロード順序などによる）
            // ここでエラーメッセージを表示するか、ユーザーに再起動を促す
            statusMessage = Component.translatable("ccc.connect_screen.status.mod_not_initialized");
            CentralControlCenter.LOGGER.error("CCCConnectScreen: wsHandler is null during init. Mod initialization order issue?");
        }


        int buttonWidth = 150;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int startY = this.height / 2 - 40;

        this.addressField = new EditBox(this.font, centerX - 100, startY, 200, 20, Component.translatable("ccc.connect_screen.address_field"));
        this.addressField.setMaxLength(256);
        this.addressField.setValue(CCCConfig.CLIENT.manualWebSocketAddress.get());
        this.addRenderableWidget(this.addressField);

        this.connectButton = Button.builder(Component.translatable("ccc.connect_screen.connect_button"), (button) -> {
            String address = this.addressField.getValue();
            if (address.isEmpty()) {
                statusMessage = Component.translatable("ccc.connect_screen.status.error_empty_address");
                return;
            }
            try {
                URI uri = new URI(address);
                CCCConfig.CLIENT.manualWebSocketAddress.set(address);
                CCCConfig.CLIENT.useManualWebSocketAddress.set(true);
                CCCConfig.CLIENT_SPEC.save();

                // 修正: 非staticメソッドをインスタンス経由で呼び出す
                if (CentralControlCenter.wsHandler != null) {
                    CentralControlCenter.wsHandler.startClient(uri); // wsHandler インスタンスの startClient を呼び出す
                    statusMessage = Component.translatable("ccc.connect_screen.status.connecting");
                } else {
                    statusMessage = Component.translatable("ccc.connect_screen.status.mod_not_initialized");
                    CentralControlCenter.LOGGER.error("CCCConnectScreen: Cannot start client, wsHandler is null.");
                }

            } catch (URISyntaxException e) {
                statusMessage = Component.translatable("ccc.connect_screen.status.error_invalid_uri", e.getMessage());
                CentralControlCenter.LOGGER.error("Invalid WebSocket URI: " + address, e);
            }
            this.connectButton.active = false; // 接続試行中はボタンを無効化
        }).bounds(centerX - 75, startY + 30, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(this.connectButton);

        // 「完了」ボタンが押されたら親スクリーンに戻るように変更
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen); // 親スクリーンに戻る
        }).bounds(centerX - 75, startY + 60, buttonWidth, buttonHeight).build());

        // 初期ステータスを更新
        // 修正: 非staticメソッドをインスタンス経由で呼び出す
        updateButtonAndStatus(CentralControlCenter.wsHandler != null && CentralControlCenter.wsHandler.isConnected());
    }

    @Override
    public void tick() {
        super.tick();
        // 接続状態はコールバックで更新されるため、tickでは主にUIの状態のみを更新
        // 修正: 非staticメソッドをインスタンス経由で呼び出す
        boolean isConnected = CentralControlCenter.wsHandler != null && CentralControlCenter.wsHandler.isConnected();
        this.connectButton.active = !isConnected; // 接続中は接続ボタンを無効化
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(poseStack);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        drawCenteredString(poseStack, this.font, statusMessage, this.width / 2, this.height / 2 - 80, 0xFFFFFF);
        super.render(poseStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onConnectionStatusChanged(boolean connected) {
        updateButtonAndStatus(connected);
    }

    private void updateButtonAndStatus(boolean connected) {
        if (connected) {
            statusMessage = Component.translatable("ccc.connect_screen.status.connected");
            this.connectButton.active = false; // 接続済みならボタンは無効
        } else {
            // 接続中でない場合、ステータスはアドレスフィールドの内容に基づいて変わる
            if (!this.addressField.getValue().isEmpty()) {
                statusMessage = Component.translatable("ccc.connect_screen.status.disconnected");
            } else {
                statusMessage = Component.translatable("ccc.connect_screen.status.enter_address");
            }
            this.connectButton.active = true; // 切断済みならボタンは有効
        }
    }

    @Override
    public void removed() {
        // 画面が閉じられるときにコールバックを解除
        // 修正: 非staticメソッドをインスタンス経由で呼び出す
        if (CentralControlCenter.wsHandler != null) {
            CentralControlCenter.ClientWebSocketHandlerImpl.setStatusCallback(null);
        }
        super.removed();
    }
}
