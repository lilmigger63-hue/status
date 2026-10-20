package dev.medievalstatus.client;

import dev.medievalstatus.*;
import dev.medievalstatus.core.Progression;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import java.util.*;

/** Resolution-independent logical canvas, no per-frame networking or world scans. */
public final class StatusScreen extends Screen {
    private static final int W=960,H=600, TEXT=0xffe4ded0, MUTED=0xff95958c, GREEN=0xff79b394;
    private static final ResourceLocation STONE=new ResourceLocation(MedievalStatus.ID,"textures/gui/stone.png");
    private final UiLayout layout=UiLayout.load();
    private float scale=1,ox,oy;
    private int mx,my,tab,scroll,category;
    private int gold=0xffc6a665,stone=0xff171a1d;
    private String tooltip;
    private static final String[] ABBR={"STR","DEX","CON","INT","WIS","CHA"};
    private static final String[] SOURCES={"Mine natural stone and ore; chop unplaced logs; land melee hits on monsters.","Sprint across real distances; land arrow hits on monsters.","Survive monster damage, then recover from those injuries.","Enchant items, take freshly brewed potions, and craft items.","Harvest mature crops and breed animals.","Complete successful villager trades."};
    private static final String[] CATEGORIES={"All","Physical","Mobility","Survival","Knowledge","Nature","Social"};
    private static final Item[] ICONS={Items.IRON_PICKAXE,Items.IRON_AXE,Items.ANVIL,Items.LEATHER,Items.FEATHER,Items.COOKED_BEEF,Items.BOW,Items.GOLDEN_APPLE,Items.GLISTERING_MELON_SLICE,Items.IRON_CHESTPLATE,Items.ENCHANTED_BOOK,Items.BREWING_STAND,Items.CRAFTING_TABLE,Items.WHEAT,Items.IRON_HOE,Items.WHEAT_SEEDS,Items.EMERALD,Items.GOLD_INGOT};

