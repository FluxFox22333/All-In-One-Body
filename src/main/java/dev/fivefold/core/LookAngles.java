package dev.fivefold.core;
/** Shared deterministic angle math, including pitch clamping per input sample. */
public record LookAngles(double yaw,double pitch) {
 public LookAngles step(double dx,double dy){return new LookAngles(wrap(yaw+dx),Math.clamp(pitch+dy,-90,90));}
 public static double wrap(double value){double r=value%360;return r>=180?r-360:r< -180?r+360:r;}
}
