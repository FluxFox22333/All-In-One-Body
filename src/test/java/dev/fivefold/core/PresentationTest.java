package dev.fivefold.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PresentationTest {
 @Test void observerMovesEveryRenderFrameAndSettlesWithinFiftyMilliseconds(){
  var s=new SmoothedLook();s.reset(0,0,0);s.accept(1,90,30,0);
  double last=-1;for(long t=0;t<=50_000_000;t+=5_000_000){var p=s.sample(t);assertTrue(p.yaw()>last);last=p.yaw();}
  assertEquals(90,s.sample(50_000_000).yaw(),1e-8);assertEquals(30,s.sample(500_000_000).pitch(),1e-8);
 }
 @Test void repeatedSnapshotsCannotRestartTheTransition(){
  var s=new SmoothedLook();s.reset(0,0,0);s.accept(4,90,0,0);s.accept(4,90,0,25_000_000);s.accept(3,-90,0,30_000_000);assertEquals(90,s.sample(50_000_000).yaw(),1e-8);
 }
 @Test void observerCrossesYawSeamByTheShortPath(){
  var s=new SmoothedLook();s.reset(179,0,0);s.accept(1,-179,0,0);assertEquals(-180,s.sample(25_000_000).yaw(),1e-8);
 }
 @Test void retargetingAndResetDoNotProducePositionJumps(){
  var s=new SmoothedLook();s.reset(0,0,0);s.accept(1,90,0,0);double before=s.sample(20_000_000).yaw();s.accept(2,-40,-90,20_000_000);assertEquals(before,s.sample(20_000_000).yaw(),1e-8);
  s.reset(12,45,25_000_000);assertEquals(12,s.sample(25_000_000).yaw(),1e-8);assertEquals(45,s.sample(25_000_000).pitch(),1e-8);
 }
 @Test void constantTwentyHertzStreamProducesContinuousSixtyHertzView(){
  var s=new SmoothedLook();s.reset(0,0,0);double prev=0;
  for(int frame=0;frame<30;frame++){long now=Math.round(frame*1_000_000_000.0/60);if(frame%3==0)s.accept(frame/3+1,frame/3*6+6,0,now);double current=s.sample(now).yaw();if(frame>0)assertEquals(2,current-prev,.00001);prev=current;}
 }
 @Test void interpolatedPointerClickResolvesToExactlyTheRenderedPoint(){
  var t=new CursorTimeline();var h=new CursorHistory();t.reset(0,.1,.2,0);h.reset(.1,.2);
  t.accept(1,.7,.8,50_000_000);h.add(1,.7,.8);var drawn=t.sample(75_000_000);var clicked=h.resolve(drawn.from(),drawn.to(),drawn.blend());
  assertNotNull(clicked);assertEquals(.4,drawn.x(),1e-8);assertEquals(drawn.x(),clicked.x(),1e-8);assertEquals(drawn.y(),clicked.y(),1e-8);
 }
 @Test void oldSnapshotsDoNotMoveOrDelayThePointer(){
  var t=new CursorTimeline();t.reset(0,0,0,0);t.accept(1,1,1,50_000_000);t.accept(1,1,1,100_000_000);t.accept(0,0,0,110_000_000);assertEquals(1,t.sample(110_000_000).x(),1e-8);
 }
 @Test void cursorStopsWithoutExtrapolationAndResetsBetweenMenus(){
  var t=new CursorTimeline();t.reset(0,.2,.2,0);t.accept(1,.8,.8,50_000_000);assertEquals(.8,t.sample(1_000_000_000).x(),1e-8);t.reset(0,.1,.1,1_000_000_000);assertEquals(.1,t.sample(1_000_000_000).x(),1e-8);
 }
 @Test void invalidOrExpiredPointerReferencesCannotClick(){
  var h=new CursorHistory();h.reset(0,0);h.add(1,1,1);h.add(2,1,0);assertNotNull(h.resolve(0,2,.5));assertNull(h.resolve(1,0,.5));assertNull(h.resolve(0,1,Double.NaN));assertNull(h.resolve(0,1,1.1));assertNull(h.resolve(0,99,0));
  for(int i=3;i<200;i++)h.add(i,0,0);assertNull(h.resolve(0,1,.5));assertNotNull(h.resolve(198,199,.5));h.reset(.5,.5);assertNull(h.resolve(198,199,.5));
 }
 @Test void burstPointerPacketsRemainFinite(){
  var t=new CursorTimeline();t.reset(0,0,0,0);for(int i=1;i<10;i++)t.accept(i,i*.1,i*.1,50_000_000);var halfway=t.sample(75_000_000);assertEquals(.45,halfway.x(),1e-8);var p=t.sample(100_000_000);assertTrue(Double.isFinite(p.x()));assertEquals(.9,p.x(),1e-8);
 }
}
