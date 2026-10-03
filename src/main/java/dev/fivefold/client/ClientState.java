package dev.fivefold.client;
import com.google.gson.*;
import dev.fivefold.core.Action;
import dev.fivefold.core.MovementInput;
import dev.fivefold.core.RemoteMovement;
import dev.fivefold.core.PredictedLook;
import dev.fivefold.core.SmoothedLook;
import dev.fivefold.core.CursorTimeline;
import dev.fivefold.net.Wire;
import dev.fivefold.mixin.KeyAccess;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class ClientState {
 public static JsonObject state;
 public static List<ItemStack> items=List.of();
 public static net.minecraft.world.item.trading.MerchantOffers offers;
 static int challengeHeld;
 public static String error="";
 public static int physical,merged,attackClicks,useClicks;
 static final RemoteMovement realtimeMovement=new RemoteMovement();
 static int otherHeld;static long frameRevision=-1;static boolean frameOpen=true;
 static long lastFrame;static boolean lastContext;static int deadTicks;
 static double scrollRemainder;static String sentBindings="";
 static final SmoothedLook observerLook=new SmoothedLook(20_000_000L);
 static final CursorTimeline cursorTimeline=new CursorTimeline();
 static CursorTimeline.Sample drawnCursor;
 static double cursorX=.5,cursorY=.5;static long cursorSeq,cursorAck,cursorSerial=-1,lastCursorSend;static int cursorEpoch=-1;static boolean cursorDirty;
 static final PredictedLook predictedLook=new PredictedLook();
 static String viewRoom="";static int viewEpoch=-1;static boolean viewReady;static long lastLookSend;

 public static Minecraft mc(){return Minecraft.getInstance();}
 public static boolean linked(){return state!=null&&state.get("linked").getAsBoolean()&&mc().player!=null;}
 public static boolean body(){return state!=null&&mc().player!=null&&state.get("body").getAsString().equals(mc().player.getUUID().toString());}
 public static boolean sharedOpen(){return linked()&&state.get("open").getAsBoolean();}
 public static boolean gameContext(){return mc().gui.screen()==null||mc().gui.screen() instanceof SharedScreen;}
 public static int mask(){
  if(state==null||mc().player==null)return 0;
  for(var el:state.getAsJsonArray("people")){var o=el.getAsJsonObject();if(o.get("id").getAsString().equals(mc().player.getUUID().toString()))return o.get("mask").getAsInt();}return 0;
 }
 public static void send(JsonObject o){if(mc().getConnection()!=null)ClientPacketDistributor.sendToServer(new Wire(o));}
 public static void command(String command){send(Wire.message(command));}
 public static void receive(Wire w){
  var o=w.data();switch(o.get("kind").getAsString()){
   case "state" -> {state=o;items=w.items();offers=w.offers();SharedBody.apply();physical&=mask();if(linked()){acceptView(o);acceptCursor(o,false);applyMovement();}else clearView();}
   case "error" -> error=o.get("text").getAsString();
   case "left" -> {clearView();state=null;items=List.of();physical=merged=attackClicks=useClicks=0;KeyMapping.releaseAll();if(mc().player!=null)mc().setCameraEntity(mc().player);if(mc().gui.screen() instanceof SharedScreen)mc().gui.setScreen(null);}
   case "frame" -> {if(!linked()||!body()||!o.get("room").getAsString().equals(state.get("code").getAsString()))return;otherHeld=o.get("otherHeld").getAsInt();frameRevision=o.get("revision").getAsLong();frameOpen=o.get("open").getAsBoolean();merged=o.get("held").getAsInt();attackClicks+=o.has("attackClicks")?o.get("attackClicks").getAsInt():0;useClicks+=o.has("useClicks")?o.get("useClicks").getAsInt():0;lastFrame=System.nanoTime();applyMovement();}
   case "activity" -> TeamInputHud.receive(o);
   case "realtime_move" -> {
    if(!linked()||!body()||!o.get("room").getAsString().equals(state.get("code").getAsString()))return;
    if(realtimeMovement.accept(o.get("serial").getAsLong(),o.get("epoch").getAsInt(),o.get("revision").getAsLong(),o.get("otherHeld").getAsInt(),o.get("enabled").getAsBoolean(),System.nanoTime()))applyMovement();
   }
   case "view" -> {if(linked()&&o.get("room").getAsString().equals(state.get("code").getAsString()))acceptView(o);}
   case "cursor" -> {if(linked()&&o.get("room").getAsString().equals(state.get("code").getAsString()))acceptCursor(o,true);}
   case "scroll" -> {if(body())mc().player.getInventory().setSelectedSlot(Math.floorMod(mc().player.getInventory().getSelectedSlot()-o.get("direction").getAsInt(),9));}
  }
 }
 public static Map<Action,KeyMapping> mappings(){
  var o=mc().options;EnumMap<Action,KeyMapping> m=new EnumMap<>(Action.class);
  m.put(Action.FORWARD,o.keyUp);m.put(Action.BACK,o.keyDown);m.put(Action.LEFT,o.keyLeft);m.put(Action.RIGHT,o.keyRight);m.put(Action.JUMP,o.keyJump);
  m.put(Action.ATTACK,o.keyAttack);m.put(Action.USE,o.keyUse);m.put(Action.DROP,o.keyDrop);m.put(Action.INVENTORY,o.keyInventory);m.put(Action.SNEAK,o.keyShift);m.put(Action.SPRINT,o.keySprint);return m;
 }
 static void hold(Action a,boolean down){
  int rawBefore=challengeHeld,before=physical;
  if((dev.fivefold.core.CooperationTasks.ELIGIBLE&a.bit())!=0)challengeHeld=down?challengeHeld|a.bit():challengeHeld&~a.bit();
  if((mask()&a.bit())!=0)physical=down?physical|a.bit():physical&~a.bit();
  if(before!=physical||rawBefore!=challengeHeld){TeamInputHud.observeLocal();sendInput();applyMovement();}
 }
 static void sendInput(){var o=Wire.message("input");
  var bindings=new JsonArray();for(Action a:Action.values())bindings.add(ControlLabels.key(a));String signature=bindings.toString();if(!signature.equals(sentBindings)){o.add("bindings",bindings);sentBindings=signature;}
  if(state!=null&&state.has("tradeScroll"))o.addProperty("tradeScroll",state.get("tradeScroll").getAsInt());o.addProperty("held",physical);o.addProperty("challengeHeld",challengeHeld);if(state!=null&&state.has("task"))o.addProperty("task",state.getAsJsonObject("task").get("id").getAsLong());if(state!=null){o.addProperty("room",state.get("code").getAsString());o.addProperty("revision",state.get("revision").getAsLong());o.addProperty("epoch",viewEpoch);o.addProperty("menuEpoch",state.get("menuEpoch").getAsInt());if(drawnCursor!=null){o.addProperty("pointerFrom",drawnCursor.from());o.addProperty("pointerTo",drawnCursor.to());o.addProperty("pointerBlend",drawnCursor.blend());}}flushCursor(true);send(o);}
 public static boolean key(int action,KeyEvent event){
  if(mc().player!=null&&event.key()==InputConstants.KEY_F8){if(action==InputConstants.PRESS){physical=challengeHeld=0;if(linked())sendInput();mc().gui.setScreen(new SettingsScreen(mc().gui.screen()));}return true;}
  if(!linked())return false;
  if(event.key()==InputConstants.KEY_F7){if(action==InputConstants.PRESS)OverlayHud.cycle();return true;}
  if(event.key()==InputConstants.KEY_F9){if(action==InputConstants.PRESS){if(OverlayHud.detailed()||mc().gui.screen()!=null)TeamInputHud.nextPage();else OverlayHud.showDetails();}return true;}
  if(gameContext()&&mc().options.keyAdvancements.matches(event)){
   if(action==InputConstants.PRESS){physical=challengeHeld=0;sendInput();mc().gui.setScreen(new AdvancementsScreen(mc().getConnection().getAdvancements()));applyMovement();}
   return true;
  }
  if(event.key()==InputConstants.KEY_F10){if(action==InputConstants.PRESS){physical=challengeHeld=0;sendInput();mc().gui.setScreen(new PauseScreen(true));}return true;}
  if(!gameContext())return false;
  if(action!=InputConstants.PRESS&&action!=InputConstants.RELEASE)return true;
  if(event.key()==InputConstants.KEY_ESCAPE){hold(Action.CLOSE,action==InputConstants.PRESS);return true;}
  for(var e:mappings().entrySet())if(e.getValue().matches(event))hold(e.getKey(),action==InputConstants.PRESS);
  return true;
 }
 public static boolean mouse(MouseButtonInfo info,int action){
  if(!linked()||!gameContext())return false;
  if(info.button()==InputConstants.MOUSE_BUTTON_LEFT)hold(Action.ATTACK,action==InputConstants.PRESS);
  if(info.button()==InputConstants.MOUSE_BUTTON_RIGHT)hold(Action.USE,action==InputConstants.PRESS);
  return true;
 }
 public static boolean scroll(double delta){
  if(!linked()||!gameContext())return false;
  scrollRemainder+=delta;int d=(int)scrollRemainder;scrollRemainder-=d;
  if(d!=0&&(mask()&(d>0?Action.SCROLL_UP:Action.SCROLL_DOWN).bit())!=0){TeamInputHud.pulseLocal(d>0?Action.SCROLL_UP:Action.SCROLL_DOWN);var o=Wire.message("scroll");o.addProperty("room",state.get("code").getAsString());o.addProperty("menuEpoch",state.get("menuEpoch").getAsInt());o.addProperty("direction",Integer.signum(d));send(o);}return true;
 }
 static void clearView(){challengeHeld=0;offers=null;TeamInputHud.reset();sentBindings="";realtimeMovement.reset();otherHeld=0;frameRevision=-1;frameOpen=true;lastFrame=0;viewReady=false;viewRoom="";viewEpoch=-1;lastLookSend=0;predictedLook.reset(0,0);cursorEpoch=-1;cursorSeq=cursorAck=0;cursorSerial=-1;cursorDirty=false;drawnCursor=null;}
 static void acceptView(JsonObject o){
  String room=o.get("room").getAsString();int epoch=o.get("epoch").getAsInt();
  if(viewReady&&room.equals(viewRoom)&&epoch<viewEpoch)return;
  if(!viewReady||!room.equals(viewRoom)||epoch!=viewEpoch){predictedLook.reset(o.get("yaw").getAsDouble(),o.get("pitch").getAsDouble());observerLook.reset(o.get("yaw").getAsDouble(),o.get("pitch").getAsDouble(),System.nanoTime());viewRoom=room;viewEpoch=epoch;viewReady=true;lastLookSend=0;sentBindings="";}
  var acks=o.getAsJsonObject("viewAcks");String me=mc().player.getUUID().toString();long ack=acks.has(me)?acks.get(me).getAsLong():0;
  predictedLook.reconcile(o.get("viewSerial").getAsLong(),o.get("yaw").getAsDouble(),o.get("pitch").getAsDouble(),ack);
  observerLook.accept(o.get("viewSerial").getAsLong(),o.get("yaw").getAsDouble(),o.get("pitch").getAsDouble(),System.nanoTime());
  applyView();
 }
 static void applyView(){
  if(!viewReady||!linked()||mc().level==null)return;
  var entity=body()?mc().player:mc().level.getEntity(state.get("entity").getAsInt());if(entity==null)return;
  var angle=predictedLook.angles();entity.setYRot((float)angle.yaw());entity.setXRot((float)angle.pitch());
  // Rotation must not pass through remote entity interpolation a second time.
  entity.yRotO=(float)angle.yaw();entity.xRotO=(float)angle.pitch();
 }
 static void flushLook(){
  if(!viewReady||!linked())return;
  long now=System.nanoTime();if(now-lastLookSend<16_666_667L)return;
  var samples=predictedLook.unsent();if(samples.isEmpty())return;
  JsonObject o=Wire.message("look_batch");o.addProperty("room",viewRoom);o.addProperty("epoch",viewEpoch);o.addProperty("revision",state.get("revision").getAsLong());JsonArray batch=new JsonArray();
  for(var sample:samples){JsonArray a=new JsonArray();a.add(sample.sequence());a.add(sample.dx());a.add(sample.dy());batch.add(a);}o.add("samples",batch);
  send(o);predictedLook.sent(samples.getLast().sequence());lastLookSend=now;
 }
 public static void look(double dx,double dy){
  if(!viewReady||!linked()||!gameContext()||sharedOpen()||!state.get("active").getAsBoolean()||(mask()&Action.LOOK.bit())==0)return;
  var opt=mc().options;double sensitivity=Math.pow(opt.sensitivity().get()*0.6+0.2,3)*8*0.15;
  // Mirror vanilla's reduced sensitivity when using a spyglass.
  if(mc().getCameraEntity() instanceof net.minecraft.world.entity.player.Player living&&living.isScoping()&&opt.getCameraType().isFirstPerson())sensitivity/=8;
  double x=Math.clamp(dx*sensitivity*(opt.invertMouseX().get()?-1:1),-3600,3600);
  double y=Math.clamp(dy*sensitivity*(opt.invertMouseY().get()?-1:1),-3600,3600);
  if(x!=0||y!=0)TeamInputHud.pulseLocal(Action.LOOK);
  predictedLook.predict(x,y);applyView();flushLook();
 }
 public static void camera(ViewportEvent.ComputeCameraAngles event){
  if(!viewReady||!linked()||mc().getCameraEntity()==null||mc().getCameraEntity().getId()!=state.get("entity").getAsInt())return;
  flushLook();applyView();var angle=(mask()&Action.LOOK.bit())!=0?predictedLook.angles():observerLook.sample(System.nanoTime());
  // Camera.setup applies third-person mirroring AFTER this event.
  event.setYaw((float)angle.yaw());event.setPitch((float)angle.pitch());
 }
 static void acceptCursor(JsonObject o,boolean fast){
  int epoch=o.get("menuEpoch").getAsInt();if(epoch<cursorEpoch)return;
  boolean reset=epoch!=cursorEpoch;
  if(reset){cursorEpoch=epoch;cursorSeq=cursorAck=0;cursorSerial=-1;cursorDirty=false;}
  long serial=o.get("cursorSerial").getAsLong();if(serial<cursorSerial)return;
  cursorSerial=serial;
  if(reset){drawnCursor=null;cursorTimeline.reset(serial,o.get("x").getAsDouble(),o.get("y").getAsDouble(),System.nanoTime());}
  else cursorTimeline.accept(serial,o.get("x").getAsDouble(),o.get("y").getAsDouble(),System.nanoTime());
  if(fast&&o.get("owner").getAsString().equals(mc().player.getUUID().toString()))cursorAck=Math.max(cursorAck,o.get("ack").getAsLong());
  if(reset||(mask()&Action.LOOK.bit())==0||!cursorDirty&&cursorAck>=cursorSeq){cursorX=o.get("x").getAsDouble();cursorY=o.get("y").getAsDouble();}
 }
 public static CursorTimeline.Sample renderCursor(){
  drawnCursor=(mask()&Action.LOOK.bit())!=0?new CursorTimeline.Sample(cursorSerial,cursorSerial,0,cursorX,cursorY):cursorTimeline.sample(System.nanoTime());return drawnCursor;
 }
 public static void cursor(double x,double y){
  if((mask()&Action.LOOK.bit())==0||!sharedOpen()||!state.get("active").getAsBoolean())return;
  x=Math.clamp(x,0,1);y=Math.clamp(y,0,1);if(x==cursorX&&y==cursorY)return;
  cursorX=x;cursorY=y;cursorDirty=true;TeamInputHud.pulseLocal(Action.LOOK);flushCursor(false);
 }
 static void flushCursor(boolean force){
  long now=System.nanoTime();if(!cursorDirty||!sharedOpen()||!force&&now-lastCursorSend<16_666_667L)return;
  var o=Wire.message("look");o.addProperty("room",state.get("code").getAsString());o.addProperty("menuEpoch",cursorEpoch);o.addProperty("seq",++cursorSeq);o.addProperty("x",cursorX);o.addProperty("y",cursorY);send(o);cursorDirty=false;lastCursorSend=now;
 }
 public static int movementHeld(){
  if(!linked()||!body())return 0;
  boolean enabled=gameContext()&&!mc().player.isDeadOrDying()&&state.get("active").getAsBoolean()&&!sharedOpen()&&realtimeMovement.active(viewEpoch,state.get("revision").getAsLong(),System.nanoTime());
  return MovementInput.resolve(realtimeMovement.held(),physical,mask(),enabled,mc().isWindowActive());
 }
 public static void applyMovement(){
  if(!linked()||!body())return;
  int held=movementHeld();
  for(var entry:mappings().entrySet())if((entry.getKey().bit()&MovementInput.MASK)!=0)((KeyAccess)entry.getValue()).fivefold$down((held&entry.getKey().bit())!=0);
 }
 public static void tick(ClientTickEvent.Pre e){
  if(mc().player==null){clearView();state=null;items=List.of();physical=merged=0;return;}
  if(!linked())return;
  SharedBody.apply();
  if(!body()&&mc().level!=null){var entity=mc().level.getEntity(state.get("entity").getAsInt());if(entity!=null&&mc().getCameraEntity()!=entity)mc().setCameraEntity(entity);}
  boolean context=gameContext();if(!context&&lastContext){physical=challengeHeld=0;sendInput();}lastContext=context;
  if(!mc().isWindowActive())physical=challengeHeld=0;
  TeamInputHud.observeLocal();
  sendInput();
  flushLook();flushCursor(false);applyView();
  if(sharedOpen()&&mc().gui.screen()==null)mc().gui.setScreen(new SharedScreen());
  if(!sharedOpen()&&mc().gui.screen() instanceof SharedScreen)mc().gui.setScreen(null);
  if(body()&&mc().player.isDeadOrDying()){
   physical=merged=0;if(++deadTicks==60){mc().player.respawn();mc().gui.setScreen(null);}return;
  }deadTicks=0;
  // Raw input is intercepted; only authoritative merged input drives the body.
  for(KeyMapping key:mc().options.keyMappings){((KeyAccess)key).fivefold$down(false);((KeyAccess)key).fivefold$clicks(0);}
  int held=body()&&context&&System.nanoTime()-lastFrame<600_000_000L?merged:0;
  for(var entry:mappings().entrySet()){
   Action a=entry.getKey();if(a==Action.DROP||a==Action.INVENTORY)continue;
   KeyAccess k=(KeyAccess)entry.getValue();k.fivefold$down((held&a.bit())!=0);
   if(body()&&context&&!sharedOpen())k.fivefold$clicks(a==Action.ATTACK?Math.min(attackClicks,5):a==Action.USE?Math.min(useClicks,5):0);
  }attackClicks=useClicks=0;applyMovement();
 }
 public static void initScreen(ScreenEvent.Init.Post e){
  Screen s=e.getScreen();if(s instanceof OptionsScreen||s instanceof PauseScreen)e.addListener(Button.builder(Component.literal("多人一体"),b->mc().gui.setScreen(new SettingsScreen(s))).bounds(5,5,85,20).build());
 }
 public static void opening(ScreenEvent.Opening e){if(linked()&&e.getNewScreen() instanceof AbstractContainerScreen<?>)e.setNewScreen(new SharedScreen());}
 public static void hud(RenderGuiEvent.Post e){
  if(!linked()||mc().gui.screen()!=null||mc().gui.hud.isHidden()||OverlayHud.hidden())return;var g=e.getGuiGraphics();int h=mc().getWindow().getGuiScaledHeight(),w=mc().getWindow().getGuiScaledWidth();
  if(!OverlayHud.detailed()){OverlayHud.compact(g,w);return;}
  int panelWidth=Math.min(300,w-16);var lines=ControlLabels.hudLines(mask(),panelWidth-12);
  int panelBottom=37+lines.size()*12;
  g.fill(4,4,4+panelWidth,panelBottom,0x80101c29);
  g.text(mc().font,"共控 "+state.get("code").getAsString()+" · 我的操作",10,9,0xff8de2c2);
  g.text(mc().font,"F7收起 · F8分工 · F10菜单 · "+mc().options.keyAdvancements.getTranslatedKeyMessage().getString()+"进度",10,21,0xffb8c7d8);
  int lineY=35;for(String line:lines){g.text(mc().font,line,10,lineY,0xffe7f0ff);lineY+=12;}
  if(!state.get("active").getAsBoolean())g.text(mc().font,"分工不完整，身体操作已停止",10,panelBottom+4,0xffffbb66);
  int taskBottom=TaskHud.render(g,w,panelBottom+(state.get("active").getAsBoolean()?4:16));
  TeamInputHud.render(g,w,h,taskBottom);
 }
}