    public StatusScreen() { super(Component.literal("Medieval Status")); }
    @Override public boolean isPauseScreen() { return false; }
    @Override protected void init() { transform(); }
    private void transform() {
        scale=(float)Math.min(width/(double)W,height/(double)H)*(float)Math.min(1,Config.UI_SCALE.get());
        ox=(width-W*scale)/2; oy=(height-H*scale)/2;
        gold=parse(Config.GOLD.get(),0xffc6a665); stone=parse(Config.STONE.get(),0xff171a1d);
    }
    private static int parse(String s,int fallback) { try { return 0xff000000|(Integer.parseInt(s,16)&0xffffff); } catch (RuntimeException e) { return fallback; } }
    private void text(GuiGraphics g,String s,int x,int y,int color) { g.drawString(font,s,x,y,color,false); }
    private void line(GuiGraphics g,String s,int x,int y,int color,int max) { text(g,font.plainSubstrByWidth(s,max),x,y,color); }
    private boolean over(int x,int y,int w,int h) { return mx>=x && mx<x+w && my>=y && my<y+h; }
    private void panel(GuiGraphics g,int x,int y,int w,int h,String title) {
        g.fill(x,y,x+w,y+h,0xff090c0e); g.fill(x+1,y+1,x+w-1,y+h-1,gold);
        g.fill(x+2,y+2,x+w-2,y+h-2,stone);
        // Small stone tiles and asymmetric bevels make original, lightweight ornamentation.
        for (int yy=y+3;yy<y+h-4;yy+=32) for (int xx=x+3;xx<x+w-4;xx+=32)
            g.blit(STONE,xx,yy,0,0,Math.min(32,x+w-3-xx),Math.min(32,y+h-3-yy),32,32);
        g.fill(x+5,y+5,x+w-5,y+6,0xff44423a);
        for (int cx:new int[]{x+4,x+w-7}) for (int cy:new int[]{y+4,y+h-7}) g.fill(cx,cy,cx+3,cy+3,gold);
        if (!title.isEmpty()) { text(g,title,x+13,y+13,gold); g.fill(x+13,y+28,x+w-13,y+29,0xff4b4636); }
    }
    private void bar(GuiGraphics g,int x,int y,int w,double fraction,int color) {
        g.fill(x,y,x+w,y+5,0xff080c0e); g.fill(x,y,x+(int)(w*Math.max(0,Math.min(1,fraction))),y+5,color);
    }
    private void button(GuiGraphics g,String s,int x,int y,int w,boolean enabled) {
        g.fill(x,y,x+w,y+25,enabled&&over(x,y,w,25)?0xff51472f:0xff25282a);
        g.fill(x,y+24,x+w,y+25,enabled?gold:0xff494a49);
        g.drawCenteredString(font,s,x+w/2,y+8,enabled?TEXT:MUTED);
    }
    private static String number(double v) { return v>=1_000_000?String.format(Locale.ROOT,"%.1fM",v/1_000_000):String.format(Locale.ROOT,"%.0f",v); }
    private static String percent(double v) { return String.format(Locale.ROOT,"%.1f%%",v*100); }

    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        renderBackground(g); transform(); mx=(int)((mouseX-ox)/scale); my=(int)((mouseY-oy)/scale); tooltip=null;
        g.pose().pushPose(); g.pose().translate(ox,oy,0); g.pose().scale(scale,scale,1);
        g.fill(0,0,W,H,0xf20b0e10);
        panel(g,8,8,944,65,"");
        if (minecraft.player!=null) PlayerFaceRenderer.draw(g,minecraft.player.getSkinTextureLocation(),22,21,38);
        text(g,minecraft.player==null?"Wanderer":minecraft.player.getGameProfile().getName(),73,20,TEXT);
        text(g,"MEDIEVAL STATUS  /  Character level "+ClientState.data.progression.level(),73,37,gold);
        double required=Progression.required(ClientState.data.progression.level(),ClientState.snapshot.getDouble("overallBase"),ClientState.snapshot.getDouble("overallPower"));
        bar(g,73,55,305,ClientState.data.progression.overall().xp()/required,GREEN);
        int glow=(int)((18+12*Math.sin(System.currentTimeMillis()/900.0))*Config.ANIMATION.get());
        g.fill(73,55,378,56,(glow<<24)|0xffffff);
        text(g,number(ClientState.data.progression.overall().xp())+" / "+number(required)+" XP",390,53,MUTED);
        text(g,"SKILL POINTS",750,23,gold); text(g,""+ClientState.data.progression.available(),850,42,TEXT);
        button(g,"X",910,25,24,true);
        String[] tabs={"STATUS","SKILL UPGRADES","ACTIVITY DETAILS","SETTINGS"};
        for (int i=0;i<4;i++) { button(g,tabs[i],16+i*235,83,223,true); if (tab==i) g.fill(16+i*235,107,239+i*235,109,gold); }
        if (!ClientState.ready) text(g,"Waiting for character data from the server...",24,125,MUTED);
        else switch(tab) { case 0 -> status(g); case 1 -> upgrades(g); case 2 -> activities(g); default -> settings(g); }
        text(g,"P  Character sheet    |    ESC  Return to world",18,580,MUTED);
        text(g,"Attributes grow through action. Every character level grants 5 skill points.",465,580,gold);
        g.pose().popPose();
        if (tooltip!=null) g.renderTooltip(font,font.split(Component.literal(tooltip),Math.min(300,width-20)),mouseX,mouseY);
    }
    private void status(GuiGraphics g) {
        for (int i=0;i<6;i++) {
            int x=16+i*156; var a=ClientState.data.progression.attribute(i);
            panel(g,x,120,148,101,ABBR[i]+"  /  "+PlayerData.ATTRIBUTES[i]);
            text(g,"LEVEL "+a.level(),x+14,158,TEXT);
            text(g,"+"+Math.max(0,(a.level()-1)/2)+" proficiency",x+14,175,MUTED);
            double req=Progression.required(a.level(),ClientState.snapshot.getDouble("attributeBase"),ClientState.snapshot.getDouble("attributePower"));
            bar(g,x+14,193,120,a.xp()/req,GREEN); text(g,number(a.xp())+" / "+number(req)+" XP",x+14,203,MUTED);
            if (over(x,120,148,101)) tooltip=PlayerData.ATTRIBUTES[i]+"\n"+SOURCES[i]+"\nLifetime XP: "+number(a.lifetime())+"\nProficiency is a display value. Actual bonuses use capped diminishing returns.";
        }
        int left=layout.leftWidth,mid=layout.activityWidth,cx=16+left+layout.gap,rx=cx+mid+layout.gap,rw=944-rx;
        panel(g,16,234,left,327,"VITALS & EQUIPMENT");
        var p=minecraft.player; if (p==null) return;
        String[] labels={"Health","Armor","Armor toughness","Attack damage","Attack speed","Movement / sec","Knockback resistance"};
        double[] values={p.getHealth(),p.getArmorValue(),p.getAttributeValue(Attributes.ARMOR_TOUGHNESS),p.getAttributeValue(Attributes.ATTACK_DAMAGE),p.getAttributeValue(Attributes.ATTACK_SPEED),p.getAttributeValue(Attributes.MOVEMENT_SPEED)*43.17,p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)};
        for(int i=0;i<labels.length;i++) { text(g,labels[i],30,275+i*22,MUTED); text(g,String.format(Locale.ROOT,"%.1f",values[i])+(i==0?" / "+number(p.getMaxHealth()):""),16+left-72,275+i*22,TEXT); }
        int i=0; for(ItemStack stack:p.getArmorSlots()) { g.renderItem(stack,30+i*38,445); if(over(30+i*38,445,18,18)) tooltip=stack.getHoverName().getString(); i++; }
        line(g,"Main: "+p.getMainHandItem().getHoverName().getString(),30,480,TEXT,left-26);
        line(g,"Off: "+p.getOffhandItem().getHoverName().getString(),30,502,TEXT,left-26);
        text(g,"SURVIVAL  /  "+p.level().dimension().location().getPath(),30,536,gold);
        panel(g,cx,234,mid,327,"ACTIVITY PROFICIENCIES");
        int row=0;
        if(Config.SHOW_ACTIVITIES.get()) for (String activity:Config.REWARDS.keySet()) {
            if(row>=12) break;
            text(g,activity,cx+14,275+row*22,TEXT);
            text(g,number(ClientState.data.activities.getOrDefault(activity,0L)),cx+mid-55,275+row*22,gold);
            if(over(cx+12,271+row*22,mid-24,20)) tooltip=activity+": "+ClientState.data.activities.getOrDefault(activity,0L)+" rewarded actions. These are activity counts, not separate skill levels.";
            row++;
        }
        panel(g,rx,234,rw,327,"ACTIVE BONUSES");
        String[] keys={"Mining","Woodcutting","Melee damage","Movement","Attack speed","Bonus health","Bonus armor","Arrow damage","Crop yield chance","Enchant refund chance","Potion duration","Trade discount","Recovery pulse"};
        CompoundTag b=ClientState.snapshot.getCompound("bonuses");
        for(int n=0;n<keys.length;n++) {
            String key=keys[n]; double value=b.getDouble(key);
            text(g,key,rx+14,273+n*20,MUTED);
            text(g,(key.startsWith("Bonus")||key.equals("Recovery pulse"))?String.format(Locale.ROOT,"+%.1f",value):"+"+percent(value),rx+rw-72,273+n*20,GREEN);
        }
        int purchased=0; for(Upgrade u:Upgrade.values()) purchased+=ClientState.data.progression.rank(u.id());
        text(g,"Passives: "+purchased+" ranks  /  "+ClientState.data.progression.available()+" points",rx+14,545,gold);
    }
    private List<Upgrade> visibleUpgrades() { return Arrays.stream(Upgrade.values()).filter(u->category==0||u.category.equals(CATEGORIES[category])).toList(); }
    private CompoundTag upgradeConfig(Upgrade u) { return ClientState.snapshot.getCompound("upgradeConfig").getCompound(u.name()); }
    private void upgrades(GuiGraphics g) {
        panel(g,16,120,928,441,"PASSIVE UPGRADES  /  "+ClientState.data.progression.available()+" available points");
        for(int i=0;i<CATEGORIES.length;i++) button(g,CATEGORIES[i],30+i*128,157,118,true);
        List<Upgrade> list=visibleUpgrades(); scroll=Math.max(0,Math.min(scroll,Math.max(0,list.size()-6)));
        for(int i=0;i<6 && i+scroll<list.size();i++) {
            Upgrade u=list.get(i+scroll); CompoundTag cfg=upgradeConfig(u); int rank=ClientState.data.progression.rank(u.id()),y=193+i*51;
            g.fill(30,y,930,y+46,over(30,y,900,46)?0xff30322e:0xff212528);
            text(g,u.category.toUpperCase(Locale.ROOT),42,y+7,gold); text(g,u.title,155,y+7,TEXT);
            g.renderItem(new ItemStack(ICONS[u.id()]),128,y+15);
            String effect=u.effect+": +"+(u.perRank<1?percent(cfg.getDouble("effect")):number(cfg.getDouble("effect")))+" / rank";
            line(g,effect,155,y+25,MUTED,505);
            text(g,rank+" / "+cfg.getInt("max"),680,y+17,gold);
            boolean enabled=cfg.getBoolean("enabled") && rank<cfg.getInt("max") && ClientState.data.progression.available()>=cfg.getInt("cost");
            button(g,rank>=cfg.getInt("max")?"MAX":"Buy: "+cfg.getInt("cost")+" pt",790,y+10,123,enabled);
            if(over(30,y,745,46)) tooltip=u.title+"\n"+effect+"\nEach purchase is validated by the server. Bonuses respect global caps; disabled upgrades contribute nothing.";
        }
        button(g,"Previous",30,521,110,scroll>0); button(g,"Next",150,521,110,scroll+6<list.size());
        button(g,"Reset Skills",790,521,123,ClientState.snapshot.getBoolean("resetEnabled"));
        text(g,"Earned: "+ClientState.data.progression.earned()+"  /  Spent: "+ClientState.data.progression.spent(),290,530,MUTED);
    }
    private void activities(GuiGraphics g) {
        panel(g,16,120,928,441,"ACTIVITY DETAILS  /  lifetime rewarded actions");
        int i=0; for(String activity:Config.REWARDS.keySet()) {
            int y=165+i*28; text(g,activity,36,y,TEXT); text(g,number(ClientState.data.activities.getOrDefault(activity,0L)),350,y,gold);
            text(g,"Completed actions only; subject to reward cooldowns and validation.",440,y,MUTED); i++;
        }
    }
    private void settings(GuiGraphics g) {
        panel(g,16,120,928,441,"INTERFACE SETTINGS");
        text(g,"These settings affect your interface. Gameplay rules belong to the server.",36,164,MUTED);
        button(g,"UI scale: "+String.format(Locale.ROOT,"%.2f",Config.UI_SCALE.get()),36,202,260,true);
        button(g,"Activity panel: "+(Config.SHOW_ACTIVITIES.get()?"Shown":"Hidden"),36,242,260,true);
        button(g,"Animation: "+(Config.ANIMATION.get()>0?"On":"Off"),36,282,260,true);
        text(g,"Keybinding: Options > Controls > Medieval Status (default P)",36,347,TEXT);
        text(g,"Colors and scale: config/medievalstatus-client.toml",36,377,MUTED);
        text(g,"Panel widths and spacing: config/medievalstatus-layout.json",36,407,MUTED);
        text(g,"Invalid layout values fall back to the original layout.",36,437,MUTED);
        text(g,"Server balance: <world>/serverconfig/medievalstatus-server.toml",36,477,gold);
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        mx=(int)((x-ox)/scale); my=(int)((y-oy)/scale);
        if(button!=0) return super.mouseClicked(x,y,button);
        if(over(910,25,24,25)) { onClose(); return true; }
        for(int i=0;i<4;i++) if(over(16+i*235,83,223,25)) { tab=i; scroll=0; return true; }
        if(tab==1 && ClientState.ready) {
            for(int i=0;i<CATEGORIES.length;i++) if(over(30+i*128,157,118,25)) { category=i; scroll=0; return true; }
            List<Upgrade> list=visibleUpgrades();
            for(int i=0;i<6 && i+scroll<list.size();i++) if(over(790,203+i*51,123,25)) {
                Upgrade u=list.get(i+scroll); CompoundTag cfg=upgradeConfig(u);
                if(cfg.getBoolean("enabled") && ClientState.data.progression.rank(u.id())<cfg.getInt("max") && ClientState.data.progression.available()>=cfg.getInt("cost")) Network.request(u.id());
                return true;
            }
            if(over(30,521,110,25)) { scroll=Math.max(0,scroll-6); return true; }
            if(over(150,521,110,25)) { scroll=Math.min(Math.max(0,list.size()-6),scroll+6); return true; }
            if(over(790,521,123,25) && ClientState.snapshot.getBoolean("resetEnabled")) {
                minecraft.setScreen(new ConfirmScreen(confirmed->{ minecraft.setScreen(this); if(confirmed) Network.request(-1); },Component.literal("Reset purchased skills?"),Component.literal("Refund spent skill points and remove upgrades. Attributes and character XP stay. Cost: "+ClientState.snapshot.getInt("resetCost")+" vanilla levels; cooldown applies.")));
                return true;
            }
        }
        if(tab==3) {
            if(over(36,202,260,25)) { double v=Config.UI_SCALE.get(); Config.UI_SCALE.set(v>=1? .65 : Math.min(1,v+.1)); Config.CLIENT.save(); return true; }
            if(over(36,242,260,25)) { Config.SHOW_ACTIVITIES.set(!Config.SHOW_ACTIVITIES.get()); Config.CLIENT.save(); return true; }
            if(over(36,282,260,25)) { Config.ANIMATION.set(Config.ANIMATION.get()>0?0d:.4); Config.CLIENT.save(); return true; }
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta) {
        if(tab==1) { scroll=Math.max(0,Math.min(Math.max(0,visibleUpgrades().size()-6),scroll-(int)Math.signum(delta))); return true; }
        return super.mouseScrolled(x,y,delta);
    }
}
