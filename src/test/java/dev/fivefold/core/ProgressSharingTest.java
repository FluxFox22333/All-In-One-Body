package dev.fivefold.core;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ProgressSharingTest {
 static class Member implements ProgressSharing.Store {
  final Set<String> progress=new HashSet<>();int writes;
  Member(String... criteria){progress.addAll(Arrays.asList(criteria));}
  public Set<String> completed(){return Set.copyOf(progress);}
  public void set(String criterion,boolean granted){writes++;if(granted)progress.add(criterion);else progress.remove(criterion);}
 }
 @Test void startupUnionKeepsEveryMembersPartialCriteria(){
  var a=new Member("biome_a");var b=new Member("biome_b");var c=new Member();
  ProgressSharing.merge(List.of(a,b,c));for(var member:List.of(a,b,c))assertEquals(Set.of("biome_a","biome_b"),member.progress);
 }
 @Test void repeatedMergeDoesNotReplayAlreadyObtainedCriteria(){
  var a=new Member("a");var b=new Member();ProgressSharing.merge(List.of(a,b));ProgressSharing.merge(List.of(a,b));
  assertEquals(0,a.writes);assertEquals(1,b.writes);
 }
 @Test void grantToOneMemberCopiesOnlyMissingProgress(){
  var a=new Member("a");var b=new Member();var c=new Member();
  ProgressSharing.change(List.of(a,b,c),"a",true);assertEquals(0,a.writes);assertEquals(1,b.writes);assertEquals(1,c.writes);
 }
 @Test void revokePropagatesWithoutRemovingOtherCriteria(){
  var a=new Member("a","b");var b=new Member("b");
  ProgressSharing.change(List.of(a,b),"a",false);assertEquals(Set.of("b"),a.progress);assertEquals(Set.of("b"),b.progress);assertEquals(0,b.writes);
 }
 @Test void unrelatedPlayersAreUntouched(){
  var member=new Member("a");var unrelated=new Member("private");ProgressSharing.merge(List.of(member));
  assertEquals(Set.of("private"),unrelated.progress);assertEquals(0,unrelated.writes);
 }
}
