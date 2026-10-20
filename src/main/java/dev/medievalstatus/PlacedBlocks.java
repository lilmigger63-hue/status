package dev.medievalstatus;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent provenance, rather than an evictable cache that can be farmed after eviction. */
public final class PlacedBlocks extends SavedData {
    private final LongOpenHashSet positions = new LongOpenHashSet();
    public static PlacedBlocks get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PlacedBlocks::load, PlacedBlocks::new, "medievalstatus_placed");
    }
    private static PlacedBlocks load(CompoundTag t) {
        PlacedBlocks d = new PlacedBlocks(); for (long v : t.getLongArray("positions")) d.positions.add(v); return d;
    }
    public void mark(BlockPos pos) { if (positions.add(pos.asLong())) setDirty(); }
    public boolean contains(BlockPos pos) { return positions.contains(pos.asLong()); }
    public boolean consume(BlockPos pos) { if (!positions.remove(pos.asLong())) return false; setDirty(); return true; }
    @Override public CompoundTag save(CompoundTag tag) { tag.putLongArray("positions", positions.toLongArray()); return tag; }
}
