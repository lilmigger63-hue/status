package dev.medievalstatus;

import net.minecraft.server.level.ServerPlayer;

/** Shared server-side transaction boundary; never accepts client balances or ranks. */
public final class SkillService {
    public static boolean apply(ServerPlayer p,int operation,long nowMillis) {
        if (p.isCreative() || p.isSpectator() || !p.isAlive()) return false;
        PlayerData d=PlayerData.get(p);
        boolean changed;
        if (operation==-1) {
            if (!Config.RESET_ENABLED.get() || nowMillis<d.resetAt || p.experienceLevel<Config.RESET_COST.get()) return false;
            p.giveExperienceLevels(-Config.RESET_COST.get());
            d.progression.reset(); d.resetAt=nowMillis+Config.RESET_COOLDOWN.get()*1000L; changed=true;
        } else if (operation>=0 && operation<Upgrade.values().length) {
            Upgrade u=Upgrade.values()[operation];
            changed=Config.ENABLED.get(u).get() && d.progression.purchase(u.id(),Config.COSTS.get(u).get(),Config.RANKS.get(u).get());
        } else return false;
        if (changed) {
            d.bonusesDirty=true; Bonuses.of(d).apply(p); d.bonusesDirty=false;
            if (p.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu menu) Events.refreshTrades(p,menu);
            d.dirty=true; PlayerData.save(p);
        }
        return changed;
    }
    private SkillService() {}
}
