package dev.fivefold.core;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PermissionsTest {
 private Permissions team(int n){Permissions p=new Permissions();for(int i=0;i<n;i++)p.join(UUID.randomUUID());return p;}
 @Test void everySupportedTeamSizeHasValidCompleteDefaults(){
  for(int n=2;n<=32;n++){
   Permissions p=team(n);assertTrue(p.complete(),"size="+n);
   for(var e:List.copyOf(p.members().entrySet())){assertNotEquals(0,e.getValue());assertNotEquals(Action.ALL,e.getValue());p.set(e.getKey(),e.getValue(),p.revision());}
   assertTrue(p.complete());
  }
 }
 @Test void fivePlayerPresetPreservesRequestedSwap(){
  Permissions p=team(5);var ids=new ArrayList<>(p.members().keySet());
  for(int i=0;i<5;i++)assertEquals(Action.preset(i),p.members().get(ids.get(i)));
  assertTrue(p.owns(ids.get(2),Action.SPRINT));assertTrue(p.owns(ids.get(4),Action.CLOSE));
 }
 @Test void limitsRejectSoloAndOverflowWithoutMutating(){
  assertFalse(team(1).complete());Permissions p=team(32);long revision=p.revision();
  assertThrows(IllegalArgumentException.class,()->p.join(UUID.randomUUID()));assertEquals(revision,p.revision());assertEquals(32,p.members().size());
 }
 @Test void staleRequestsAndUnknownActionsAreRejected(){
  Permissions p=team(3);UUID id=p.members().keySet().iterator().next();long revision=p.revision();p.set(id,0,revision);
  assertThrows(IllegalArgumentException.class,()->p.set(id,Action.ATTACK.bit(),revision));
  assertThrows(IllegalArgumentException.class,()->p.set(id,1<<20,p.revision()));assertFalse(p.complete());
 }
 @Test void sharedOwnershipAndMultipleKeysAreAllowed(){
  Permissions p=team(3);var ids=new ArrayList<>(p.members().keySet());int mask=Action.FORWARD.bit()|Action.BACK.bit()|Action.ATTACK.bit();
  p.set(ids.get(0),mask,p.revision());p.set(ids.get(2),mask,p.revision());
  assertTrue(p.owns(ids.get(0),Action.ATTACK));assertTrue(p.owns(ids.get(2),Action.ATTACK));
  assertEquals(mask,p.filter(ids.get(0),Action.ALL));assertEquals(0,p.filter(UUID.randomUUID(),Action.ALL));
 }
 @Test void threePlusRetainsExclusionsButTwoPlayerModeRelaxesThem(){
  Permissions p=team(3);UUID id=p.members().keySet().iterator().next();
  for(int mask:new int[]{Action.FORWARD.bit()|Action.LOOK.bit(),Action.SNEAK.bit()|Action.LOOK.bit(),Action.BACK.bit()|Action.SPRINT.bit()})
   assertThrows(IllegalArgumentException.class,()->p.set(id,mask,p.revision()));
  p.set(id,Action.LEFT.bit()|Action.LOOK.bit()|Action.SPRINT.bit(),p.revision());
  Permissions two=team(2);UUID other=two.members().keySet().iterator().next();two.set(other,Action.SNEAK.bit()|Action.LOOK.bit(),two.revision());
 }
 @Test void nobodyCanClaimEveryAction(){
  Permissions p=team(2);UUID id=p.members().keySet().iterator().next();assertThrows(IllegalArgumentException.class,()->p.set(id,Action.ALL,p.revision()));
 }
 @Test void sharedPressHasOneEdgeAndReleasingOneHolderDoesNotStopIt(){
  Permissions p=team(2);var ids=new ArrayList<>(p.members().keySet());int key=Action.ATTACK.bit();for(UUID id:ids)p.set(id,key,p.revision());
  InputMixer m=new InputMixer();assertEquals(key,m.update(ids.get(0),key,0,p));assertEquals(0,m.update(ids.get(1),key,1,p));
  assertEquals(0,m.update(ids.get(0),0,2,p));assertEquals(key,m.held(2,p));m.update(ids.get(1),0,3,p);assertEquals(0,m.held(3,p));assertEquals(key,m.update(ids.get(0),key,4,p));
 }
 @Test void timeoutRevocationAndUnauthorizedInputAreHandled(){
  Permissions p=team(2);UUID id=p.members().keySet().iterator().next();p.set(id,Action.ATTACK.bit(),p.revision());InputMixer m=new InputMixer();
  assertEquals(Action.ATTACK.bit(),m.update(id,Action.ALL,0,p));assertEquals(0,m.held(11,p));
  m.update(id,Action.ATTACK.bit(),12,p);p.set(id,0,p.revision());assertEquals(0,m.held(12,p));
  assertEquals(0,m.update(UUID.randomUUID(),Action.ALL,13,p));m.clear();assertEquals(0,m.held(13,p));
 }
 @Test void everyParticipantNeedsKeyboardAndMouse(){
  Permissions p=team(6);UUID last=new ArrayList<>(p.members().keySet()).get(5);
  p.set(last,Action.ATTACK.bit(),p.revision());assertFalse(p.complete());
  p.set(last,Action.FORWARD.bit()|Action.ATTACK.bit(),p.revision());assertTrue(p.complete());
 }
}
