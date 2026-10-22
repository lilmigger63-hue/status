package dev.medievalstatus.core;

/** Pure, platform-independent progression and point accounting. */
public final class Progression {
    public static final int ATTRIBUTE_COUNT = 6, UPGRADE_COUNT = 18;
    private final Level overall = new Level();
    private final Level[] attributes = new Level[ATTRIBUTE_COUNT];
    private final int[] ranks = new int[UPGRADE_COUNT];
    private long earned, spent;

    public Progression() { for (int i = 0; i < attributes.length; i++) attributes[i] = new Level(); }
    public long level() { return overall.level(); }
    public Level overall() { return overall; }
    public Level attribute(int id) { return attributes[id]; }
    public long earned() { return earned; }
    public long spent() { return spent; }
    public long available() { return earned - spent; }
    public int rank(int id) { return id >= 0 && id < ranks.length ? ranks[id] : 0; }
    public void awardOverall(double amount, double base, double exponent) {
        long before = overall.level();
        overall.award(amount, base, exponent);
        earned = add(earned, multiply(overall.level() - before, 5));
    }
    public boolean purchase(int id, int cost, int maximum) {
        if (id < 0 || id >= ranks.length || cost < 1 || maximum < 1 || ranks[id] >= maximum || available() < cost) return false;
        ranks[id]++;
        spent += cost;
        return true;
    }
    public void reset() { java.util.Arrays.fill(ranks, 0); spent = 0; }
    public void restorePoints(long earned, long spent, int[] ranks) {
        this.earned = Math.max(0, earned);
        this.spent = Math.max(0, Math.min(this.earned, spent));
        for (int i = 0; i < this.ranks.length; i++) this.ranks[i] = i < ranks.length ? Math.max(0, Math.min(100, ranks[i])) : 0;
    }
    public static long add(long a, long b) { return b > Long.MAX_VALUE - a ? Long.MAX_VALUE : a + b; }
    private static long multiply(long a, long b) { return a > Long.MAX_VALUE / b ? Long.MAX_VALUE : a * b; }
    public static double required(long level, double base, double exponent) {
        return Math.max(1, Math.min(1e100, Math.floor(base * Math.pow(Math.max(1, level), exponent))));
    }
    public static double scale(long level, double cap) { return cap * (1 - Math.exp(-Math.max(0, level - 1) / 40.0)); }
    public static double activityBonus(long progress) { return (Math.max(0, progress) / 50L) * .025; }

    public static final class Level {
        private long level = 1;
        private double xp, lifetime;
        public long level() { return level; }
        public double xp() { return xp; }
        public double lifetime() { return lifetime; }
        public void award(double amount, double base, double exponent) {
            if (!Double.isFinite(amount) || amount <= 0) return;
            // Per-action input cap bounds work even with extreme server configuration.
            amount = Math.min(amount, 1_000_000);
            lifetime = Math.min(1e100, lifetime + amount);
            xp = Math.min(1e100, xp + amount);
            int advanced = 0;
            while (level < Long.MAX_VALUE && xp >= required(level, base, exponent) && advanced++ < 4096) {
                xp -= required(level, base, exponent);
                level++;
            }
        }
        public void restore(long level, double xp, double lifetime) {
            this.level = Math.max(1, level);
            this.xp = Double.isFinite(xp) ? Math.max(0, Math.min(1e100, xp)) : 0;
            this.lifetime = Double.isFinite(lifetime) ? Math.max(this.xp, Math.min(1e100, lifetime)) : this.xp;
        }
    }
}
