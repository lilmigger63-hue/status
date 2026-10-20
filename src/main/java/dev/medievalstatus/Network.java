package dev.medievalstatus;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Network {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(MedievalStatus.ID,"main"), () -> "1", "1"::equals, "1"::equals);
    public record Request(int operation) {
        public Request(FriendlyByteBuf b) { this(b.readInt()); }
        public void encode(FriendlyByteBuf b) { b.writeInt(operation); }
    }
    public record Snapshot(CompoundTag data) {
        public Snapshot(FriendlyByteBuf b) { this(b.readNbt()); }
        public void encode(FriendlyByteBuf b) { b.writeNbt(data); }
    }
    public static void init() {
        CHANNEL.messageBuilder(Request.class,0,NetworkDirection.PLAY_TO_SERVER)
            .encoder(Request::encode).decoder(Request::new).consumerMainThread((packet, context) -> {
                ServerPlayer p = context.get().getSender();
                if (p == null) return;
                PlayerData d = PlayerData.get(p);
                if (!d.allow("packet",p.serverLevel().getGameTime(),5)) return;
                if (packet.operation == -2) { sync(p); return; }
                boolean changed=SkillService.apply(p,packet.operation,System.currentTimeMillis());
                if (!changed && packet.operation==-1) p.displayClientMessage(Component.literal("Reset unavailable: check cooldown and vanilla level cost."),true);
                sync(p);
            }).add();
        CHANNEL.messageBuilder(Snapshot.class,1,NetworkDirection.PLAY_TO_CLIENT)
            .encoder(Snapshot::encode).decoder(Snapshot::new).consumerMainThread((packet,context) ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> dev.medievalstatus.client.ClientState.accept(packet.data))).add();
    }
    public static void request(int id) { CHANNEL.sendToServer(new Request(id)); }
    public static void sync(ServerPlayer p) {
        PlayerData d = PlayerData.get(p);
        PlayerData.save(p);
        CompoundTag snapshot = d.write(); snapshot.put("bonuses", Bonuses.of(d).write());
        snapshot.putDouble("attributeBase", Config.ATTRIBUTE_BASE.get()); snapshot.putDouble("attributePower", Config.ATTRIBUTE_POWER.get());
        snapshot.putDouble("overallBase", Config.OVERALL_BASE.get()); snapshot.putDouble("overallPower", Config.OVERALL_POWER.get());
        CompoundTag upgrades = new CompoundTag();
        for (Upgrade u : Upgrade.values()) {
            CompoundTag t = new CompoundTag(); t.putInt("cost",Config.COSTS.get(u).get()); t.putInt("max",Config.RANKS.get(u).get());
            t.putDouble("effect",Config.EFFECTS.get(u).get()); t.putBoolean("enabled",Config.ENABLED.get(u).get()); upgrades.put(u.name(),t);
        }
        snapshot.put("upgradeConfig",upgrades);
        snapshot.putDouble("miningCap",Config.MINING_CAP.get());
        snapshot.putBoolean("resetEnabled",Config.RESET_ENABLED.get()); snapshot.putInt("resetCost",Config.RESET_COST.get());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Snapshot(snapshot));
        d.dirty = false;
    }
    private Network() {}
}
