package dev.medievalstatus.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.medievalstatus.*;
import net.minecraft.client.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid=MedievalStatus.ID,value=Dist.CLIENT)
public final class ClientEvents {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(MedievalStatus.ID,"medievalstatus"));
    public static final KeyMapping OPEN = new KeyMapping("key.medievalstatus.open",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_P,CATEGORY);
    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        Minecraft mc=Minecraft.getInstance();
        while (OPEN.consumeClick()) if (mc.player!=null) { Network.request(-2); mc.setScreen(new StatusScreen()); }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e) { ClientState.clear(); }
    @SubscribeEvent public static void mining(PlayerEvent.BreakSpeed e) {
        if (!e.getEntity().level().isClientSide() || !ClientState.ready) return;
        var b=Nbt.compound(ClientState.snapshot,"bonuses");
        String key=e.getState().is(BlockTags.LOGS)?"Woodcutting":e.getState().is(BlockTags.MINEABLE_WITH_PICKAXE)?"Mining":b.contains("Other block mining")?"Other block mining":"Mining";
        double bonus=Nbt.decimal(b,key);
        if (e.getState().getBlock() instanceof CropBlock) bonus+=Math.min(.5,skill(Upgrade.EXPERIENCED_FARMER));
        if (e.getEntity().isUnderWater() || !e.getEntity().onGround()) bonus+=Math.min(.6,skill(Upgrade.SURE_GRIP));
        e.setNewSpeed((float)(e.getOriginalSpeed()*(1+Math.min(Nbt.decimal(ClientState.snapshot,"miningCap"),bonus))));
    }
    public static void sendRequest(int operation) {
        ClientPacketDistributor.sendToServer(new Network.Request(operation));
    }
    private static double skill(Upgrade u) {
        var cfg=Nbt.compound(Nbt.compound(ClientState.snapshot,"upgradeConfig"),u.name());
        return Nbt.bool(cfg,"enabled")?Math.min(ClientState.data.progression.rank(u.id()),Nbt.integer(cfg,"max"))*Nbt.decimal(cfg,"effect"):0;
    }
    @EventBusSubscriber(modid=MedievalStatus.ID,value=Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e) { e.registerCategory(CATEGORY); e.register(OPEN); }
        @SubscribeEvent public static void payloads(RegisterClientPayloadHandlersEvent e) {
            e.register(Network.Snapshot.TYPE,(packet,context)->ClientState.accept(packet.data()));
        }
    }
}
