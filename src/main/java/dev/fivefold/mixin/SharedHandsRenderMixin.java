package dev.fivefold.mixin;
import dev.fivefold.client.SharedHandsFlag;
import dev.fivefold.client.SharedHands;
import dev.fivefold.client.SharedBody;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(GameRenderer.class)
public abstract class SharedHandsRenderMixin {
 @Inject(method="renderItemInHand",at=@At("HEAD"))
 private void fivefold$prepareHands(CameraRenderState camera,PlayerRenderState state,GpuTextureView depth,CallbackInfo ci){
  // Prepare at the final render boundary: vanilla extracted the local spectator's
  // hands, not the player being controlled. Do not carry spectator state forward.
  ((SharedHandsFlag)state).fivefold$sharedHands(false);
  var body=SharedHands.subject();
  if(body==null||!camera.isFirstPerson||camera.isPanoramicMode)return;
  SharedBody.apply();
  if(Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(body,camera.cameraEntityPartialTicks) instanceof AvatarRenderState avatar)
   SharedHands.extract(body,camera,avatar,state);
 }
 @Redirect(method="renderItemInHand",at=@At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;getPlayerMode()Lnet/minecraft/world/level/GameType;"))
 private GameType fivefold$allowHands(MultiPlayerGameMode mode,CameraRenderState camera,PlayerRenderState state,GpuTextureView depth){
  return ((SharedHandsFlag)state).fivefold$sharedHands()?GameType.SURVIVAL:mode.getPlayerMode();
 }
}
