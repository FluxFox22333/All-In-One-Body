package dev.fivefold.mixin;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(MerchantScreen.class)
public interface MerchantScreenAccess {
 @Accessor("shopItem") void fivefold$selection(int value);
 @Accessor("scrollOff") void fivefold$scroll(int value);
}
