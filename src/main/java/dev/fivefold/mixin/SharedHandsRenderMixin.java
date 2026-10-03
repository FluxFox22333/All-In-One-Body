package dev.fivefold.mixin;
import dev.fivefold.client.SharedHandsFlag;
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
 @Redirect(method="renderItemInHand",at=@At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;getPlayerMode()Lnet/minecraft/world/level/GameType;"))
 private GameType fivefold$allowHands(MultiPlayerGameMode mode,CameraRenderState camera,PlayerRenderState state,GpuTextureView depth){
  return ((SharedHandsFlag)state).fivefold$sharedHands()?GameType.SURVIVAL:mode.getPlayerMode();
 }
}
