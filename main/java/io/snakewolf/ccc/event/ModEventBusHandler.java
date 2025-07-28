package io.snakewolf.ccc.event;

import io.snakewolf.ccc.CentralControlCenter;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = CentralControlCenter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusHandler {

    // CentralControlCenterで直接処理されるため、ここには特別なリスナーは不要
    // 必要に応じて、Modのアイテムやブロックなどの登録イベントをここに追加できます。

    // 例:
    // @SubscribeEvent
    // public static void registerItems(RegistryEvent.Register<Item> event) {
    //     // アイテム登録ロジック
    // }
}