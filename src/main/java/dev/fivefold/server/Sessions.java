package dev.fivefold.server;

import com.google.gson.*;
import dev.fivefold.core.*;
import dev.fivefold.net.Wire;
import dev.fivefold.mixin.MenuAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

public final class Sessions {
 static final Map<String,Room> rooms=new HashMap<>();
 static final Map<UUID,Room> membership=new HashMap<>();
 static final Random random=new java.security.SecureRandom();
 static long clock;
 static class Room {
  CooperationTasks tasks=new CooperationTasks(random);int tradeSelection,tradeScroll;
  String code;UUID body;Permissions permissions=new Permissions();
  Map<UUID,List<String>> bindings=new HashMap<>();
  Map<UUID,GameType> previousMode=new HashMap<>();InputMixer frames=new InputMixer();
  RealtimeControls controls=new RealtimeControls();int lookEpoch,lookEntity=-1;String lookDimension="";
  CursorHistory cursorHistory=new CursorHistory();
  AbstractContainerMenu lastMenu;int menuEpoch;boolean lastOpen;long cursorSerial;
  boolean linked,open;int sequence,attackClicks,useClicks;double cursorX=0.5,cursorY=0.5;int merged;
  Room(String code,UUID body){this.code=code;this.body=body;}
 }
 static ServerPlayer player(Room r,UUID id){var s=net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();return s==null?null:s.getPlayerList().getPlayer(id);}
 static void send(ServerPlayer p,JsonObject o){PacketDistributor.sendToPlayer(p,new Wire(o));}
 static void broadcast(Room r,JsonObject o){for(UUID id:r.permissions.members().keySet()){var p=player(r,id);if(p!=null)send(p,o);}}
 static void error(ServerPlayer p,String text){JsonObject o=Wire.message("error");o.addProperty("text",text);send(p,o);}
 public static void receive(ServerPlayer p,Wire w){
  try {
   if(!w.items().isEmpty()||w.offers()!=null)return;
   JsonObject o=w.data();String kind=o.get("kind").getAsString();Room r=membership.get(p.getUUID());
   if(kind.equals("create")){
    if(r!=null)throw new IllegalArgumentException("请先退出当前房间");
    if(p.isSpectator()||!p.isAlive())throw new IllegalArgumentException("身体玩家需要处于存活的生存或创造模式");
    String code;do{code=String.format(Locale.ROOT,"%06d",random.nextInt(1000000));}while(rooms.containsKey(code));
    r=new Room(code,p.getUUID());rooms.put(code,r);join(r,p);state(r);return;
   }
   if(kind.equals("join")){
    if(r!=null)throw new IllegalArgumentException("请先退出当前房间");
    Room target=rooms.get(o.get("code").getAsString());if(target==null)throw new IllegalArgumentException("房间号码不存在");
    join(target,p);state(target);return;
   }
   if(r==null){if(kind.equals("query")){send(p,Wire.message("left"));return;}throw new IllegalArgumentException("先创建或加入多人房间");}
   switch(kind){
    case "query" -> state(r);
    case "leave" -> dissolve(r);
    case "claim" -> {r.permissions.set(p.getUUID(),o.get("mask").getAsInt(),o.get("revision").getAsLong());r.frames.clear();r.attackClicks=r.useClicks=0;resetLook(r);state(r);}
    case "defaults" -> {if(!r.body.equals(p.getUUID()))throw new IllegalArgumentException("只有房主可恢复全队默认分工");r.permissions.defaults();r.frames.clear();r.attackClicks=r.useClicks=0;resetLook(r);state(r);}
    case "start" -> {if(!r.body.equals(p.getUUID()))throw new IllegalArgumentException("只有房主可启动");if(!r.permissions.complete())throw new IllegalArgumentException("需要2至32人、全部操作有人负责，且每人均有键盘和鼠标权限");link(r);state(r);}
    case "input" -> {
     if(!r.linked||!r.permissions.complete())return;
     if(!r.code.equals(o.get("room").getAsString())||r.lookEpoch!=o.get("epoch").getAsInt()||r.permissions.revision()!=o.get("revision").getAsLong())return;
     if(o.has("bindings")){
      var labels=o.getAsJsonArray("bindings");if(labels.size()==Action.values().length){var list=new ArrayList<String>();for(var label:labels){String text=label.getAsString().replaceAll("[\\p{Cntrl}§]","");list.add(text.substring(0,Math.min(24,text.length())));}r.bindings.put(p.getUUID(),List.copyOf(list));}
     }
     if(o.has("challengeHeld")&&o.has("task")){r.tasks.input(p.getUUID(),o.get("task").getAsLong(),o.get("challengeHeld").getAsInt());TaskRewards.drain(r);}
     int beforeOther=r.frames.heldExcept(clock,r.permissions,r.body);
     int before=combined(r);boolean wasOpen=r.open;
     int pressed=r.frames.update(p.getUUID(),o.get("held").getAsInt(),clock,r.permissions);var body=player(r,r.body);if(body==null)return;
     if((pressed&Action.INVENTORY.bit())!=0)r.open=true;
     if((pressed&Action.CLOSE.bit())!=0){body.closeContainer();r.open=false;}
     if((pressed&Action.DROP.bit())!=0){if(r.open){if(menuMatches(r,o)){var point=clickPoint(r,p,o);if(point!=null)drop(r,body,point);}}else {body.drop((combined(r)&Action.SPRINT.bit())!=0);int selected=body.getInventory().getSelectedSlot();body.connection.send(new net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket(selected,body.getInventory().getItem(selected).copy()));body.containerMenu.broadcastChanges();}}
     if(!r.open){if((pressed&Action.ATTACK.bit())!=0)r.attackClicks++;if((pressed&Action.USE.bit())!=0)r.useClicks++;}
     if(r.open){
      var point=menuMatches(r,o)&&(!(body.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu)||o.has("tradeScroll")&&o.get("tradeScroll").getAsInt()==r.tradeScroll)?clickPoint(r,p,o):null;
      if((pressed&Action.ATTACK.bit())!=0&&point!=null)click(r,body,0,point);
      if((pressed&Action.USE.bit())!=0&&point!=null)click(r,body,1,point);
     }
     if(InputMixer.needsRelay(before,combined(r),beforeOther,r.frames.heldExcept(clock,r.permissions,r.body))||pressed!=0||wasOpen!=r.open)sendFrame(r,body);
     if(wasOpen!=r.open||pressed!=0&&(r.open||wasOpen||(pressed&Action.DROP.bit())!=0))state(r);
    }
    case "look" -> {
     if(!active(r)||!r.open||!r.permissions.owns(p.getUUID(),Action.LOOK)||!menuMatches(r,o))return;
     double x=o.get("x").getAsDouble(),y=o.get("y").getAsDouble();if(!Double.isFinite(x)||!Double.isFinite(y))return;
     r.cursorX=Math.clamp(x,0,1);r.cursorY=Math.clamp(y,0,1);r.cursorSerial++;r.cursorHistory.add(r.cursorSerial,r.cursorX,r.cursorY);FastRelay.pulse(r.controls,p.getUUID(),Action.LOOK.bit());
     var cursor=Wire.message("cursor");writeCursor(r,cursor);cursor.addProperty("owner",p.getUUID().toString());cursor.addProperty("ack",o.get("seq").getAsLong());broadcast(r,cursor);
    }
    case "scroll" -> {
     int d=Integer.signum(o.get("direction").getAsInt());if(d==0||!active(r))return;
     if(!r.permissions.owns(p.getUUID(),d>0?Action.SCROLL_UP:Action.SCROLL_DOWN))return;
     FastRelay.pulse(r.controls,p.getUUID(),(d>0?Action.SCROLL_UP:Action.SCROLL_DOWN).bit());
     if(r.open){if(menuMatches(r,o)&&player(r,r.body).containerMenu instanceof net.minecraft.world.inventory.MerchantMenu merchant){r.tradeScroll=Math.clamp(r.tradeScroll-d,0,Math.max(0,merchant.getOffers().size()-7));state(r);}return;}
     JsonObject msg=Wire.message("scroll");msg.addProperty("direction",d);send(player(r,r.body),msg);
    }
   }
  } catch(IllegalArgumentException|IllegalStateException|NullPointerException ex){error(p,ex.getMessage()==null?"无效请求":ex.getMessage());}
 }
 static boolean active(Room r){return r.linked&&r.permissions.complete();}
 static void join(Room r,ServerPlayer p){if(r.linked)throw new IllegalArgumentException("共控已启动，请先解散再按新人数组队");if(!p.isAlive())throw new IllegalArgumentException("请先复活");r.permissions.join(p.getUUID());membership.put(p.getUUID(),r);}
 static void link(Room r){
  if(r.linked)return;
  var body=player(r,r.body);if(body==null||!body.isAlive())throw new IllegalArgumentException("身体玩家当前不可用");
  for(UUID id:r.permissions.members().keySet())if(!id.equals(r.body)){
   var p=player(r,id);if(p==null)throw new IllegalArgumentException("有成员离线");r.previousMode.put(id,p.gameMode.getGameModeForPlayer());
  }
  Recovery.save(body.level().getServer(),r.previousMode);
  SharedAdvancements.merge(r);
  r.linked=true;r.frames.clear();r.lookEntity=-1;resetLook(r);
  for(UUID id:r.previousMode.keySet()){var p=player(r,id);p.setGameMode(GameType.SPECTATOR);p.setCamera(body);}
 }
 static void dissolve(Room r){
  FastRelay.remove(r.controls);
  for(UUID id:r.permissions.members().keySet()){
   membership.remove(id);var p=player(r,id);if(p!=null){p.setCamera(p);if(r.previousMode.containsKey(id))Recovery.restore(p);send(p,Wire.message("left"));}
  }
  rooms.remove(r.code);
 }
 static int combined(Room r){return r.frames.held(clock,r.permissions);}
 static CursorHistory.Point clickPoint(Room r,ServerPlayer p,JsonObject o){
  if(r.permissions.owns(p.getUUID(),Action.LOOK))return new CursorHistory.Point(r.cursorX,r.cursorY);
  if(!o.has("pointerFrom")||!o.has("pointerTo")||!o.has("pointerBlend"))return null;
  return r.cursorHistory.resolve(o.get("pointerFrom").getAsLong(),o.get("pointerTo").getAsLong(),o.get("pointerBlend").getAsDouble());
 }
 static void click(Room r,ServerPlayer body,int button,CursorHistory.Point point){
  var menu=body.containerMenu;
  // Fixed logical canvas: positions are normalized so different GUI scales agree.
  int x=(int)(point.x()*420)-122,y=(int)(point.y()*300)-45;
  if(button==0&&menu instanceof net.minecraft.world.inventory.MerchantMenu merchant){
   int offer=dev.fivefold.core.TradeLayout.offerAt(x,y,r.tradeScroll,merchant.getOffers().size());
   if(offer>=0){r.tradeSelection=offer;merchant.setSelectionHint(offer);merchant.tryMoveItems(offer);merchant.broadcastChanges();return;}
   if(x>=94&&x<100&&y>=18&&y<158){r.tradeScroll=dev.fivefold.core.TradeLayout.scrollAt(y,merchant.getOffers().size());return;}
  }
  int slot=-999;
  for(var s:menu.slots)if(x>=s.x&&x<s.x+16&&y>=s.y&&y<s.y+16){slot=s.index;break;}
  if(slot==-999)return; // no accidental outside-window drop; use dedicated Q owner.
  menu.clicked(slot,button,(combined(r)&Action.SNEAK.bit())!=0?ContainerInput.QUICK_MOVE:ContainerInput.PICKUP,body);
  menu.broadcastChanges();
 }
 static void drop(Room r,ServerPlayer body,CursorHistory.Point point){
  var menu=body.containerMenu;boolean all=(combined(r)&Action.SPRINT.bit())!=0;
  if(!menu.getCarried().isEmpty()){menu.clicked(-999,all?0:1,ContainerInput.PICKUP,body);menu.broadcastChanges();return;}
  int x=(int)(point.x()*420)-122,y=(int)(point.y()*300)-45;
  for(var s:menu.slots)if(x>=s.x&&x<s.x+16&&y>=s.y&&y<s.y+16){menu.clicked(s.index,all?1:0,ContainerInput.THROW,body);menu.broadcastChanges();break;}
 }
 public static void tick(ServerTickEvent.Post event){
  clock++;
  for(Room r:List.copyOf(rooms.values())){
   ServerPlayer body=player(r,r.body);if(body==null){dissolve(r);continue;}
   if(r.linked){
    if(r.lookEntity!=body.getId()||!r.lookDimension.equals(body.level().dimension().toString()))resetLook(r);
    if(!body.isAlive())r.open=false;else {
     var angle=r.controls.view().angles();body.setYRot((float)angle.yaw());body.setXRot((float)angle.pitch());
     if(body.containerMenu!=body.inventoryMenu)r.open=true;
    }
    for(UUID id:r.permissions.members().keySet())if(!id.equals(r.body)){
     var follower=player(r,id);if(follower==null){dissolve(r);break;}
     if(follower.level()!=body.level())follower.setCamera(follower);
     if(follower.getCamera()!=body)follower.setCamera(body);
    }
    if(!rooms.containsKey(r.code))continue;
    TaskRewards.tick(r);
    r.merged=active(r)?combined(r):0;
    sendFrame(r,body);
   }
   if(r.open||r.lastOpen||clock%2==0)state(r);
  }
 }
 static boolean menuMatches(Room r,JsonObject o){return o.has("menuEpoch")&&o.has("room")&&r.code.equals(o.get("room").getAsString())&&r.menuEpoch==o.get("menuEpoch").getAsInt()&&r.lastMenu==player(r,r.body).containerMenu;}
 static void sendFrame(Room r,ServerPlayer body){
  publishControls(r);
  r.merged=active(r)?combined(r):0;
  JsonObject frame=Wire.message("frame");frame.addProperty("room",r.code);frame.addProperty("held",r.open?0:r.merged);frame.addProperty("otherHeld",r.open||!active(r)?0:r.frames.heldExcept(clock,r.permissions,r.body));frame.addProperty("revision",r.permissions.revision());frame.addProperty("open",r.open);frame.addProperty("attackClicks",r.open||!active(r)?0:r.attackClicks);frame.addProperty("useClicks",r.open||!active(r)?0:r.useClicks);r.attackClicks=r.useClicks=0;send(body,frame);
 }
 static void writeCursor(Room r,JsonObject o){o.addProperty("room",r.code);o.addProperty("menuEpoch",r.menuEpoch);o.addProperty("cursorSerial",r.cursorSerial);o.addProperty("x",r.cursorX);o.addProperty("y",r.cursorY);}
 static void resetLook(Room r){
  var body=player(r,r.body);if(body==null)return;
  // Keep accepted orientation on permission changes; take the body's actual pose for respawn/dimension transitions.
  boolean same=r.lookEntity==body.getId()&&r.lookDimension.equals(body.level().dimension().toString());
  var current=r.controls.view().angles();
  double yaw=same?current.yaw():body.getYRot(),pitch=same?current.pitch():body.getXRot();
  r.lookEpoch++;r.lookEntity=body.getId();r.lookDimension=body.level().dimension().toString();
  publishControls(r,yaw,pitch);
 }
 static void publishControls(Room r,double yaw,double pitch){
  var body=player(r,r.body);if(body==null)return;
  var peers=new HashMap<UUID,net.minecraft.network.Connection>();
  for(UUID id:r.permissions.members().keySet()){var p=player(r,id);if(p!=null)peers.put(id,p.connection.getConnection());}
  var cfg=new RealtimeControls.Config(r.code,r.lookEpoch,r.permissions.revision(),r.body,r.permissions.members(),active(r)&&!r.open&&body.isAlive());
  FastRelay.publish(r.controls,cfg,yaw,pitch,peers);
 }
 static void publishControls(Room r){var body=player(r,r.body);if(body!=null)publishControls(r,body.getYRot(),body.getXRot());}
 static void writeLook(Room r,JsonObject o){FastRelay.writeView(r.controls,o);}
 static String menuType(AbstractContainerMenu menu){
  try{return BuiltInRegistries.MENU.getKey(menu.getType()).toString();}catch(IllegalStateException|NullPointerException ex){return "unsupported";}
 }
 static void state(Room r){
  ServerPlayer body=player(r,r.body);if(body==null)return;
  if(r.lastOpen!=r.open)resetLook(r);
  if(r.lastMenu!=body.containerMenu||r.lastOpen!=r.open){r.tradeSelection=r.tradeScroll=0;r.menuEpoch++;r.lastMenu=body.containerMenu;r.lastOpen=r.open;r.cursorSerial=0;r.cursorHistory.reset(r.cursorX,r.cursorY);}
  publishControls(r);
  JsonObject o=Wire.message("state");writeLook(r,o);writeCursor(r,o);
  boolean inventory=body.containerMenu==body.inventoryMenu;
  o.addProperty("menuType",inventory?"inventory":menuType(body.containerMenu));o.addProperty("menuId",body.containerMenu.containerId);
  JsonArray data=new JsonArray();if(r.open)for(var d:((MenuAccess)body.containerMenu).fivefold$data())data.add(d.get());o.add("menuData",data);o.addProperty("code",r.code);o.addProperty("body",r.body.toString());o.addProperty("entity",body.getId());
  o.addProperty("revision",r.permissions.revision());o.addProperty("linked",r.linked);o.addProperty("active",active(r));o.addProperty("open",r.open);
  o.addProperty("x",r.cursorX);o.addProperty("y",r.cursorY);o.addProperty("health",body.getHealth());o.addProperty("maxHealth",body.getMaxHealth());
  o.addProperty("food",body.getFoodData().getFoodLevel());o.addProperty("xp",body.experienceLevel);o.addProperty("selected",body.getInventory().getSelectedSlot());
  o.addProperty("air",body.getAirSupply());o.addProperty("maxAir",body.getMaxAirSupply());o.addProperty("underWater",body.isUnderWater());o.addProperty("xpProgress",body.experienceProgress);o.addProperty("survival",!body.isCreative()&&!body.isSpectator());
  o.addProperty("usingItem",body.isUsingItem());o.addProperty("useHand",body.getUsedItemHand().name());o.addProperty("useRemaining",body.getUseItemRemainingTicks());o.addProperty("mainArm",body.getMainArm().name());o.addProperty("scoping",body.isScoping());
  if(body.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu merchant){o.addProperty("tradeSelection",r.tradeSelection);o.addProperty("tradeScroll",r.tradeScroll);o.addProperty("tradeLevel",merchant.getTraderLevel());o.addProperty("tradeXp",merchant.getTraderXp());o.addProperty("tradeProgress",merchant.showProgressBar());o.addProperty("tradeRestock",merchant.canRestock());}
  JsonArray people=new JsonArray();for(var e:r.permissions.members().entrySet()){
   JsonObject member=new JsonObject();member.addProperty("id",e.getKey().toString());var p=player(r,e.getKey());member.addProperty("name",p==null?"离线":p.getName().getString());member.addProperty("mask",e.getValue());JsonArray keys=new JsonArray();for(String key:r.bindings.getOrDefault(e.getKey(),List.of()))keys.add(key);member.add("bindings",keys);people.add(member);
  }o.add("people",people);
  List<ItemStack> items=new ArrayList<>();for(int i=0;i<9;i++)items.add(body.getInventory().getItem(i).copy());
  JsonArray slots=new JsonArray();if(r.open){for(var s:body.containerMenu.slots){if(items.size()>=508)break;JsonObject slot=new JsonObject();slot.addProperty("x",s.x);slot.addProperty("y",s.y);slot.addProperty("id",s.index);slots.add(slot);items.add(s.getItem().copy());}items.add(body.containerMenu.getCarried().copy());}o.add("slots",slots);
  o.addProperty("equipmentOffset",items.size());items.add(body.getMainHandItem().copy());items.add(body.getOffhandItem().copy());
  var effectList=new JsonArray();for(var effect:body.getActiveEffects()){var e=new JsonObject();e.addProperty("type",BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString());e.addProperty("duration",effect.getDuration());e.addProperty("amplifier",effect.getAmplifier());e.addProperty("ambient",effect.isAmbient());e.addProperty("visible",effect.isVisible());e.addProperty("icon",effect.showIcon());effectList.add(e);}o.add("effectList",effectList);
  StringBuilder effects=new StringBuilder();for(var effect:body.getActiveEffects())effects.append(effect.getEffect().value().getDisplayName().getString()).append(' ').append(effect.getAmplifier()+1).append("  ");o.addProperty("effects",effects.toString());
  var offers=r.open&&body.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu merchant?merchant.getOffers().copy():null;
  for(UUID id:r.permissions.members().keySet()){var p=player(r,id);if(p!=null){o.add("task",taskState(r,id));PacketDistributor.sendToPlayer(p,new Wire(o.toString(),items,offers));}}
 }
 static JsonObject taskState(Room r,UUID viewer){
  var t=r.tasks;var o=new JsonObject();o.addProperty("id",t.id());o.addProperty("owner",t.owner()==null?"":t.owner().toString());o.addProperty("action",t.action()==null?-1:t.action().ordinal());o.addProperty("remaining",t.remainingTicks());o.addProperty("done",t.done());o.addProperty("paused",t.paused());o.addProperty("best",t.best());o.addProperty("window",t.windowRemainingTicks());o.addProperty("pool",t.poolHalfPoints());o.addProperty("mine",t.penalty(viewer));
  var responses=new JsonArray();for(var id:t.current())responses.add(id.toString());o.add("responses",responses);
  var leaders=new JsonArray();for(var e:t.leaders().entrySet()){var m=new JsonObject();m.addProperty("id",e.getKey().toString());m.addProperty("points",e.getValue());leaders.add(m);}o.add("leaders",leaders);return o;
 }
 public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)Recovery.restore(p);}
 public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)Recovery.restore(p);Room r=membership.get(e.getEntity().getUUID());if(r!=null)dissolve(r);}
 public static void stopping(ServerStoppingEvent e){for(Room r:List.copyOf(rooms.values()))dissolve(r);rooms.clear();membership.clear();clock=0;}
}
