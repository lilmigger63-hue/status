package dev.medievalstatus;

import dev.medievalstatus.core.Progression;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Single source of truth for capped effects, shared with the UI through snapshots. */
public record Bonuses(double mining, double wood, double melee, double move, double attackSpeed,
                      double health, double armor, double arrows, double crops, double enchant,
                      double potion, double trade, double recovery, double otherMining,
                      double breeding, double crafting) {
    public static double skill(PlayerData d, Upgrade u) {
        return Config.ENABLED.get(u).get() ? Math.min(d.progression.rank(u.id()), Config.RANKS.get(u).get()) * Config.EFFECTS.get(u).get() : 0;
    }
    private static double stat(PlayerData d, int index, double cap) {
        return Math.min(cap, Progression.scale(d.progression.attribute(index).level(), cap) * Config.EFFECTIVENESS.get());
    }
    private static double activity(PlayerData d,String name) {
        return Progression.activityBonus(d.activities.getOrDefault(name,0L));
    }
    public static Bonuses of(PlayerData d) {
        if (d.cachedBonuses!=null && !d.bonusesDirty) return d.cachedBonuses;
        double mine = stat(d,0,.65) + skill(d,Upgrade.EFFICIENT_MINER);
        d.cachedBonuses = new Bonuses(Math.min(Config.MINING_CAP.get(), mine + activity(d,"Mining")),
            Math.min(Config.MINING_CAP.get(), mine + skill(d,Upgrade.LUMBERJACK) + activity(d,"Woodcutting")),
            Math.min(Config.DAMAGE_CAP.get(), stat(d,0,.25) + skill(d,Upgrade.HEAVY_LIFTER) + activity(d,"Melee")),
            Math.min(Config.MOVE_CAP.get(), stat(d,1,.15) + skill(d,Upgrade.LIGHT_FOOTED) + activity(d,"Exploration")),
            stat(d,1,.12), Math.min(Config.HEALTH_CAP.get(), stat(d,2,12) + 2*skill(d,Upgrade.IRON_BODY) + d.baseHealth*activity(d,"Endurance")),
            Math.min(Config.ARMOR_CAP.get(), stat(d,2,2) + skill(d,Upgrade.TOUGH_SKIN)),
            Math.min(.35, stat(d,1,.2) + skill(d,Upgrade.STEADY_AIM) + activity(d,"Archery")),
            Math.min(.25, stat(d,4,.15) + skill(d,Upgrade.GREEN_THUMB) + activity(d,"Farming")),
            Math.min(.35, stat(d,3,.2) + skill(d,Upgrade.EXPERIENCED_ENCHANTER) + activity(d,"Enchanting")),
            Math.min(.5, stat(d,3,.2) + skill(d,Upgrade.EXPERIENCED_BREWER) + activity(d,"Brewing")),
            Math.min(Config.TRADE_CAP.get(), stat(d,5,.15) + skill(d,Upgrade.MERCHANT) + activity(d,"Trading")),
            Math.min(2, stat(d,2,1) + skill(d,Upgrade.NATURAL_RECOVERY)),
            Math.min(Config.MINING_CAP.get(),mine),
            Math.min(.4,skill(d,Upgrade.ANIMAL_CARETAKER) + activity(d,"Breeding")),
            Math.min(.5,skill(d,Upgrade.CAREFUL_CRAFTER) + activity(d,"Crafting")));
        return d.cachedBonuses;
    }
    public void apply(ServerPlayer p) {
        modifier(p, Attributes.MAX_HEALTH, "health", health, AttributeModifier.Operation.ADD_VALUE);
        modifier(p, Attributes.ARMOR, "armor", armor, AttributeModifier.Operation.ADD_VALUE);
        modifier(p, Attributes.MOVEMENT_SPEED, "movement", move, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(p, Attributes.ATTACK_DAMAGE, "damage", melee, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(p, Attributes.ATTACK_SPEED, "attack_speed", attackSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }
    private static void modifier(ServerPlayer p, Holder<Attribute> a, String key, double value, AttributeModifier.Operation op) {
        AttributeInstance instance = p.getAttribute(a);
        if (instance == null) return;
        Identifier id = Identifier.fromNamespaceAndPath(MedievalStatus.ID,key);
        AttributeModifier old = instance.getModifier(id);
        if (old != null && old.amount() == value) return;
        instance.removeModifier(id);
        if (value != 0) instance.addTransientModifier(new AttributeModifier(id, value, op));
    }
    public CompoundTag write() {
        CompoundTag t = new CompoundTag();
        double[] v = {mining,wood,melee,move,attackSpeed,health,armor,arrows,crops,enchant,potion,trade,recovery,otherMining,breeding,crafting};
        String[] keys = {"Mining","Woodcutting","Melee damage","Movement","Attack speed","Bonus health","Bonus armor","Arrow damage","Crop yield chance","Enchant refund chance","Potion duration","Trade discount","Recovery pulse","Other block mining","Growth reduction","Tool repair chance"};
        for (int i=0;i<v.length;i++) t.putDouble(keys[i],v[i]);
        return t;
    }
}
