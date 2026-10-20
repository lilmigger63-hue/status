package dev.medievalstatus.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.medievalstatus.*;
import net.minecraft.client.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.CropBlock;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=MedievalStatus.ID,value=Dist.CLIENT)
public final class ClientEvents {
    public static final KeyMapping OPEN = new KeyMapping("key.medievalstatus.open",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_P,"key.categories.medievalstatus");
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getInstance();
        while (OPEN.consumeClick()) if (mc.player!=null) { Network.request(-2); mc.setScreen(new StatusScreen()); }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e) { ClientState.clear(); }
    @SubscribeEvent public static void mining(PlayerEvent.BreakSpeed e) {
        if (!e.getEntity().level().isClientSide || !ClientState.ready) return;
        var b=ClientState.snapshot.getCompound("bonuses");
        double bonus=b.getDouble(e.getState().is(BlockTags.LOGS)?"Woodcutting":"Mining");
        if (e.getState().getBlock() instanceof CropBlock) bonus+=Math.min(.5,skill(Upgrade.EXPERIENCED_FARMER));
        if (e.getEntity().isUnderWater() || !e.getEntity().onGround()) bonus+=Math.min(.6,skill(Upgrade.SURE_GRIP));
        e.setNewSpeed((float)(e.getOriginalSpeed()*(1+Math.min(ClientState.snapshot.getDouble("miningCap"),bonus))));
    }
    private static double skill(Upgrade u) {
        var cfg=ClientState.snapshot.getCompound("upgradeConfig").getCompound(u.name());
        return cfg.getBoolean("enabled")?Math.min(ClientState.data.progression.rank(u.id()),cfg.getInt("max"))*cfg.getDouble("effect"):0;
    }
    @Mod.EventBusSubscriber(modid=MedievalStatus.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e) { e.register(OPEN); }
    }
}
