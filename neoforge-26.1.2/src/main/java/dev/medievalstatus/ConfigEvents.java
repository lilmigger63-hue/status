package dev.medievalstatus;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid=MedievalStatus.ID)
public final class ConfigEvents {
    @SubscribeEvent public static void reloaded(ModConfigEvent.Reloading event) {
        if (!event.getConfig().getModId().equals(MedievalStatus.ID) || event.getConfig().getType()!=ModConfig.Type.SERVER) return;
        var server=ServerLifecycleHooks.getCurrentServer();
        if(server!=null) server.execute(()->server.getPlayerList().getPlayers().forEach(p->{
            PlayerData d=PlayerData.get(p); d.dirty=true; d.bonusesDirty=true;
        }));
    }
}
