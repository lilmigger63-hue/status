package dev.medievalstatus.mixin;

import dev.medievalstatus.LegacyPlayerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The loader renamed ForgeData; recover the original mod-owned keys during entity loading. */
@Mixin(Entity.class)
abstract class LegacyEntityMixin {
    @Inject(method = "load", at = @At("TAIL"))
    private void medievalstatus$readOriginalData(ValueInput input, CallbackInfo callback) {
        input.read("ForgeData", CompoundTag.CODEC).ifPresent(original ->
            LegacyPlayerData.importEntityData(((Entity)(Object)this).getPersistentData(), original));
    }
}
