package dev.medievalstatus.mixin;

import dev.medievalstatus.Events;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Player overrides actuallyHurt and must receive the same original arrow hook. */
@Mixin(Player.class)
abstract class PlayerArrowDamageMixin extends LivingEntity {
    protected PlayerArrowDamageMixin(EntityType<? extends LivingEntity> type,Level level) { super(type,level); }

    @Inject(method="actuallyHurt",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/common/damagesource/DamageContainer;getNewDamage()F",ordinal=0))
    private void medievalstatus$arrowBonus(ServerLevel level,DamageSource source,float amount,CallbackInfo callback) {
        Events.damageBonus(damageContainers.peek());
    }
}
