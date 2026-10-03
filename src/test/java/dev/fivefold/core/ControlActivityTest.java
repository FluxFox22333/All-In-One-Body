package dev.fivefold.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class ControlActivityTest {
 final UUID a=UUID.randomUUID(),b=UUID.randomUUID();
 final Map<UUID,Integer> permissions=Map.of(a,Action.ALL,b,Action.RIGHT.bit());
 @Test void eachMemberKeepsIndependentHeldKeys(){
  var t=new ControlActivity();t.observe(a,Action.LEFT.bit(),1);t.observe(b,Action.RIGHT.bit(),2);
  var s=t.snapshot(permissions,3);assertEquals(Action.LEFT.bit(),s.get(a).held());assertEquals(Action.RIGHT.bit(),s.get(b).held());
 }
 @Test void fastPressReleaseLeavesOnlyARecentPulse(){
  var t=new ControlActivity();t.observe(a,Action.ATTACK.bit(),1);t.observe(a,0,2);
  var s=t.snapshot(permissions,3).get(a);assertEquals(0,s.held());assertEquals(Action.ATTACK.bit(),s.recent());
  assertEquals(0,t.snapshot(permissions,220_000_002L).get(a).recent());
 }
 @Test void mouseAndWheelPulsesNeverBecomeHeldMovement(){
  var t=new ControlActivity();t.pulse(a,Action.LOOK.bit()|Action.SCROLL_UP.bit(),1);
  var s=t.snapshot(permissions,2).get(a);assertEquals(0,s.held());assertEquals(Action.LOOK.bit()|Action.SCROLL_UP.bit(),s.recent());
 }
 @Test void staleAndUnauthorizedKeysDisappear(){
  var t=new ControlActivity();t.observe(b,Action.ALL,1);
  assertEquals(Action.RIGHT.bit(),t.snapshot(permissions,2).get(b).held());
  assertEquals(0,t.snapshot(permissions,550_000_002L).get(b).held());
 }
 @Test void menuTransitionReleasesHoldsButPreservesTheRecentOpenKey(){
  var t=new ControlActivity();t.observe(a,Action.INVENTORY.bit(),1);t.releaseAll();
  var s=t.snapshot(permissions,2).get(a);assertEquals(0,s.held());assertEquals(Action.INVENTORY.bit(),s.recent());
 }
 @Test void telemetryCannotAuthorizeBodyMovementOrView(){
  var c=new RealtimeControls();c.configure(new RealtimeControls.Config("r",1,1,a,permissions,true),0,0);
  c.observe(b,"r",1,1,Action.ALL,0);
  assertEquals(0,c.movement(0).otherHeld());assertEquals(0,c.view().angles().yaw());
  assertEquals(Action.RIGHT.bit(),c.pollActivity(0).members().get(b).held());
 }
 @Test void sharedUiInputIsVisibleWhileMovementIsDisabled(){
  var c=new RealtimeControls();c.configure(new RealtimeControls.Config("r",1,1,a,permissions,false),0,0);
  assertTrue(c.observe(a,"r",1,1,Action.ATTACK.bit(),0));assertFalse(c.input(a,"r",1,1,Action.FORWARD.bit(),0));
  assertEquals(Action.ATTACK.bit(),c.pollActivity(0).members().get(a).held());
 }
 @Test void oldRoomAndRevisionCannotSpoofAnotherRoomDisplay(){
  var c=new RealtimeControls();c.configure(new RealtimeControls.Config("r",2,3,a,permissions,true),0,0);
  assertFalse(c.observe(b,"wrong",2,3,Action.ALL,0));assertFalse(c.observe(b,"r",1,3,Action.ALL,0));assertFalse(c.observe(b,"r",2,2,Action.ALL,0));
  assertEquals(0,c.pollActivity(0).members().get(b).held());
 }
}
