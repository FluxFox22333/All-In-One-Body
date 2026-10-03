package dev.fivefold.core;
import java.util.*;
/** Display-only physical input telemetry. Never used to drive the shared body. */
public final class ControlActivity {
 public record State(int held,int recent){}
 private static final long PULSE=220_000_000L,EXPIRY=550_000_000L;
 private static class Entry {int held;long seen;final long[] pulses=new long[Action.values().length];}
 private final Map<UUID,Entry> entries=new HashMap<>();
 public void clear(){entries.clear();}
 public void releaseAll(){for(var entry:entries.values())entry.held=0;}
 public void observe(UUID owner,int held,long now){
  var e=entries.computeIfAbsent(owner,k->new Entry());int rising=held&~e.held;
  e.held=held&Action.ALL;e.seen=now;pulse(owner,rising,now);
 }
 public void pulse(UUID owner,int bits,long now){
  var e=entries.computeIfAbsent(owner,k->new Entry());
  for(Action a:Action.values())if((bits&a.bit())!=0)e.pulses[a.ordinal()]=now+PULSE;
 }
 public Map<UUID,State> snapshot(Map<UUID,Integer> permissions,long now){
  var result=new HashMap<UUID,State>();
  for(var member:permissions.entrySet()){
   var e=entries.get(member.getKey());int held=0,recent=0;
   if(e!=null){held=now-e.seen<=EXPIRY?e.held:0;for(Action a:Action.values())if(e.pulses[a.ordinal()]>now)recent|=a.bit();}
   result.put(member.getKey(),new State(held&member.getValue(),recent&member.getValue()));
  }
  return Map.copyOf(result);
 }
}
