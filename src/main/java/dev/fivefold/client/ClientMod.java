package dev.fivefold.client;
import dev.fivefold.net.Wire;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
@Mod(value="fivefold",dist=Dist.CLIENT)
public final class ClientMod {
 public ClientMod(IEventBus bus,ModContainer container){
  container.registerExtensionPoint(IConfigScreenFactory.class,(IConfigScreenFactory)(mod,parent)->new SettingsScreen(parent));
  bus.addListener((RegisterClientPayloadHandlersEvent e)->e.register(Wire.TYPE,(w,c)->c.enqueueWork(()->ClientState.receive(w))));
  NeoForge.EVENT_BUS.addListener(ClientState::tick);
  NeoForge.EVENT_BUS.addListener(ClientState::initScreen);
  NeoForge.EVENT_BUS.addListener(ClientState::opening);
  NeoForge.EVENT_BUS.addListener(ClientState::hud);
  NeoForge.EVENT_BUS.addListener(ClientState::camera);
 }
}
