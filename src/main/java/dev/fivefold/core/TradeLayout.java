package dev.fivefold.core;
/** Matches vanilla MerchantScreen's seven offer buttons in panel-local coordinates. */
public final class TradeLayout {
 public static int offerAt(int x,int y,int scroll,int count){if(x<5||x>=93||y<18||y>=158)return -1;int index=scroll+(y-18)/20;return index>=0&&index<count?index:-1;}
 public static int scrollAt(int y,int count){return Math.clamp(Math.round((y-18)/139f*Math.max(0,count-7)),0,Math.max(0,count-7));}
 private TradeLayout(){}
}
