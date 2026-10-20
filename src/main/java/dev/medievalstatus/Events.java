package dev.medievalstatus;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.brewing.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.List;

@Mod.EventBusSubscriber(modid = MedievalStatus.ID)
public final class Events {
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            PlayerData.reload(p); PlayerData d = PlayerData.get(p);
            d.lastEnchants = p.getStats().getValue(Stats.CUSTOM.get(Stats.ENCHANT_ITEM));
            Bonuses.of(d).apply(p); Network.sync(p);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { confirmBreaks(p); restoreTrades(PlayerData.get(p)); PlayerData.remove(p); }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { PlayerData.clear(); }
    @SubscribeEvent public static void clone(PlayerEvent.Clone e) {
        if (e.getOriginal() instanceof ServerPlayer old && e.getEntity() instanceof ServerPlayer fresh) {
            CompoundTag saved = PlayerData.get(old).write();
            fresh.getPersistentData().put(PlayerData.KEY,saved); PlayerData.reload(fresh);
        }
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) { Bonuses.of(PlayerData.get(p)).apply(p); Network.sync(p); } }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) { Bonuses.of(PlayerData.get(p)).apply(p); Network.sync(p); } }

    @SubscribeEvent(priority=EventPriority.LOWEST) public static void placed(BlockEvent.EntityPlaceEvent e) {
        if (e.getLevel() instanceof ServerLevel level) {
            PlacedBlocks.get(level).mark(e.getPos());
            if (e instanceof BlockEvent.EntityMultiPlaceEvent multi) multi.getReplacedBlockSnapshots().forEach(s -> PlacedBlocks.get(level).mark(s.getPos()));
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void piston(PistonEvent.Pre e) {
        if (!(e.getLevel() instanceof ServerLevel l)) return;
        var resolver = e.getStructureHelper();
        if (resolver != null && resolver.resolve()) {
            var direction = e.getPistonMoveType().isExtend ? e.getDirection() : e.getDirection().getOpposite();
            // Conservatively mark every moved block, including formerly natural blocks.
            for (BlockPos pos : resolver.getToPush()) { PlacedBlocks.get(l).mark(pos); PlacedBlocks.get(l).mark(pos.relative(direction)); }
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void generated(BlockEvent.FluidPlaceBlockEvent e) {
        if (e.getLevel() instanceof ServerLevel level) PlacedBlocks.get(level).mark(e.getPos());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void broken(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p)) return;
        PlayerData d=PlayerData.get(p);
        if (d.pendingBreaks.size()>=128) return;
        d.pendingBreaks.put(e.getPos().immutable(),new PlayerData.PendingBreak(p.serverLevel(),e.getPos().immutable(),e.getState(),PlacedBlocks.get(p.serverLevel()).contains(e.getPos())));
    }
    static void confirmBreaks(ServerPlayer p) {
        PlayerData d=PlayerData.get(p);
        if (d.pendingBreaks.isEmpty()) return;
        for (PlayerData.PendingBreak pending:List.copyOf(d.pendingBreaks.values())) {
            var s=pending.state(); var current=pending.level().getBlockState(pending.pos());
            if (current.equals(s)) continue;
            if (current.isAir()) PlacedBlocks.get(pending.level()).consume(pending.pos());
            if (p.isCreative() || p.isSpectator()) continue;
            if (s.getBlock() instanceof CropBlock crop && crop.isMaxAge(s)) {
                d.award(p,4,"Farming",1);
                if (p.getRandom().nextDouble() < Bonuses.of(d).crops()) {
                    Item produce = crop == Blocks.WHEAT ? Items.WHEAT : crop == Blocks.CARROTS ? Items.CARROT : crop == Blocks.POTATOES ? Items.POTATO : crop == Blocks.BEETROOTS ? Items.BEETROOT : Items.AIR;
                    if (produce != Items.AIR) Block.popResource(pending.level(),pending.pos(),new ItemStack(produce));
                }
            } else if (!pending.placed() && s.is(BlockTags.LOGS)) d.award(p,0,"Woodcutting",1);
            else if (!pending.placed() && s.is(BlockTags.MINEABLE_WITH_PICKAXE) && s.getDestroySpeed(pending.level(),pending.pos()) >= 1) {
                d.award(p,0,"Mining",Math.min(3, s.getDestroySpeed(pending.level(),pending.pos())/1.5));
            }
        }
        d.pendingBreaks.clear();
    }
    @SubscribeEvent public static void speed(PlayerEvent.BreakSpeed e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        PlayerData d = PlayerData.get(p); Bonuses b = Bonuses.of(d);
        double bonus = e.getState().is(BlockTags.LOGS) ? b.wood() : e.getState().is(BlockTags.MINEABLE_WITH_PICKAXE) ? b.mining() : b.otherMining();
        if (e.getState().getBlock() instanceof CropBlock) bonus += Math.min(.5,Bonuses.skill(d,Upgrade.EXPERIENCED_FARMER));
        if (p.isUnderWater() || !p.onGround()) bonus += Math.min(.6,Bonuses.skill(d,Upgrade.SURE_GRIP));
        e.setNewSpeed((float)(e.getOriginalSpeed()*(1+Math.min(Config.MINING_CAP.get(),bonus))));
    }
    @SubscribeEvent public static void damageBonus(LivingHurtEvent e) {
        if (e.getSource().getEntity() instanceof ServerPlayer p && e.getSource().getDirectEntity() instanceof AbstractArrow)
            e.setAmount((float)(e.getAmount()*(1+Bonuses.of(PlayerData.get(p)).arrows())));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void damaged(LivingDamageEvent e) {
        if (e.getAmount() < 1) return;
        if (e.getSource().getEntity() instanceof ServerPlayer p && e.getEntity() instanceof Monster enemy && enemy.isAlive()) {
            PlayerData d = PlayerData.get(p); long now = p.serverLevel().getGameTime();
            CompoundTag tag = enemy.getPersistentData();
            double awarded = tag.getDouble("medievalstatus_damage");
            if (awarded >= enemy.getMaxHealth()*2) return;
            if (!d.allow("combat",now,Config.ACTION_COOLDOWN.get())) return;
            tag.putDouble("medievalstatus_damage", awarded + Math.min(e.getAmount(),enemy.getHealth()));
            boolean arrow = e.getSource().getDirectEntity() instanceof AbstractArrow;
            d.award(p,arrow?1:0,arrow?"Archery":"Melee",Math.min(3,e.getAmount()/4.0));
        }
        if (e.getEntity() instanceof ServerPlayer p && e.getSource().getEntity() instanceof Monster enemy && e.getAmount() < p.getHealth()) {
            PlayerData d = PlayerData.get(p); long now=p.serverLevel().getGameTime();
            double endured=enemy.getPersistentData().getDouble("medievalstatus_endured");
            if (endured>=enemy.getMaxHealth()*2) return;
            if (!d.allow("endurance",now,200)) return;
            enemy.getPersistentData().putDouble("medievalstatus_endured",endured+e.getAmount());
            d.combatAt=now; d.recoverable=Math.min(10,d.recoverable+e.getAmount());
            d.award(p,2,"Endurance",Math.min(3,e.getAmount()/3.0));
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void healed(LivingHealEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getAmount()<=0) return;
        PlayerData d=PlayerData.get(p); long now=p.serverLevel().getGameTime();
        if (now-d.combatAt>1200 || d.recoverable<=0) return;
        double amount=Math.min(d.recoverable,Math.min(e.getAmount(),p.getMaxHealth()-p.getHealth()));
        d.recoverable-=amount;
        if (amount>0 && d.allow("recovery",now,100)) d.award(p,2,"Recovery",Math.min(1,amount));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void bred(BabyEntitySpawnEvent e) {
        if (e.getCausedByPlayer() instanceof ServerPlayer p && e.getChild()!=null) {
            PlayerData d=PlayerData.get(p);
            if (d.allow("breed",p.serverLevel().getGameTime(),Config.ACTION_COOLDOWN.get())) d.award(p,4,"Breeding",1);
            if (d.offspring.size()<32) d.offspring.add(e.getChild());
        }
    }
    @SubscribeEvent public static void brewed(PotionBrewEvent.Post e) {
        for (int i=0;i<Math.min(3,e.getLength());i++) if (!e.getItem(i).isEmpty()) e.getItem(i).getOrCreateTag().putUUID("medievalstatus_brew_token",java.util.UUID.randomUUID());
    }
    @SubscribeEvent public static void potionTaken(PlayerBrewedPotionEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ItemStack s=e.getStack();
        // Shift-click has already emptied the source stack and copied it into inventory.
        // Read its raw tag and redeem the same token on every destination copy.
        CompoundTag tag=s.getTag();
        if (tag==null || !tag.hasUUID("medievalstatus_brew_token")) return;
        java.util.UUID token=tag.getUUID("medievalstatus_brew_token");
        tag.remove("medievalstatus_brew_token");
        for(int i=0;i<p.getInventory().getContainerSize();i++) clearBrewToken(p.getInventory().getItem(i),token);
        clearBrewToken(p.containerMenu.getCarried(),token);
        p.getInventory().setChanged();
        PlayerData d=PlayerData.get(p);
        if (d.allow("brew",p.serverLevel().getGameTime(),Config.ACTION_COOLDOWN.get())) d.award(p,3,"Brewing",1);
    }
    private static void clearBrewToken(ItemStack s,java.util.UUID token) {
        CompoundTag tag=s.getTag();
        if(tag!=null && tag.hasUUID("medievalstatus_brew_token") && token.equals(tag.getUUID("medievalstatus_brew_token"))) tag.remove("medievalstatus_brew_token");
    }
    @SubscribeEvent public static void drank(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(e.getItem().getItem() instanceof PotionItem)) return;
        double bonus=Bonuses.of(PlayerData.get(p)).potion();
        for (MobEffectInstance effect:PotionUtils.getMobEffects(e.getItem())) {
            if (effect.getEffect().isInstantenous() || effect.getDuration()<=0) continue;
            p.addEffect(new MobEffectInstance(effect.getEffect(),(int)Math.min(72000,effect.getDuration()*(1+bonus)),effect.getAmplifier(),effect.isAmbient(),effect.isVisible(),effect.showIcon()));
        }
    }
    @SubscribeEvent public static void crafted(PlayerEvent.ItemCraftedEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getCrafting().isEmpty()) return;
        PlayerData d=PlayerData.get(p);
        if (!d.allow("craft",p.serverLevel().getGameTime(),1200)) return;
        d.award(p,3,"Crafting",1);
        ItemStack tool=p.getMainHandItem();
        if (tool.isDamaged() && p.getRandom().nextDouble()<Bonuses.of(d).crafting()) tool.setDamageValue(tool.getDamageValue()-1);
    }
    @SubscribeEvent public static void traded(TradeWithVillagerEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        PlayerData d=PlayerData.get(p);
        if (!d.allow("trade",p.serverLevel().getGameTime(),Config.ACTION_COOLDOWN.get())) return;
        d.award(p,5,"Trading",Math.min(3,Math.max(1,e.getMerchantOffer().getXp()/5.0)));
        if (p.getRandom().nextDouble()<Math.min(.25,Bonuses.skill(d,Upgrade.NEGOTIATOR))) p.giveExperiencePoints(1);
    }
    @SubscribeEvent public static void opened(PlayerContainerEvent.Open e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(e.getContainer() instanceof MerchantMenu menu)) return;
        refreshTrades(p,menu);
    }
    static void refreshTrades(ServerPlayer p,MerchantMenu menu) {
        PlayerData d=PlayerData.get(p);
        d.tradeAdjustments.forEach((offer,change)->{
            if(offer.getSpecialPriceDiff()==change.applied()) offer.setSpecialPriceDiff(change.before());
        });
        d.tradeAdjustments.clear();
        for (MerchantOffer offer:menu.getOffers()) {
            int reduction=(int)Math.floor(offer.getBaseCostA().getCount()*Bonuses.of(d).trade());
            if (reduction>0) {
                int before=offer.getSpecialPriceDiff(); offer.addToSpecialPriceDiff(-reduction);
                d.tradeAdjustments.put(offer,new PlayerData.PriceChange(before,offer.getSpecialPriceDiff()));
            }
        }
        // A changed cost must invalidate any result prepared using the old payment.
        menu.slotsChanged(p.getInventory());
        menu.broadcastChanges();
        p.sendMerchantOffers(menu.containerId,menu.getOffers(),menu.getTraderLevel(),menu.getTraderXp(),menu.showProgressBar(),menu.canRestock());
    }
    @SubscribeEvent public static void closed(PlayerContainerEvent.Close e) { if (e.getEntity() instanceof ServerPlayer p) restoreTrades(PlayerData.get(p)); }
    private static void restoreTrades(PlayerData d) {
        d.tradeAdjustments.forEach((offer,change)->{
            // Vanilla may have already reset prices when a trade menu closes.
            if (change.applied()!=0 && offer.getSpecialPriceDiff()==change.applied()) offer.setSpecialPriceDiff(change.before());
        });
        d.tradeAdjustments.clear();
    }

    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase!=TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        PlayerData d=PlayerData.get(p); long now=p.serverLevel().getGameTime();
        confirmBreaks(p);
        if (!d.offspring.isEmpty()) {
            double rate=Bonuses.of(d).breeding();
            for (AgeableMob baby:d.offspring) if (baby.isAlive() && baby.getAge()<0) baby.setAge((int)(baby.getAge()*(1-rate)));
            d.offspring.clear();
        }
        if (now%Config.MOVEMENT_INTERVAL.get()==0) {
            Vec3 pos=p.position();
            if (d.lastPosition!=null && p.level().dimension()==d.lastDimension && p.isSprinting() && p.onGround() && !p.isPassenger() && !p.getAbilities().flying && !p.isFallFlying()) {
                double distance=pos.distanceTo(d.lastPosition);
                if (distance>=4 && distance<=Config.MOVEMENT_INTERVAL.get()*.7) d.award(p,1,"Exploration",Math.min(2,distance/8));
            }
            d.lastPosition=pos; d.lastDimension=p.level().dimension();
        }
        if (now%20==0) {
            int enchants=p.getStats().getValue(Stats.CUSTOM.get(Stats.ENCHANT_ITEM));
            if (d.lastEnchants>=0 && enchants>d.lastEnchants) {
                d.award(p,3,"Enchanting",Math.min(3,enchants-d.lastEnchants));
                if (!p.isCreative() && p.getRandom().nextDouble()<Bonuses.of(d).enchant()) p.giveExperienceLevels(1);
            }
            d.lastEnchants=enchants;
            if (p.isSprinting() && !p.isCreative()) {
                float exhaustion=p.getFoodData().getExhaustionLevel();
                p.getFoodData().setExhaustion((float)Math.max(0,exhaustion-Math.min(.1,Bonuses.skill(d,Upgrade.ENDURING_RUNNER))));
            }
            if (d.bonusesDirty) {
                Bonuses.of(d).apply(p); d.bonusesDirty=false;
                if (p.containerMenu instanceof MerchantMenu menu) refreshTrades(p,menu);
            }
            if (d.dirty) { PlayerData.save(p); Network.sync(p); }
        }
        if (now%200==0 && p.isAlive() && p.getFoodData().getFoodLevel()>=18 && now-d.combatAt>=200 && p.getHealth()<p.getMaxHealth()) {
            float amount=(float)Bonuses.of(d).recovery();
            if (amount>0) { p.heal(amount); p.causeFoodExhaustion(amount*.5f); }
        }
    }
    private Events() {}
}
