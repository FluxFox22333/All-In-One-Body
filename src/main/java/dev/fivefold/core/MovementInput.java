package dev.fivefold.core;
/** Only continuous movement is locally predicted; action clicks remain server-authoritative. */
public final class MovementInput {
 public static final int MASK=Action.FORWARD.bit()|Action.BACK.bit()|Action.LEFT.bit()|Action.RIGHT.bit()|Action.JUMP.bit()|Action.SNEAK.bit()|Action.SPRINT.bit();
 public static int merge(int otherHeld,int localHeld,int permission,boolean enabled){return enabled?(otherHeld|(localHeld&permission))&MASK:0;}
 /** Losing focus releases this member only, not teammates controlling the body. */
 public static int resolve(int otherHeld,int localHeld,int permission,boolean enabled,boolean localFocused){
  return merge(otherHeld,localFocused?localHeld:0,permission,enabled);
 }
 private MovementInput(){}
}
