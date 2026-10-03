package dev.fivefold.server;
import dev.fivefold.core.CooperationTasks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.*;
import net.minecraft.network.chat.Component;
import java.util.Set;

final class TaskRewards {
 private static final Set<String> EXCLUDED=Set.of("instant_health","instant_damage","darkness","blindness","mining_fatigue");
 static void tick(Sessions.Room room){
  var body=Sessions.player(room,room.body);if(body==null)return;
  room.tasks.configure(room.permissions.members());
  room.tasks.tick(Sessions.active(room)&&body.isAlive()&&!room.open);
  drain(room);
 }
 static void drain(Sessions.Room room){
  CooperationTasks.Outcome outcome;
  while((outcome=room.tasks.poll())!=null){
   if(outcome.reward())apply(room,true);
   if(outcome.punishment())apply(room,false);
  }
 }
 private static void apply(Sessions.Room room,boolean good){
  var body=Sessions.player(room,room.body);if(body==null)return;
  var choices=BuiltInRegistries.MOB_EFFECT.listElements().filter(h->{var id=h.key().identifier();return id.getNamespace().equals("minecraft")&&!EXCLUDED.contains(id.getPath())&&!h.value().isInstantaneous()&&h.value().getCategory()==(good?MobEffectCategory.BENEFICIAL:MobEffectCategory.HARMFUL);}).toList();
  if(choices.isEmpty())return;
  var effect=choices.get(Sessions.random.nextInt(choices.size()));
  // Preserve stronger/longer pre-existing effects; standard addEffect merges level-I rewards safely.
  body.addEffect(new MobEffectInstance(effect,good?1200:200,0));
  String text=(good?"协作奖励：":"协作惩罚：")+effect.value().getDisplayName().getString()+(good?" I · 60秒":" I · 10秒");
  for(var id:room.permissions.members().keySet()){var p=Sessions.player(room,id);if(p!=null)p.sendSystemMessage(Component.literal(text),false);}
 }
 private TaskRewards(){}
}
