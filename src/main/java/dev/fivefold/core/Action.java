package dev.fivefold.core;

public enum Action {
    FORWARD("前进",0), BACK("后退",0), LEFT("左移",0), RIGHT("右移",0), JUMP("跳跃",0),
    ATTACK("左键／挖掘",1), USE("右键／使用",1), LOOK("鼠标滑动",1), SCROLL_UP("滚轮上滚",1), SCROLL_DOWN("滚轮下滚",1),
    DROP("丢弃",2), INVENTORY("打开背包",2), SPRINT("疾跑",2), SNEAK("潜行",2), CLOSE("关闭共享界面",2);
    public final String label;
    public final int group;
    Action(String label,int group){this.label=label;this.group=group;}
    public int bit(){return 1<<ordinal();}
    public static final int ALL=(1<<values().length)-1;
    public static int preset(int seat){return (1<<seat)|(1<<(5+seat))|(1<<(10+seat));}
}
