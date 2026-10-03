package dev.fivefold.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ControlLayoutTest {
 @Test void threeMembersFitSideBySideAtMinimumStandardGuiSize(){
  var l=ControlLayout.of(320,240,3);assertEquals(3,l.visibleMembers());assertTrue(l.memberWidth()>=58);
  assertTrue(l.columnX(2)+l.memberWidth()<=308);assertTrue(l.rowY(4)+l.rowHeight()<240-61);
 }
 @Test void fiveRowsNeverOverlapFooterAtCommonGuiScales(){
  for(int w:new int[]{320,427,640,854,1280})for(int h:new int[]{240,360,480,720})for(int count:new int[]{2,3,5,32}){
   var l=ControlLayout.of(w,h,count);assertTrue(l.rowY(4)+l.rowHeight()<h-61);
   assertTrue(l.visibleMembers()<=count);assertTrue(l.columnX(l.visibleMembers()-1)+l.memberWidth()<=w-12);
  }
 }
 @Test void largerTeamsUsePagesInsteadOfShrinkingThirtyTwoColumns(){
  var l=ControlLayout.of(320,240,32);assertEquals(3,l.visibleMembers());assertTrue(l.memberWidth()>=58);
 }
}
