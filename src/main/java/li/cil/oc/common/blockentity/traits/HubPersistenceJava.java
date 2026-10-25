package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.network.Node;
import li.cil.oc.util.ExtendedCompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.Optional;
import java.util.Queue;

/** NBT serialization for relay plug nodes and queued packets. */
final class HubPersistenceJava {
    private HubPersistenceJava() { }

    static int load(CompoundTag nbt, HolderLookup.Provider provider, String plugsTag, String queueTag,
                    String sideTag, String cooldownTag, Node[] plugs,
                    Queue<HubPacketEntry> queue, int relayCooldown)
        throws UnrecoverablePersistanceException {
        ListTag savedPlugs = nbt.getList(plugsTag, Tag.TAG_COMPOUND);
        for (int index = 0; index < savedPlugs.size(); index++) {
            plugs[index].loadData(savedPlugs.getCompound(index), provider);
        }

        ListTag savedPackets = nbt.getList(queueTag, Tag.TAG_COMPOUND);
        for (int index = 0; index < savedPackets.size(); index++) {
            CompoundTag savedPacket = savedPackets.getCompound(index);
            Optional<Direction> side = new ExtendedCompoundTag(savedPacket).getDirectionOptional(sideTag);
            Direction sourceSide = side.orElse(null);
            queue.add(new HubPacketEntry(sourceSide, Network.newPacket(savedPacket)));
        }
        return nbt.contains(cooldownTag) ? nbt.getInt(cooldownTag) : relayCooldown;
    }

    static void save(CompoundTag nbt, HolderLookup.Provider provider, String plugsTag, String queueTag,
                     String sideTag, String cooldownTag, Node[] plugs,
                     Queue<HubPacketEntry> queue, int relayCooldown) {
        ListTag savedPlugs = new ListTag();
        for (Node node : plugs) {
            CompoundTag plugTag = new CompoundTag();
            if (node != null) node.saveData(plugTag, provider);
            savedPlugs.add(plugTag);
        }
        nbt.put(plugsTag, savedPlugs);

        ListTag savedPackets = new ListTag();
        for (HubPacketEntry entry : queue) {
            CompoundTag packetTag = new CompoundTag();
            new ExtendedCompoundTag(packetTag).setDirection(sideTag, entry.sourceSideOptional());
            entry.packet().saveData(packetTag);
            savedPackets.add(packetTag);
        }
        nbt.put(queueTag, savedPackets);
        if (relayCooldown > 0) nbt.putInt(cooldownTag, relayCooldown);
    }
}
