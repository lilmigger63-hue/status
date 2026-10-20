package dev.medievalstatus;

import com.mojang.authlib.GameProfile;
import dev.medievalstatus.core.Progression;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(MedievalStatus.ID)
@PrefixGameTestTemplate(false)
public final class ActivityBonusGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        return FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"ActivityTest"));
    }
    private static void close(GameTestHelper h,double actual,double expected,String label) {
        h.assertTrue(Math.abs(actual-expected)<1e-9,label+": expected "+expected+", got "+actual);
    }
    @GameTest(template="empty") public static void isolatedActivityMappings(GameTestHelper h) {
        String[] activities={"Mining","Woodcutting","Farming","Exploration","Archery","Endurance","Enchanting","Brewing","Trading","Melee","Breeding","Crafting"};
        String[] effects={"Mining","Woodcutting","Crop yield chance","Movement","Arrow damage","Bonus health","Enchant refund chance","Potion duration","Trade discount","Melee damage","Growth reduction","Tool repair chance"};
        CompoundTag baseline=Bonuses.of(new PlayerData()).write();
        for(int i=0;i<activities.length;i++) {
            PlayerData d=new PlayerData(); d.activities.put(activities[i],50L);
            CompoundTag b=Bonuses.of(d).write();
            close(h,b.getDouble(effects[i]),i==5?.5:.025,activities[i]+" maps to its own effect");
            for(String key:baseline.getAllKeys()) if(!key.equals(effects[i])) close(h,b.getDouble(key),baseline.getDouble(key),activities[i]+" must not change "+key);
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void existingStackingAndCaps(GameTestHelper h) {
        PlayerData d=new PlayerData();
        d.progression.attribute(0).restore(10,0,0); d.progression.attribute(2).restore(10,0,0);
        d.progression.awardOverall(600,150,1.5); d.progression.purchase(0,1,5);
        Bonuses baseline=Bonuses.of(d);
        close(h,baseline.mining(),Progression.scale(10,.65)+.04,"Existing STR and purchased mining contribution unchanged");
        d.activities.put("Mining",150L); d.activities.put("Farming",150L); d.bonusesDirty=true;
        Bonuses combined=Bonuses.of(d);
        close(h,combined.mining(),baseline.mining()+.075,"Milestones add rather than compound");
        close(h,combined.wood(),baseline.wood(),"Mining does not change woodcutting");
        close(h,combined.crops(),.075,"Chance gains percentage points");
        for(String activity:Config.REWARDS.keySet()) d.activities.put(activity,Long.MAX_VALUE);
        d.bonusesDirty=true; Bonuses capped=Bonuses.of(d); CompoundTag all=capped.write();
        close(h,capped.mining(),Config.MINING_CAP.get(),"Mining cap"); close(h,capped.move(),Config.MOVE_CAP.get(),"Movement cap");
        close(h,capped.health(),Config.HEALTH_CAP.get(),"Health cap"); close(h,capped.trade(),Config.TRADE_CAP.get(),"Trade cap");
        close(h,capped.melee(),Config.DAMAGE_CAP.get(),"Melee cap"); close(h,capped.arrows(),.35,"Arrow cap");
        close(h,capped.potion(),.5,"Potion cap"); close(h,capped.crops(),.25,"Crop cap"); close(h,capped.enchant(),.35,"Enchant cap");
        close(h,all.getDouble("Growth reduction"),.4,"Growth cap"); close(h,all.getDouble("Tool repair chance"),.5,"Repair cap");
        close(h,capped.armor(),baseline.armor(),"Endurance must not change armor"); close(h,capped.recovery(),baseline.recovery(),"Endurance must not change recovery");
        h.succeed();
    }
    @GameTest(template="empty") public static void activityPersistenceAndReset(GameTestHelper h) {
        ServerPlayer p=player(h); PlayerData d=PlayerData.get(p);
        d.activities.put("Mining",200L); d.activities.put("Endurance",100L); d.activities.put("Breeding",150L);
        d.progression.awardOverall(600,150,1.5); d.bonusesDirty=true;
        CompoundTag saved=d.write(); PlayerData loaded=PlayerData.read(saved);
        h.assertTrue(saved.equals(loaded.write()),"Activity integration must preserve existing NBT format and values");
        close(h,Bonuses.of(loaded).mining(),.1,"Loaded existing counter grants activity bonus");
        h.assertTrue(SkillService.apply(p,0,1000000),"Existing purchased upgrade remains usable");
        close(h,Bonuses.of(d).mining(),.14,"Purchased and natural contributions combine");
        h.assertTrue(SkillService.apply(p,-1,1000000),"Existing reset still works");
        close(h,Bonuses.of(d).mining(),.1,"Reset retains natural bonus");
        h.assertTrue(d.activities.get("Mining")==200 && d.progression.available()==d.progression.earned(),"Reset preserves counters and refunds only purchased points");
        close(h,p.getAttributeValue(Attributes.MAX_HEALTH),21,"Endurance uses 20 base health, not purchased or modified health");
        float current=p.getHealth(); Bonuses.of(d).apply(p); Bonuses.of(d).apply(p);
        close(h,p.getAttributeValue(Attributes.MAX_HEALTH),21,"Repeated application cannot stack"); close(h,p.getHealth(),current,"Recalculation cannot heal");
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);
        Bonuses.of(PlayerData.get(p)).apply(p);
        close(h,p.getAttributeValue(Attributes.MAX_HEALTH),42,"Endurance follows actual changed base health");
        ServerPlayer fresh=player(h); Events.clone(new PlayerEvent.Clone(fresh,p,true));
        close(h,Bonuses.of(PlayerData.get(fresh)).mining(),.1,"Death clone retains activity effects");
        PlayerData.remove(p); PlayerData.remove(fresh); h.succeed();
    }
    @GameTest(template="empty") public static void milestoneCacheAndMiningClassification(GameTestHelper h) {
        ServerPlayer p=player(h); PlayerData d=PlayerData.get(p); d.activities.put("Mining",49L);
        Bonuses initial=Bonuses.of(d); d.bonusesDirty=false;
        d.award(p,0,"Mining",1);
        h.assertTrue(d.bonusesDirty,"49 to 50 must invalidate bonuses even without an attribute level-up");
        Bonuses milestone=Bonuses.of(d); close(h,milestone.mining(),.025,"50th action updates bonus");
        d.bonusesDirty=false; d.award(p,0,"Mining",1);
        h.assertTrue(!d.bonusesDirty && Bonuses.of(d)==milestone,"50 to 51 reuses cached gameplay bonuses");
        var stone=new PlayerEvent.BreakSpeed(p,Blocks.STONE.defaultBlockState(),10,null);
        var log=new PlayerEvent.BreakSpeed(p,Blocks.OAK_LOG.defaultBlockState(),10,null);
        var dirt=new PlayerEvent.BreakSpeed(p,Blocks.DIRT.defaultBlockState(),10,null);
        Events.speed(stone); Events.speed(log); Events.speed(dirt);
        close(h,stone.getNewSpeed(),10.25,"Mining increases appropriate block speed"); close(h,log.getNewSpeed(),10,"Mining cannot increase woodcutting"); close(h,dirt.getNewSpeed(),10,"Mining cannot increase unrelated digging");
        PlayerData.remove(p); h.succeed();
    }
}
