package io.snakewolf.ccc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.snakewolf.ccc.client.gui.CCCConnectScreen;
import io.snakewolf.ccc.config.CCCConfig;
import io.snakewolf.ccc.event.ModEventBusHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.enums.ReadyState;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraftforge.event.TickEvent;

import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;


@Mod(CentralControlCenter.MOD_ID)
public class CentralControlCenter {
    public static final String MOD_ID = "centralcontrolcenter";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static ClientWebSocketHandlerImpl wsHandler;

    public CentralControlCenter() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CCCConfig.CLIENT_SPEC);

        modEventBus.register(new ModEventBusHandler());
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onClientSetup);

        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("Central Control Center Mod Initialized.");
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("Common setup for CCC Mod completed.");
        });
    }

    public void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("Client setup for CCC Mod completed.");

            if (CCCConfig.CLIENT.clientUUID.get().isEmpty() || CCCConfig.CLIENT.clientUUID.get().equals("generated")) {
                CCCConfig.CLIENT.clientUUID.set(UUID.randomUUID().toString());
                CCCConfig.CLIENT_SPEC.save();
                LOGGER.info("Generated new client UUID for WebSocket: " + CCCConfig.CLIENT.clientUUID.get());
            }

            wsHandler = null;

            String manualAddress = CCCConfig.CLIENT.manualWebSocketAddress.get();
            if (CCCConfig.CLIENT.useManualWebSocketAddress.get() && !manualAddress.isEmpty()) {
                try {
                    URI uri = new URI(manualAddress);
                    if (wsHandler != null) {
                        wsHandler.stopClient();
                    }
                    wsHandler = new ClientWebSocketHandlerImpl(uri);
                    wsHandler.connect();
                } catch (URISyntaxException e) {
                    LOGGER.error("Invalid WebSocket URI from config: " + manualAddress, e);
                }
            } else {
                LOGGER.warn("WebSocket manual address not set or not enabled. Single player will be disabled until manually connected.");
            }
        });
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {

        private static boolean wasGameMenuOpenedByDisconnection = false;

        @SubscribeEvent
        public static void onClientLoggedOut(LoggingOut event) {
            if (wsHandler != null) {
                wsHandler.stopClient();
                CentralControlCenter.LOGGER.info("Client logged out, stopping client WebSocket handler.");
            }
            wasGameMenuOpenedByDisconnection = false;
        }

        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init.Post event) {
            Screen screen = event.getScreen();

            if (screen instanceof TitleScreen) {
                screen.children().forEach(widget -> {
                    if (widget instanceof Button button) {
                        Component message = button.getMessage();
                        if (message.getString().equals(Component.translatable("menu.singleplayer").getString())) {
                            boolean isConnected = (wsHandler != null && wsHandler.isConnected());
                            button.active = isConnected;
                            if (!isConnected) {
                                button.setMessage(Component.translatable("menu.singleplayer").copy());
                            } else {
                                button.setMessage(Component.translatable("menu.singleplayer"));
                            }
                        } else if (message.getString().equals(Component.translatable("menu.multiplayer").getString())) {
                            button.active = false;
                            button.setMessage(Component.translatable("menu.multiplayer").copy());
                        } else if (message.getString().equals(Component.translatable("menu.realm").getString())) {
                            button.active = false;
                            button.setMessage(Component.translatable("menu.realm").copy());
                        }
                    }
                });
            } else if (screen instanceof OptionsScreen optionsScreen) {
                Button oldTelemetryOptionsButton = null;

                for (Renderable widget : optionsScreen.renderables) {
                    if (widget instanceof Button button) {
                        if (button.getMessage().getString().equals(Component.translatable("options.telemetry").getString())) {
                            oldTelemetryOptionsButton = button;
                            break;
                        } else if (button.getMessage().getString().equals(Component.translatable("options.online").getString())) {
                            button.active = false;
                            button.setMessage(Component.translatable("options.telemetry").copy());
                        }
                    }
                }

                if (oldTelemetryOptionsButton != null) {
                    oldTelemetryOptionsButton.active = false;
                    oldTelemetryOptionsButton.setMessage(Component.empty());
                    CentralControlCenter.LOGGER.info("Disabled old 'Telemetry Options' button and cleared its text.");

                    Button newCCCButton = Button.builder(Component.translatable("ccc.button.options_screen_link"), (btn) -> {
                                Minecraft.getInstance().setScreen(new CCCConnectScreen(optionsScreen));
                            })
                            .bounds(oldTelemetryOptionsButton.getX(), oldTelemetryOptionsButton.getY(), oldTelemetryOptionsButton.getWidth(), oldTelemetryOptionsButton.getHeight())
                            .build();

                    event.addListener(newCCCButton);
                    CentralControlCenter.LOGGER.info("Added new 'CCC Settings' button over the disabled one.");
                }
            } else if (screen instanceof PauseScreen pauseScreen) {
                if (Minecraft.getInstance().level != null && (wsHandler == null || !wsHandler.isConnected())) {
                    boolean returnToGameButtonDisabled = false;
                    for (Renderable widget : pauseScreen.renderables) {
                        if (widget instanceof Button button) {
                            if (button.getMessage().getString().equals(Component.translatable("menu.returnToGame").getString())) {
                                button.active = false;
                                CentralControlCenter.LOGGER.info("Disabled 'Return to Game' button due to WebSocket disconnection.");
                                returnToGameButtonDisabled = true;
                                break;
                            }
                        }
                    }

                    if (returnToGameButtonDisabled) {
                        event.addListener(new AbstractWidget(0, 0, 0, 0, Component.empty()) {
                            @Override
                            public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
                                if (pKeyCode == GLFW.GLFW_KEY_ESCAPE) {
                                    CentralControlCenter.LOGGER.debug("ESC key pressed while 'Return to Game' is disabled. Consuming event.");
                                    return true;
                                }
                                return super.keyPressed(pKeyCode, pScanCode, pModifiers);
                            }

                            @Override
                            public void renderWidget(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
                                // This widget is meant to be invisible and not render anything.
                            }

                            @Override
                            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

                            }
                        });
                        CentralControlCenter.LOGGER.info("Added ESC key listener to block 'Return to Game' on PauseScreen.");
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                Minecraft mc = Minecraft.getInstance();

                if (wsHandler != null && wsHandler.isOperationProhibited()) {
                    if (mc.screen == null || !(mc.screen instanceof io.snakewolf.ccc.client.gui.ProhibitionScreen)) {
                        mc.setScreen(wsHandler.getProhibitionScreen());
                    }
                }

                if (mc.level != null && (wsHandler == null || !wsHandler.isConnected())) {
                    if (!(mc.screen instanceof PauseScreen) && !(mc.screen instanceof TitleScreen) && !(mc.screen instanceof io.snakewolf.ccc.client.gui.ProhibitionScreen) && !wasGameMenuOpenedByDisconnection) {
                        CentralControlCenter.LOGGER.warn("WebSocket disconnected while in-game. Opening game menu.");
                        mc.setScreen(new PauseScreen(true));
                        wasGameMenuOpenedByDisconnection = true;
                    }
                } else if (wsHandler != null && wsHandler.isConnected() && wasGameMenuOpenedByDisconnection) {
                    wasGameMenuOpenedByDisconnection = false;
                }
            }
        }
    }

    public static class ClientWebSocketHandlerImpl extends WebSocketClient {

        private static final Gson GSON = new Gson();
        private boolean isProhibited = false;
        private io.snakewolf.ccc.client.gui.ProhibitionScreen prohibitionScreen;
        private ScheduledExecutorService reconnectionScheduler;

        public interface ConnectionStatusCallback {
            void onConnectionStatusChanged(boolean connected);
        }

        private static ConnectionStatusCallback statusCallback;

        public ClientWebSocketHandlerImpl(URI serverUri) {
            super(serverUri);
        }

        public static void setStatusCallback(ConnectionStatusCallback callback) {
            statusCallback = callback;
        }

        public void startClient(URI uri) {
            CentralControlCenter.LOGGER.info("Client WebSocket Handler: 手動設定されたアドレスで接続を試みます。URI: " + uri);
            try {
                if (this.isOpen()) {
                    CentralControlCenter.LOGGER.warn("Client WebSocket Handler: 既に接続済みです。既存の接続を閉じます。");
                    this.closeBlocking();
                }

                this.reconnect();
                CentralControlCenter.LOGGER.info("Client WebSocket Handler: サーバー " + uri + " への接続を再試行中...");

            } catch (Exception e) {
                CentralControlCenter.LOGGER.error("Client WebSocket Handler: サーバー " + uri + " への接続中に例外が発生しました: " + e.getMessage(), e);
                notifyConnectionStatus(false);
            }
        }

        private static void notifyConnectionStatus(boolean connected) {
            if (statusCallback != null) {
                Minecraft.getInstance().execute(() -> statusCallback.onConnectionStatusChanged(connected));
            }
        }

        public void stopClient() {
            if (reconnectionScheduler != null && !reconnectionScheduler.isShutdown()) {
                reconnectionScheduler.shutdownNow();
                reconnectionScheduler = null;
            }
            this.close();
            notifyConnectionStatus(false);
            CentralControlCenter.LOGGER.info("Client WebSocket Handler: WebSocketクライアントを停止しました。");
        }

        public boolean isConnected() {
            return this.isOpen();
        }

        public boolean isOperationProhibited() {
            return isProhibited;
        }

        public io.snakewolf.ccc.client.gui.ProhibitionScreen getProhibitionScreen() {
            if (prohibitionScreen == null) {
                prohibitionScreen = new io.snakewolf.ccc.client.gui.ProhibitionScreen();
            }
            return prohibitionScreen;
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            CentralControlCenter.LOGGER.info("Client WebSocket Handler: サーバーに接続しました。");
            sendClientModInfo();
            notifyConnectionStatus(true);
            if (reconnectionScheduler != null && !reconnectionScheduler.isShutdown()) {
                reconnectionScheduler.shutdownNow();
                reconnectionScheduler = null;
                CentralControlCenter.LOGGER.info("Client WebSocket Handler: 接続成功により定期再接続を停止しました。");
            }
        }

        @Override
        public void onMessage(String message) {
            CentralControlCenter.LOGGER.info("Client WebSocket Handler: メッセージを受信: " + message);
            Minecraft.getInstance().execute(() -> handleCommand(message));
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            CentralControlCenter.LOGGER.info("Client WebSocket Handler: バイナリメッセージを受信しました。");
        }


        @Override
        public void onClose(int code, String reason, boolean remote) {
            CentralControlCenter.LOGGER.info("Client WebSocket Handler: 接続が閉じられました: " + reason + " (コード: " + code + ")");
            notifyConnectionStatus(false);

            if (reconnectionScheduler != null && !reconnectionScheduler.isShutdown()) {
                reconnectionScheduler.shutdownNow();
                reconnectionScheduler = null;
            }

            if (code != 1000) {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) {
                    CentralControlCenter.LOGGER.info("Client WebSocket Handler: 異常切断を検知。1秒後に定期的に再接続を試みます。");
                    reconnectionScheduler = Executors.newSingleThreadScheduledExecutor();
                    reconnectionScheduler.scheduleAtFixedRate(() -> {
                        if (!this.isOpen() && !this.isClosing()) {
                            CentralControlCenter.LOGGER.info("Client WebSocket Handler: 定期再接続を試行中...");
                            this.reconnect();
                        } else {
                            CentralControlCenter.LOGGER.info("Client WebSocket Handler: 接続が確立されたか、既に接続/クローズ中のため、定期再接続を停止します。");
                            if (reconnectionScheduler != null) {
                                reconnectionScheduler.shutdownNow();
                                reconnectionScheduler = null;
                            }
                        }
                    }, 1, 1, TimeUnit.SECONDS);

                    if (mc.hasSingleplayerServer()) {
                        IntegratedServer server = mc.getSingleplayerServer();

                        CentralControlCenter.LOGGER.info("Client WebSocket Handler: 接続できなかったため、セーブして抜けます");

                        if (server != null && server.isRunning()) {
                            // セーブ処理を行う
                            server.halt(true); // true = セーブして終了
                        }

                        // ワールドを閉じてタイトル画面に戻る
                        Objects.requireNonNull(mc.level).disconnect();
                        mc.setScreen(null); // もしくは `new TitleScreen()` で明示的に
                    }
                } else {
                    CentralControlCenter.LOGGER.warn("Client WebSocket Handler: Minecraftインスタンスが存在しないため、自動再接続は行いません。");
                }
            }
        }

        @Override
        public void onError(Exception ex) {
            CentralControlCenter.LOGGER.error("Client WebSocket Handler: エラーが発生しました: " + ex.getMessage(), ex);
            notifyConnectionStatus(false);
            if (reconnectionScheduler != null && !reconnectionScheduler.isShutdown()) {
                reconnectionScheduler.shutdownNow();
                reconnectionScheduler = null;
            }
        }

        private void sendClientModInfo() {
            String clientModUuidStr = CCCConfig.CLIENT.clientUUID.get();
            if (clientModUuidStr.isEmpty() || clientModUuidStr.equals("generated")) {
                clientModUuidStr = UUID.randomUUID().toString();
                CCCConfig.CLIENT.clientUUID.set(clientModUuidStr);
                CCCConfig.CLIENT_SPEC.save();
                CentralControlCenter.LOGGER.warn("Client WebSocket Handler: clientUUID が未設定または初期値のためランダムに生成し、設定に保存しました。");
            }
            UUID clientModUuid = UUID.fromString(clientModUuidStr);
            String clientModUsername = CCCConfig.CLIENT.clientUsername.get();

            JsonObject connectMsg = new JsonObject();
            connectMsg.addProperty("type", "clientConnect");
            JsonObject data = new JsonObject();
            data.addProperty("playerUuid", clientModUuid.toString());
            data.addProperty("username", clientModUsername);
            connectMsg.add("data", data);
            this.send(GSON.toJson(connectMsg));
            CentralControlCenter.LOGGER.info("Client WebSocket Handler: 自身のMod UUID (" + clientModUuid + ") とユーザー名 (" + clientModUsername + ") をサーバーに送信しました。");
        }

        private void handleCommand(String command) {
            Minecraft mc = Minecraft.getInstance();
            if (command.startsWith("ACTION:PROHIBIT_OPS")) {
                isProhibited = true;
                if (mc.player != null) {
                    mc.setScreen(getProhibitionScreen());
                    mc.player.sendSystemMessage(Component.literal("[CCC Mod] 操作が禁止されました。"));
                }
            } else if (command.startsWith("ACTION:RELEASE_OPS")) {
                isProhibited = false;
                if (mc.screen instanceof io.snakewolf.ccc.client.gui.ProhibitionScreen) {
                    mc.setScreen(null);
                }
                if (mc.player != null) {
                    mc.player.sendSystemMessage(Component.literal("[CCC Mod] 操作禁止が解除されました。"));
                }
            } else if (command.startsWith("ACTION:FINISH_MINECRAFT")) {
                CentralControlCenter.LOGGER.info("ACTION:FINISH_MINECRAFT command received. Shutting down Minecraft.");
                mc.execute(mc::stop);
            } else if (command.startsWith("ACTION:SAVE_AND_FINISH_WORLD")) {
                if (mc.hasSingleplayerServer()) {
                    IntegratedServer server = mc.getSingleplayerServer();

                    CentralControlCenter.LOGGER.info("ACTION:SAVE_AND_FINISH_WORLD command received. Saving and returning to title screen.");

                    if (server != null && server.isRunning()) {
                        // セーブ処理を行う
                        server.halt(true); // true = セーブして終了
                    }

                    // ワールドを閉じてタイトル画面に戻る
                    Objects.requireNonNull(mc.level).disconnect();
                    mc.setScreen(null); // もしくは `new TitleScreen()` で明示的に
                } else {
                    CentralControlCenter.LOGGER.warn("ACTION:SAVE_AND_FINISH_WORLD received, but not in a world. Command ignored.");
                }
            } else if (command.startsWith("CUSTOM_COMMAND:")) {
                String customCommand = command.substring("CUSTOM_COMMAND:".length());
                if (mc.player != null) {
                    mc.player.connection.sendCommand(customCommand);
                    mc.player.sendSystemMessage(Component.literal("[CCC Mod] コマンドを送信しました: " + customCommand));
                }
            }
        }
    }
}