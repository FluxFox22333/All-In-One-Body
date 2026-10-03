package dev.fivefold.client;
import com.google.gson.*;
import dev.fivefold.core.Action;
import dev.fivefold.core.ControlLayout;
import dev.fivefold.net.Wire;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Category tabs keep five readable rows; member columns reveal ownership without hovering. */
public final class SettingsScreen extends Screen {
 final Screen parent;
 final Map<Action,Button> buttons=new EnumMap<>(Action.class);
 EditBox code;Button start,reset;boolean hadRoom,queried;int group,page,memberCount;String shownRoom="";ControlLayout layout;
 final String[] groups={"移动","鼠标","辅助"};
 public SettingsScreen(Screen parent){super(Component.literal("多人一体 · 操作分工"));this.parent=parent;}
 List<JsonObject> people(){var list=new ArrayList<JsonObject>();if(ClientState.state!=null)for(var el:ClientState.state.getAsJsonArray("people"))list.add(el.getAsJsonObject());return list;}
 boolean mine(JsonObject person){return minecraft.player!=null&&person.get("id").getAsString().equals(minecraft.player.getUUID().toString());}
 protected void init(){
  buttons.clear();hadRoom=ClientState.state!=null;int panel=Math.min(300,width-24),x=(width-panel)/2;
  if(!hadRoom){
   addRenderableWidget(Button.builder(Component.literal("创建房间：由我作为身体"),b->ClientState.command("create")).bounds(x,70,panel,20).build());
   code=addRenderableWidget(new EditBox(font,x,105,panel-85,20,Component.literal("六位房间号")));code.setMaxLength(6);
   addRenderableWidget(Button.builder(Component.literal("加入房间"),b->{var o=Wire.message("join");o.addProperty("code",code.getValue().trim());ClientState.send(o);}).bounds(x+panel-80,105,80,20).build());
  }else{
   var members=people();memberCount=members.size();layout=ControlLayout.of(width,height,Math.max(1,memberCount));
   String room=ClientState.state.get("code").getAsString();
   if(!room.equals(shownRoom)){shownRoom=room;for(int i=0;i<members.size();i++)if(mine(members.get(i)))page=i/layout.visibleMembers();}
   page=Math.clamp(page,0,(memberCount-1)/layout.visibleMembers());
   int tabWidth=Math.min(88,(width-100)/3);
   for(int i=0;i<3;i++){final int selected=i;var tab=addRenderableWidget(Button.builder(Component.literal(groups[i]),b->{group=selected;rebuildWidgets();}).bounds(12+i*(tabWidth+3),52,tabWidth,18).build());tab.active=i!=group;}
   int pages=(memberCount+layout.visibleMembers()-1)/layout.visibleMembers();
   if(pages>1){
    var prev=addRenderableWidget(Button.builder(Component.literal("‹"),b->{page--;rebuildWidgets();}).bounds(width-61,52,22,18).build());prev.active=page>0;
    var next=addRenderableWidget(Button.builder(Component.literal("›"),b->{page++;rebuildWidgets();}).bounds(width-35,52,22,18).build());next.active=page+1<pages;
   }
   for(int c=0;c<layout.visibleMembers();c++){
    int index=page*layout.visibleMembers()+c;if(index>=members.size())break;
    if(!mine(members.get(index)))continue;
    for(Action a:Action.values())if(a.group==group){
     var button=Button.builder(Component.empty(),b->{ClientState.error="";var o=Wire.message("claim");o.addProperty("mask",ClientState.mask()^a.bit());o.addProperty("revision",ClientState.state.get("revision").getAsLong());ClientState.send(o);}).bounds(layout.columnX(c)+2,layout.rowY(a.ordinal()%5)+1,layout.memberWidth()-4,layout.rowHeight()-2).build();
     buttons.put(a,addRenderableWidget(button));
    }
   }
   int cell=(panel-6)/3;
   start=addRenderableWidget(Button.builder(Component.literal("启动共控"),b->ClientState.command("start")).bounds(x,layout.footerY(),cell,20).build());
   reset=addRenderableWidget(Button.builder(Component.literal("按人数分配"),b->ClientState.command("defaults")).bounds(x+cell+3,layout.footerY(),cell,20).build());
   addRenderableWidget(Button.builder(Component.literal("退出并解散"),b->ClientState.command("leave")).bounds(x+2*(cell+3),layout.footerY(),cell,20).build());
   refresh();
  }
  addRenderableWidget(Button.builder(Component.literal("返回游戏"),b->onClose()).bounds(width/2-50,height-24,100,20).build());
  if(!queried&&minecraft.player!=null){queried=true;ClientState.command("query");}
 }
 void refresh(){
  for(var e:buttons.entrySet()){
   boolean assigned=(ClientState.mask()&e.getKey().bit())!=0;
   e.getValue().setMessage(Component.literal(assigned?"✓ 已分配":"＋ 领取"));
   e.getValue().setTooltip(Tooltip.create(Component.literal(ControlLabels.describe(e.getKey())+"\n"+(assigned?"点击取消自己的权限":"点击领取；可与队友共享"))));
  }
  if(start!=null){start.active=ClientState.body()&&!ClientState.linked();reset.active=ClientState.body();}
 }
 public void tick(){
  if(hadRoom!=(ClientState.state!=null)||hadRoom&&people().size()!=memberCount){rebuildWidgets();return;}
  if(hadRoom)refresh();
 }
 public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
  g.fill(0,0,width,height,0xff101924);g.text(font,title,12,8,0xffe8f2ff);
  if(hadRoom&&ClientState.state!=null){
   var members=people();
   g.text(font,"房间 "+ClientState.state.get("code").getAsString()+" · "+members.size()+"人 · "+(ClientState.linked()?"已连接":"等待启动"),12,24,0xff77cbbb);
   g.text(font,"点击自己的列分配；同一操作可多人负责。",12,38,0xffb8c7d8);
   g.text(font,"操作（本机按键）",12,78,0xffaebed0);
   for(int c=0;c<layout.visibleMembers();c++){
    int index=page*layout.visibleMembers()+c;if(index>=members.size())break;
    var person=members.get(index);int cx=layout.columnX(c);boolean self=mine(person);
    String full=person.get("name").getAsString()+(self?"（我）":"");
    g.text(font,font.plainSubstrByWidth(full,layout.memberWidth()-6),cx+3,78,self?0xff8df0be:0xffe8f2ff);
    if(mx>=cx&&mx<cx+layout.memberWidth()&&my>=74&&my<91)g.setTooltipForNextFrame(font,Component.literal(full+(person.get("id").getAsString().equals(ClientState.state.get("body").getAsString())?" · 身体":"")),mx,my);
   }
   for(Action a:Action.values())if(a.group==group){
    int row=a.ordinal()%5,y=layout.rowY(row);boolean covered=false;
    g.fill(12,y,width-12,y+layout.rowHeight()-1,row%2==0?0xff1a2938:0xff152330);
    String text=ControlLabels.describe(a);g.text(font,font.plainSubstrByWidth(ControlLabels.table(a),layout.actionWidth()-8),16,y+(layout.rowHeight()-9)/2,0xffe8f2ff);
    if(mx>=12&&mx<layout.columnX(0)&&my>=y&&my<y+layout.rowHeight())g.setTooltipForNextFrame(font,Component.literal(text),mx,my);
    for(var person:members)covered|=(person.get("mask").getAsInt()&a.bit())!=0;
    for(int c=0;c<layout.visibleMembers();c++){
     int index=page*layout.visibleMembers()+c;if(index>=members.size())break;
     var person=members.get(index);if(mine(person))continue;
     boolean assigned=(person.get("mask").getAsInt()&a.bit())!=0;
     String label=assigned?"✓":"—";g.text(font,label,layout.columnX(c)+(layout.memberWidth()-font.width(label))/2,y+(layout.rowHeight()-9)/2,assigned?0xff7ee8ae:0xff607589);
    }
    if(!covered)g.fill(12,y,14,y+layout.rowHeight()-1,0xffffa45d);
   }
   String status=ClientState.error.isEmpty()?"橙边=无人负责 · 每人需键盘和鼠标权限":ClientState.error;
   g.text(font,font.plainSubstrByWidth(status,width-24),12,height-61,ClientState.error.isEmpty()?0xffb8c7d8:0xffffb088);
   if(!ClientState.error.isEmpty()&&my>=height-65&&my<height-49)g.setTooltipForNextFrame(font,Component.literal(ClientState.error),mx,my);
  }else{
   g.text(font,"同版本客户端进入同一世界后创建或加入房间。",12,32,0xffaebed0);
   if(!ClientState.error.isEmpty())g.text(font,font.plainSubstrByWidth(ClientState.error,width-24),12,height-44,0xffffb088);
  }
  super.extractRenderState(g,mx,my,dt);
 }
 public boolean isPauseScreen(){return false;}
 public void onClose(){minecraft.gui.setScreen(parent instanceof SharedScreen?(ClientState.sharedOpen()?parent:null):parent);}
}
