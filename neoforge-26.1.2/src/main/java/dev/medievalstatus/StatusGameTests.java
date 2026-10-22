package dev.medievalstatus;

import com.mojang.authlib.GameProfile;
import dev.medievalstatus.core.Progression;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import java.util.UUID;

public final class StatusGameTests {
    private static java.util.Map<String, java.util.function.Consumer<GameTestHelper>> tests() {
        var tests = new java.util.LinkedHashMap<String, java.util.function.Consumer<GameTestHelper>>();
        tests.put("nbtroundtrip", StatusGameTests::nbtRoundTrip);
        tests.put("legacynbtdefaults", StatusGameTests::legacyNbtDefaults);
        tests.put("legacyunversionednbt", StatusGameTests::legacyUnversionedNbt);
        tests.put("malformednbtclamps", StatusGameTests::malformedNbtClamps);
        tests.put("legacynumericanduuidnbt", StatusGameTests::legacyNumericAndUuidNbt);
        tests.put("legacyforgeplayerenvelope", StatusGameTests::legacyForgePlayerEnvelope);
        tests.put("legacyforgeentityloadinjection", StatusGameTests::legacyForgeEntityLoadInjection);
        tests.put("deathclone", StatusGameTests::deathClone);
        tests.put("purchaseresetandmodifiers", StatusGameTests::purchaseResetAndModifiers);
        tests.put("invalidtransactions", StatusGameTests::invalidTransactions);
        tests.put("sixrewardtracks", StatusGameTests::sixRewardTracks);
        tests.put("boundedbonuses", StatusGameTests::boundedBonuses);
        tests.put("plantedcroprewards", StatusGameTests::plantedCropRewards);
        tests.put("failedbreakretainsprotection", StatusGameTests::failedBreakRetainsProtection);
        tests.put("shiftclickbrewingrewardonce", StatusGameTests::shiftClickBrewingRewardOnce);
        tests.put("merchantclosepreservesvanillareset", StatusGameTests::merchantClosePreservesVanillaReset);
        tests.put("resetrevalidatestradepayment", StatusGameTests::resetRevalidatesTradePayment);
        return tests;
    }
    public static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> tests().forEach((name, test) ->
            registry.register(Identifier.fromNamespaceAndPath(MedievalStatus.ID, name), test)));
    }
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(Identifier.fromNamespaceAndPath(MedievalStatus.ID, "statusgametests"), new TestEnvironmentDefinition.AllOf());
        tests().forEach((name, test) -> {
            Identifier id = Identifier.fromNamespaceAndPath(MedievalStatus.ID, name);
            event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id),
                new TestData<>(environment, Identifier.fromNamespaceAndPath(MedievalStatus.ID, "empty"), 100, 0, true)));
        });
    }
    private static ServerPlayer player(GameTestHelper h) {
        return FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"StatusTest"));
    }
    public static void legacyNbtDefaults(GameTestHelper h) {
        PlayerData loaded=PlayerData.read(new CompoundTag());
        h.assertTrue(loaded.write().equals(new PlayerData().write()),"Missing legacy fields migrate to unchanged fresh defaults");
        CompoundTag wrongTypes=new CompoundTag();
        wrongTypes.putString("overall","invalid"); wrongTypes.putString("earned","invalid");
        wrongTypes.putString("ranks","invalid"); wrongTypes.putString("activities","invalid");
        h.assertTrue(PlayerData.read(wrongTypes).write().equals(new PlayerData().write()),"Wrong NBT types use Forge-compatible zero and empty defaults");
        h.succeed();
    }
    public static void legacyForgePlayerEnvelope(GameTestHelper h) {
        PlayerData original=new PlayerData();original.progression.awardOverall(900,150,1.5);
        original.progression.attribute(2).award(270,100,1.6);original.progression.purchase(7,1,5);
        original.activities.put("Mining",150L);original.resetAt=123456;
        CompoundTag payload=original.write(),forgeData=new CompoundTag(),savedPlayer=new CompoundTag(),destination=new CompoundTag();
        forgeData.put(PlayerData.KEY,payload);savedPlayer.put("ForgeData",forgeData);
        CompoundTag untouched=savedPlayer.copy();
        h.assertTrue(LegacyPlayerData.importTag(destination,savedPlayer),"Original ForgeData.medievalstatus envelope imports");
        h.assertTrue(Nbt.compound(destination,PlayerData.KEY).equals(payload),"Every original payload field copies exactly");
        h.assertTrue(savedPlayer.equals(untouched),"Import leaves source envelope unchanged");
        Nbt.compound(destination,PlayerData.KEY).putLong("resetAt",999);
        h.assertTrue(savedPlayer.equals(untouched),"Imported payload does not alias source compounds");
        CompoundTag expected=destination.copy();
        h.assertTrue(!LegacyPlayerData.importTag(destination,savedPlayer) && destination.equals(expected),"Existing modern payload is never overwritten");
        CompoundTag mistyped=new CompoundTag();mistyped.putString(PlayerData.KEY,"already present");
        h.assertTrue(!LegacyPlayerData.importTag(mistyped,savedPlayer),"Any existing destination key prevents legacy overwrite");
        h.assertTrue(!LegacyPlayerData.importTag(new CompoundTag(),new CompoundTag()),"Missing original envelope does not fabricate saved progression");
        h.succeed();
    }
    public static void legacyForgeEntityLoadInjection(GameTestHelper h) {
        CompoundTag original=new PlayerData().write();original.putLong("resetAt",123456);
        CompoundTag forge=new CompoundTag();forge.put(PlayerData.KEY,original);
        forge.putDouble("medievalstatus_damage",12.5);forge.putDouble("medievalstatus_endured",7.25);
        forge.putString("unrelated_other_mod","leave this in ForgeData");
        CompoundTag neo=new CompoundTag();neo.putString("modern_sentinel","preserve");
        CompoundTag saved=new CompoundTag();saved.put("ForgeData",forge);saved.put("NeoForgeData",neo);
        CompoundTag untouched=saved.copy();
        var entity=net.minecraft.world.entity.EntityType.ZOMBIE.create(h.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        h.assertTrue(entity!=null,"Migration fixture entity is created");
        entity.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        CompoundTag loaded=entity.getPersistentData();
        h.assertTrue(Nbt.compound(loaded,PlayerData.KEY).equals(original),"Injected Entity.load imports original Forge progression exactly");
        h.assertTrue(Nbt.decimal(loaded,"medievalstatus_damage")==12.5 && Nbt.decimal(loaded,"medievalstatus_endured")==7.25,"Injected Entity.load imports original primitive combat allowance tags");
        h.assertTrue(Nbt.string(loaded,"modern_sentinel").equals("preserve") && !loaded.contains("unrelated_other_mod"),"Injection preserves modern data and imports only Medieval Status keys");
        Nbt.compound(loaded,PlayerData.KEY).putLong("resetAt",999);
        h.assertTrue(saved.equals(untouched),"Entity load and later mutation leave original saved tag unchanged");

        CompoundTag modernPayload=new CompoundTag();modernPayload.putLong("resetAt",987654);
        CompoundTag authoritative=new CompoundTag();authoritative.put(PlayerData.KEY,modernPayload);
        authoritative.putDouble("medievalstatus_damage",42);authoritative.putDouble("medievalstatus_endured",19);
        CompoundTag both=new CompoundTag();both.put("ForgeData",forge);both.put("NeoForgeData",authoritative);
        CompoundTag bothUntouched=both.copy();
        var existing=net.minecraft.world.entity.EntityType.ZOMBIE.create(h.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        h.assertTrue(existing!=null,"Existing-data fixture entity is created");
        existing.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),both));
        h.assertTrue(existing.getPersistentData().equals(authoritative),"Injected Entity.load never overwrites existing NeoForge progression or combat counters");
        h.assertTrue(both.equals(bothUntouched),"Preserving existing modern values leaves both input envelopes unchanged");
        h.succeed();
    }
    public static void legacyNumericAndUuidNbt(GameTestHelper h) {
        CompoundTag saved=new CompoundTag();saved.putInt("numeric",37);
        h.assertTrue(Nbt.longValue(saved,"numeric")==37 && Nbt.decimal(saved,"numeric")==37,"Legacy numeric NBT converts int to long/double");
        saved.putDouble("numeric",37.75);
        h.assertTrue(Nbt.integer(saved,"numeric")==37 && Nbt.longValue(saved,"numeric")==37,"Legacy numeric NBT truncates decimal to integers");
        UUID expected=UUID.fromString("12345678-9abc-def0-8123-456789abcdef");
        saved.putIntArray("medievalstatus_brew_token",new int[]{0x12345678,0x9abcdef0,0x81234567,0x89abcdef});
        h.assertTrue(Nbt.hasUuid(saved,"medievalstatus_brew_token") && expected.equals(Nbt.uuid(saved,"medievalstatus_brew_token")),"Legacy four-int brewing UUID retains signed high/low bits");
        CompoundTag rewritten=new CompoundTag();Nbt.putUuid(rewritten,"medievalstatus_brew_token",expected);
        h.assertTrue(java.util.Arrays.equals(Nbt.intArray(saved,"medievalstatus_brew_token"),Nbt.intArray(rewritten,"medievalstatus_brew_token")),"UUID compatibility writer retains legacy array representation");
        saved.putIntArray("medievalstatus_brew_token",new int[]{1,2,3});
        h.assertTrue(!Nbt.hasUuid(saved,"medievalstatus_brew_token"),"Malformed legacy UUID array is not accepted");
        h.succeed();
    }
    public static void legacyUnversionedNbt(GameTestHelper h) {
        PlayerData previous=new PlayerData(); previous.progression.awardOverall(900,150,1.5);
        previous.progression.attribute(3).award(270,100,1.6); previous.progression.purchase(7,1,5);
        previous.activities.put("Mining",200L);previous.resetAt=123456;
        CompoundTag saved=previous.write();saved.remove("version");
        PlayerData loaded=PlayerData.read(saved);
        h.assertTrue(previous.write().equals(loaded.write()),"Unversioned NBT retains level, XP, ranks, points, activities and reset deadline");
        h.succeed();
    }
    public static void malformedNbtClamps(GameTestHelper h) {
        CompoundTag root=new CompoundTag(),overall=new CompoundTag(),activities=new CompoundTag();
        overall.putLong("level",-4);overall.putDouble("xp",Double.NaN);overall.putDouble("lifetime",Double.POSITIVE_INFINITY);
        root.put("overall",overall);root.putLong("earned",15);root.putLong("spent",99);
        root.putIntArray("ranks",new int[]{-1,101,5});root.putLong("resetAt",-1);
        activities.putLong("Mining",-5);activities.putLong("Farming",50);activities.putLong("UnknownLegacyActivity",100);
        root.put("activities",activities);PlayerData loaded=PlayerData.read(root);
        h.assertTrue(loaded.progression.level()==1 && loaded.progression.overall().xp()==0 && loaded.progression.overall().lifetime()==0,"Malformed saved progression keeps original repair rules");
        h.assertTrue(loaded.progression.earned()==15 && loaded.progression.spent()==15 && loaded.progression.available()==0,"Spent points clamp to earned points");
        h.assertTrue(loaded.progression.rank(0)==0 && loaded.progression.rank(1)==100 && loaded.progression.rank(2)==5 && loaded.progression.rank(17)==0,"Saved ranks clamp and missing ranks become zero");
        h.assertTrue(loaded.resetAt==0 && loaded.activities.get("Mining")==0 && loaded.activities.get("Farming")==50 && !loaded.activities.containsKey("UnknownLegacyActivity"),"Reset and activity migration keep original clamps and recognized names");
        h.succeed();
    }
    public static void nbtRoundTrip(GameTestHelper h) {
        PlayerData d=new PlayerData();
        d.progression.attribute(3).award(270,100,1.6);
        d.progression.awardOverall(900,150,1.5);
        d.progression.purchase(7,1,5); d.activities.put("Enchanting",12L); d.resetAt=123456;
        CompoundTag saved=d.write(); PlayerData loaded=PlayerData.read(saved);
        h.assertTrue(saved.equals(loaded.write()),"Versioned NBT must round-trip every persistent field");
        h.succeed();
    }
    public static void deathClone(GameTestHelper h) {
        ServerPlayer original=player(h),fresh=player(h);
        PlayerData.get(original).progression.awardOverall(600,150,1.5);
        PlayerData.get(original).progression.attribute(1).award(200,100,1.6);
        CompoundTag expected=PlayerData.get(original).write();
        Events.clone(new PlayerEvent.Clone(fresh,original,true));
        h.assertTrue(expected.equals(PlayerData.get(fresh).write()),"Death must preserve levels, XP and point accounting");
        PlayerData.remove(original); PlayerData.remove(fresh); h.succeed();
    }
    public static void purchaseResetAndModifiers(GameTestHelper h) {
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
    public static void invalidTransactions(GameTestHelper h) {
        ServerPlayer p=player(h);
        h.assertTrue(!SkillService.apply(p,Integer.MAX_VALUE,1000),"Invalid purchase ID must fail");
        h.assertTrue(!SkillService.apply(p,-100,1000),"Invalid negative ID must fail");
        h.assertTrue(!SkillService.apply(p,0,1000),"Insufficient points must fail");
        h.assertTrue(PlayerData.get(p).progression.available()==0,"Invalid requests cannot mutate balance");
        PlayerData.remove(p); h.succeed();
    }
    public static void sixRewardTracks(GameTestHelper h) {
        ServerPlayer p=player(h); PlayerData d=PlayerData.get(p);
        String[] actions={"Mining","Exploration","Endurance","Enchanting","Farming","Trading"};
        for(int i=0;i<6;i++) { d.award(p,i,actions[i],1); h.assertTrue(d.progression.attribute(i).lifetime()>0,"Attribute "+i+" must receive XP"); }
        h.assertTrue(d.progression.overall().lifetime()>0,"Meaningful actions also award independent overall XP");
        PlayerData.remove(p); h.succeed();
    }
    public static void boundedBonuses(GameTestHelper h) {
        PlayerData d=new PlayerData(); for(int i=0;i<6;i++) d.progression.attribute(i).restore(Long.MAX_VALUE,0,0);
        Bonuses b=Bonuses.of(d);
        h.assertTrue(Double.isFinite(b.mining()) && b.mining()<=Config.MINING_CAP.get(),"Mining cap");
        h.assertTrue(b.health()<=Config.HEALTH_CAP.get() && b.move()<=Config.MOVE_CAP.get(),"Health and speed caps");
        h.assertTrue(b.trade()<=Config.TRADE_CAP.get(),"Trade cap"); h.succeed();
    }
    public static void plantedCropRewards(GameTestHelper h) {
        ServerPlayer p=player(h); var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,1,1));
        var state=net.minecraft.world.level.block.Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE,7);
        h.getLevel().setBlock(pos,state,2); PlacedBlocks.get(h.getLevel()).mark(pos);
        Events.broken(new net.neoforged.neoforge.event.level.block.BreakBlockEvent(h.getLevel(),pos,state,p));
        h.assertTrue(PlayerData.get(p).progression.attribute(4).xp()==0,"Attempting a break cannot award XP");
        h.getLevel().setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        Events.confirmBreaks(p);
        h.assertTrue(PlayerData.get(p).progression.attribute(4).xp()>0,"Player-planted mature crops must award Farming XP");
        PlayerData.remove(p); h.succeed();
    }
    public static void failedBreakRetainsProtection(GameTestHelper h) {
        ServerPlayer p=player(h); var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,1,1));
        var state=net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        h.getLevel().setBlock(pos,state,2); PlacedBlocks.get(h.getLevel()).mark(pos);
        Events.broken(new net.neoforged.neoforge.event.level.block.BreakBlockEvent(h.getLevel(),pos,state,p));
        Events.confirmBreaks(p);
        h.assertTrue(PlayerData.get(p).progression.attribute(0).xp()==0,"Unremoved blocks cannot award XP");
        h.assertTrue(PlacedBlocks.get(h.getLevel()).contains(pos),"Failed break must retain provenance");
        h.getLevel().setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        PlacedBlocks.get(h.getLevel()).consume(pos);
        PlayerData.remove(p); h.succeed();
    }
    public static void shiftClickBrewingRewardOnce(GameTestHelper h) {
        ServerPlayer p=player(h); var container=new net.minecraft.world.SimpleContainer(5);
        var stacks=net.minecraft.core.NonNullList.withSize(5,net.minecraft.world.item.ItemStack.EMPTY);
        stacks.set(0,net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION,net.minecraft.world.item.alchemy.Potions.AWKWARD));
        Events.brewed(new net.neoforged.neoforge.event.brewing.PotionBrewEvent.Post(stacks));
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
    public static void merchantClosePreservesVanillaReset(GameTestHelper h) {
        ServerPlayer p=player(h); PlayerData d=PlayerData.get(p);
        var offer=new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(net.minecraft.world.item.Items.EMERALD,10),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD),10,2,.05f);
        offer.setSpecialPriceDiff(0);
        d.tradeAdjustments.put(offer,new PlayerData.PriceChange(2,0));
        Events.closed(new net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Close(p,new net.minecraft.world.inventory.MerchantMenu(1,p.getInventory())));
        h.assertTrue(offer.getSpecialPriceDiff()==0,"Closing may not resurrect prices already reset by vanilla");
        PlayerData.remove(p); h.succeed();
    }
    public static void resetRevalidatesTradePayment(GameTestHelper h) {
        ServerPlayer p=player(h); PlayerData d=PlayerData.get(p); d.progression.awardOverall(600,150,1.5);
        var villager=net.minecraft.world.entity.EntityType.VILLAGER.create(h.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        var offers=new net.minecraft.world.item.trading.MerchantOffers();
        offers.add(new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(net.minecraft.world.item.Items.EMERALD,50),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD),10,2,.05f));
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
