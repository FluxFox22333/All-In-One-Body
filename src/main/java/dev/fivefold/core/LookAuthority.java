package dev.fivefold.core;
import java.util.*;
/** Ordered, deduplicated authoritative input from all authorized look controllers. */
public final class LookAuthority {
 private LookAngles angles=new LookAngles(0,0);
 private long serial;
 private final Map<UUID,Long> acknowledgements=new HashMap<>();
 public void reset(double yaw,double pitch){angles=new LookAngles(LookAngles.wrap(yaw),Math.clamp(pitch,-90,90));serial=0;acknowledgements.clear();}
 public boolean accept(UUID owner,long sequence,double dx,double dy){
  if(!Double.isFinite(dx)||!Double.isFinite(dy)||Math.abs(dx)>3600||Math.abs(dy)>3600||sequence<=acknowledgements.getOrDefault(owner,0L))return false;
  angles=angles.step(dx,dy);acknowledgements.put(owner,sequence);serial++;return true;
 }
 public LookAngles angles(){return angles;}
 public long serial(){return serial;}
 public Map<UUID,Long> acknowledgements(){return Map.copyOf(acknowledgements);}
}
