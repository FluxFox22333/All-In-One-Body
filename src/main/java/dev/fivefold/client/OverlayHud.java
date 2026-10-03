package dev.fivefold.client;
import dev.fivefold.core.Action;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Local presentation preference; never changes control or task authority. */
public final class OverlayHud {
 private static int mode; // compact by default, then detailed, then hidden
 public static void cycle(){mode=(mode+1)%3;}
 public static void showDetails(){mode=1;}
 public static boolean detailed(){return mode==1;}
 public static boolean hidden(){return mode==2;}
 public static void compact(GuiGraphicsExtractor g,int width){
  var mc=ClientState.mc();int w=Math.max(1,Math.min(260,width-12));int y=5;
  String status=ClientState.state.get("active").getAsBoolean()?"共控 "+ClientState.state.get("code").getAsString():"分工不完整，身体操作已停止";
  line(g,status+" · F7展开/隐藏",w,y,0xff8de2c2);y+=11;
  if(!ClientState.state.has("task"))return;
  var t=ClientState.state.getAsJsonObject("task");int a=t.get("action").getAsInt();boolean done=t.get("done").getAsBoolean();
  String task=t.get("paused").getAsBoolean()?"任务暂停":a<0?"等待任务":done?"任务完成 ✓":ControlLabels.key(Action.values()[a])+" · "+((t.get("remaining").getAsLong()+19)/20)+"秒 · "+t.getAsJsonArray("responses").size()+"/"+ClientState.state.getAsJsonArray("people").size();
  line(g,task+" · 公共 "+t.get("pool").getAsInt()/2.0+"/4",w,y,done?0xffff8888:0xffe5eaf5);
  if(a>=0&&!done&&!t.get("paused").getAsBoolean())line(g,"目标："+TaskHud.name(t.get("owner").getAsString())+" · "+Action.values()[a].label,w,y+11,0xffe5eaf5);
 }
 private static void line(GuiGraphicsExtractor g,String text,int width,int y,int color){
  var font=ClientState.mc().font;String clipped=font.plainSubstrByWidth(text,width-8);
  g.fill(4,y-1,12+font.width(clipped),y+10,0x60101c29);g.text(font,clipped,8,y,color);
 }
 private OverlayHud(){}
}
