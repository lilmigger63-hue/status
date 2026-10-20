package dev.medievalstatus;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid=MedievalStatus.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ConfigEvents {
    @SubscribeEvent public static void reloaded(ModConfigEvent.Reloading event) {
        if (!event.getConfig().getModId().equals(MedievalStatus.ID) || event.getConfig().getType()!=ModConfig.Type.SERVER) return;
        var server=ServerLifecycleHooks.getCurrentServer();
        if(server!=null) server.execute(()->server.getPlayerList().getPlayers().forEach(p->{
            PlayerData d=PlayerData.get(p); d.dirty=true; d.bonusesDirty=true;
        }));
    }
}
