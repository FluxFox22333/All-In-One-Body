package dev.fivefold.mixin;
import dev.fivefold.client.ClientState;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MouseHandler.class)
public class MouseMixin {
 @Shadow private double accumulatedDX;
 @Shadow private double accumulatedDY;
 @Inject(method="onButton",at=@At("HEAD"),cancellable=true)
 private void fivefold$button(long window,MouseButtonInfo info,int action,CallbackInfo ci){if(ClientState.mouse(info,action))ci.cancel();}
 @Inject(method="onScroll",at=@At("HEAD"),cancellable=true)
 private void fivefold$scroll(long window,double x,double y,CallbackInfo ci){if(ClientState.scroll(y))ci.cancel();}
 @Inject(method="turnPlayer",at=@At("HEAD"),cancellable=true)
 private void fivefold$turn(double dt,CallbackInfo ci){if(ClientState.linked()){ClientState.look(accumulatedDX,accumulatedDY);ci.cancel();}}
}
