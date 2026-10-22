package dev.medievalstatus;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Persistent provenance, rather than an evictable cache that can be farmed after eviction. */
public final class PlacedBlocks extends SavedData {
    static final Identifier ID = Identifier.fromNamespaceAndPath(MedievalStatus.ID,"medievalstatus_placed");
    static final Codec<PlacedBlocks> CODEC = CompoundTag.CODEC.xmap(PlacedBlocks::load, data -> data.save(new CompoundTag()));
    static final SavedDataType<PlacedBlocks> TYPE = new SavedDataType<>(ID,PlacedBlocks::new,CODEC,null);
    private final LongOpenHashSet positions = new LongOpenHashSet();
    public static PlacedBlocks get(ServerLevel level) {
        var storage=level.getDataStorage();
        PlacedBlocks loaded=storage.get(TYPE);
        if (loaded!=null) return loaded;
        Path worldRoot=level.getServer().getWorldPath(LevelResource.ROOT);
        Path modern=modernDataFile(worldRoot,level.dimension());
        if (Files.exists(modern))
            throw new IllegalStateException("Unable to load existing Medieval Status block provenance: "+modern);
        Optional<Path> legacy=legacyImportPath(worldRoot,level.dimension());
        if (legacy.isPresent()) {
            try {
                PlacedBlocks imported=readLegacy(legacy.get());
                storage.set(TYPE,imported);
                return imported;
            } catch (IOException e) {
                throw new IllegalStateException("Cannot import existing Medieval Status block provenance from "+legacy.get(),e);
            }
        }
        return storage.computeIfAbsent(TYPE);
    }
    static Path modernDataFile(Path worldRoot,ResourceKey<Level> dimension) {
        return ID.withSuffix(".dat").resolveAgainst(DimensionType.getStorageFolder(dimension,worldRoot).resolve("data"));
    }
    static Optional<Path> legacyImportPath(Path worldRoot,ResourceKey<Level> dimension) {
        Path dataFolder=DimensionType.getStorageFolder(dimension,worldRoot).resolve("data");
        // Never revive older consumed entries when a modern file is present.
        if (Files.exists(modernDataFile(worldRoot,dimension))) return Optional.empty();
        Path current=dataFolder.resolve("medievalstatus_placed.dat");
        if (Files.exists(current)) return Optional.of(current);
        Path historical=legacyDataFile(worldRoot,dimension);
        return Files.exists(historical)?Optional.of(historical):Optional.empty();
    }
    static Path legacyDataFile(Path worldRoot,ResourceKey<Level> dimension) {
        Path dimensionRoot=dimension.equals(Level.OVERWORLD)?worldRoot:
            dimension.equals(Level.NETHER)?worldRoot.resolve("DIM-1"):
            dimension.equals(Level.END)?worldRoot.resolve("DIM1"):
            DimensionType.getStorageFolder(dimension,worldRoot);
        return dimensionRoot.resolve("data").resolve("medievalstatus_placed.dat");
    }
    static PlacedBlocks readLegacy(Path file) throws IOException {
        CompoundTag root=NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap());
        PlacedBlocks imported=load(Nbt.compound(root,"data"));
        imported.setDirty();
        return imported;
    }
    private static PlacedBlocks load(CompoundTag t) {
        PlacedBlocks d = new PlacedBlocks(); for (long v : Nbt.longArray(t,"positions")) d.positions.add(v); return d;
    }
    public void mark(BlockPos pos) { if (positions.add(pos.asLong())) setDirty(); }
    public boolean contains(BlockPos pos) { return positions.contains(pos.asLong()); }
    public boolean consume(BlockPos pos) { if (!positions.remove(pos.asLong())) return false; setDirty(); return true; }
    public CompoundTag save(CompoundTag tag) { tag.putLongArray("positions", positions.toLongArray()); return tag; }
}
