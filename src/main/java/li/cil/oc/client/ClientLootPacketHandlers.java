package li.cil.oc.client;

import li.cil.oc.common.Loot;
import li.cil.oc.common.PacketHandlerBase.PacketParser;
import li.cil.oc.integration.Mods;
import li.cil.oc.integration.jei.ModJEI;
import net.minecraft.world.item.ItemStack;
import scala.jdk.javaapi.CollectionConverters;

import java.io.IOException;

/** Applies clientbound loot disk and EEPROM registry updates. */
public final class ClientLootPacketHandlers {
    private ClientLootPacketHandlers() {}

    public static void lootDisk(PacketParser packet) throws IOException {
        ItemStack stack = packet.readItemStack();
        if (!stack.isEmpty() && !containsSameStack(Loot.disksForClient, stack)) {
            Loot.disksForClient.append(stack);
            if (Mods.JustEnoughItems().isModAvailable()) ModJEI.addDiskAtRuntime(stack);
        }
    }

    public static void resetLootDisks() {
        Loot.resetDisksForClient();
        Loot.disksForCyclingClient.clear();
    }

    public static void resetLootEeproms() {
        Loot.eepromsForClient.clear();
    }

    public static void lootEeprom(PacketParser packet) throws IOException {
        ItemStack stack = packet.readItemStack();
        if (!stack.isEmpty() && !containsSameStack(Loot.eepromsForClient, stack)) {
            Loot.eepromsForClient.append(stack);
            if (Mods.JustEnoughItems().isModAvailable()) ModJEI.addItemAtRuntime(stack);
        }
    }

    public static Object cyclingDisk(PacketParser packet) throws IOException {
        ItemStack stack = packet.readItemStack();
        if (!stack.isEmpty() && !containsSameStack(Loot.disksForCyclingClient, stack)) {
            return Loot.disksForCyclingClient.$plus$eq(stack);
        }
        return scala.runtime.BoxedUnit.UNIT;
    }

    private static boolean containsSameStack(scala.collection.mutable.ArrayBuffer<ItemStack> stacks, ItemStack stack) {
        for (ItemStack existing : CollectionConverters.asJava(stacks)) {
            if (ItemStack.isSameItemSameComponents(existing, stack)) return true;
        }
        return false;
    }
}
