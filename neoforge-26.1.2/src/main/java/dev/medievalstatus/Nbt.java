package dev.medievalstatus;

import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

/** Retains 1.20.1's absent/wrong-type zero/empty defaults without changing saved keys or types. */
public final class Nbt {
    public static int integer(CompoundTag t,String key) { return t.getInt(key).orElse(0); }
    public static long longValue(CompoundTag t,String key) { return t.getLong(key).orElse(0L); }
    public static double decimal(CompoundTag t,String key) { return t.getDouble(key).orElse(0d); }
    public static boolean bool(CompoundTag t,String key) { return t.getBoolean(key).orElse(false); }
    public static String string(CompoundTag t,String key) { return t.getString(key).orElse(""); }
    public static CompoundTag compound(CompoundTag t,String key) { return t.getCompound(key).orElseGet(CompoundTag::new); }
    public static int[] intArray(CompoundTag t,String key) { return t.getIntArray(key).orElseGet(()->new int[0]); }
    public static long[] longArray(CompoundTag t,String key) { return t.getLongArray(key).orElseGet(()->new long[0]); }
    public static boolean hasUuid(CompoundTag t,String key) { return intArray(t,key).length==4; }
    public static UUID uuid(CompoundTag t,String key) {
        int[] a=intArray(t,key);
        return new UUID(((long)a[0]<<32)|(a[1]&0xffffffffL),((long)a[2]<<32)|(a[3]&0xffffffffL));
    }
    public static void putUuid(CompoundTag t,String key,UUID id) {
        long high=id.getMostSignificantBits(),low=id.getLeastSignificantBits();
        t.putIntArray(key,new int[]{(int)(high>>>32),(int)high,(int)(low>>>32),(int)low});
    }
    private Nbt() {}
}
