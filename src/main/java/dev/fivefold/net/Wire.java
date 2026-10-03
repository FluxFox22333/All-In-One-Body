package dev.fivefold.net;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
public record Wire(String json,List<ItemStack> items,net.minecraft.world.item.trading.MerchantOffers offers) implements CustomPacketPayload {
 public static final Type<Wire> TYPE=new Type<>(Identifier.fromNamespaceAndPath("fivefold","wire"));
 public static final StreamCodec<RegistryFriendlyByteBuf,Wire> CODEC=new StreamCodec<>() {
  public Wire decode(RegistryFriendlyByteBuf b){
   String json=b.readUtf(32767);int n=b.readVarInt();if(n<0||n>512)throw new IllegalArgumentException("slots");
   List<ItemStack> items=new ArrayList<>();for(int i=0;i<n;i++)items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(b));var offers=b.readBoolean()?net.minecraft.world.item.trading.MerchantOffers.STREAM_CODEC.decode(b):null;return new Wire(json,items,offers);
  }
  public void encode(RegistryFriendlyByteBuf b,Wire w){b.writeUtf(w.json,32767);b.writeVarInt(w.items.size());for(ItemStack s:w.items)ItemStack.OPTIONAL_STREAM_CODEC.encode(b,s);b.writeBoolean(w.offers!=null);if(w.offers!=null)net.minecraft.world.item.trading.MerchantOffers.STREAM_CODEC.encode(b,w.offers);}
 };
 public Wire(String json,List<ItemStack> items){this(json,items,null);}
 public Wire(JsonObject data){this(data.toString(),List.of());}
 public JsonObject data(){return JsonParser.parseString(json).getAsJsonObject();}
 public Type<Wire> type(){return TYPE;}
 public static JsonObject message(String kind){JsonObject o=new JsonObject();o.addProperty("kind",kind);return o;}
}
