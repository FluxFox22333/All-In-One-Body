package dev.fivefold.core;

import java.util.*;

/** Server-owned allocation for 2–32 participants, with shared permissions. */
public final class Permissions {
    public static final int MIN_PLAYERS=2, MAX_PLAYERS=32;
    private final LinkedHashMap<UUID,Integer> members=new LinkedHashMap<>();
    private long revision;
    public Map<UUID,Integer> members(){return Collections.unmodifiableMap(members);}
    public long revision(){return revision;}
    public void join(UUID id){
        if(members.containsKey(id))throw new IllegalArgumentException("已经加入");
        if(members.size()>=MAX_PLAYERS)throw new IllegalArgumentException("房间上限为32人");
        members.put(id,0);defaults();
    }
    public boolean owns(UUID id,Action a){return (members.getOrDefault(id,0)&a.bit())!=0;}
    public int filter(UUID id,int input){return input&members.getOrDefault(id,0)&Action.ALL;}
    public void set(UUID id,int mask,long expectedRevision){
        if(!members.containsKey(id))throw new IllegalArgumentException("尚未加入");
        if(expectedRevision!=revision)throw new IllegalArgumentException("队友已更新分工，请重试");
        if((mask&~Action.ALL)!=0)throw new IllegalArgumentException("未知权限");
        if(mask==Action.ALL)throw new IllegalArgumentException("不能把全部操作交给同一人");
        // Two players cannot satisfy the old five-way exclusions simultaneously.
        // For 3+ players retain those exclusions, while permitting multiple keys/category.
        if(members.size()>=3){
            int forwardBack=Action.FORWARD.bit()|Action.BACK.bit();
            if((mask&Action.LOOK.bit())!=0 && (mask&(forwardBack|Action.JUMP.bit()))!=0)
                throw new IllegalArgumentException("鼠标滑动不能与前进、后退或跳跃配对");
            if((mask&Action.SNEAK.bit())!=0 && (mask&(forwardBack|Action.LOOK.bit()))!=0)
                throw new IllegalArgumentException("潜行不能与前进、后退或鼠标滑动配对");
            if((mask&Action.SPRINT.bit())!=0 && (mask&forwardBack)!=0)
                throw new IllegalArgumentException("疾跑不能与前进或后退配对");
        }
        members.put(id,mask);revision++;
    }
    public void defaults(){
        int count=members.size(),i=0;
        for(UUID id:members.keySet()){
            int mask;
            if(count==1)mask=Action.preset(0);
            else if(count==2)mask=i==0?Action.preset(0)|Action.preset(1):Action.preset(2)|Action.preset(3)|Action.preset(4);
            else if(count==3)mask=i==0?Action.preset(0)|Action.preset(1):i==1?Action.preset(2):Action.preset(3)|Action.preset(4);
            else if(count==4)mask=i==0?Action.preset(0)|Action.preset(1):Action.preset(i+1);
            else mask=Action.preset(i%5);
            members.put(id,mask);i++;
        }
        revision++;
    }
    private boolean hasBothHands(int mask){
        int mouse=0,keyboard=0;
        for(Action a:Action.values())if(a.group==1)mouse|=a.bit();else keyboard|=a.bit();
        return (mask&mouse)!=0&&(mask&keyboard)!=0;
    }
    public boolean complete(){
        return members.size()>=MIN_PLAYERS && members.values().stream().allMatch(m->m!=0&&m!=Action.ALL&&hasBothHands(m))
            && members.values().stream().reduce(0,(a,b)->a|b)==Action.ALL;
    }
}
