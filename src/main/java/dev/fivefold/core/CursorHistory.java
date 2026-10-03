package dev.fivefold.core;
import java.util.LinkedHashMap;
/** Resolve a viewer's rendered point without giving the click owner pointer-movement permission. */
public final class CursorHistory {
 public record Point(double x,double y){}
 private final LinkedHashMap<Long,Point> points=new LinkedHashMap<>();
 public void reset(double x,double y){points.clear();add(0,x,y);}
 public void add(long serial,double x,double y){points.put(serial,new Point(x,y));while(points.size()>128)points.remove(points.keySet().iterator().next());}
 public Point resolve(long from,long to,double blend){
  if(!Double.isFinite(blend)||blend<0||blend>1||from>to||to-from>64)return null;
  Point a=points.get(from),b=points.get(to);if(a==null||b==null)return null;
  return new Point(a.x()+(b.x()-a.x())*blend,a.y()+(b.y()-a.y())*blend);
 }
}
