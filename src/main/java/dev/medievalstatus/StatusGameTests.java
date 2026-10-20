package dev.medievalstatus;

import com.mojang.authlib.GameProfile;
import dev.medievalstatus.core.Progression;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder(MedievalStatus.ID)
@PrefixGameTestTemplate(false)
public final class StatusGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        return FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"StatusTest"));
    }
    @GameTest(template="empty") public static void nbtRoundTrip(GameTestHelper h) {
        PlayerData d=new PlayerData();
        d.progression.attribute(3).award(270,100,1.6);
        d.progression.awardOverall(900,150,1.5);
        d.progression.purchase(7,1,5); d.activities.put("Enchanting",12L); d.resetAt=123456;
        CompoundTag saved=d.write(); PlayerData loaded=PlayerData.read(saved);
        h.assertTrue(saved.equals(loaded.write()),"Versioned NBT must round-trip every persistent field");
        h.succeed();
    }
    @GameTest(template="empty") public static void deathClone(GameTestHelper h) {
        ServerPlayer original=player(h),fresh=player(h);
        PlayerData.get(original).progression.awardOverall(600,150,1.5);
        PlayerData.get(original).progression.attribute(1).award(200,100,1.6);
        CompoundTag expected=PlayerData.get(original).write();
        Events.clone(new PlayerEvent.Clone(fresh,original,true));
        h.assertTrue(expected.equals(PlayerData.get(fresh).write()),"Death must preserve levels, XP and point accounting");
        PlayerData.remove(original); PlayerData.remove(fresh); h.succeed();
    }
    @GameTest(template="empty") public static void purchaseResetAndModifiers(GameTestHelper h) {
        ServerPlayer p=player(h); PlayerData d=PlayerData.get(p);
        d.progression.awardOverall(600,150,1.5);
        double before=p.getAttributeValue(Attributes.MAX_HEALTH);
        h.assertTrue(SkillService.apply(p,Upgrade.IRON_BODY.id(),1000000),"Valid purchase should succeed");
        h.assertTrue(p.getAttributeValue(Attributes.MAX_HEALTH)>before,"Iron Body must really increase maximum health");
        double boosted=p.getAttributeValue(Attributes.MAX_HEALTH);
        Bonuses.of(d).apply(p); Bonuses.of(d).apply(p);
        h.assertTrue(p.getAttributeValue(Attributes.MAX_HEALTH)==boosted,"Repeated refresh must not stack duplicate modifiers");
        long earned=d.progression.earned(); long overall=d.progression.level();
        h.assertTrue(SkillService.apply(p,-1,1000000),"Reset should succeed");
        h.assertTrue(d.progression.available()==earned && d.progression.level()==overall,"Reset must refund points and preserve level");
        h.assertTrue(p.getAttributeValue(Attributes.MAX_HEALTH)==before,"Reset removes purchased bonuses");
        h.assertTrue(!SkillService.apply(p,-1,1000001),"Reset cooldown is enforced");
        PlayerData.remove(p); h.succeed();
    }
    @GameTest(template="empty") public static void invalidTransactions(GameTestHelper h) {
        ServerPlayer p=player(h);
        h.assertTrue(!SkillService.apply(p,Integer.MAX_VALUE,1000),"Invalid purchase ID must fail");
        h.assertTrue(!SkillService.apply(p,-100,1000),"Invalid negative ID must fail");
        h.assertTrue(!SkillService.apply(p,0,1000),"Insufficient points must fail");
        h.assertTrue(PlayerData.get(p).progression.available()==0,"Invalid requests cannot mutate balance");
        PlayerData.remove(p); h.succeed();
    }
    @GameTest(template="empty") public static void sixRewardTracks(GameTestHelper h) {
        ServerPlayer p=player(h); PlayerData d=PlayerData.get(p);
        String[] actions={"Mining","Exploration","Endurance","Enchanting","Farming","Trading"};
        for(int i=0;i<6;i++) { d.award(p,i,actions[i],1); h.assertTrue(d.progression.attribute(i).lifetime()>0,"Attribute "+i+" must receive XP"); }
        h.assertTrue(d.progression.overall().lifetime()>0,"Meaningful actions also award independent overall XP");
        PlayerData.remove(p); h.succeed();
    }
    @GameTest(template="empty") public static void boundedBonuses(GameTestHelper h) {
        PlayerData d=new PlayerData(); for(int i=0;i<6;i++) d.progression.attribute(i).restore(Long.MAX_VALUE,0,0);
        Bonuses b=Bonuses.of(d);
        h.assertTrue(Double.isFinite(b.mining()) && b.mining()<=Config.MINING_CAP.get(),"Mining cap");
        h.assertTrue(b.health()<=Config.HEALTH_CAP.get() && b.move()<=Config.MOVE_CAP.get(),"Health and speed caps");
        h.assertTrue(b.trade()<=Config.TRADE_CAP.get(),"Trade cap"); h.succeed();
    }
    @GameTest(template="empty") public static void plantedCropRewards(GameTestHelper h) {
        ServerPlayer p=player(h); var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,1,1));
        var state=net.minecraft.world.level.block.Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE,7);
        h.getLevel().setBlock(pos,state,2); PlacedBlocks.get(h.getLevel()).mark(pos);
        Events.broken(new net.minecraftforge.event.level.BlockEvent.BreakEvent(h.getLevel(),pos,state,p));
        h.assertTrue(PlayerData.get(p).progression.attribute(4).xp()==0,"Attempting a break cannot award XP");
        h.getLevel().setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        Events.confirmBreaks(p);
        h.assertTrue(PlayerData.get(p).progression.attribute(4).xp()>0,"Player-planted mature crops must award Farming XP");
        PlayerData.remove(p); h.succeed();
    }
    @GameTest(template="empty") public static void failedBreakRetainsProtection(GameTestHelper h) {
        ServerPlayer p=player(h); var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,1,1));
        var state=net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        h.getLevel().setBlock(pos,state,2); PlacedBlocks.get(h.getLevel()).mark(pos);
        Events.broken(new net.minecraftforge.event.level.BlockEvent.BreakEvent(h.getLevel(),pos,state,p));
        Events.confirmBreaks(p);
        h.assertTrue(PlayerData.get(p).progression.attribute(0).xp()==0,"Unremoved blocks cannot award XP");
        h.assertTrue(PlacedBlocks.get(h.getLevel()).contains(pos),"Failed break must retain provenance");
        h.getLevel().setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        PlacedBlocks.get(h.getLevel()).consume(pos);
        PlayerData.remove(p); h.succeed();
    }
    @GameTest(template="empty") public static void shiftClickBrewingRewardOnce(GameTestHelper h) {
        ServerPlayer p=player(h); var container=new net.minecraft.world.SimpleContainer(5);
        var stacks=net.minecraft.core.NonNullList.withSize(5,net.minecraft.world.item.ItemStack.EMPTY);
        stacks.set(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.POTION));
        Events.brewed(new net.minecraftforge.event.brewing.PotionBrewEvent.Post(stacks));
        container.setItem(0,stacks.get(0));
        var menu=new net.minecraft.world.inventory.BrewingStandMenu(1,p.getInventory(),container,new net.minecraft.world.inventory.SimpleContainerData(2));
        menu.quickMoveStack(p,0);
        double xp=PlayerData.get(p).progression.attribute(3).lifetime();
        h.assertTrue(xp>0,"Shift-clicked freshly brewed potion must reward Intelligence XP");
        PlayerData.get(p).cooldowns.clear();
        net.minecraft.world.item.ItemStack potion=net.minecraft.world.item.ItemStack.EMPTY;
        for(int i=0;i<p.getInventory().getContainerSize();i++) if(p.getInventory().getItem(i).is(net.minecraft.world.item.Items.POTION)) { potion=p.getInventory().removeItemNoUpdate(i); break; }
        container.setItem(0,potion); menu.quickMoveStack(p,0);
        h.assertTrue(PlayerData.get(p).progression.attribute(3).lifetime()==xp,"Reinserting a brewed potion cannot redeem it twice");
        PlayerData.remove(p); h.succeed();
    }
    @GameTest(template="empty") public static void merchantClosePreservesVanillaReset(GameTestHelper h) {
        ServerPlayer p=player(h); PlayerData d=PlayerData.get(p);
        var offer=new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD,10),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD),10,2,.05f);
        offer.setSpecialPriceDiff(0);
        d.tradeAdjustments.put(offer,new PlayerData.PriceChange(2,0));
        Events.closed(new net.minecraftforge.event.entity.player.PlayerContainerEvent.Close(p,new net.minecraft.world.inventory.MerchantMenu(1,p.getInventory())));
        h.assertTrue(offer.getSpecialPriceDiff()==0,"Closing may not resurrect prices already reset by vanilla");
        PlayerData.remove(p); h.succeed();
    }
    @GameTest(template="empty") public static void resetRevalidatesTradePayment(GameTestHelper h) {
        ServerPlayer p=player(h); PlayerData d=PlayerData.get(p); d.progression.awardOverall(600,150,1.5);
        var villager=net.minecraft.world.entity.EntityType.VILLAGER.create(h.getLevel());
        var offers=new net.minecraft.world.item.trading.MerchantOffers();
        offers.add(new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD,50),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD),10,2,.05f));
        villager.getOffers().clear(); villager.getOffers().addAll(offers); villager.setTradingPlayer(p);
        var menu=new net.minecraft.world.inventory.MerchantMenu(1,p.getInventory(),villager); p.containerMenu=menu;
        h.assertTrue(SkillService.apply(p,Upgrade.MERCHANT.id(),1000000),"Merchant purchase should succeed");
        menu.getSlot(0).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD,49));
        menu.slotsChanged(p.getInventory());
        h.assertTrue(!menu.getSlot(2).getItem().isEmpty(),"Discounted payment must initially buy bread; price="+offers.get(0).getCostA().getCount()+", diff="+offers.get(0).getSpecialPriceDiff()+", bonus="+Bonuses.of(d).trade()+", offers="+menu.getOffers().size()+", input="+menu.getSlot(0).getItem());
        h.assertTrue(SkillService.apply(p,-1,1000000),"Reset should succeed");
        h.assertTrue(menu.getSlot(2).getItem().isEmpty(),"Changing prices must clear output when old payment is insufficient");
        p.containerMenu=p.inventoryMenu; villager.setTradingPlayer(null); PlayerData.remove(p); h.succeed();
    }
}
