package dev.fivefold.mixin;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(KeyMapping.class)
public interface KeyAccess {
 @Accessor("isDown") void fivefold$down(boolean down);
 @Accessor("clickCount") void fivefold$clicks(int clicks);
}
