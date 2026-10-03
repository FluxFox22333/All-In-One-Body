package dev.fivefold.client;
import java.util.Set;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import dev.fivefold.mixin.MenuAccess;

/** Native screens render isolated client menus; only the shared server menu performs actions. */
public final class SharedScreen extends Screen {
 public static boolean preparing;
 private static final Set<String> NATIVE=Set.of("generic_9x1","generic_9x2","generic_9x3","generic_9x4","generic_9x5","generic_9x6","generic_3x3","hopper","shulker_box","crafting","furnace","blast_furnace","smoker","brewing_stand","grindstone","smithing","cartography_table","merchant");
 private AbstractContainerScreen<?> nativeScreen;
 private int epoch=-1;private Object renderedItems;
 private double lastX=Double.NaN,lastY=Double.NaN;
 public SharedScreen(){super(Component.literal("共享容器"));}
 public boolean shouldCloseOnEsc(){return false;}
 public boolean isPauseScreen(){return false;}
 public void onClose(){}
 @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float dt){}
 @Override public boolean isInGameUi(){return true;}
 // Deliberately never invoke the delegate's removed()/onClose(): those mutate a player's menu.
 @Override protected void init(){epoch=-1;lastX=lastY=Double.NaN;}
 @SuppressWarnings({"rawtypes","unchecked"})
 private void updateMenu(){
  var state=ClientState.state;int next=state.get("menuEpoch").getAsInt();
  if(epoch!=next){
   epoch=next;nativeScreen=null;renderedItems=null;lastX=lastY=Double.NaN;
   String type=state.get("menuType").getAsString();
   var mirror=new RemotePlayer(minecraft.level,minecraft.player.getGameProfile());
   try {
    preparing=true;
    if(type.equals("inventory"))nativeScreen=new SharedInventoryScreen(mirror);
    else if(type.startsWith("minecraft:")&&NATIVE.contains(type.substring(10))){
     MenuType menuType=BuiltInRegistries.MENU.getValue(Identifier.parse(type));
     if(menuType!=null){var menu=menuType.create(state.get("menuId").getAsInt(),mirror.getInventory());var factory=MenuScreens.getScreenFactory(menuType);
      if(factory.isPresent()){var screen=((MenuScreens.ScreenConstructor)factory.get()).create(menu,mirror.getInventory(),Component.translatable(titleFor(type.substring(10))));if(screen instanceof AbstractContainerScreen<?> container)nativeScreen=container;}}
    }
    if(nativeScreen!=null){
     if(nativeScreen.getMenu().slots.size()!=state.getAsJsonArray("slots").size())nativeScreen=null;
     else {nativeScreen.init(width,height);((dev.fivefold.mixin.ContainerScreenAccess)nativeScreen).fivefold$mouseActions().clear();for(var child:nativeScreen.children())if(child instanceof AbstractWidget widget)widget.active=false;}
    }
   } catch(RuntimeException ex){nativeScreen=null;} finally {preparing=false;}
  }
  if(nativeScreen!=null&&renderedItems!=ClientState.items){
   var menu=nativeScreen.getMenu();var items=ClientState.items;
   if(menu instanceof net.minecraft.world.inventory.MerchantMenu merchant&&ClientState.offers!=null){
    merchant.setOffers(ClientState.offers.copy());merchant.setMerchantLevel(state.get("tradeLevel").getAsInt());merchant.setXp(state.get("tradeXp").getAsInt());merchant.setShowProgressBar(state.get("tradeProgress").getAsBoolean());merchant.setCanRestock(state.get("tradeRestock").getAsBoolean());
    int selected=Math.clamp(state.get("tradeSelection").getAsInt(),0,Math.max(0,merchant.getOffers().size()-1));merchant.setSelectionHint(selected);
    var access=(dev.fivefold.mixin.MerchantScreenAccess)nativeScreen;access.fivefold$selection(selected);access.fivefold$scroll(Math.clamp(state.get("tradeScroll").getAsInt(),0,Math.max(0,merchant.getOffers().size()-7)));
   }
   if(items.size()>=10+menu.slots.size())menu.initializeContents(0,items.subList(9,9+menu.slots.size()).stream().map(net.minecraft.world.item.ItemStack::copy).toList(),items.get(9+menu.slots.size()).copy());
   var data=state.getAsJsonArray("menuData");int count=((MenuAccess)menu).fivefold$data().size();for(int i=0;i<Math.min(count,data.size());i++)menu.setData(i,data.get(i).getAsInt());renderedItems=items;
  }
 }
 private static String titleFor(String type){return switch(type){case "merchant"->"entity.minecraft.villager";case "generic_9x6"->"container.chestDouble";case "generic_9x1","generic_9x2","generic_9x3","generic_9x4","generic_9x5"->"container.chest";case "generic_3x3"->"container.dispenser";case "shulker_box"->"container.shulkerBox";case "crafting"->"container.crafting";case "brewing_stand"->"container.brewing";case "cartography_table"->"container.cartography_table";case "smithing"->"container.upgrade";default->"container."+type;};}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
  if(ClientState.state==null||!ClientState.sharedOpen())return;
  updateMenu();
  int left=nativeScreen==null?width/2-88:nativeScreen.getLeftPos(),top=nativeScreen==null?height/2-105:nativeScreen.getTopPos();
  // Transmit slot-space coordinates, independent of resolution, GUI scale and native panel size.
  double x=(mx-left+122)/420.0,y=(my-top+45)/300.0;
  if(mx!=lastX||my!=lastY){ClientState.cursor(x,y);lastX=mx;lastY=my;}
  var pointer=ClientState.renderCursor();int cx=left+(int)(pointer.x()*420)-122,cy=top+(int)(pointer.y()*300)-45;
  if(nativeScreen!=null){
   // 26.3 draws screen backgrounds separately from extractRenderState.
   // Delegating only the latter leaves textures, models and progress bars invisible.
   g.fill(left,top,left+nativeScreen.getImageWidth(),top+nativeScreen.getImageHeight(),0xffc6c6c6);
   g.enableScissor(left,top,left+nativeScreen.getImageWidth(),top+nativeScreen.getImageHeight());
   try{nativeScreen.extractBackground(g,cx,cy,dt);}finally{g.disableScissor();}
   g.nextStratum();nativeScreen.extractRenderState(g,cx,cy,dt);
  }else extractFallback(g,left,top,cx,cy);
  g.nextStratum();g.fill(cx-4,cy,cx+5,cy+1,0xff63edce);g.fill(cx,cy-4,cx+1,cy+5,0xff63edce);
  TeamInputHud.renderInScreen(g,width,top);
  g.text(font,"Esc负责人关闭 · F8分工 · F9按键翻页 · F10菜单",8,height-11,0xffe8f2ff);
 }
 private void extractFallback(GuiGraphicsExtractor g,int left,int top,int cx,int cy){
  int minX=left-8,minY=top-8,maxX=left+176,maxY=top+166;
  for(var el:ClientState.state.getAsJsonArray("slots")){var slot=el.getAsJsonObject();minX=Math.min(minX,left+slot.get("x").getAsInt()-8);minY=Math.min(minY,top+slot.get("y").getAsInt()-8);maxX=Math.max(maxX,left+slot.get("x").getAsInt()+24);maxY=Math.max(maxY,top+slot.get("y").getAsInt()+24);}
  g.fill(minX,minY,maxX,maxY,0xff101824);g.text(font,"兼容槽位界面",minX+6,minY+2,0xffffcc88);
  var slots=ClientState.state.getAsJsonArray("slots");int i=9;
  for(var el:slots){var s=el.getAsJsonObject();int sx=left+s.get("x").getAsInt(),sy=top+s.get("y").getAsInt();g.fill(sx-1,sy-1,sx+17,sy+17,0xff415166);g.fill(sx,sy,sx+16,sy+16,0xff172331);if(i<ClientState.items.size()){var stack=ClientState.items.get(i);g.item(stack,sx,sy);g.itemDecorations(font,stack,sx,sy);if(cx>=sx&&cx<sx+16&&cy>=sy&&cy<sy+16&&!stack.isEmpty())g.setTooltipForNextFrame(font,stack,cx,cy);}i++;}
  if(i<ClientState.items.size()&&!ClientState.items.get(i).isEmpty()){g.item(ClientState.items.get(i),cx-8,cy-8);g.itemDecorations(font,ClientState.items.get(i),cx-8,cy-8);}
 }
}
