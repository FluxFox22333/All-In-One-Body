package dev.fivefold.mixin;
import dev.fivefold.client.SharedHandsFlag;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerRenderState.class)
public abstract class PlayerHandsStateMixin implements SharedHandsFlag {
 @Unique private boolean fivefold$bodyHands;
 public boolean fivefold$sharedHands(){return fivefold$bodyHands;}
 public void fivefold$sharedHands(boolean value){fivefold$bodyHands=value;}
 @Inject(method="reset",at=@At("HEAD"))
 private void fivefold$reset(CallbackInfo ci){fivefold$bodyHands=false;}
}
