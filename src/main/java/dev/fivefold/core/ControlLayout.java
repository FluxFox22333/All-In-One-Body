package dev.fivefold.core;
/** Shared, testable geometry for a five-row responsibility table. */
public record ControlLayout(int left,int actionWidth,int memberWidth,int visibleMembers,int rowTop,int rowHeight,int footerY) {
 public static ControlLayout of(int width,int height,int count){
  int columns=Math.min(count,Math.max(1,Math.min(3,(width-130)/58)));
  int action=Math.min(150,Math.max(90,(width-24)/3));
  int member=Math.max(1,(width-24-action)/columns);
  int rowHeight=Math.max(12,Math.min(28,(height-158)/5));
  return new ControlLayout(12,action,member,columns,92,rowHeight,height-47);
 }
 public int columnX(int column){return left+actionWidth+column*memberWidth;}
 public int rowY(int row){return rowTop+row*rowHeight;}
}
