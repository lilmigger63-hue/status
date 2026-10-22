package dev.medievalstatus;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class Network {
    public record Request(int operation) implements CustomPacketPayload {
        public static final Type<Request> TYPE=new Type<>(Identifier.fromNamespaceAndPath(MedievalStatus.ID,"request"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,p)->b.writeInt(p.operation()),b->new Request(b.readInt()));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Snapshot(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE=new Type<>(Identifier.fromNamespaceAndPath(MedievalStatus.ID,"snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,p)->b.writeNbt(p.data()),b->new Snapshot(b.readNbt()));
        @Override public Type<Snapshot> type() { return TYPE; }
    }
    public static void init(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(Request.TYPE,Request.CODEC,Network::handleRequest).playToClient(Snapshot.TYPE,Snapshot.CODEC);
    }
    private static void handleRequest(Request packet,IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer p)) return;
        PlayerData d=PlayerData.get(p);
        if (!d.allow("packet",p.level().getGameTime(),5)) return;
        if (packet.operation()==-2) { sync(p); return; }
        boolean changed=SkillService.apply(p,packet.operation(),System.currentTimeMillis());
        if (!changed && packet.operation()==-1) p.sendOverlayMessage(Component.literal("Reset unavailable: check cooldown and vanilla level cost."));
        sync(p);
    }
    public static void request(int id) { dev.medievalstatus.client.ClientEvents.sendRequest(id); }
    public static void sync(ServerPlayer p) {
        PlayerData d=PlayerData.get(p); PlayerData.save(p);
        CompoundTag snapshot=d.write(); snapshot.put("bonuses",Bonuses.of(d).write());
        snapshot.putDouble("attributeBase",Config.ATTRIBUTE_BASE.get()); snapshot.putDouble("attributePower",Config.ATTRIBUTE_POWER.get());
        snapshot.putDouble("overallBase",Config.OVERALL_BASE.get()); snapshot.putDouble("overallPower",Config.OVERALL_POWER.get());
        CompoundTag upgrades=new CompoundTag();
        for (Upgrade u:Upgrade.values()) {
            CompoundTag t=new CompoundTag(); t.putInt("cost",Config.COSTS.get(u).get()); t.putInt("max",Config.RANKS.get(u).get());
            t.putDouble("effect",Config.EFFECTS.get(u).get()); t.putBoolean("enabled",Config.ENABLED.get(u).get()); upgrades.put(u.name(),t);
        }
        snapshot.put("upgradeConfig",upgrades); snapshot.putDouble("miningCap",Config.MINING_CAP.get());
        snapshot.putBoolean("resetEnabled",Config.RESET_ENABLED.get()); snapshot.putInt("resetCost",Config.RESET_COST.get());
        PacketDistributor.sendToPlayer(p,new Snapshot(snapshot)); d.dirty=false;
    }
    private Network() {}
}
