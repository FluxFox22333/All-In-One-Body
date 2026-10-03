package dev.fivefold.server;
import com.google.gson.*;
import dev.fivefold.core.*;
import dev.fivefold.net.Wire;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;

/** Network-thread relay. No entity, inventory, world, or mutable Sessions map is touched here. */
public final class FastRelay {
 private record Route(UUID owner,RealtimeControls controls,Map<UUID,Connection> peers){}
 private static final Map<Connection,Route> routes=new ConcurrentHashMap<>();
 // All publication/revocation callers are on the server main thread.
 public static void publish(RealtimeControls controls,RealtimeControls.Config config,double yaw,double pitch,Map<UUID,Connection> peers){
  synchronized(controls){
   controls.configure(config,yaw,pitch);
   Map<UUID,Connection> immutable=Map.copyOf(peers);
   for(var e:immutable.entrySet())routes.put(e.getValue(),new Route(e.getKey(),controls,immutable));
   movement(controls,immutable);activity(controls,immutable);
  }
 }
 public static void remove(RealtimeControls controls){
  synchronized(controls){controls.disable();routes.entrySet().removeIf(e->e.getValue().controls()==controls);}
 }
 private static void send(Connection connection,JsonObject message){
  if(connection!=null)connection.send(new ClientboundCustomPayloadPacket(new Wire(message)));
 }
 private static void movement(RealtimeControls controls,Map<UUID,Connection> peers){
  var cfg=controls.config();if(cfg==null)return;
  var value=controls.pollMovement(System.nanoTime());if(value==null)return;
  var o=Wire.message("realtime_move");o.addProperty("room",cfg.room());o.addProperty("epoch",cfg.epoch());o.addProperty("revision",cfg.revision());
  o.addProperty("serial",value.serial());o.addProperty("otherHeld",value.otherHeld());o.addProperty("enabled",value.enabled());
  send(peers.get(cfg.body()),o);
 }
 private static void activity(RealtimeControls controls,Map<UUID,Connection> peers){
  var value=controls.pollActivity(System.nanoTime());if(value==null)return;
  var cfg=controls.config();var o=Wire.message("activity");o.addProperty("room",cfg.room());o.addProperty("epoch",cfg.epoch());o.addProperty("revision",cfg.revision());o.addProperty("serial",value.serial());
  var members=new JsonArray();for(var e:value.members().entrySet()){var member=new JsonObject();member.addProperty("id",e.getKey().toString());member.addProperty("held",e.getValue().held());member.addProperty("recent",e.getValue().recent());members.add(member);}o.add("members",members);
  for(var connection:peers.values())send(connection,o);
 }
 public static void pulse(RealtimeControls controls,UUID owner,int bits){
  synchronized(controls){controls.pulse(owner,bits,System.nanoTime());for(var route:routes.values())if(route.controls()==controls){activity(controls,route.peers());break;}}
 }
 public static void writeView(RealtimeControls controls,JsonObject o){
  synchronized(controls){
   var cfg=controls.config();var view=controls.view();
   o.addProperty("room",cfg.room());o.addProperty("epoch",cfg.epoch());o.addProperty("viewSerial",view.serial());
   o.addProperty("yaw",view.angles().yaw());o.addProperty("pitch",view.angles().pitch());
   var acks=new JsonObject();for(var e:view.acknowledgements().entrySet())acks.addProperty(e.getKey().toString(),e.getValue());o.add("viewAcks",acks);
  }
 }
 /** true means the packet is fully handled; input still proceeds to main-thread action handling. */
 public static boolean receive(Connection connection,Wire wire){
  JsonObject o;String kind;
  try{o=wire.data();kind=o.get("kind").getAsString();}catch(RuntimeException ex){return true;}
  if(!kind.equals("input")&&!kind.equals("look_batch"))return false;
  boolean consume=kind.equals("look_batch");
  Route route=routes.get(connection);
  if(route==null||(!wire.items().isEmpty()||wire.offers()!=null))return consume;
  var controls=route.controls();
  synchronized(controls){
   // A queued packet must not resurrect a revoked route.
   route=routes.get(connection);if(route==null||route.controls()!=controls)return consume;
   try{
    String room=o.get("room").getAsString();int epoch=o.get("epoch").getAsInt();long revision=o.get("revision").getAsLong();
    if(kind.equals("input")){
     int bits=o.get("held").getAsInt();long now=System.nanoTime();
     if(controls.input(route.owner(),room,epoch,revision,bits,now))movement(controls,route.peers());
     if(controls.observe(route.owner(),room,epoch,revision,bits,now))activity(controls,route.peers());
    }else{
     JsonArray raw=o.getAsJsonArray("samples");if(raw.size()>64)return true;
     var samples=new ArrayList<PredictedLook.Sample>();
     for(var el:raw){var a=el.getAsJsonArray();if(a.size()!=3)return true;samples.add(new PredictedLook.Sample(a.get(0).getAsLong(),a.get(1).getAsDouble(),a.get(2).getAsDouble()));}
     if(controls.look(route.owner(),room,epoch,revision,samples)){
      var update=Wire.message("view");writeView(controls,update);
      for(var peer:route.peers().values())send(peer,update);
      controls.pulse(route.owner(),Action.LOOK.bit(),System.nanoTime());activity(controls,route.peers());
     }
    }
   }catch(IllegalArgumentException|IllegalStateException|NullPointerException ex){return consume;}
  }
  return consume;
 }
}
