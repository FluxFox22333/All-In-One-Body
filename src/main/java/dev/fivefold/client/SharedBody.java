package dev.fivefold.client;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;

/** Synchronize only the remote camera entity; never overwrite a spectator's personal inventory. */
public final class SharedBody {
 private static com.google.gson.JsonObject lastState;
 private static AbstractClientPlayer lastEntity;
 private static java.util.List<net.minecraft.world.effect.MobEffectInstance> effects=java.util.List.of();
 public static java.util.Collection<net.minecraft.world.effect.MobEffectInstance> effects(){return effects;}
 public static AbstractClientPlayer player(){
  var mc=ClientState.mc();if(!ClientState.linked()||ClientState.body()||mc.level==null)return null;
  var entity=mc.level.getEntity(ClientState.state.get("entity").getAsInt());return entity instanceof AbstractClientPlayer p?p:null;
 }
 public static boolean viewing(){var p=player();return p!=null&&ClientState.mc().getCameraEntity()==p;}
 public static void apply(){
  var body=player();if(body==null)return;var state=ClientState.state;var items=ClientState.items;
  if(items.size()<9||!state.has("equipmentOffset"))return;
  if(state==lastState&&body==lastEntity)return;lastState=state;lastEntity=body;
  var next=new java.util.ArrayList<net.minecraft.world.effect.MobEffectInstance>();
  for(var e:state.getAsJsonArray("effectList")){var o=e.getAsJsonObject();net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(net.minecraft.resources.Identifier.parse(o.get("type").getAsString())).ifPresent(effect->next.add(new net.minecraft.world.effect.MobEffectInstance(effect,o.get("duration").getAsInt(),o.get("amplifier").getAsInt(),o.get("ambient").getAsBoolean(),o.get("visible").getAsBoolean(),o.get("icon").getAsBoolean())));}
  effects=java.util.List.copyOf(next);
  for(int i=0;i<9;i++)body.getInventory().setItem(i,items.get(i).copy());
  body.getInventory().setSelectedSlot(Math.clamp(state.get("selected").getAsInt(),0,8));
  int offset=state.get("equipmentOffset").getAsInt();if(items.size()>offset+1){body.setItemSlot(EquipmentSlot.MAINHAND,items.get(offset).copy());body.setItemSlot(EquipmentSlot.OFFHAND,items.get(offset+1).copy());}
  body.setHealth(state.get("health").getAsFloat());body.getFoodData().setFoodLevel(state.get("food").getAsInt());body.setAirSupply(state.get("air").getAsInt());
  body.experienceLevel=state.get("xp").getAsInt();body.experienceProgress=state.get("xpProgress").getAsFloat();
  boolean using=state.get("usingItem").getAsBoolean();InteractionHand hand=InteractionHand.valueOf(state.get("useHand").getAsString());
  if(using&&(!body.isUsingItem()||body.getUsedItemHand()!=hand))body.startUsingItem(hand);
  else if(!using&&body.isUsingItem())body.stopUsingItem();
 }
 private SharedBody(){}
}
