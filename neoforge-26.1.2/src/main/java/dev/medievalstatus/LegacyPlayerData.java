package dev.medievalstatus;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Imports the original Forge envelope without changing the mod's version-one payload. */
public final class LegacyPlayerData {
    public static void importEntityData(CompoundTag destination, CompoundTag original) {
        for (String key : new String[]{PlayerData.KEY, "medievalstatus_damage", "medievalstatus_endured"}) {
            if (!destination.contains(key) && original.contains(key)) destination.put(key, original.get(key).copy());
        }
    }

    static boolean importTag(CompoundTag destination, CompoundTag savedPlayer) {
        if (destination.contains(PlayerData.KEY)) return false;
        var original = Nbt.compound(savedPlayer, "ForgeData").getCompound(PlayerData.KEY);
        if (original.isEmpty()) return false;
        destination.put(PlayerData.KEY, original.get().copy());
        return true;
    }

    static void migrate(ServerPlayer player) {
        CompoundTag destination = player.getPersistentData();
        if (destination.contains(PlayerData.KEY)) return;
        var server = player.level().getServer();
        // The engine handles its own relocated players/data directory and singleplayer owner.
        var loaded = server.getPlayerList().loadPlayerData(new NameAndId(player.getGameProfile()));
        if (loaded.isPresent() && importTag(destination, loaded.get())) return;
        // Retain a read-only fallback for an original 1.20.1 playerdata directory.
        Path root = server.getWorldPath(LevelResource.ROOT);
        Path legacy = root.resolve("playerdata").resolve(player.getStringUUID() + ".dat");
        if (Files.isRegularFile(legacy)) {
            if (importTag(destination, read(legacy))) return;
        }
        // Original integrated servers can keep their owner in level.dat instead.
        if (server.isSingleplayerOwner(new NameAndId(player.getGameProfile()))) {
            Path level = root.resolve("level.dat");
            if (Files.isRegularFile(level)) importTag(destination, Nbt.compound(Nbt.compound(read(level), "Data"), "Player"));
        }
    }

    private static CompoundTag read(Path path) {
        try {
            return NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
        } catch (IOException ex) {
            // Do not silently initialize empty progression over unreadable original data.
            throw new IllegalStateException("Unable to read original Medieval Status save: " + path, ex);
        }
    }
    private LegacyPlayerData() {}
}
