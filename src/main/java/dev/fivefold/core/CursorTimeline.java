package dev.fivefold.core;
import java.util.ArrayDeque;
/** Short render buffer; samples include an exact server-verifiable segment reference. */
public final class CursorTimeline {
 public record Point(long serial,long time,double x,double y){}
 public record Sample(long from,long to,double blend,double x,double y){}
 private final ArrayDeque<Point> points=new ArrayDeque<>();
 public void reset(long serial,double x,double y,long now){points.clear();points.add(new Point(serial,now,x,y));}
 public void accept(long serial,double x,double y,long now){
  if(!points.isEmpty()&&serial<=points.getLast().serial())return;
  // Several packets commonly arrive together on one server tick. Treat the burst as one render sample.
  if(points.size()>1&&now-points.getLast().time()<8_000_000L){now=points.removeLast().time();}
  points.add(new Point(serial,now,x,y));while(points.size()>128)points.removeFirst();
 }
 public Sample sample(long now){
  if(points.isEmpty())return new Sample(0,0,0,.5,.5);
  long time=now-50_000_000L;Point a=points.getFirst();
  for(Point b:points){if(b.time()>time){double t=b.time()==a.time()?0:Math.clamp((time-a.time())/(double)(b.time()-a.time()),0,1);return new Sample(a.serial(),b.serial(),t,a.x()+(b.x()-a.x())*t,a.y()+(b.y()-a.y())*t);}a=b;}
  return new Sample(a.serial(),a.serial(),0,a.x(),a.y());
 }
}
