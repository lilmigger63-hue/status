package dev.medievalstatus;

import dev.medievalstatus.core.Progression;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Random;

/** Differential regression suite: the reference is loaded from the immutable released JAR. */
public final class OriginalJarParityTest {
    private static final String BASELINE_SHA256 = "777589dff5eb83bd9f57a9bdfbf21e0c31f0b4fec384826620872ac8023eb855";
    private static int checks;
    private static void same(Object expected, Object actual, String label) {
        checks++;
        if (!expected.equals(actual)) throw new AssertionError(label + ": original=" + expected + ", target=" + actual);
    }
    private static Object invoke(Object instance, String name, Class<?>[] types, Object... args) throws Exception {
        return instance.getClass().getMethod(name, types).invoke(instance, args);
    }
    private static Object get(Object instance, String name) throws Exception {
        return invoke(instance, name, new Class<?>[0]);
    }
    private static Object attribute(Object instance, int id) throws Exception {
        return invoke(instance, "attribute", new Class<?>[]{int.class}, id);
    }
    private static void level(Object original, Progression.Level target, String label) throws Exception {
        same(get(original,"level"),target.level(),label+" level");
        same(get(original,"xp"),target.xp(),label+" XP bits");
        same(get(original,"lifetime"),target.lifetime(),label+" lifetime bits");
    }
    private static void state(Object original, Progression target, String label) throws Exception {
        for (String name : new String[]{"level","earned","spent","available"}) {
            Object actual = switch(name) {case "level" -> target.level(); case "earned" -> target.earned(); case "spent" -> target.spent(); default -> target.available();};
            same(get(original,name),actual,label+" "+name);
        }
        level(get(original,"overall"),target.overall(),label+" overall");
        for(int i=0;i<6;i++) level(attribute(original,i),target.attribute(i),label+" attribute "+i);
        for(int i=-1;i<=18;i++) same(invoke(original,"rank",new Class<?>[]{int.class},i),target.rank(i),label+" rank "+i);
    }
    private static void award(Object original, Progression target, double amount, double base, double power) throws Exception {
        invoke(original,"awardOverall",new Class<?>[]{double.class,double.class,double.class},amount,base,power);
        target.awardOverall(amount,base,power);
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=1) throw new IllegalArgumentException("Usage: OriginalJarParityTest <immutable-original.jar>");
        Path jar=Path.of(args[0]).toAbsolutePath();
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar)));
        same(BASELINE_SHA256,hash,"Pinned original JAR SHA-256");
        // A null parent prevents falling back to the target application class loader.
        try(URLClassLoader loader=new URLClassLoader(new java.net.URL[]{jar.toUri().toURL()},null)) {
            Class<?> originalClass=Class.forName("dev.medievalstatus.core.Progression",true,loader);
            same(loader,originalClass.getClassLoader(),"Reference class loader isolation");
            same(jar.toUri().toURL(),originalClass.getProtectionDomain().getCodeSource().getLocation(),"Reference origin");
            same(originalClass.getField("ATTRIBUTE_COUNT").get(null),Progression.ATTRIBUTE_COUNT,"attribute count");
            same(originalClass.getField("UPGRADE_COUNT").get(null),Progression.UPGRADE_COUNT,"upgrade count");
            Method required=originalClass.getMethod("required",long.class,double.class,double.class);
            Method scale=originalClass.getMethod("scale",long.class,double.class);
            Method milestone=originalClass.getMethod("activityBonus",long.class);
            Method add=originalClass.getMethod("add",long.class,long.class);
            long[] levels={Long.MIN_VALUE,-1,0,1,2,3,10,40,100,4096,1_000_000,Long.MAX_VALUE};
            for(long l:levels) {
                for(double base:new double[]{1,100,150,100_000,1e100})
                    for(double power:new double[]{1,1.5,1.6,4}) same(required.invoke(null,l,base,power),Progression.required(l,base,power),"curve "+l+"/"+base+"/"+power);
                for(double cap:new double[]{0,.15,.25,.35,.65,1,12,20}) same(scale.invoke(null,l,cap),Progression.scale(l,cap),"scale "+l+"/"+cap);
            }
            for(long count:new long[]{Long.MIN_VALUE,-1,0,1,49,50,51,99,100,149,150,199,200,499,500,999,1000,Long.MAX_VALUE})
                same(milestone.invoke(null,count),Progression.activityBonus(count),"milestone "+count);
            for(long a:new long[]{0,1,50,Long.MAX_VALUE-5,Long.MAX_VALUE})
                for(long b:new long[]{0,1,5,6,Long.MAX_VALUE}) same(add.invoke(null,a,b),Progression.add(a,b),"saturating add "+a+"/"+b);
            Object original=originalClass.getConstructor().newInstance(); Progression target=new Progression(); state(original,target,"fresh");
            invoke(attribute(original,0),"award",new Class<?>[]{double.class,double.class,double.class},100d,100d,1.6);
            target.attribute(0).award(100,100,1.6); state(original,target,"independent attribute XP");
            same(0L,target.available(),"attribute XP awards no points");
            award(original,target,150,150,1.5); state(original,target,"first overall level"); same(5L,target.earned(),"five points per overall level");
            award(original,target,Progression.required(2,150,1.5)+Progression.required(3,150,1.5),150,1.5);
            state(original,target,"multiple overall levels"); same(15L,target.earned(),"multiple level point award");
            for(int id:new int[]{-100,-1,0,17,18,Integer.MAX_VALUE}) for(int cost:new int[]{-1,0,1,16}) for(int max:new int[]{0,1,5})
                same(invoke(original,"purchase",new Class<?>[]{int.class,int.class,int.class},id,cost,max),target.purchase(id,cost,max),"purchase validation");
            state(original,target,"purchases"); invoke(original,"reset",new Class<?>[0]); target.reset(); state(original,target,"reset");
            same(15L,target.available(),"reset refunds all purchased points");
            double[] extreme={Double.NaN,Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY,-1,0,Double.MIN_VALUE,1,1_000_001,1e100};
            for(double amount:extreme) {award(original,target,amount,150,1.5);state(original,target,"extreme XP "+amount);}
            Class<?>[] restore={long.class,double.class,double.class};
            for(long l:levels) for(double xp:new double[]{-1,0,99,1e101,Double.NaN,Double.POSITIVE_INFINITY}) {
                invoke(get(original,"overall"),"restore",restore,l,xp,Double.NaN);target.overall().restore(l,xp,Double.NaN);
                level(get(original,"overall"),target.overall(),"restore "+l+"/"+xp);
            }
            for(long earned:new long[]{-1,0,15,Long.MAX_VALUE}) for(long spent:new long[]{-1,0,20,Long.MAX_VALUE}) {
                int[] ranks={-1,0,1,5,100,101,Integer.MAX_VALUE};
                invoke(original,"restorePoints",new Class<?>[]{long.class,long.class,int[].class},earned,spent,ranks);
                target.restorePoints(earned,spent,ranks);state(original,target,"restore points");
            }
            invoke(get(original,"overall"),"restore",restore,Long.MAX_VALUE-2,1e100,1e100);target.overall().restore(Long.MAX_VALUE-2,1e100,1e100);
            award(original,target,1,1,1);state(original,target,"level ceiling");same(Long.MAX_VALUE,target.level(),"level never overflows");
            invoke(get(original,"overall"),"restore",restore,1L,1e100,1e100);target.overall().restore(1,1e100,1e100);
            award(original,target,1,1,1);state(original,target,"4096 advancement bound");same(4097L,target.level(),"per-call advancement limit");
            invoke(get(original,"overall"),"restore",restore,1L,0d,0d);target.overall().restore(1,0,0);
            invoke(original,"restorePoints",new Class<?>[]{long.class,long.class,int[].class},Long.MAX_VALUE-1,0L,new int[0]);
            target.restorePoints(Long.MAX_VALUE-1,0,new int[0]);award(original,target,150,150,1.5);
            state(original,target,"point award overflow saturation");same(Long.MAX_VALUE,target.earned(),"earned points saturate");
            Random random=new Random(0x4d4544494556414cL);
            for(int step=0;step<1500;step++) {
                int op=random.nextInt(5);
                if(op==0) award(original,target,random.nextDouble()*10_000,150,1.5);
                else if(op==1) {int id=random.nextInt(6);double amount=random.nextDouble()*1000;invoke(attribute(original,id),"award",new Class<?>[]{double.class,double.class,double.class},amount,100d,1.6);target.attribute(id).award(amount,100,1.6);}
                else if(op==2) {int id=random.nextInt(22)-2,cost=random.nextInt(5),max=random.nextInt(8);same(invoke(original,"purchase",new Class<?>[]{int.class,int.class,int.class},id,cost,max),target.purchase(id,cost,max),"random purchase "+step);}
                else if(op==3) {invoke(original,"reset",new Class<?>[0]);target.reset();}
                else {long earned=random.nextInt(500),spent=random.nextInt(600);int[] ranks={random.nextInt(120)-10};invoke(original,"restorePoints",new Class<?>[]{long.class,long.class,int[].class},earned,spent,ranks);target.restorePoints(earned,spent,ranks);}
                state(original,target,"seeded sequence "+step);
            }
            System.out.println("Original JAR: "+jar+"\nSHA-256: "+hash+"\nReference class loader: isolated URLClassLoader (parent=null)\nTarget core origin: "+Progression.class.getProtectionDomain().getCodeSource().getLocation()+"\nOriginal JAR parity: "+checks+" exact comparisons passed");
        }
    }
}
