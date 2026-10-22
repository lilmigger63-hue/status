package dev.medievalstatus;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.brewing.PotionBrewEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import java.util.*;

/** Exercises the platform transitions through the real damage and component paths. */
public final class PlatformGameTests {
    private static Map<String,java.util.function.Consumer<GameTestHelper>> tests() {
        var tests=new LinkedHashMap<String,java.util.function.Consumer<GameTestHelper>>();
        tests.put("killingblowafterabsorption",PlatformGameTests::killingBlowAfterAbsorption);
        tests.put("fullyabsorbedcombat",PlatformGameTests::fullyAbsorbedCombat);
        tests.put("survivingandfatalendurance",PlatformGameTests::survivingAndFatalEndurance);
        tests.put("potioncomponentdurationcap",PlatformGameTests::potionComponentDurationCap);
        tests.put("foodexhaustionroundtrip",PlatformGameTests::foodExhaustionRoundTrip);
        tests.put("arrowbonusretainsrawiframes",PlatformGameTests::arrowBonusRetainsRawIframes);
        tests.put("zerocountbrewingredemption",PlatformGameTests::zeroCountBrewingRedemption);
        return tests;
    }
    public static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION,registry->tests().forEach((name,test)->
            registry.register(Identifier.fromNamespaceAndPath(MedievalStatus.ID,name),test)));
    }
    public static void register(RegisterGameTestsEvent event) {
        var environment=event.registerEnvironment(Identifier.fromNamespaceAndPath(MedievalStatus.ID,"platformgametests"),new TestEnvironmentDefinition.AllOf());
        tests().forEach((name,test)->{
            Identifier id=Identifier.fromNamespaceAndPath(MedievalStatus.ID,name);
            event.registerTest(id,new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,id),
                new TestData<>(environment,Identifier.fromNamespaceAndPath(MedievalStatus.ID,"empty"),100,0,true)));
        });
    }
    private static ServerPlayer player(GameTestHelper h) {
        return FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"PlatformTest"));
    }
    private static ServerPlayer damageablePlayer(GameTestHelper h) {
        // FakePlayer normally disables damage and never loads a real client.
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"DamageTest")) {
            @Override public boolean isInvulnerableTo(ServerLevel level,DamageSource source) { return false; }
            @Override public boolean canHarmPlayer(Player other) { return true; }
        };
        p.setInvulnerable(false);
        return p;
    }
    public static void killingBlowAfterAbsorption(GameTestHelper h) {
        ServerPlayer p=player(h); PlayerData d=PlayerData.get(p);
        var enemy=h.spawn(EntityType.ZOMBIE,1,1,1);
        enemy.getAttribute(Attributes.ARMOR).setBaseValue(0);
        enemy.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(10);
        enemy.setHealth(4); enemy.setAbsorptionAmount(3);
        enemy.hurtServer(h.getLevel(),p.damageSources().playerAttack(p),10);
        h.assertTrue(!enemy.isAlive(),"The fixture must be a killing blow");
        h.assertTrue(d.activities.getOrDefault("Melee",0L)==1,"A killing blow still earns the original melee action");
        h.assertTrue(Nbt.decimal(enemy.getPersistentData(),"medievalstatus_damage")==4,"Overkill bookkeeping caps damage at pre-hit health");
        double reward=Config.REWARDS.get("Melee").get()*(7/4.0)*Config.REWARD_MULTIPLIER.get()*Config.ATTRIBUTE_MULTIPLIER.get()*Config.ATTRIBUTE_MULTIPLIERS.get(0).get();
        h.assertTrue(Math.abs(d.progression.attribute(0).lifetime()-reward)<1e-9,"Combat XP uses seven health damage after three absorption hearts");
        enemy.discard(); PlayerData.remove(p); h.succeed();
    }
    public static void fullyAbsorbedCombat(GameTestHelper h) {
        ServerPlayer p=player(h); PlayerData d=PlayerData.get(p);
        var enemy=h.spawn(EntityType.ZOMBIE,1,1,1);
        enemy.getAttribute(Attributes.ARMOR).setBaseValue(0);
        enemy.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(10);
        enemy.setAbsorptionAmount(10); float health=enemy.getHealth();
        enemy.hurtServer(h.getLevel(),p.damageSources().playerAttack(p),6);
        h.assertTrue(enemy.getHealth()==health && enemy.getAbsorptionAmount()==4,"The fixture consumes only absorption");
        h.assertTrue(d.activities.getOrDefault("Melee",0L)==0 && d.progression.attribute(0).lifetime()==0,"Fully absorbed damage earns no action or XP");
        h.assertTrue(!enemy.getPersistentData().contains("medievalstatus_damage"),"Fully absorbed damage consumes no combat reward allowance");
        enemy.discard(); PlayerData.remove(p); h.succeed();
    }
    public static void survivingAndFatalEndurance(GameTestHelper h) {
        var enemy=h.spawn(EntityType.ZOMBIE,1,1,1);
        ServerPlayer survivor=damageablePlayer(h); PlayerData endured=PlayerData.get(survivor);
        survivor.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(10);
        survivor.setHealth(20); survivor.setAbsorptionAmount(2);
        DamageSource source=new DamageSource(survivor.damageSources().mobAttack(enemy).typeHolder(),enemy) {
            @Override public boolean scalesWithDifficulty() { return false; }
        };
        survivor.hurtServer(h.getLevel(),source,6);
        h.assertTrue(survivor.getHealth()==16,"The survival fixture loses four health after absorption");
        h.assertTrue(endured.activities.getOrDefault("Endurance",0L)==1 && endured.recoverable==4,"A survived monster hit retains the original endurance and recoverable damage");
        h.assertTrue(Nbt.decimal(enemy.getPersistentData(),"medievalstatus_endured")==4,"Endurance allowance counts only health damage");
        ServerPlayer fatal=damageablePlayer(h); PlayerData rejected=PlayerData.get(fatal);
        fatal.setHealth(4);
        fatal.hurtServer(h.getLevel(),source,6);
        h.assertTrue(!fatal.isAlive(),"The second fixture must be fatal");
        h.assertTrue(rejected.activities.getOrDefault("Endurance",0L)==0 && rejected.recoverable==0,"A fatal hit grants neither endurance nor recoverable damage");
        h.assertTrue(Nbt.decimal(enemy.getPersistentData(),"medievalstatus_endured")==4,"Fatal damage does not consume the enemy endurance allowance");
        enemy.discard(); PlayerData.remove(survivor); PlayerData.remove(fatal); h.succeed();
    }
    public static void potionComponentDurationCap(GameTestHelper h) {
        ServerPlayer p=player(h); PlayerData d=PlayerData.get(p);
        d.activities.put("Brewing",Long.MAX_VALUE); d.bonusesDirty=true;
        ItemStack potion=new ItemStack(Items.POTION);
        var shortEffect=new MobEffectInstance(MobEffects.SPEED,2400,1,true,false,false);
        var longEffect=new MobEffectInstance(MobEffects.FIRE_RESISTANCE,60000);
        potion.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.empty(),List.of(shortEffect,longEffect),Optional.empty()));
        Events.drank(new LivingEntityUseItemEvent.Finish(p,potion,0,new ItemStack(Items.GLASS_BOTTLE)));
        var speed=p.getEffect(MobEffects.SPEED); var fire=p.getEffect(MobEffects.FIRE_RESISTANCE);
        h.assertTrue(speed!=null && speed.getDuration()==3600 && speed.getAmplifier()==1,"Potion components retain the original fifty-percent duration bonus");
        h.assertTrue(speed.isAmbient() && !speed.isVisible() && !speed.showIcon(),"Potion extension preserves ambient, particles and icon flags");
        h.assertTrue(fire!=null && fire.getDuration()==72000,"Potion extension retains the original 72000-tick cap");
        PlayerData.remove(p); h.succeed();
    }
    public static void foodExhaustionRoundTrip(GameTestHelper h) {
        ServerPlayer p=player(h); PlayerData d=PlayerData.get(p);
        int[] ranks=new int[Upgrade.values().length]; ranks[Upgrade.ENDURING_RUNNER.id()]=5;
        d.progression.restorePoints(5,5,ranks);
        d.lastEnchants=p.getStats().getValue(Stats.CUSTOM.get(Stats.ENCHANT_ITEM));
        d.dirty=false; d.bonusesDirty=false; p.setSprinting(true);
        CompoundTag food=new CompoundTag();
        food.putInt("foodLevel",15); food.putInt("foodTickTimer",37);
        food.putFloat("foodSaturationLevel",3.25f); food.putFloat("foodExhaustionLevel",.06f);
        p.getFoodData().readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),food));
        h.succeedWhen(()->{
            h.assertTrue(h.getLevel().getGameTime()%20==0,"Wait for the existing once-per-second sprint adjustment");
            Events.tick(new PlayerTickEvent.Post(p));
            var output=TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
            p.getFoodData().addAdditionalSaveData(output); CompoundTag actual=output.buildResult();
            h.assertTrue(actual.getFloat("foodExhaustionLevel").orElse(-1f)==0,"Sprint recovery clamps exhaustion to zero");
            food.putFloat("foodExhaustionLevel",0);
            h.assertTrue(actual.equals(food),"Exhaustion adjustment preserves food, saturation and the regeneration timer exactly");
            PlayerData.remove(p);
        });
    }
    public static void arrowBonusRetainsRawIframes(GameTestHelper h) {
        ServerPlayer attacker=player(h); PlayerData data=PlayerData.get(attacker);
        data.activities.put("Archery",500L); data.bonusesDirty=true;
        h.assertTrue(Bonuses.of(data).arrows()==.25,"The fixture supplies the original twenty-five-percent arrow bonus");
        var arrow=EntityType.ARROW.create(h.getLevel(),EntitySpawnReason.COMMAND);
        DamageSource shot=attacker.damageSources().arrow(arrow,attacker);
        var enemy=h.spawn(EntityType.ZOMBIE,1,1,1);
        enemy.getAttribute(Attributes.ARMOR).setBaseValue(0);
        enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); enemy.setHealth(100);
        enemy.hurtServer(h.getLevel(),shot,10);
        h.assertTrue(enemy.getHealth()==87.5f,"First arrow applies the original bonus before armor");
        enemy.hurtServer(h.getLevel(),attacker.damageSources().playerAttack(attacker),11);
        h.assertTrue(enemy.getHealth()==86.5f,"Raw eleven damage exceeds raw ten within invulnerability and applies one melee damage");
        ServerPlayer victim=damageablePlayer(h); victim.setHealth(20);
        victim.hurtServer(h.getLevel(),shot,10);
        h.assertTrue(victim.getHealth()==7.5f,"The Player actuallyHurt override applies the arrow bonus once");
        victim.hurtServer(h.getLevel(),attacker.damageSources().playerAttack(attacker),11);
        h.assertTrue(victim.getHealth()==6.5f,"Player invulnerability also compares the original raw hit amounts");
        enemy.discard(); arrow.discard(); PlayerData.remove(attacker); PlayerData.remove(victim); h.succeed();
    }
    public static void zeroCountBrewingRedemption(GameTestHelper h) {
        ServerPlayer p=player(h); PlayerData d=PlayerData.get(p);
        var stacks=net.minecraft.core.NonNullList.withSize(5,ItemStack.EMPTY);
        ItemStack source=PotionContents.createItemStack(Items.POTION,Potions.AWKWARD); stacks.set(0,source);
        Events.brewed(new PotionBrewEvent.Post(stacks));
        p.getInventory().setItem(0,source.copy()); source.setCount(0);
        Events.potionTaken(new PlayerBrewedPotionEvent(p,source));
        h.assertTrue(source.getCount()==0,"Reading legacy shift-click data restores the zero source count");
        h.assertTrue(d.activities.getOrDefault("Brewing",0L)==1,"A zero-count source redeems its original copied brewing token");
        d.cooldowns.clear();
        Events.potionTaken(new PlayerBrewedPotionEvent(p,p.getInventory().getItem(0)));
        h.assertTrue(d.activities.getOrDefault("Brewing",0L)==1,"Destination copies lose the same token and cannot redeem twice");
        PlayerData.remove(p); h.succeed();
    }
    private PlatformGameTests() {}
}
