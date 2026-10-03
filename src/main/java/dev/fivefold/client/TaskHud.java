package dev.fivefold.client;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import dev.fivefold.core.Action;
import com.google.gson.*;
import java.util.*;
public final class TaskHud {
 private static String name(String id){for(var e:ClientState.state.getAsJsonArray("people")){var p=e.getAsJsonObject();if(p.get("id").getAsString().equals(id))return p.get("name").getAsString();}return "—";}
 public static int render(GuiGraphicsExtractor g,int width,int y){
  if(!ClientState.state.has("task"))return y;
  var t=ClientState.state.getAsJsonObject("task");var mc=ClientState.mc();int w=Math.min(390,width-16);int a=t.get("action").getAsInt();
  var lines=new ArrayList<String>();boolean done=t.get("done").getAsBoolean(),paused=t.get("paused").getAsBoolean();
  lines.add("协作任务 · "+(paused?"暂停（界面/身体不可用）":((t.get("remaining").getAsLong()+19)/20)+"秒")+" · 公共 "+(t.get("pool").getAsInt()/2.0)+"/4");
  lines.add(a<0?"等待可用任务":name(t.get("owner").getAsString())+" · "+Action.values()[a].label+" ["+ControlLabels.key(Action.values()[a])+"]"+(done?" ✓":""));
  lines.add(done?"已完成，等待下一轮":"3秒内各按一次 · 当前 "+t.getAsJsonArray("responses").size()+"/"+ClientState.state.getAsJsonArray("people").size()+" · 最佳 "+t.get("best").getAsInt());
  var answered=new HashSet<String>();for(var e:t.getAsJsonArray("responses"))answered.add(e.getAsString());
  StringBuilder response=new StringBuilder();for(var e:ClientState.state.getAsJsonArray("people")){var p=e.getAsJsonObject();if(!response.isEmpty())response.append("  ");response.append(answered.contains(p.get("id").getAsString())?"✓":"·").append(p.get("name").getAsString());}
  if(!done)lines.add(response.toString());
  StringBuilder top=new StringBuilder();for(var e:t.getAsJsonArray("leaders")){var p=e.getAsJsonObject();if(!top.isEmpty())top.append("、");top.append(name(p.get("id").getAsString())).append(' ').append(p.get("points").getAsInt());}
  lines.add("我的惩罚 "+t.get("mine").getAsInt()+"/4 · 最高："+(top.isEmpty()?"无":top));
  g.fill(4,y,4+w,y+lines.size()*12+6,0xd8101c29);
  for(int i=0;i<lines.size();i++)g.text(mc.font,mc.font.plainSubstrByWidth(lines.get(i),w-12),10,y+3+i*12,i==1&&done?0xffff6666:0xffe5eaf5);
  return y+lines.size()*12+6;
 }
 private TaskHud(){}
}
