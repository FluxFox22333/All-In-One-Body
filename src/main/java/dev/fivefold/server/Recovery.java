package dev.fivefold.server;
import com.google.gson.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
/** Persist original modes BEFORE changing any player to spectator. */
final class Recovery {
 static Path path(MinecraftServer server){return server.getWorldPath(LevelResource.ROOT).resolve("fivefold-recovery.json");}
 static JsonObject read(MinecraftServer server)throws IOException{
  Path path=path(server);return Files.exists(path)?JsonParser.parseString(Files.readString(path)).getAsJsonObject():new JsonObject();
 }
 static void write(MinecraftServer server,JsonObject data)throws IOException{
  Path path=path(server),tmp=path.resolveSibling("fivefold-recovery.json.tmp");Files.createDirectories(path.getParent());Files.writeString(tmp,data.toString());
  try{Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException ex){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}
 }
 static void save(MinecraftServer server,Map<UUID,GameType> modes){
  try{var data=read(server);for(var e:modes.entrySet())data.addProperty(e.getKey().toString(),e.getValue().getName());write(server,data);}catch(IOException|RuntimeException ex){throw new IllegalStateException("无法保存退出恢复记录，未启动共控",ex);}
 }
 static void restore(ServerPlayer player){
  var server=player.level().getServer();
  try{var data=read(server);String id=player.getUUID().toString();if(!data.has(id))return;
   player.setCamera(player);player.setGameMode(GameType.byName(data.get(id).getAsString(),GameType.SURVIVAL));data.remove(id);write(server,data);
  }catch(IOException|RuntimeException ex){com.mojang.logging.LogUtils.getLogger().error("Fivefold: could not restore player mode",ex);}
 }
}
