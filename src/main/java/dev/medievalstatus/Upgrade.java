package dev.medievalstatus;

public enum Upgrade {
    EFFICIENT_MINER("Efficient Miner", "Physical", "Mining speed", .04),
    LUMBERJACK("Lumberjack", "Physical", "Woodcutting speed", .08),
    HEAVY_LIFTER("Heavy Lifter", "Physical", "Melee damage", .02),
    SURE_GRIP("Sure Grip", "Physical", "Underwater / airborne mining speed", .12),
    LIGHT_FOOTED("Light Footed", "Mobility", "Movement speed", .01),
    ENDURING_RUNNER("Enduring Runner", "Mobility", "Sprinting exhaustion recovered per second", .02),
    STEADY_AIM("Steady Aim", "Mobility", "Arrow damage", .025),
    IRON_BODY("Iron Body", "Survival", "Maximum health (hearts)", 1),
    NATURAL_RECOVERY("Natural Recovery", "Survival", "Health restored per recovery pulse", .2),
    TOUGH_SKIN("Tough Skin", "Survival", "Armor points", .5),
    EXPERIENCED_ENCHANTER("Experienced Enchanter", "Knowledge", "Chance to refund one paid enchantment level", .04),
    EXPERIENCED_BREWER("Experienced Brewer", "Knowledge", "Drunk potion duration", .05),
    CAREFUL_CRAFTER("Careful Crafter", "Knowledge", "Chance to repair one point of held tool wear after crafting", .10),
    GREEN_THUMB("Green Thumb", "Nature", "Chance for one additional crop produce", .025),
    EXPERIENCED_FARMER("Experienced Farmer", "Nature", "Crop harvesting tool speed", .10),
    ANIMAL_CARETAKER("Animal Caretaker", "Nature", "Newborn growth time reduction", .05),
    MERCHANT("Merchant", "Social", "Additional trade discount", .02),
    NEGOTIATOR("Negotiator", "Social", "Chance for one bonus vanilla XP after trading", .04);

    public final String title, category, effect;
    public final double perRank;
    Upgrade(String title, String category, String effect, double perRank) {
        this.title = title; this.category = category; this.effect = effect; this.perRank = perRank;
    }
    public int id() { return ordinal(); }
}
