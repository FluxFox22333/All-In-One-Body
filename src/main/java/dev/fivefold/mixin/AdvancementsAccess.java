package dev.fivefold.mixin;
import java.util.Set;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.advancements.AdvancementHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(PlayerAdvancements.class)
public interface AdvancementsAccess {
 @Accessor("progressChanged") Set<AdvancementHolder> fivefold$changed();
 @Invoker("registerListeners") void fivefold$register(AdvancementHolder holder);
 @Invoker("unregisterListeners") void fivefold$unregister(AdvancementHolder holder);
 @Invoker("markForVisibilityUpdate") void fivefold$visibility(AdvancementHolder holder);
}
