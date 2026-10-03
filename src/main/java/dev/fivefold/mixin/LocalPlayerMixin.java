package dev.fivefold.mixin;
import dev.fivefold.client.ClientState;
import dev.fivefold.core.Action;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
 @Shadow protected int sprintTriggerTime;
 @Inject(method="aiStep",at=@At("HEAD"))
 private void fivefold$noDoubleTap(CallbackInfo ci){if(ClientState.linked())sprintTriggerTime=0;}
 @Inject(method="aiStep",at=@At("TAIL"))
 private void fivefold$sprint(CallbackInfo ci){if(ClientState.linked()&&(ClientState.movementHeld()&Action.SPRINT.bit())==0)((LocalPlayer)(Object)this).setSprinting(false);}
 @Inject(method="isAutoJumpEnabled",at=@At("HEAD"),cancellable=true)
 private void fivefold$noAutoJump(CallbackInfoReturnable<Boolean> ci){if(ClientState.linked())ci.setReturnValue(false);}
}
