package dev.fivefold.server;
import dev.fivefold.core.ProgressSharing;
import dev.fivefold.mixin.AdvancementsAccess;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import java.util.*;

/** Main-thread criterion sharing. Silent copies do not award XP, loot, recipes or reward functions. */
public final class SharedAdvancements {
 private record NativeStore(ServerPlayer player,AdvancementHolder holder) implements ProgressSharing.Store {
  public Set<String> completed(){
   Set<String> out=new HashSet<>();player.getAdvancements().getOrStartProgress(holder).getCompletedCriteria().forEach(out::add);return out;
  }
  public void set(String criterion,boolean granted){
   var manager=player.getAdvancements();var progress=manager.getOrStartProgress(holder);
   boolean changed=granted?progress.grantProgress(criterion):progress.revokeProgress(criterion);
   if(!changed)return;
   var access=(AdvancementsAccess)manager;
   if(granted)access.fivefold$unregister(holder);else access.fivefold$register(holder);
   access.fivefold$changed().add(holder);access.fivefold$visibility(holder);
  }
 }
 private static List<NativeStore> stores(Sessions.Room room,AdvancementHolder holder){
  var result=new ArrayList<NativeStore>();
  for(var id:room.permissions.members().keySet()){var p=Sessions.player(room,id);if(p!=null)result.add(new NativeStore(p,holder));}
  return result;
 }
 public static void merge(Sessions.Room room){
  var body=Sessions.player(room,room.body);if(body==null)return;
  for(var holder:body.level().getServer().getAdvancements().getAllAdvancements())ProgressSharing.merge(stores(room,holder));
  for(var id:room.permissions.members().keySet()){var p=Sessions.player(room,id);if(p!=null)p.getAdvancements().flushDirty(p,false);}
 }
 public static void progressed(AdvancementEvent.AdvancementProgressEvent event){
  if(!(event.getEntity() instanceof ServerPlayer source))return;
  var room=Sessions.membership.get(source.getUUID());if(room==null||!room.linked)return;
  boolean grant=event.getProgressType()==AdvancementEvent.AdvancementProgressEvent.ProgressType.GRANT;
  ProgressSharing.change(stores(room,event.getAdvancement()),event.getCriterionName(),grant);
 }
}
