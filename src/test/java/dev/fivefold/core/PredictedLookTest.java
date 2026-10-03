package dev.fivefold.core;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PredictedLookTest {
 private static void angle(PredictedLook p,double yaw,double pitch){assertEquals(yaw,p.angles().yaw(),1e-8);assertEquals(pitch,p.angles().pitch(),1e-8);}
 @Test void mouseChangesLocalViewWithoutAnyServerAcknowledgement(){
  PredictedLook p=new PredictedLook();p.reset(10,20);p.predict(7,-3);angle(p,17,17);assertEquals(1,p.pendingCount());
 }
 @Test void acknowledgingOwnInputNeverAppliesItTwice(){
  PredictedLook p=new PredictedLook();p.reset(0,0);p.predict(10,2);p.reconcile(1,10,2,1);angle(p,10,2);assertEquals(0,p.pendingCount());
  p.reconcile(1,10,2,1);angle(p,10,2);
 }
 @Test void oldSnapshotsCannotUndoNewerRotation(){
  PredictedLook p=new PredictedLook();p.reset(0,0);p.reconcile(4,30,5,0);p.reconcile(3,-100,-20,0);angle(p,30,5);
 }
 @Test void partialAcknowledgementsPreserveNewerLocalInput(){
  PredictedLook p=new PredictedLook();p.reset(0,0);p.predict(10,3);p.predict(20,4);p.reconcile(1,10,3,1);angle(p,30,7);assertEquals(1,p.pendingCount());
 }
 @Test void otherControllerRotationCombinesWithUnacknowledgedLocalMovement(){
  PredictedLook p=new PredictedLook();p.reset(0,0);p.predict(10,0);p.reconcile(1,5,2,0);angle(p,15,2);p.reconcile(2,15,2,1);angle(p,15,2);
 }
 @Test void pitchReversalAtTheLimitIsPreservedInsideABatch(){
  PredictedLook p=new PredictedLook();p.reset(0,85);p.predict(0,20);p.predict(0,-20);angle(p,0,70);
  LookAuthority server=new LookAuthority();server.reset(0,85);UUID owner=UUID.randomUUID();
  for(var s:p.unsent())server.accept(owner,s.sequence(),s.dx(),s.dy());
  p.reconcile(server.serial(),server.angles().yaw(),server.angles().pitch(),server.acknowledgements().get(owner));angle(p,0,70);
 }
 @Test void simulatedTwelveFrameAckDelayDoesNotDelayLocalRotation(){
  PredictedLook p=new PredictedLook();p.reset(0,0);LookAuthority server=new LookAuthority();server.reset(0,0);UUID id=UUID.randomUUID();List<PredictedLook.Sample> samples=new ArrayList<>();
  for(int frame=0;frame<240;frame++){
   samples.add(p.predict(1,0));
   if(frame>=12){var s=samples.get(frame-12);server.accept(id,s.sequence(),s.dx(),s.dy());p.reconcile(server.serial(),server.angles().yaw(),server.angles().pitch(),server.acknowledgements().get(id));}
   angle(p,LookAngles.wrap(frame+1),0);
  }
 }
 @Test void duplicatesAndInvalidInputDoNotRotateTheServer(){
  LookAuthority server=new LookAuthority();UUID id=UUID.randomUUID();server.reset(0,0);
  assertTrue(server.accept(id,1,5,2));assertFalse(server.accept(id,1,5,2));assertFalse(server.accept(id,2,Double.NaN,0));assertFalse(server.accept(id,2,4000,0));
  assertEquals(1,server.serial());assertEquals(5,server.angles().yaw());
 }
 @Test void sentSamplesStayPendingUntilAckButAreNotResent(){
  PredictedLook p=new PredictedLook();p.predict(1,1);p.sent(1);assertTrue(p.unsent().isEmpty());assertEquals(1,p.pendingCount());p.predict(2,0);assertEquals(2,p.unsent().getFirst().sequence());
 }
 @Test void resetDropsPreviousSessionAndPendingInput(){
  PredictedLook p=new PredictedLook();p.predict(90,0);p.reset(-20,5);angle(p,-20,5);assertEquals(0,p.pendingCount());assertEquals(1,p.predict(1,0).sequence());
 }
}
