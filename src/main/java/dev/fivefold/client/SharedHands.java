package dev.fivefold.client;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState.HandRenderSelection;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;

/** Extract the body's native equipment/skin into the first-person render state without modifying inventories. */
public final class SharedHands {
 public static AbstractClientPlayer subject(){
  if(!ClientState.linked()||ClientState.body()||!ClientState.mc().options.getCameraType().isFirstPerson())return null;
  if(ClientState.mc().getCameraEntity() instanceof AbstractClientPlayer player&&player.getId()==ClientState.state.get("entity").getAsInt()&&ClientState.state.has("equipmentOffset")&&ClientState.items.size()>ClientState.state.get("equipmentOffset").getAsInt()+1&&player.isAlive()&&!player.isSleeping())return player;
  return null;
 }
 public static void extract(AbstractClientPlayer body,CameraRenderState camera,AvatarRenderState avatar,PlayerRenderState state){
  var mc=ClientState.mc();var hands=state.firstPersonHandsAndItems;
  state.avatarRenderState=avatar;state.hasPlayer=true;
  var snapshot=ClientState.state;avatar.mainArm=HumanoidArm.valueOf(snapshot.get("mainArm").getAsString());avatar.isUsingItem=snapshot.get("usingItem").getAsBoolean();avatar.useItemHand=InteractionHand.valueOf(snapshot.get("useHand").getAsString());
  var swing=body.getCurrentSwing();hands.attackHand=swing==null?InteractionHand.MAIN_HAND:swing.hand();
  hands.viewXRot=camera.xRot;hands.viewYRot=camera.yRot;hands.xBob=hands.viewXRot;hands.yBob=hands.viewYRot;
  hands.isScoping=snapshot.get("scoping").getAsBoolean();hands.useItemRemainingTicks=snapshot.get("useRemaining").getAsInt();
  int offset=snapshot.get("equipmentOffset").getAsInt();hands.mainHandItem=ClientState.items.get(offset).copy();hands.offHandItem=ClientState.items.get(offset+1).copy();
  hands.handRenderSelection=selection(hands.mainHandItem,hands.offHandItem,avatar.isUsingItem,avatar.useItemHand);
  // The remote body has no LocalPlayer equip-height controller. Keep a fully raised native hand pose.
  hands.mainHandHeight=hands.oldMainHandHeight=hands.offHandHeight=hands.oldOffHandHeight=1;
  boolean right=avatar.mainArm==HumanoidArm.RIGHT;
  var main=right?ItemDisplayContext.FIRST_PERSON_RIGHT_HAND:ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
  var off=right?ItemDisplayContext.FIRST_PERSON_LEFT_HAND:ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
  hands.mainHandRenderState.clear();hands.offHandRenderState.clear();
  mc.getItemModelResolver().updateForTopItem(hands.mainHandRenderState,hands.mainHandItem,main,body.level(),body,body.getId()+main.ordinal());
  mc.getItemModelResolver().updateForTopItem(hands.offHandRenderState,hands.offHandItem,off,body.level(),body,body.getId()+off.ordinal());
  hands.mainHandUseDuration=hands.mainHandItem.getUseDuration(body);hands.offHandUseDuration=hands.offHandItem.getUseDuration(body);
  hands.mainHandChargeDuration=CrossbowItem.getChargeDuration(hands.mainHandItem,body);hands.offHandChargeDuration=CrossbowItem.getChargeDuration(hands.offHandItem,body);
  hands.mainHandSwapScale=mc.getItemModelResolver().swapAnimationScale(hands.mainHandItem);hands.offHandSwapScale=mc.getItemModelResolver().swapAnimationScale(hands.offHandItem);
  hands.hasMainHandMapData=map(body,hands.mainHandItem,hands.mainHandMapRenderState);hands.hasOffHandMapData=map(body,hands.offHandItem,hands.offHandMapRenderState);
  ((SharedHandsFlag)state).fivefold$sharedHands(true);
 }
 private static boolean map(AbstractClientPlayer body,ItemStack stack,MapRenderState state){
  var id=stack.get(DataComponents.MAP_ID);var data=id==null?null:MapItem.getSavedData(stack,body.level());
  if(id==null||data==null)return false;
  ClientState.mc().getMapRenderer().extractRenderState(id,data,state);return true;
 }
 private static boolean charged(ItemStack stack){return stack.is(Items.CROSSBOW)&&CrossbowItem.isCharged(stack);}
 private static HandRenderSelection selection(ItemStack main,ItemStack off,boolean using,InteractionHand hand){
  if(!main.is(Items.BOW)&&!off.is(Items.BOW)&&!main.is(Items.CROSSBOW)&&!off.is(Items.CROSSBOW))return HandRenderSelection.RENDER_BOTH_HANDS;
  if(!using)return charged(main)?HandRenderSelection.RENDER_MAIN_HAND_ONLY:HandRenderSelection.RENDER_BOTH_HANDS;
  var item=hand==InteractionHand.MAIN_HAND?main:off;
  if(item.is(Items.BOW)||item.is(Items.CROSSBOW))return HandRenderSelection.onlyForHand(hand);
  return hand==InteractionHand.MAIN_HAND&&charged(off)?HandRenderSelection.RENDER_MAIN_HAND_ONLY:HandRenderSelection.RENDER_BOTH_HANDS;
 }
 private SharedHands(){}
}
