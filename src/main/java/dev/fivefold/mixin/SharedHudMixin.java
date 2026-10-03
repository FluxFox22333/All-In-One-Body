package dev.fivefold.mixin;
import dev.fivefold.client.*;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reuse vanilla hotbar/hearts/food/air drawing with the synchronized camera player. */
@Mixin(Hud.class)
public abstract class SharedHudMixin {
 @Shadow private void extractItemHotbar(GuiGraphicsExtractor g,DeltaTracker dt){throw new AssertionError();}
 @Shadow private void extractHealthLevel(GuiGraphicsExtractor g){throw new AssertionError();}
 @Shadow private void extractArmorLevel(GuiGraphicsExtractor g){throw new AssertionError();}
 @Shadow private void extractFoodLevel(GuiGraphicsExtractor g){throw new AssertionError();}
 @Shadow private void extractAirLevel(GuiGraphicsExtractor g){throw new AssertionError();}
 @Redirect(method="extractEffects",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;getActiveEffects()Ljava/util/Collection;"))
 private java.util.Collection<net.minecraft.world.effect.MobEffectInstance> fivefold$effects(net.minecraft.client.player.LocalPlayer player){return SharedBody.viewing()?SharedBody.effects():player.getActiveEffects();}
 @Inject(method="extractHotbar",at=@At("HEAD"),cancellable=true)
 private void fivefold$hotbar(GuiGraphicsExtractor g,DeltaTracker dt,CallbackInfo ci){
  if(!SharedBody.viewing())return;
  SharedBody.apply();extractItemHotbar(g,dt);
  if(ClientState.state.get("survival").getAsBoolean()){
   extractHealthLevel(g);extractArmorLevel(g);extractFoodLevel(g);extractAirLevel(g);
   int x=g.guiWidth()/2-91,y=g.guiHeight()-29;
   g.blitSprite(RenderPipelines.GUI_TEXTURED,Identifier.withDefaultNamespace("hud/experience_bar_background"),x,y,182,5);
   int progress=(int)(Math.clamp(ClientState.state.get("xpProgress").getAsFloat(),0,1)*183);
   if(progress>0)g.blitSprite(RenderPipelines.GUI_TEXTURED,Identifier.withDefaultNamespace("hud/experience_bar_progress"),182,5,0,0,x,y,progress,5);
   int level=ClientState.state.get("xp").getAsInt();if(level>0)ContextualBar.extractExperienceLevel(g,ClientState.mc().font,level);
  }
  ci.cancel();
 }
 @Inject(method={"extractContextualInfoBarBackground","extractContextualInfoBar","extractExperienceLevel","maybeExtractSpectatorTooltip"},at=@At("HEAD"),cancellable=true)
 private void fivefold$noSpectatorHud(GuiGraphicsExtractor g,DeltaTracker dt,CallbackInfo ci){if(SharedBody.viewing())ci.cancel();}
}
