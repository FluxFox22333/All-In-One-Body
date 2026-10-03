package dev.fivefold.core;
import java.util.*;
/** Criterion-level sharing; adapters mark native progress dirty without replaying rewards. */
public final class ProgressSharing {
 public interface Store {
  Set<String> completed();
  void set(String criterion,boolean granted);
 }
 public static void merge(List<? extends Store> members){
  Set<String> union=new HashSet<>();for(var s:members)union.addAll(s.completed());
  for(var s:members){var own=s.completed();for(String criterion:union)if(!own.contains(criterion))s.set(criterion,true);}
 }
 public static void change(List<? extends Store> members,String criterion,boolean granted){
  for(var s:members)if(s.completed().contains(criterion)!=granted)s.set(criterion,granted);
 }
 private ProgressSharing(){}
}
