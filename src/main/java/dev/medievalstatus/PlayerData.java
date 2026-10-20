package dev.medievalstatus;

import dev.medievalstatus.core.Progression;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public final class PlayerData {
    public static final String KEY = "medievalstatus";
    public static final String[] ATTRIBUTES = {"Strength", "Dexterity", "Constitution", "Intelligence", "Wisdom", "Charisma"};
    private static final Map<UUID, PlayerData> ONLINE = new HashMap<>();
    public final Progression progression = new Progression();
    public final Map<String, Long> activities = new TreeMap<>();
    final Map<String, Long> cooldowns = new HashMap<>();
    public long resetAt;
    public boolean dirty = true, bonusesDirty = true;
    public Bonuses cachedBonuses;
    public long combatAt = -1200;
    public double recoverable;
    public int lastEnchants = -1;
    public final java.util.List<net.minecraft.world.entity.AgeableMob> offspring = new java.util.ArrayList<>();
    public record PendingBreak(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state,boolean placed) {}
    public final java.util.Map<net.minecraft.core.BlockPos,PendingBreak> pendingBreaks=new java.util.LinkedHashMap<>();
    public record PriceChange(int before, int applied) {}
    public final java.util.Map<net.minecraft.world.item.trading.MerchantOffer,PriceChange> tradeAdjustments = new java.util.IdentityHashMap<>();
    public net.minecraft.world.phys.Vec3 lastPosition;
    public net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> lastDimension;

    public static PlayerData get(ServerPlayer player) {
        return ONLINE.computeIfAbsent(player.getUUID(), id -> read(player.getPersistentData().getCompound(KEY)));
    }
    public static void remove(ServerPlayer p) { save(p); ONLINE.remove(p.getUUID()); }
    public static void clear() { ONLINE.clear(); }
    public static void save(ServerPlayer p) { p.getPersistentData().put(KEY, get(p).write()); }
    public static void reload(ServerPlayer p) { ONLINE.remove(p.getUUID()); }
    public boolean allow(String action, long now, long interval) {
        Long previous = cooldowns.get(action);
        if (previous != null && now >= previous && now - previous < interval) return false;
        if (cooldowns.size() >= 128) cooldowns.clear();
        cooldowns.put(action, now);
        return true;
    }
    public void award(ServerPlayer p, int attribute, String activity, double difficulty) {
        if (p.isCreative() || p.isSpectator() || difficulty <= 0 || !Double.isFinite(difficulty)) return;
        double reward = Config.REWARDS.get(activity).get() * Math.min(4, difficulty) * Config.REWARD_MULTIPLIER.get();
        if (reward <= 0) return;
        long before = progression.attribute(attribute).level();
        progression.attribute(attribute).award(reward * Config.ATTRIBUTE_MULTIPLIER.get() * Config.ATTRIBUTE_MULTIPLIERS.get(attribute).get(), Config.ATTRIBUTE_BASE.get(), Config.ATTRIBUTE_POWER.get());
        progression.awardOverall(reward * Config.OVERALL_MULTIPLIER.get(), Config.OVERALL_BASE.get(), Config.OVERALL_POWER.get());
        activities.merge(activity, 1L, Progression::add);
        dirty = true;
        if (before != progression.attribute(attribute).level()) bonusesDirty = true;
        save(p);
    }
    public CompoundTag write() {
        CompoundTag root = new CompoundTag(); root.putInt("version", 1);
        root.put("overall", writeLevel(progression.overall()));
        for (int i = 0; i < 6; i++) root.put(ATTRIBUTES[i], writeLevel(progression.attribute(i)));
        root.putLong("earned", progression.earned()); root.putLong("spent", progression.spent());
        int[] ranks = new int[Upgrade.values().length];
        for (int i = 0; i < ranks.length; i++) ranks[i] = progression.rank(i);
        root.putIntArray("ranks", ranks); root.putLong("resetAt", resetAt);
        CompoundTag counts = new CompoundTag(); activities.forEach(counts::putLong); root.put("activities", counts);
        return root;
    }
    private static CompoundTag writeLevel(Progression.Level l) {
        CompoundTag t = new CompoundTag(); t.putLong("level", l.level()); t.putDouble("xp", l.xp()); t.putDouble("lifetime", l.lifetime()); return t;
    }
    private static void readLevel(CompoundTag t, Progression.Level l) { l.restore(t.getLong("level"), t.getDouble("xp"), t.getDouble("lifetime")); }
    public static PlayerData read(CompoundTag t) {
        PlayerData d = new PlayerData();
        // Missing fields from pre-v1 data migrate to the level-one defaults.
        readLevel(t.getCompound("overall"), d.progression.overall());
        for (int i = 0; i < 6; i++) readLevel(t.getCompound(ATTRIBUTES[i]), d.progression.attribute(i));
        d.progression.restorePoints(t.getLong("earned"), t.getLong("spent"), t.getIntArray("ranks"));
        CompoundTag counts = t.getCompound("activities");
        for (String k : Config.REWARDS.keySet()) if (counts.contains(k)) d.activities.put(k, Math.max(0, counts.getLong(k)));
        d.resetAt = Math.max(0, t.getLong("resetAt"));
        return d;
    }
}
