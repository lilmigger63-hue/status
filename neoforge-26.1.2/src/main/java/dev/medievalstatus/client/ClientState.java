package dev.medievalstatus.client;

import dev.medievalstatus.PlayerData;
import net.minecraft.nbt.CompoundTag;

public final class ClientState {
    public static CompoundTag snapshot = new CompoundTag();
    public static PlayerData data = new PlayerData();
    public static boolean ready;
    public static void accept(CompoundTag t) {
        if (t == null) return;
        snapshot = t; data = PlayerData.read(t); ready = true;
    }
    public static void clear() { snapshot = new CompoundTag(); data = new PlayerData(); ready = false; }
    private ClientState() {}
}
