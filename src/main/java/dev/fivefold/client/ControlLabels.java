package dev.fivefold.client;
import dev.fivefold.core.Action;
import java.util.*;
public final class ControlLabels {
 public static String key(Action a){
  if(a==Action.ATTACK)return "鼠标左键";
  if(a==Action.USE)return "鼠标右键";
  if(a==Action.LOOK)return "移动鼠标";
  if(a==Action.SCROLL_UP)return "滚轮↑";
  if(a==Action.SCROLL_DOWN)return "滚轮↓";
  if(a==Action.CLOSE)return "Esc";
  var mapping=ClientState.mappings().get(a);return mapping==null?"—":mapping.getTranslatedKeyMessage().getString();
 }
 public static String describe(Action a){
  return switch(a){
   case ATTACK -> "鼠标左键 · 攻击/挖掘";
   case USE -> "鼠标右键 · 使用/放置";
   case LOOK -> "移动鼠标 · 视角";
   case SCROLL_UP -> "滚轮↑ · 快捷栏";
   case SCROLL_DOWN -> "滚轮↓ · 快捷栏";
   default -> key(a)+" · "+a.label;
  };
 }
 public static String table(Action a){
  String label=switch(a){case ATTACK->"挖掘";case USE->"使用";case LOOK->"视角";case SCROLL_UP,SCROLL_DOWN->"快捷栏";case INVENTORY->"背包";case CLOSE->"关闭";default->a.label;};
  return key(a)+" "+label;
 }
 public static List<String> hudLines(int mask,int maxWidth){
  var font=ClientState.mc().font;var result=new ArrayList<String>();String[] groups={"移动","鼠标","辅助"};
  for(int group=0;group<3;group++){
   String line=groups[group]+"：";boolean any=false;
   for(Action a:Action.values())if(a.group==group&&(mask&a.bit())!=0){
    String token=describe(a),next=line+(any?"  /  ":"")+token;
    if(font.width(next)>maxWidth&&any){result.add(line);line="  "+token;}else line=next;
    any=true;
   }
   result.add(line+(any?"":"无"));
  }
  return result;
 }
 private ControlLabels(){}
}
