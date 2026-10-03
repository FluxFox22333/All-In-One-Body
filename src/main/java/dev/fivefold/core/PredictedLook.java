package dev.fivefold.core;
import java.util.*;
/** Render-frame prediction with authoritative reconciliation; acknowledged input is never replayed twice. */
public final class PredictedLook {
 public record Sample(long sequence,double dx,double dy){}
 private final ArrayDeque<Sample> pending=new ArrayDeque<>();
 private LookAngles base=new LookAngles(0,0),visible=base;
 private long serial=-1,nextSequence=0,acknowledged=0,sentThrough=0;
 public void reset(double yaw,double pitch){pending.clear();base=visible=new LookAngles(LookAngles.wrap(yaw),Math.clamp(pitch,-90,90));serial=-1;nextSequence=acknowledged=sentThrough=0;}
 public LookAngles angles(){return visible;}
 public int pendingCount(){return pending.size();}
 public Sample predict(double dx,double dy){
  if(!Double.isFinite(dx)||!Double.isFinite(dy)||(dx==0&&dy==0)||pending.size()>=4096)return null;
  Sample s=new Sample(++nextSequence,dx,dy);pending.addLast(s);visible=visible.step(dx,dy);return s;
 }
 public List<Sample> unsent(){return pending.stream().filter(s->s.sequence()>sentThrough).limit(64).toList();}
 public void sent(long sequence){sentThrough=Math.max(sentThrough,sequence);}
 public void reconcile(long serverSerial,double yaw,double pitch,long ack){
  if(serverSerial<serial)return;
  serial=serverSerial;acknowledged=Math.max(acknowledged,ack);
  while(!pending.isEmpty()&&pending.peekFirst().sequence()<=acknowledged)pending.removeFirst();
  base=new LookAngles(LookAngles.wrap(yaw),Math.clamp(pitch,-90,90));visible=base;
  for(Sample s:pending)visible=visible.step(s.dx(),s.dy());
 }
}
