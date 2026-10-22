package dev.medievalstatus;

import java.util.*;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public static final ModConfigSpec SERVER, CLIENT;
    public static final ModConfigSpec.DoubleValue ATTRIBUTE_BASE, ATTRIBUTE_POWER, OVERALL_BASE, OVERALL_POWER,
        ATTRIBUTE_MULTIPLIER, OVERALL_MULTIPLIER, REWARD_MULTIPLIER, EFFECTIVENESS;
    public static final ModConfigSpec.DoubleValue HEALTH_CAP, MOVE_CAP, MINING_CAP, DAMAGE_CAP, TRADE_CAP, ARMOR_CAP;
    public static final ModConfigSpec.IntValue MOVEMENT_INTERVAL, ACTION_COOLDOWN, RESET_COOLDOWN, RESET_COST;
    public static final ModConfigSpec.BooleanValue RESET_ENABLED;
    public static final Map<Upgrade, ModConfigSpec.IntValue> COSTS = new EnumMap<>(Upgrade.class), RANKS = new EnumMap<>(Upgrade.class);
    public static final Map<Upgrade, ModConfigSpec.DoubleValue> EFFECTS = new EnumMap<>(Upgrade.class);
    public static final Map<Upgrade, ModConfigSpec.BooleanValue> ENABLED = new EnumMap<>(Upgrade.class);
    public static final Map<String, ModConfigSpec.DoubleValue> REWARDS = new LinkedHashMap<>();
    public static final List<ModConfigSpec.DoubleValue> ATTRIBUTE_MULTIPLIERS=new ArrayList<>();
    public static final ModConfigSpec.DoubleValue UI_SCALE, ANIMATION;
    public static final ModConfigSpec.BooleanValue SHOW_ACTIVITIES;
    public static final ModConfigSpec.ConfigValue<String> GOLD, STONE;
    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("progression");
        ATTRIBUTE_BASE = b.defineInRange("attributeBase", 100d, 1d, 100000d);
        ATTRIBUTE_POWER = b.defineInRange("attributeExponent", 1.6, 1d, 4d);
        OVERALL_BASE = b.defineInRange("overallBase", 150d, 1d, 100000d);
        OVERALL_POWER = b.defineInRange("overallExponent", 1.5, 1d, 4d);
        ATTRIBUTE_MULTIPLIER = b.defineInRange("attributeMultiplier", 1d, 0d, 100d);
        OVERALL_MULTIPLIER = b.defineInRange("overallMultiplier", 1d, 0d, 100d);
        REWARD_MULTIPLIER = b.defineInRange("rewardMultiplier", 1d, 0d, 100d);
        for(String name:new String[]{"Strength","Dexterity","Constitution","Intelligence","Wisdom","Charisma"})
            ATTRIBUTE_MULTIPLIERS.add(b.defineInRange(name+"Multiplier",1d,0d,100d));
        b.comment("Exactly five points per character level is a fixed progression invariant.");
        b.pop().push("rewards");
        String[] activities = {"Mining", "Woodcutting", "Farming", "Exploration", "Archery", "Endurance", "Enchanting", "Brewing", "Trading", "Melee", "Breeding", "Crafting", "Recovery"};
        double[] rewards = {2,4,5,2,6,4,18,10,8,5,10,1,1};
        for (int i = 0; i < activities.length; i++) REWARDS.put(activities[i], b.defineInRange(activities[i], rewards[i], 0d, 1000d));
        b.pop().push("limits");
        MOVEMENT_INTERVAL = b.defineInRange("movementSampleTicks", 40, 20, 200);
        ACTION_COOLDOWN = b.defineInRange("activityCooldownTicks", 20, 1, 1200);
        HEALTH_CAP = b.defineInRange("maxBonusHealth", 20d, 0d, 100d);
        MOVE_CAP = b.defineInRange("maxMovementFraction", .25, 0d, .5);
        MINING_CAP = b.defineInRange("maxMiningFraction", 1d, 0d, 5d);
        DAMAGE_CAP = b.defineInRange("maxMeleeFraction", .35, 0d, 1d);
        TRADE_CAP = b.defineInRange("maxTradeDiscount", .25, 0d, .5);
        ARMOR_CAP = b.defineInRange("maxBonusArmor", 5d, 0d, 10d);
        EFFECTIVENESS = b.defineInRange("attributeEffectiveness", 1d, 0d, 5d);
        b.pop().push("reset");
        RESET_ENABLED = b.define("enabled", true);
        RESET_COOLDOWN = b.defineInRange("cooldownSeconds", 300, 0, 86400);
        RESET_COST = b.comment("Vanilla experience levels consumed by a confirmed reset.").defineInRange("vanillaLevelCost", 0, 0, 100);
        b.pop().push("upgrades");
        for (Upgrade u : Upgrade.values()) {
            b.push(u.name().toLowerCase(Locale.ROOT));
            COSTS.put(u, b.defineInRange("cost", 1, 1, 100));
            RANKS.put(u, b.defineInRange("maxRank", 5, 1, 100));
            EFFECTS.put(u, b.defineInRange("effectPerRank", u.perRank, 0d, 10d));
            ENABLED.put(u, b.define("enabled", true));
            b.pop();
        }
        b.pop(); SERVER = b.build();
        ModConfigSpec.Builder c = new ModConfigSpec.Builder();
        UI_SCALE = c.defineInRange("interfaceScale", 1d, .5, 2d);
        ANIMATION = c.defineInRange("animationIntensity", .4, 0d, 1d);
        SHOW_ACTIVITIES = c.define("showActivities", true);
        GOLD = c.define("goldColor", "C6A665");
        STONE = c.define("stoneColor", "171A1D");
        CLIENT = c.build();
    }
    private Config() {}
}
