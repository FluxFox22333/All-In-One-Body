package dev.fivefold.mixin;
import dev.fivefold.client.ClientState;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
 @Inject(method="keyPress",at=@At("HEAD"),cancellable=true)
 private void fivefold$key(long window,int action,KeyEvent event,CallbackInfo ci){if(ClientState.key(action,event))ci.cancel();}
}
