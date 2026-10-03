package dev.fivefold.core;
/** Client-side ordered realtime movement, separate from slower action/state frames. */
public final class RemoteMovement {
 private long serial=-1,revision=-1,received;
 private int epoch=-1,held;
 private boolean enabled;
 public void reset(){serial=revision=-1;epoch=-1;held=0;received=0;enabled=false;}
 public boolean accept(long serial,int epoch,long revision,int held,boolean enabled,long now){
  if(serial<=this.serial)return false;
  this.serial=serial;this.epoch=epoch;this.revision=revision;this.held=held&MovementInput.MASK;this.enabled=enabled;received=now;return true;
 }
 public boolean active(int epoch,long revision,long now){return enabled&&this.epoch==epoch&&this.revision==revision&&now-received<600_000_000L;}
 public int held(){return held;}
}
