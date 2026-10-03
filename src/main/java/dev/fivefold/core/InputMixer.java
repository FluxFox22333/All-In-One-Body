package dev.fivefold.core;
import java.util.*;
/** Shared keys remain held until ALL holders release; one rising edge per merged press. */
public final class InputMixer {
    private record Frame(int held,long tick){}
    private final Map<UUID,Frame> frames=new HashMap<>();
    public int held(long tick,Permissions permissions){
        return heldExcept(tick,permissions,null);
    }
    public int heldExcept(long tick,Permissions permissions,UUID excluded){
        int held=0;
        for(var e:frames.entrySet())if(!e.getKey().equals(excluded)&&tick-e.getValue().tick()<=10)
            held|=permissions.filter(e.getKey(),e.getValue().held());
        return held;
    }
    public int update(UUID id,int input,long tick,Permissions permissions){
        int before=held(tick,permissions);
        frames.put(id,new Frame(permissions.filter(id,input),tick));
        return held(tick,permissions)&~before;
    }
    /** Body-local prediction needs otherHeld edges even when the union is unchanged. */
    public static boolean needsRelay(int before,int after,int beforeOther,int afterOther){
        return before!=after || beforeOther!=afterOther;
    }
    public void clear(){frames.clear();}
}
