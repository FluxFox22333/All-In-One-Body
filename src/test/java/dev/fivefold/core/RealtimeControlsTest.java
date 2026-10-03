package dev.fivefold.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;

class RealtimeControlsTest {
 final UUID body=UUID.randomUUID(),left=UUID.randomUUID(),right=UUID.randomUUID();
 RealtimeControls controls(){
  var c=new RealtimeControls();c.configure(config(1,1,true),0,0);return c;
 }
 RealtimeControls.Config config(int epoch,long revision,boolean enabled){
  return new RealtimeControls.Config("room",epoch,revision,body,Map.of(body,Action.ALL,left,Action.LEFT.bit()|Action.LOOK.bit(),right,Action.RIGHT.bit()|Action.LEFT.bit()|Action.LOOK.bit()),enabled);
 }
 List<PredictedLook.Sample> sample(long seq,double x,double y){return List.of(new PredictedLook.Sample(seq,x,y));}
 @Test void remoteADArrivesWithoutAnyServerTick(){
  var c=controls();assertTrue(c.input(left,"room",1,1,Action.LEFT.bit(),1));
  assertEquals(Action.LEFT.bit(),c.movement(1).otherHeld());
  assertTrue(c.input(left,"room",1,1,0,2));assertEquals(0,c.movement(2).otherHeld());
 }
 @Test void bodyInputIsExcludedAndActionsCannotEnterMovementLane(){
  var c=controls();c.input(body,"room",1,1,Action.ALL,0);c.input(left,"room",1,1,Action.ALL,0);
  assertEquals(Action.LEFT.bit(),c.movement(0).otherHeld());
 }
 @Test void sharedKeyRequiresBothRemoteHoldersToRelease(){
  var c=controls();c.input(left,"room",1,1,Action.LEFT.bit(),0);c.input(right,"room",1,1,Action.LEFT.bit(),0);
  c.input(left,"room",1,1,0,1);assertEquals(Action.LEFT.bit(),c.movement(1).otherHeld());
  c.input(right,"room",1,1,0,2);assertEquals(0,c.movement(2).otherHeld());
 }
 @Test void heldInputsExpireByTimeEvenIfServerTicksStall(){
  var c=controls();c.input(left,"room",1,1,Action.LEFT.bit(),0);
  assertEquals(0,c.movement(RealtimeControls.EXPIRY+1).otherHeld());
 }
 @Test void roomRevisionEpochAndMembershipAreValidated(){
  var c=controls();
  assertFalse(c.input(left,"old",1,1,Action.ALL,0));assertFalse(c.input(left,"room",0,1,Action.ALL,0));
  assertFalse(c.input(left,"room",1,0,Action.ALL,0));assertFalse(c.input(UUID.randomUUID(),"room",1,1,Action.ALL,0));
  assertEquals(0,c.movement(0).otherHeld());
 }
 @Test void revokeAndMenuOpenImmediatelyClearAndRejectInput(){
  var c=controls();c.input(left,"room",1,1,Action.LEFT.bit(),0);c.configure(config(1,1,false),0,0);
  assertEquals(0,c.movement(1).otherHeld());assertFalse(c.look(left,"room",1,1,sample(1,4,2)));
  assertFalse(c.input(left,"room",1,1,Action.ALL,1));c.configure(config(1,1,true),0,0);
  assertEquals(0,c.movement(2).otherHeld());c.disable();assertFalse(c.input(left,"room",1,1,Action.ALL,3));
 }
 @Test void sixtyLookUpdatesDoNotRequireTwentyServerTicks(){
  var c=controls();
  for(int i=1;i<=60;i++){assertTrue(c.look(left,"room",1,1,sample(i,1,0)));assertEquals(i,c.view().serial());}
  assertEquals(60,c.view().angles().yaw());assertEquals(60L,c.view().acknowledgements().get(left));
 }
 @Test void oldSnapshotsCannotReapplyAcknowledgedLook(){
  var c=controls();var local=new PredictedLook();local.reset(0,0);
  local.predict(10,2);c.look(left,"room",1,1,sample(1,10,2));var v=c.view();
  local.reconcile(v.serial(),v.angles().yaw(),v.angles().pitch(),v.acknowledgements().get(left));
  local.reconcile(0,0,0,0);assertEquals(10,local.angles().yaw());assertEquals(0,local.pendingCount());
  assertFalse(c.look(left,"room",1,1,sample(1,10,2)));
 }
 @Test void wholeBatchValidationAndPitchClampArePreserved(){
  var c=controls();assertFalse(c.look(left,"room",1,1,List.of(new PredictedLook.Sample(1,10,2),new PredictedLook.Sample(2,Double.NaN,0))));
  assertEquals(0,c.view().serial());assertTrue(c.look(left,"room",1,1,List.of(new PredictedLook.Sample(1,0,100),new PredictedLook.Sample(2,0,-10))));
  assertEquals(80,c.view().angles().pitch());
 }
 @Test void epochResetDropsHeldInputsAndAcknowledgements(){
  var c=controls();c.input(left,"room",1,1,Action.LEFT.bit(),0);c.look(left,"room",1,1,sample(1,10,0));
  c.configure(config(2,2,true),90,5);assertEquals(0,c.movement(1).otherHeld());assertEquals(90,c.view().angles().yaw());
  assertTrue(c.view().acknowledgements().isEmpty());assertFalse(c.look(left,"room",1,1,sample(2,10,0)));
  assertTrue(c.look(left,"room",2,2,sample(1,10,0)));
 }
 @Test void heartbeatsAreCoalescedButPressAndReleaseAreImmediate(){
  var c=controls();assertNotNull(c.pollMovement(0));assertNull(c.pollMovement(1));
  c.input(left,"room",1,1,Action.LEFT.bit(),2);assertNotNull(c.pollMovement(2));
  c.input(left,"room",1,1,0,3);assertNotNull(c.pollMovement(3));assertNull(c.pollMovement(4));
  assertNotNull(c.pollMovement(50_000_003L));
 }
 @Test void twoNetworkThreadsAndMainPublicationKeepCoherentSnapshots() throws Exception {
  var c=controls();var pool=Executors.newFixedThreadPool(3);
  try{
   var jobs=new ArrayList<Callable<Void>>();
   for(UUID owner:List.of(left,right))jobs.add(()->{for(int i=1;i<=500;i++)c.look(owner,"room",1,1,sample(i,.1,0));return null;});
   jobs.add(()->{for(int i=0;i<500;i++){c.configure(config(1,1,true),0,0);var v=c.view();assertEquals(v.serial(),v.acknowledgements().values().stream().mapToLong(Long::longValue).sum());}return null;});
   for(var future:pool.invokeAll(jobs))future.get();
   assertEquals(1000,c.view().serial());assertEquals(100,c.view().angles().yaw(),1e-8);
  }finally{pool.shutdownNow();}
 }
 @Test void bodyViewerTransitionsInTwentyMillisecondsWithoutChangingAuthority(){
  var c=controls();var smooth=new SmoothedLook(20_000_000L);smooth.reset(0,0,0);
  c.look(left,"room",1,1,sample(1,20,10));var v=c.view();smooth.accept(v.serial(),v.angles().yaw(),v.angles().pitch(),0);
  assertEquals(10,smooth.sample(10_000_000L).yaw());assertEquals(20,smooth.sample(20_000_000L).yaw());
  assertEquals(20,c.view().angles().yaw());
  smooth.accept(v.serial(),v.angles().yaw(),v.angles().pitch(),25_000_000L);assertEquals(20,smooth.sample(25_000_000L).yaw());
 }
}
