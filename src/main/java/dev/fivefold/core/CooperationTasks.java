package dev.fivefold.core;

import java.util.*;

/** Server-owned game-time clock. Raw challenge input never becomes body input. Points use half-units. */
public final class CooperationTasks {
 public static final int ELIGIBLE=Action.FORWARD.bit()|Action.BACK.bit()|Action.LEFT.bit()|Action.RIGHT.bit()|Action.JUMP.bit()|Action.ATTACK.bit()|Action.USE.bit()|Action.SPRINT.bit()|Action.SNEAK.bit();
 public static final long ROUND=300,WINDOW=60; // 20 ticks per second
 public record Outcome(UUID owner,int participants,int total,int rewardHalfPoints,boolean reward,boolean punishment){}
 private final Random random;
 private final Map<UUID,Integer> penalties=new HashMap<>(),held=new HashMap<>();
 private final Map<UUID,Long> responses=new HashMap<>();
 private final ArrayDeque<UUID> rotation=new ArrayDeque<>();
 private Map<UUID,Integer> members=Map.of();
 private final ArrayDeque<Outcome> outcomes=new ArrayDeque<>();
 private long time,deadline,id;private UUID owner;private Action action;private int best,pool;private boolean done,paused=true;
 public CooperationTasks(Random random){this.random=random;}
 public void configure(Map<UUID,Integer> next){
  if(members.equals(next))return;
  if(members.keySet().equals(next.keySet())){members=Map.copyOf(next);return;} // A valid reassignment must not erase an ongoing result.
  members=Map.copyOf(next);penalties.keySet().retainAll(next.keySet());held.keySet().retainAll(next.keySet());
  rotation.clear();owner=null;responses.clear();done=false;best=0;id++;
 }
 public void tick(boolean enabled){
  if(!enabled){paused=true;responses.clear();return;}
  paused=false;
  if(owner==null)start();
  else if(time>=deadline){if(!done)settle();start();}
  time++;
 }
 private void start(){
  rotation.removeIf(player->(members.getOrDefault(player,0)&ELIGIBLE)==0);
  if(rotation.isEmpty()){
   var eligible=new ArrayList<UUID>();for(var e:members.entrySet())if((e.getValue()&ELIGIBLE)!=0)eligible.add(e.getKey());
   Collections.shuffle(eligible,random);rotation.addAll(eligible);
  }
  if(rotation.isEmpty()){owner=null;action=null;return;}
  owner=rotation.removeFirst();var actions=new ArrayList<Action>();for(var a:Action.values())if((members.get(owner)&ELIGIBLE&a.bit())!=0)actions.add(a);
  action=actions.get(random.nextInt(actions.size()));id++;deadline=time+ROUND;responses.clear();best=0;done=false;
 }
 public void input(UUID player,long taskId,int bits){
  if(!members.containsKey(player))return;
  bits&=ELIGIBLE;int before=held.getOrDefault(player,0);held.put(player,bits);
  if(paused||done||owner==null||taskId!=id||time>=deadline||(bits&~before&action.bit())==0)return;
  responses.put(player,time);int count=current().size();if(responses.containsKey(owner)&&current().contains(owner))best=Math.max(best,count);
  if(best==members.size()){settle();start();} // Full success starts a fresh 15-second round immediately.
 }
 public Set<UUID> current(){
  var current=new HashSet<UUID>();if(paused||done)return current;
  for(var e:responses.entrySet())if(time-e.getValue()<=WINDOW)current.add(e.getKey());return Set.copyOf(current);
 }
 private void settle(){
  done=true;int points=best==members.size()?2:best*4>=members.size()?1:0;
  int penalty=penalties.getOrDefault(owner,0);if(points==0)penalty++;else if(points==2)penalty=Math.max(0,penalty-1);
  boolean punishment=penalty>=4;if(punishment)penalty=Math.max(0,penalty-2);penalties.put(owner,penalty);
  pool+=points;boolean reward=pool>=8;if(reward)pool=0;
  outcomes.add(new Outcome(owner,best,members.size(),points,reward,punishment));
 }
 public Outcome poll(){return outcomes.poll();}
 public long id(){return id;}public UUID owner(){return owner;}public Action action(){return action;}
 public int best(){return best;}public boolean done(){return done;}public boolean paused(){return paused;}
 public long remainingTicks(){return owner==null?ROUND:Math.max(0,deadline-time);}
 public long windowRemainingTicks(){long remaining=0;for(UUID player:current())remaining=Math.max(remaining,WINDOW-(time-responses.get(player)));return Math.min(remaining,remainingTicks());}
 public int poolHalfPoints(){return pool;}public int penalty(UUID player){return penalties.getOrDefault(player,0);}
 public Map<UUID,Integer> leaders(){
  int max=penalties.values().stream().mapToInt(Integer::intValue).max().orElse(0);if(max==0)return Map.of();
  var top=new LinkedHashMap<UUID,Integer>();for(UUID p:members.keySet())if(penalty(p)==max)top.put(p,max);return Map.copyOf(top);
 }
}
