package dev.medievalstatus;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

@Mod(MedievalStatus.ID)
public final class MedievalStatus {
    public static final String ID = "medievalstatus";
    public MedievalStatus() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT);
        Network.init();
    }
}
