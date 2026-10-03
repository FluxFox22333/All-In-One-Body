package dev.fivefold.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class CooperationTasksTest {
 static final List<UUID> PLAYERS=java.util.stream.IntStream.range(0,5).mapToObj(i->new UUID(0,i+1)).toList();
 static Map<UUID,Integer> members(int n){var m=new LinkedHashMap<UUID,Integer>();for(int i=0;i<n;i++)m.put(PLAYERS.get(i),Action.FORWARD.bit());return m;}
 static CooperationTasks game(int n){var g=new CooperationTasks(new Random(4));g.configure(members(n));g.tick(true);return g;}
 static void ticks(CooperationTasks g,int n){for(int i=0;i<n;i++)g.tick(true);}
 static void press(CooperationTasks g,UUID p){g.input(p,g.id(),0);g.input(p,g.id(),g.action().bit());}
 static List<UUID> others(CooperationTasks g){return PLAYERS.stream().filter(p->!p.equals(g.owner())).toList();}
 static void finish(CooperationTasks g){long id=g.id();while(g.id()==id)g.tick(true);}
 @Test void tapsWithinThreeSecondsWinWithoutHolding(){var g=game(5);press(g,g.owner());for(var p:others(g)){ticks(g,10);press(g,p);}assertTrue(g.done());var result=g.poll();assertEquals(2,result.rewardHalfPoints());assertNull(g.poll());}
 @Test void expiredPressDropsOutButCanReenter(){var g=game(5);UUID owner=g.owner();press(g,owner);ticks(g,61);for(var p:others(g))press(g,p);assertFalse(g.done());assertEquals(1,g.best());press(g,owner);assertTrue(g.done());}
 @Test void nonOwnerCannotCompleteOrEarnPartial(){var g=game(5);for(var p:others(g))press(g,p);finish(g);var r=g.poll();assertEquals(0,r.rewardHalfPoints());assertEquals(1,g.penalty(r.owner()));}
 @Test void threeAndFourResponsesKeepSliding(){var g=game(5);var peers=others(g);press(g,g.owner());for(int i=0;i<3;i++)press(g,peers.get(i));ticks(g,61);for(var p:PLAYERS)press(g,p);assertTrue(g.done());assertEquals(2,g.poll().rewardHalfPoints());}
 @Test void partialBestSurvivesExpiryAndOnlySettlesAtDeadline(){var g=game(5);press(g,g.owner());press(g,others(g).getFirst());assertNull(g.poll());ticks(g,80);assertTrue(g.current().isEmpty());assertEquals(2,g.best());finish(g);assertEquals(1,g.poll().rewardHalfPoints());}
 @Test void oneOutOfFiveIsPenalty(){var g=game(5);UUID owner=g.owner();press(g,owner);finish(g);assertEquals(0,g.poll().rewardHalfPoints());assertEquals(1,g.penalty(owner));}
 @Test void exactlyQuarterGetsHalfPoint(){var g=game(4);press(g,g.owner());finish(g);assertEquals(1,g.poll().rewardHalfPoints());}
 @Test void repeatedPacketsOrHeldKeyCannotRenewResponse(){var g=game(5);UUID owner=g.owner();g.input(owner,g.id(),g.action().bit());for(int i=0;i<80;i++){g.tick(true);g.input(owner,g.id(),g.action().bit());}assertFalse(g.current().contains(owner));}
 @Test void wrongTaskAndNonMemberCannotScore(){var g=game(5);g.input(g.owner(),g.id()-1,Action.FORWARD.bit());g.input(new UUID(8,8),g.id(),Action.FORWARD.bit());assertEquals(0,g.best());assertTrue(g.current().isEmpty());}
 @Test void pausedClockAndResponsesCannotScore(){var g=game(5);long remaining=g.remainingTicks();for(int i=0;i<400;i++)g.tick(false);for(var p:PLAYERS)press(g,p);assertEquals(remaining,g.remainingTicks());assertEquals(0,g.best());assertNull(g.poll());}
 @Test void allFiveOwnersSelectedBeforeRepeat(){var g=game(5);Set<UUID> owners=new HashSet<>();for(int i=0;i<5;i++){assertTrue(owners.add(g.owner()));finish(g);g.poll();}assertEquals(5,owners.size());}
 @Test void inventoryAndMouseMotionNeverTasks(){var g=new CooperationTasks(new Random(3));g.configure(Map.of(PLAYERS.get(0),Action.INVENTORY.bit()|Action.LOOK.bit()|Action.SCROLL_UP.bit()|Action.CLOSE.bit()|Action.DROP.bit(),PLAYERS.get(1),Action.JUMP.bit()));g.tick(true);assertEquals(PLAYERS.get(1),g.owner());assertEquals(Action.JUMP,g.action());}
 @Test void rewardAtFourPointsClearsPoolAndDoesNotRepeat(){var g=game(5);for(int n=0;n<4;n++){for(var p:PLAYERS)press(g,p);var r=g.poll();assertEquals(n==3,r.reward());assertNull(g.poll());assertEquals(n==3?0:(n+1)*2,g.poolHalfPoints());finish(g);} }
 @Test void penaltyTriggersAtFourSubtractsTwoAndSuccessReducesOnlyOwner(){var g=game(5);boolean punished=false;for(int i=0;i<20;i++){UUID owner=g.owner();int before=g.penalty(owner);finish(g);var r=g.poll();if(before==3){assertTrue(r.punishment());assertEquals(2,g.penalty(owner));punished=true;}else assertFalse(r.punishment());}assertTrue(punished);UUID owner=g.owner();var before=new HashMap<UUID,Integer>();for(var p:PLAYERS)before.put(p,g.penalty(p));for(var p:PLAYERS)press(g,p);g.poll();for(var p:PLAYERS)assertEquals(p.equals(owner)?Math.max(0,before.get(p)-1):before.get(p),g.penalty(p));}
 @Test void successNeverCreatesNegativePenalty(){var g=game(5);for(var p:PLAYERS)press(g,p);for(var p:PLAYERS)assertEquals(0,g.penalty(p));}
 @Test void heldKeyDoesNotAutomaticallyRespondToNextRound(){var g=game(5);for(var p:PLAYERS)g.input(p,g.id(),Action.FORWARD.bit());g.poll();finish(g);for(var p:PLAYERS)g.input(p,g.id(),Action.FORWARD.bit());assertEquals(0,g.best());}
 @Test void membershipChangeCancelsWithoutPunishment(){var g=game(5);long id=g.id();g.configure(Map.of(PLAYERS.get(0),Action.JUMP.bit(),PLAYERS.get(1),Action.USE.bit()));g.tick(true);assertTrue(g.id()>id);assertNull(g.poll());assertTrue(g.leaders().isEmpty());}
 @Test void rewardOverflowIsClearedInsteadOfCarried(){var g=game(5);for(int n=0;n<7;n++){press(g,g.owner());press(g,others(g).getFirst());finish(g);assertFalse(g.poll().reward());}assertEquals(7,g.poolHalfPoints());for(var p:PLAYERS)press(g,p);assertTrue(g.poll().reward());assertEquals(0,g.poolHalfPoints());}
 @Test void responsesAtThreeSecondBoundaryCountButOlderOnesDoNot(){var g=game(5);UUID owner=g.owner();press(g,owner);ticks(g,60);assertTrue(g.current().contains(owner));ticks(g,1);assertFalse(g.current().contains(owner));}
 @Test void reassignmentCannotEraseCurrentTaskOrItsBestResult(){var g=game(5);long id=g.id();UUID owner=g.owner();press(g,owner);press(g,others(g).getFirst());var changed=new LinkedHashMap<>(members(5));changed.put(owner,Action.JUMP.bit());g.configure(changed);assertEquals(id,g.id());assertEquals(Action.FORWARD,g.action());assertEquals(2,g.best());finish(g);assertEquals(1,g.poll().rewardHalfPoints());}
}
