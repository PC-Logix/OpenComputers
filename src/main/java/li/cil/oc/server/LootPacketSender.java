package li.cil.oc.server;

import li.cil.oc.common.Loot;
import li.cil.oc.common.PacketType;
import li.cil.oc.common.SimplePacketBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import scala.Tuple2;

import java.io.IOException;

public final class LootPacketSender {
    private LootPacketSender() {}

    public static void sendDisks(ServerPlayer player) throws IOException {
        new SimplePacketBuilder(PacketType.LootDisksReset).sendToPlayer(player);
        scala.collection.Iterator<Tuple2<ItemStack, Object>> worldDisks = Loot.worldDisks.iterator();
        while (worldDisks.hasNext()) sendItem(PacketType.LootDisk, worldDisks.next()._1(), player);
        scala.collection.Iterator<ItemStack> cyclingDisks = Loot.disksForCyclingServer.iterator();
        while (cyclingDisks.hasNext()) sendItem(PacketType.CyclingDisk, cyclingDisks.next(), player);
    }

    public static void sendEeproms(ServerPlayer player) throws IOException {
        new SimplePacketBuilder(PacketType.LootEEPROMsReset).sendToPlayer(player);
        scala.collection.Iterator<ItemStack> eeproms = Loot.eepromsForServer.iterator();
        while (eeproms.hasNext()) sendItem(PacketType.LootEEPROM, eeproms.next(), player);
    }

    public static void sendDisksUnchecked(ServerPlayer player) {
        try {
            sendDisks(player);
        } catch (IOException error) {
            LootPacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    public static void sendEepromsUnchecked(ServerPlayer player) {
        try {
            sendEeproms(player);
        } catch (IOException error) {
            LootPacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    private static void sendItem(PacketType type, ItemStack stack, ServerPlayer player) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(type);
        packet.writeItemStack(stack, player.server.registryAccess());
        packet.sendToPlayer(player);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwUnchecked(Throwable error) throws E {
        throw (E) error;
    }
}
