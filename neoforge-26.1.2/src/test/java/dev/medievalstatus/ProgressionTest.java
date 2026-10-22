package dev.medievalstatus;

import dev.medievalstatus.core.Progression;

/** Dependency-free regression suite; run with Gradle's progressionTest task. */
public final class ProgressionTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        Progression p = new Progression();
        check(p.level() == 1 && p.available() == 0, "fresh character");
        p.attribute(0).award(100, 100, 1.6);
        check(p.attribute(0).level() == 2, "attribute levels independently");
        check(p.attribute(1).level() == 1 && p.available() == 0, "attribute XP never grants points");
        p.awardOverall(150, 150, 1.5);
        check(p.level() == 2 && p.available() == 5, "exactly five points per level");
        p.awardOverall(Progression.required(2, 150, 1.5) + Progression.required(3, 150, 1.5), 150, 1.5);
        check(p.level() == 4 && p.earned() == 15, "multi-level award");
        check(!p.purchase(-1, 1, 5) && !p.purchase(99, 1, 5), "invalid IDs rejected");
        check(!p.purchase(0, 16, 5), "insufficient points rejected");
        check(p.purchase(0, 2, 1), "valid purchase");
        check(!p.purchase(0, 2, 1), "rank cap enforced");
        check(p.available() == 13 && p.spent() == 2, "point accounting");
        check(p.attribute(0).level() == 2, "purchase preserves attributes");
        p.reset();
        check(p.available() == 15 && p.spent() == 0 && p.rank(0) == 0, "reset refunds");
        check(p.level() == 4 && p.attribute(0).level() == 2, "reset preserves progression");
        p.awardOverall(Double.NaN, 150, 1.5);
        p.awardOverall(-1, 150, 1.5);
        check(p.level() == 4, "malformed XP ignored");
        check(Double.isFinite(Progression.required(Long.MAX_VALUE, 150, 1.5)), "overflow-safe curve");
        Progression.Level track = new Progression.Level();
        track.restore(-4, Double.NaN, -1);
        check(track.level() == 1 && track.xp() == 0, "corrupt saved values repaired");
        check(Progression.scale(Long.MAX_VALUE, .5) <= .5, "bounded derived bonus");
        long[] activityProgress={0,1,49,50,51,99,100,149,150,200,500,1000};
        double[] expected={0,0,0,.025,.025,.025,.05,.05,.075,.1,.25,.5};
        for(int i=0;i<activityProgress.length;i++)
            check(Math.abs(Progression.activityBonus(activityProgress[i])-expected[i])<1e-12,"activity milestone "+activityProgress[i]);
        check(Progression.activityBonus(-1)==0 && Progression.activityBonus(Long.MIN_VALUE)==0,"negative activity progress gives no bonus");
        check(Double.isFinite(Progression.activityBonus(Long.MAX_VALUE)),"large activity progress remains finite");
        System.out.println("Progression: " + checks + " checks passed");
    }
}
