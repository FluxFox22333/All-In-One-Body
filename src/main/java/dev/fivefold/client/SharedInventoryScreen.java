package dev.fivefold.client;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.player.Player;
/** Vanilla inventory renderer, with recipe shortcuts and personal creative switching disabled. */
final class SharedInventoryScreen extends InventoryScreen {
 SharedInventoryScreen(Player mirror){super(mirror);}
 @Override protected void init(){leftPos=(width-imageWidth)/2;topPos=(height-imageHeight)/2;}
 @Override public void containerTick(){}
 @Override protected void extractSlots(GuiGraphicsExtractor g,int x,int y){for(var slot:menu.slots)if(slot.isActive())extractSlot(g,slot,x,y);}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float dt){extractContents(g,x,y,dt);extractCarriedItem(g,x,y);extractTooltip(g,x,y);}
 @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float dt){
  g.blit(RenderPipelines.GUI_TEXTURED,INVENTORY_LOCATION,leftPos,topPos,0,0,imageWidth,imageHeight,256,256);
  var mc=ClientState.mc();var entity=mc.level.getEntity(ClientState.state.get("entity").getAsInt());
  if(entity instanceof Player player)extractEntityInInventoryFollowsMouse(g,leftPos+26,topPos+8,leftPos+75,topPos+78,30,.0625f,x,y,player);
 }
}
