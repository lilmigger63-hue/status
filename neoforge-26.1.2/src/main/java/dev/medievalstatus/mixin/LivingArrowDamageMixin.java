package dev.medievalstatus.mixin;

import dev.medievalstatus.Events;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Stack;

/** Matches the original LivingHurt timing, after shield and invulnerability checks. */
@Mixin(LivingEntity.class)
abstract class LivingArrowDamageMixin {
    @Shadow protected Stack<DamageContainer> damageContainers;

    @Inject(method="actuallyHurt",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/common/damagesource/DamageContainer;getNewDamage()F",ordinal=0))
    private void medievalstatus$arrowBonus(ServerLevel level,DamageSource source,float amount,CallbackInfo callback) {
        Events.damageBonus(damageContainers.peek());
    }
}
