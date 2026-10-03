package dev.fivefold.core;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MovementInputTest {
 @Test void bodyPressAndReleaseDoNotWaitForAnEcho(){
  int w=Action.FORWARD.bit();assertEquals(w,MovementInput.merge(0,w,w,true));assertEquals(0,MovementInput.merge(0,0,w,true));
 }
 @Test void localReleaseCannotCancelAnotherHolder(){
  int w=Action.FORWARD.bit();assertEquals(w,MovementInput.merge(w,0,w,true));assertEquals(0,MovementInput.merge(0,0,w,true));
 }
 @Test void predictionCannotGrantPermissionsOrPredictClicks(){
  int w=Action.FORWARD.bit();assertEquals(w,MovementInput.merge(0,Action.ALL,w,true));assertEquals(MovementInput.MASK,MovementInput.merge(Action.ALL,Action.ALL,Action.ALL,true));assertEquals(0,MovementInput.merge(0,Action.ATTACK.bit(),Action.ALL,true));
 }
 @Test void disabledMovementStopsEvenWithHeldInputs(){assertEquals(0,MovementInput.merge(Action.ALL,Action.ALL,Action.ALL,false));}
 @Test void serverOtherMaskOmitsBodyButRetainsSharedOwnerAndTimeouts(){
  var p=new Permissions();UUID body=UUID.randomUUID(),other=null;for(int i=0;i<6;i++){UUID id=i==0?body:UUID.randomUUID();p.join(id);if(i==5)other=id;}
  var mixer=new InputMixer();int w=Action.FORWARD.bit();mixer.update(body,w,0,p);assertEquals(0,mixer.heldExcept(0,p,body));mixer.update(other,w,1,p);assertEquals(w,mixer.heldExcept(1,p,body));mixer.update(body,0,2,p);assertEquals(w,mixer.heldExcept(2,p,body));assertEquals(0,mixer.heldExcept(12,p,body));
 }
 @Test void unfocusedBodyStillExecutesRemoteAD(){
  for(Action key:new Action[]{Action.LEFT,Action.RIGHT})
   assertEquals(key.bit(),MovementInput.resolve(key.bit(),0,0,true,false));
 }
 @Test void focusLossReleasesOnlyBodyLocalKeys(){
  assertEquals(Action.RIGHT.bit(),MovementInput.resolve(Action.RIGHT.bit(),Action.LEFT.bit(),Action.LEFT.bit(),true,false));
  assertEquals(0,MovementInput.resolve(0,Action.LEFT.bit(),Action.LEFT.bit(),true,false));
 }
 @Test void focusDoesNotBypassMenuDeathRevisionOrTimeoutGate(){
  for(boolean focus:new boolean[]{true,false})
   assertEquals(0,MovementInput.resolve(Action.ALL,Action.ALL,Action.ALL,false,focus));
 }
 @Test void oppositeADRemainSeparateBitsForVanillaCancellation(){
  int both=Action.LEFT.bit()|Action.RIGHT.bit();
  assertEquals(both,MovementInput.resolve(Action.LEFT.bit(),Action.RIGHT.bit(),both,true,true));
  assertEquals(Action.LEFT.bit(),MovementInput.resolve(Action.LEFT.bit(),0,both,true,true));
 }
 @Test void sharedHolderReleaseIsRelayedEvenWhenUnionStaysDown(){
  var p=new Permissions();UUID body=UUID.randomUUID(),other=null;
  for(int i=0;i<6;i++){UUID id=i==0?body:UUID.randomUUID();p.join(id);if(i==5)other=id;}
  var mixer=new InputMixer();int w=Action.FORWARD.bit();
  mixer.update(body,w,0,p);mixer.update(other,w,0,p);
  int before=mixer.held(0,p),beforeOther=mixer.heldExcept(0,p,body);
  mixer.update(other,0,0,p);
  assertTrue(InputMixer.needsRelay(before,mixer.held(0,p),beforeOther,mixer.heldExcept(0,p,body)));
  // When the body releases next, stale otherHeld must not re-latch the key.
  assertEquals(0,MovementInput.resolve(mixer.heldExcept(0,p,body),0,w,true,true));
 }
 @Test void sharedHolderPressIsRelayedBeforeBodyReleases(){
  int a=Action.LEFT.bit();assertTrue(InputMixer.needsRelay(a,a,0,a));
  assertEquals(a,MovementInput.resolve(a,0,a,true,true));
 }
 @Test void unchangedHeartbeatDoesNotNeedImmediateDuplicateRelay(){
  assertFalse(InputMixer.needsRelay(Action.LEFT.bit(),Action.LEFT.bit(),Action.LEFT.bit(),Action.LEFT.bit()));
  assertTrue(InputMixer.needsRelay(0,Action.RIGHT.bit(),0,Action.RIGHT.bit()));
 }
 @Test void localSprintUsesSameCurrentInputAsMovement(){
  int sprint=Action.SPRINT.bit();assertEquals(sprint,MovementInput.resolve(0,sprint,sprint,true,true));
  assertEquals(0,MovementInput.resolve(0,0,sprint,true,true));
 }
}
