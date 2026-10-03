package dev.fivefold.mixin;
import dev.fivefold.client.SharedHands;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelExtractor.class)
public abstract class SharedHandsExtractMixin {
 @Shadow private EntityRenderState extractEntity(Entity entity,float partialTicks){throw new AssertionError();}
 @Inject(method="extractPlayerState",at=@At("TAIL"))
 private void fivefold$hands(Camera camera,DeltaTracker delta,float partialTicks,PlayerRenderState state,CallbackInfo ci){
  dev.fivefold.client.SharedBody.apply();
  var body=SharedHands.subject(camera);
  if(body!=null&&extractEntity(body,partialTicks) instanceof AvatarRenderState avatar)SharedHands.extract(body,camera,avatar,state);
 }
}
