package dev.fivefold.mixin;
import dev.fivefold.client.SharedScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Shared slots work independently of each viewer's personal recipe-book preferences. */
@Mixin(RecipeBookComponent.class)
public class RecipeBookMixin {
 @Inject(method="isVisibleAccordingToBookData",at=@At("HEAD"),cancellable=true)
 private void fivefold$book(CallbackInfoReturnable<Boolean> ci){if(SharedScreen.preparing)ci.setReturnValue(false);}
}
