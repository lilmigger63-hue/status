package dev.medievalstatus;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.config.ModConfig;

@Mod(MedievalStatus.ID)
public final class MedievalStatus {
    public static final String ID = "medievalstatus";
    public MedievalStatus(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, Config.SERVER);
        container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT);
        modBus.addListener(Network::init);
        modBus.addListener(StatusGameTests::registerFunctions);
        modBus.addListener(StatusGameTests::register);
        modBus.addListener(ActivityBonusGameTests::registerFunctions);
        modBus.addListener(ActivityBonusGameTests::register);
        modBus.addListener(PlatformGameTests::registerFunctions);
        modBus.addListener(PlatformGameTests::register);
    }
}
