package dev.medievalstatus;

import dev.medievalstatus.core.Progression;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import java.util.*;

/** Single source of truth for capped effects, shared with the UI through snapshots. */
public record Bonuses(double mining, double wood, double melee, double move, double attackSpeed,
                      double health, double armor, double arrows, double crops, double enchant,
                      double potion, double trade, double recovery) {
    public static double skill(PlayerData d, Upgrade u) {
        return Config.ENABLED.get(u).get() ? Math.min(d.progression.rank(u.id()), Config.RANKS.get(u).get()) * Config.EFFECTS.get(u).get() : 0;
    }
    private static double stat(PlayerData d, int index, double cap) {
        return Math.min(cap, Progression.scale(d.progression.attribute(index).level(), cap) * Config.EFFECTIVENESS.get());
    }
    public static Bonuses of(PlayerData d) {
        if (d.cachedBonuses!=null && !d.bonusesDirty) return d.cachedBonuses;
        double mine = stat(d,0,.65) + skill(d,Upgrade.EFFICIENT_MINER);
        d.cachedBonuses = new Bonuses(Math.min(Config.MINING_CAP.get(), mine),
            Math.min(Config.MINING_CAP.get(), mine + skill(d,Upgrade.LUMBERJACK)),
            Math.min(Config.DAMAGE_CAP.get(), stat(d,0,.25) + skill(d,Upgrade.HEAVY_LIFTER)),
            Math.min(Config.MOVE_CAP.get(), stat(d,1,.15) + skill(d,Upgrade.LIGHT_FOOTED)),
            stat(d,1,.12), Math.min(Config.HEALTH_CAP.get(), stat(d,2,12) + 2*skill(d,Upgrade.IRON_BODY)),
            Math.min(Config.ARMOR_CAP.get(), stat(d,2,2) + skill(d,Upgrade.TOUGH_SKIN)),
            Math.min(.35, stat(d,1,.2) + skill(d,Upgrade.STEADY_AIM)),
            Math.min(.25, stat(d,4,.15) + skill(d,Upgrade.GREEN_THUMB)),
            Math.min(.35, stat(d,3,.2) + skill(d,Upgrade.EXPERIENCED_ENCHANTER)),
            Math.min(.5, stat(d,3,.2) + skill(d,Upgrade.EXPERIENCED_BREWER)),
            Math.min(Config.TRADE_CAP.get(), stat(d,5,.15) + skill(d,Upgrade.MERCHANT)),
            Math.min(2, stat(d,2,1) + skill(d,Upgrade.NATURAL_RECOVERY)));
        return d.cachedBonuses;
    }
    public void apply(ServerPlayer p) {
        modifier(p, Attributes.MAX_HEALTH, "health", health, AttributeModifier.Operation.ADDITION);
        modifier(p, Attributes.ARMOR, "armor", armor, AttributeModifier.Operation.ADDITION);
        modifier(p, Attributes.MOVEMENT_SPEED, "movement", move, AttributeModifier.Operation.MULTIPLY_BASE);
        modifier(p, Attributes.ATTACK_DAMAGE, "damage", melee, AttributeModifier.Operation.MULTIPLY_BASE);
        modifier(p, Attributes.ATTACK_SPEED, "attack_speed", attackSpeed, AttributeModifier.Operation.MULTIPLY_BASE);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }
    private static void modifier(ServerPlayer p, Attribute a, String key, double value, AttributeModifier.Operation op) {
        AttributeInstance instance = p.getAttribute(a);
        if (instance == null) return;
        UUID id = UUID.nameUUIDFromBytes((MedievalStatus.ID + ":" + key).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        AttributeModifier old = instance.getModifier(id);
        if (old != null && old.getAmount() == value) return;
        instance.removeModifier(id);
        if (value != 0) instance.addTransientModifier(new AttributeModifier(id, "Medieval Status " + key, value, op));
    }
    public CompoundTag write() {
        CompoundTag t = new CompoundTag();
        double[] v = {mining,wood,melee,move,attackSpeed,health,armor,arrows,crops,enchant,potion,trade,recovery};
        String[] keys = {"Mining","Woodcutting","Melee damage","Movement","Attack speed","Bonus health","Bonus armor","Arrow damage","Crop yield chance","Enchant refund chance","Potion duration","Trade discount","Recovery pulse"};
        for (int i=0;i<v.length;i++) t.putDouble(keys[i],v[i]);
        return t;
    }
}
