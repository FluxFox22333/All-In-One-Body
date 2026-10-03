package dev.fivefold.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RemoteMovementTest {
 @Test void staleHeldPacketCannotUndoLatestRelease(){
  var m=new RemoteMovement();m.accept(10,1,1,Action.LEFT.bit(),true,0);m.accept(12,1,1,0,true,10);
  assertFalse(m.accept(11,1,1,Action.LEFT.bit(),true,20));assertEquals(0,m.held());
 }
 @Test void wrongPermissionOrEpochAndExpiredFramesCannotDriveMovement(){
  var m=new RemoteMovement();m.accept(1,3,4,Action.LEFT.bit(),true,100);
  assertTrue(m.active(3,4,100));assertFalse(m.active(2,4,100));assertFalse(m.active(3,3,100));assertFalse(m.active(3,4,600_000_100L));
 }
 @Test void sessionResetAndDisabledPacketReleaseInput(){
  var m=new RemoteMovement();m.accept(20,1,1,Action.LEFT.bit(),true,0);m.reset();assertFalse(m.active(1,1,1));
  assertTrue(m.accept(1,1,1,0,false,2));assertFalse(m.active(1,1,2));
 }
 @Test void clickActionsAreNeverPredictedByMovementPackets(){
  var m=new RemoteMovement();m.accept(1,1,1,Action.ALL,true,0);assertEquals(MovementInput.MASK,m.held());
 }
}
