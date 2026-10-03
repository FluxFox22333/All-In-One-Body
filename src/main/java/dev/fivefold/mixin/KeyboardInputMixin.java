package dev.fivefold.mixin;
import dev.fivefold.client.ClientState;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {
 @Inject(method="tick",at=@At("HEAD"))
 private void fivefold$movement(CallbackInfo ci){ClientState.applyMovement();}
}
