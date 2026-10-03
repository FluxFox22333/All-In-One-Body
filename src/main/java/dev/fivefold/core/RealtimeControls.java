package dev.fivefold.core;
import java.util.*;

/** Synchronized, world-free control state shared by the server tick and network handlers. */
public final class RealtimeControls {
 public record Config(String room,int epoch,long revision,UUID body,Map<UUID,Integer> permissions,boolean enabled){
  public Config { permissions=Map.copyOf(permissions); }
 }
 public record View(long serial,LookAngles angles,Map<UUID,Long> acknowledgements){}
 public record Movement(long serial,int otherHeld,boolean enabled){}
 private record Held(int bits,long time){}
 private Config config;
 private final ControlActivity activity=new ControlActivity();
 private Map<UUID,ControlActivity.State> lastActivity=Map.of();private long activitySerial,lastActivityEmit=Long.MIN_VALUE;
 public record Activity(long serial,Map<UUID,ControlActivity.State> members){}
 private final LookAuthority look=new LookAuthority();
 private final Map<UUID,Held> held=new HashMap<>();
 private long serial,lastEmit=Long.MIN_VALUE;private Config emittedConfig;private int emittedBits;
 public static final long EXPIRY=550_000_000L;
 public synchronized void configure(Config next,double yaw,double pitch){
  boolean reset=config==null||!config.room().equals(next.room())||config.epoch()!=next.epoch();
  if(reset){look.reset(yaw,pitch);held.clear();}
  if(config==null||!config.equals(next)){
   held.clear();lastActivityEmit=Long.MIN_VALUE;
   if(config==null||!config.room().equals(next.room())||config.revision()!=next.revision()||!config.permissions().equals(next.permissions()))activity.clear();else activity.releaseAll();
  }
  config=next;
 }
 public synchronized Config config(){return config;}
 public synchronized void disable(){
  if(config!=null)config=new Config(config.room(),config.epoch(),config.revision(),config.body(),config.permissions(),false);
  held.clear();activity.clear();
 }
 private boolean valid(UUID owner,String room,int epoch,long revision){
  return config!=null&&config.enabled()&&config.room().equals(room)&&config.epoch()==epoch&&config.revision()==revision&&config.permissions().containsKey(owner);
 }
 public synchronized boolean observe(UUID owner,String room,int epoch,long revision,int bits,long now){
  if(config==null||!config.room().equals(room)||config.epoch()!=epoch||config.revision()!=revision||!config.permissions().containsKey(owner))return false;
  activity.observe(owner,bits&config.permissions().get(owner),now);return true;
 }
 public synchronized void pulse(UUID owner,int bits,long now){
  if(config!=null)activity.pulse(owner,bits&config.permissions().getOrDefault(owner,0),now);
 }
 public synchronized Activity pollActivity(long now){
  if(config==null)return null;
  var next=activity.snapshot(config.permissions(),now);
  if(next.equals(lastActivity)&&lastActivityEmit!=Long.MIN_VALUE&&now-lastActivityEmit<200_000_000L)return null;
  lastActivity=next;lastActivityEmit=now;return new Activity(++activitySerial,next);
 }
 public synchronized boolean input(UUID owner,String room,int epoch,long revision,int bits,long now){
  if(!valid(owner,room,epoch,revision))return false;
  held.put(owner,new Held(bits&config.permissions().get(owner)&MovementInput.MASK,now));return true;
 }
 public synchronized boolean look(UUID owner,String room,int epoch,long revision,List<PredictedLook.Sample> samples){
  if(!valid(owner,room,epoch,revision)||(config.permissions().get(owner)&Action.LOOK.bit())==0||samples.size()>64)return false;
  // Validate the whole batch before mutating; preserve per-sample pitch clamping.
  for(var s:samples)if(s.sequence()<=0||!Double.isFinite(s.dx())||!Double.isFinite(s.dy())||Math.abs(s.dx())>3600||Math.abs(s.dy())>3600)return false;
  boolean changed=false;
  for(var s:samples)changed|=look.accept(owner,s.sequence(),s.dx(),s.dy());
  return changed;
 }
 public synchronized Movement movement(long now){
  int bits=0;
  if(config!=null&&config.enabled())for(var e:held.entrySet())
   if(!e.getKey().equals(config.body())&&now-e.getValue().time()<=EXPIRY)
    bits|=e.getValue().bits()&config.permissions().getOrDefault(e.getKey(),0);
  return new Movement(++serial,bits,config!=null&&config.enabled());
 }
 /** Coalesce unchanged member heartbeats; key edges are never rate-limited. */
 public synchronized Movement pollMovement(long now){
  var value=movement(now);
  if(Objects.equals(config,emittedConfig)&&emittedBits==value.otherHeld()&&lastEmit!=Long.MIN_VALUE&&now-lastEmit<50_000_000L)return null;
  emittedConfig=config;emittedBits=value.otherHeld();lastEmit=now;return value;
 }
 public synchronized View view(){return new View(look.serial(),look.angles(),look.acknowledgements());}
}
