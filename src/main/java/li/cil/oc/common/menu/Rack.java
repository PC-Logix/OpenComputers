package li.cil.oc.common.menu;

import li.cil.oc.api.component.RackMountable;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.util.ExtendedCompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import scala.Option;
import scala.Option$;

import java.util.Arrays;

public class Rack extends AbstractMenu {
    private static final int MAX_CONNECTIONS = 4;

    private final Container rack;
    private final boolean[][] nodePresence = new boolean[4][MAX_CONNECTIONS];
    private final Option<Direction>[][] nodeMapping;
    private boolean relayEnabled;

    @SuppressWarnings("unchecked")
    public Rack(final int id, final Inventory playerInventory, final Container rack) {
        super(MenuTypes.RACK.get(), id, playerInventory, rack);
        this.rack = rack;
        nodeMapping = (Option<Direction>[][]) new Option<?>[rack.getContainerSize()][MAX_CONNECTIONS];
        for (final Option<Direction>[] mapping : nodeMapping) {
            Arrays.fill(mapping, Option$.MODULE$.empty());
        }

        addSlotToContainer(20, 23, Slot.RackMountable, Tier.Any);
        addSlotToContainer(20, 43, Slot.RackMountable, Tier.Any);
        addSlotToContainer(20, 63, Slot.RackMountable, Tier.Any);
        addSlotToContainer(20, 83, Slot.RackMountable, Tier.Any);
        addPlayerInventorySlots(8, 128);
    }

    public Container rack() { return rack; }
    public boolean[][] nodePresence() { return nodePresence; }
    public boolean[] nodePresence(final int slot) { return nodePresence[slot]; }
    public Option<Direction>[][] nodeMapping() { return nodeMapping; }
    public Option<Direction>[] nodeMapping(final int slot) { return nodeMapping[slot]; }
    public boolean isRelayEnabled() { return relayEnabled; }
    public void isRelayEnabled_$eq(final boolean value) { relayEnabled = value; }

    @Override
    public Class<? extends li.cil.oc.api.network.EnvironmentHost> getHostClass() {
        return li.cil.oc.common.blockentity.Rack.class;
    }

    @Override
    public void updateCustomData(final CompoundTag nbt) {
        super.updateCustomData(nbt);
        final var savedMappings = nbt.getList("nodeMapping", Tag.TAG_INT_ARRAY);
        for (int slot = 0; slot < Math.min(savedMappings.size(), nodeMapping.length); slot++) {
            final int[] sides = ((IntArrayTag) savedMappings.get(slot)).getAsIntArray();
            @SuppressWarnings("unchecked")
            final Option<Direction>[] mapping = (Option<Direction>[]) new Option<?>[sides.length];
            for (int connection = 0; connection < sides.length; connection++) {
                final int side = sides[connection];
                mapping[connection] = side >= 0
                    ? Option$.MODULE$.apply(Direction.from3DDataValue(side))
                    : Option$.MODULE$.empty();
            }
            nodeMapping[slot] = mapping;
        }

        final boolean[] savedPresence = new ExtendedCompoundTag(nbt).getBooleanArray("nodePresence");
        for (int slot = 0; slot < Math.min(nodePresence.length, (savedPresence.length + MAX_CONNECTIONS - 1) / MAX_CONNECTIONS); slot++) {
            final int start = slot * MAX_CONNECTIONS;
            final int count = Math.min(MAX_CONNECTIONS, savedPresence.length - start);
            nodePresence[slot] = Arrays.copyOfRange(savedPresence, start, start + count);
        }
        relayEnabled = nbt.getBoolean("isRelayEnabled");
    }

    @Override
    public void detectCustomDataChanges(final CompoundTag nbt) {
        super.detectCustomDataChanges(nbt);
        if (!(rack instanceof li.cil.oc.common.blockentity.Rack blockEntity)) return;

        final var mappings = new net.minecraft.nbt.ListTag();
        final Option<Direction>[][] blockMappings = blockEntity.nodeMapping();
        for (final Option<Direction>[] sides : blockMappings) {
            final int[] encoded = new int[sides.length];
            for (int connection = 0; connection < sides.length; connection++) {
                encoded[connection] = sides[connection].isDefined() ? sides[connection].get().ordinal() : -1;
            }
            mappings.add(new IntArrayTag(encoded));
        }
        nbt.put("nodeMapping", mappings);

        final boolean[] presence = new boolean[blockEntity.getContainerSize() * MAX_CONNECTIONS];
        for (int slot = 0; slot < blockEntity.getContainerSize(); slot++) {
            final RackMountable mountable = blockEntity.getMountable(slot);
            if (mountable == null) continue;
            presence[slot * MAX_CONNECTIONS] = true;
            final int connectableCount = Math.min(MAX_CONNECTIONS - 1, mountable.getConnectableCount());
            for (int connectable = 0; connectable < connectableCount; connectable++) {
                presence[slot * MAX_CONNECTIONS + connectable + 1] = mountable.getConnectableAt(connectable) != null;
            }
        }
        new ExtendedCompoundTag(nbt).setBooleanArray("nodePresence", presence);
        nbt.putBoolean("isRelayEnabled", blockEntity.isRelayEnabled());
    }
}
