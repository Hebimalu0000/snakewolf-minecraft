package io.snakewolf.ccc.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class CCCConfig {

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    // サーバー側の設定は削除 (シングルプレイ特化のため)
    // public static final ForgeConfigSpec SERVER_SPEC;
    // public static final Server SERVER;

    static {
        // ★修正: configure の戻り値 (Pair) を正しく処理する
        Pair<Client, ForgeConfigSpec> clientPair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = clientPair.getRight();
        CLIENT = clientPair.getLeft();
    }

    // ★変更: クライアント側の設定のみを保持
    public static class Client {
        public final ForgeConfigSpec.BooleanValue useManualWebSocketAddress;
        public final ForgeConfigSpec.ConfigValue<String> manualWebSocketAddress;
        public final ForgeConfigSpec.IntValue broadcastPort;
        public final ForgeConfigSpec.IntValue searchTimeoutSeconds;
        public final ForgeConfigSpec.ConfigValue<String> websocketServerHost;
        public final ForgeConfigSpec.IntValue CLIENT_WEBSOCKET_PORT; // クライアントがフォールバックで使用するポート

        // ★変更: WebSocket接続用のMod固有のUUIDとユーザー名を保持 (MinecraftのプレイヤーUUID/名前とは別)
        public final ForgeConfigSpec.ConfigValue<String> clientUUID;
        public final ForgeConfigSpec.ConfigValue<String> clientUsername;

        public Client(ForgeConfigSpec.Builder builder) {
            builder.push("client_websocket");

            useManualWebSocketAddress = builder
                    .comment("Whether to use a manual WebSocket server address instead of automatic LAN discovery.")
                    .define("useManualWebSocketAddress", false);

            manualWebSocketAddress = builder
                    .comment("The manual WebSocket server address (e.g., ws://localhost:8080).")
                    .define("manualWebSocketAddress", "");

            broadcastPort = builder
                    .comment("The UDP port used for LAN server discovery broadcasts.")
                    .defineInRange("broadcastPort", 8089, 1024, 65535);

            searchTimeoutSeconds = builder
                    .comment("Timeout for LAN server discovery in seconds (0 for no timeout).")
                    .defineInRange("searchTimeoutSeconds", 30, 0, 3600);

            websocketServerHost = builder
                    .comment("Fallback WebSocket server host if auto-discovery fails and manual address is not used.")
                    .define("websocketServerHost", "localhost");

            CLIENT_WEBSOCKET_PORT = builder
                    .comment("Fallback WebSocket server port if auto-discovery fails and manual address is not used.")
                    .defineInRange("websocketServerPort", 8080, 1024, 65535);

            builder.pop(); // client_websocket

            builder.push("client_mod_identity");
            clientUUID = builder
                    .comment("A unique ID for this mod instance. Generated automatically if 'generated' or empty.")
                    .define("clientUUID", "generated");

            clientUsername = builder
                    .comment("A username for this mod instance, sent to the WebSocket server.")
                    .define("clientUsername", "CCC_Client");
            builder.pop(); // client_mod_identity
        }
    }

    // ★削除: サーバー側の設定クラス (シングルプレイ特化のため)
    // public static class Server {
    //     public final ForgeConfigSpec.IntValue websocketPort;
    //     public final ForgeConfigSpec.ConfigValue<String> broadcastMessage;
    //
    //     public Server(ForgeConfigSpec.Builder builder) {
    //         builder.push("server_websocket");
    //         websocketPort = builder
    //                 .comment("The port for the WebSocket server to listen on.")
    //                 .defineInRange("websocketPort", 8080, 1024, 65535);
    //         broadcastMessage = builder
    //                 .comment("The message sent via UDP broadcast for clients to discover the server.")
    //                 .define("broadcastMessage", "CCC_WS_SERVER:%s:%d");
    //         builder.pop();
    //     }
    // }
}