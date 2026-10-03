package dev.fivefold;
import dev.fivefold.net.Wire;
import dev.fivefold.server.Sessions;
import dev.fivefold.server.FastRelay;
import dev.fivefold.server.SharedAdvancements;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.minecraft.server.level.ServerPlayer;
@Mod("fivefold")
public final class Fivefold {
 public Fivefold(IEventBus bus){
  bus.addListener((RegisterPayloadHandlersEvent e)->e.registrar("10").executesOn(HandlerThread.NETWORK).playBidirectional(Wire.TYPE,Wire.CODEC,(w,c)->{
   if(!FastRelay.receive(c.connection(),w))c.enqueueWork(()->{if(c.player() instanceof ServerPlayer p)Sessions.receive(p,w);});
  }));
  NeoForge.EVENT_BUS.addListener(Sessions::tick);
  NeoForge.EVENT_BUS.addListener(SharedAdvancements::progressed);
  NeoForge.EVENT_BUS.addListener(Sessions::logout);
  NeoForge.EVENT_BUS.addListener(Sessions::login);
  NeoForge.EVENT_BUS.addListener(Sessions::stopping);
 }
}
