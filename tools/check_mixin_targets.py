"""Check changed injection targets against the exact API bytecode (not a game/Mixin launch)."""
from pathlib import Path
import argparse,subprocess
p=argparse.ArgumentParser();p.add_argument('--jdk',required=True,type=Path);p.add_argument('--api',required=True,type=Path);a=p.parse_args()
def code(name):
 return subprocess.check_output([str(a.jdk/'bin/javap'),'-p','-c','-s','-classpath',str(a.api),name],text=True)
checks={
 'net.minecraft.client.gui.Hud':['void extractHotbar(', 'void extractItemHotbar(', 'void extractHealthLevel(', 'void extractArmorLevel(', 'void extractFoodLevel(', 'void extractAirLevel(', 'void extractContextualInfoBarBackground(', 'void extractContextualInfoBar(', 'void extractExperienceLevel(', 'void maybeExtractSpectatorTooltip(', 'net/minecraft/client/player/LocalPlayer.getActiveEffects:()Ljava/util/Collection;'],
 'net.minecraft.client.gui.screens.inventory.MerchantScreen':['private int shopItem;', 'private int scrollOff;'],
 'net.minecraft.client.renderer.extract.LevelExtractor':['void extractPlayerState(net.minecraft.client.Camera, net.minecraft.client.DeltaTracker, float, net.minecraft.client.renderer.state.level.PlayerRenderState);', 'net.minecraft.client.renderer.entity.state.EntityRenderState extractEntity(net.minecraft.world.entity.Entity, float);'],
 'net.minecraft.client.renderer.GameRenderer':['void renderItemInHand(net.minecraft.client.renderer.state.level.CameraRenderState, net.minecraft.client.renderer.state.level.PlayerRenderState, com.mojang.renderpearl.api.textures.GpuTextureView);', 'net/minecraft/client/multiplayer/MultiPlayerGameMode.getPlayerMode:()Lnet/minecraft/world/level/GameType;'],
 'net.minecraft.client.renderer.state.level.PlayerRenderState':['void reset();'],
}
count=0
for name,needles in checks.items():
 text=code(name)
 for needle in needles:
  assert needle in text,(name,needle)
  count+=1
 print(name,':',len(needles),'targets found')
print(f'{count} bytecode target checks passed; runtime transformation/rendering not tested.')
