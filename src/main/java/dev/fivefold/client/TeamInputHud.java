package dev.fivefold.client;
import com.google.gson.*;
import dev.fivefold.core.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.*;

/** Live input feedback is presentation only; it never feeds movement or interactions. */
public final class TeamInputHud {
 private static final Map<String,ControlActivity.State> remote=new HashMap<>();
 private static final ControlActivity local=new ControlActivity();
 private static long serial=-1,revision=-1,received;private static int epoch=-1,page,pages=1;
 public static void reset(){remote.clear();local.clear();serial=revision=-1;epoch=-1;received=0;page=0;pages=1;}
 public static void nextPage(){page=(page+1)%Math.max(1,pages);}
 public static void observeLocal(){if(ClientState.mc().player!=null)local.observe(ClientState.mc().player.getUUID(),ClientState.physical&ClientState.mask(),System.nanoTime());}
 public static void pulseLocal(Action action){if(ClientState.mc().player!=null)local.pulse(ClientState.mc().player.getUUID(),action.bit()&ClientState.mask(),System.nanoTime());}
 public static void receive(JsonObject o){
  if(!ClientState.linked()||!o.get("room").getAsString().equals(ClientState.state.get("code").getAsString()))return;
  long next=o.get("serial").getAsLong();if(next<=serial)return;
  serial=next;epoch=o.get("epoch").getAsInt();revision=o.get("revision").getAsLong();received=System.nanoTime();remote.clear();
  for(var el:o.getAsJsonArray("members")){var p=el.getAsJsonObject();remote.put(p.get("id").getAsString(),new ControlActivity.State(p.get("held").getAsInt(),p.get("recent").getAsInt()));}
 }
 private record Row(String text,int color){}
 private static List<Row> rows(int width,boolean compact){
  var rows=new ArrayList<Row>();var mc=ClientState.mc();long now=System.nanoTime();
  for(var el:ClientState.state.getAsJsonArray("people")){
   var person=el.getAsJsonObject();String id=person.get("id").getAsString();int permission=person.get("mask").getAsInt();boolean own=id.equals(mc.player.getUUID().toString());
   var input=remote.getOrDefault(id,new ControlActivity.State(0,0));
   if(now-received>700_000_000L||revision!=ClientState.state.get("revision").getAsLong()||epoch!=ClientState.viewEpoch)input=new ControlActivity.State(0,0);
   if(own){int bits=mc.isWindowActive()&&ClientState.gameContext()?ClientState.physical:0;local.observe(mc.player.getUUID(),bits&permission,now);input=local.snapshot(Map.of(mc.player.getUUID(),permission),now).get(mc.player.getUUID());}
   int held=input.held()&permission,recent=input.recent()&permission;String prefix=person.get("name").getAsString()+(own?"(我)":"")+"：";
   var keys=new ArrayList<String>();var bindings=person.has("bindings")?person.getAsJsonArray("bindings"):new JsonArray();
   for(Action a:Action.values())if(((held|recent)&a.bit())!=0){String key=own?ControlLabels.key(a):bindings.size()==Action.values().length?bindings.get(a.ordinal()).getAsString():a.label;keys.add(key+((held&a.bit())==0?"·":""));}
   int color=held!=0?0xff83e6bc:recent!=0?0xffffce88:0xff99aabb;
   String line=prefix+(keys.isEmpty()?"—":"");
   for(String key:keys){String next=line+(line.equals(prefix)?"":" + ")+key;
    if(!compact&&mc.font.width(next)>width&&!line.equals(prefix)){rows.add(new Row(line,color));line="  "+key;}else line=next;
   }
   rows.add(new Row(mc.font.plainSubstrByWidth(line,width),color));
  }
  return rows;
 }
 public static void render(GuiGraphicsExtractor g,int width,int height,int ownPanelBottom){
  if(!ClientState.linked())return;
  int panelWidth=Math.min(270,width-16),x=width>=700?width-panelWidth-6:6,y=width>=700?58:ownPanelBottom+6;
  renderAt(g,x,y,panelWidth,Math.max(12,Math.min(80,height-y-48)),false);
 }
 public static void renderInScreen(GuiGraphicsExtractor g,int width,int top){
  if(!ClientState.linked())return;
  // Compact rows sit above the native panel. Very small margins page one row at a time.
  renderAt(g,6,2,width-12,Math.max(24,top-4),true);
 }
 private static void renderAt(GuiGraphicsExtractor g,int x,int y,int width,int maxHeight,boolean compact){
  var mc=ClientState.mc();var rows=rows(width-10,compact);int header=compact?0:14;int capacity=Math.max(1,(maxHeight-header)/11);
  pages=Math.max(1,(rows.size()+capacity-1)/capacity);page=Math.min(page,pages-1);
  int start=page*capacity,end=Math.min(rows.size(),start+capacity);
  g.fill(x,y,x+width,y+header+(end-start)*11,0x70101c29);
  String title="全队按键 · 点号=刚操作"+(pages>1?" · F9 "+(page+1)+"/"+pages:"");
  if(!compact)g.text(mc.font,mc.font.plainSubstrByWidth(title,width-10),x+5,y+2,0xffcee5f4);
  for(int i=start;i<end;i++)g.text(mc.font,rows.get(i).text(),x+5,y+header+(i-start)*11,rows.get(i).color());
 }
 private TeamInputHud(){}
}
