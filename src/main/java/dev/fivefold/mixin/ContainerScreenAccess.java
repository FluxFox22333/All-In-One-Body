package dev.fivefold.mixin;
import java.util.List;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccess {
 @Accessor("itemSlotMouseActions") List<ItemSlotMouseAction> fivefold$mouseActions();
}
