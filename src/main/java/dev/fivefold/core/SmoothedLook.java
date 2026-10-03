package dev.fivefold.core;
/** Render-only retargeting: authoritative aim and local prediction never use this state. */
public final class SmoothedLook {
 private LookAngles from=new LookAngles(0,0),target=from;
 private long start,serial=-1;
 private final long transition;
 public SmoothedLook(){this(50_000_000L);}
 public SmoothedLook(long transition){if(transition<=0)throw new IllegalArgumentException("transition");this.transition=transition;}
 public void reset(double yaw,double pitch,long now){from=target=new LookAngles(yaw,pitch);start=now;serial=-1;}
 public void accept(long sequence,double yaw,double pitch,long now){
  if(sequence<=serial)return;
  serial=sequence;LookAngles next=new LookAngles(yaw,pitch);
  if(Math.abs(LookAngles.wrap(target.yaw()-yaw))<1e-8&&Math.abs(target.pitch()-pitch)<1e-8)return;
  from=sample(now);target=next;start=now;
 }
 public LookAngles sample(long now){
  double t=Math.clamp((now-start)/(double)transition,0,1);
  return new LookAngles(LookAngles.wrap(from.yaw()+LookAngles.wrap(target.yaw()-from.yaw())*t),from.pitch()+(target.pitch()-from.pitch())*t);
 }
}
