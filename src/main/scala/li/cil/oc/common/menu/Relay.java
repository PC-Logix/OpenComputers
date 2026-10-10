package li.cil.oc.common.menu;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class Relay extends AbstractMenu {
    private final Container relay;
    private ItemInfo wirelessNetworkCardTier1;
    private ItemInfo wirelessNetworkCardTier2;
    private ItemInfo linkedCard;

    public Relay(int id, Inventory playerInventory, Container relay) {
        super(MenuTypes.RELAY.get(), id, playerInventory, relay);
        this.relay = relay;

        addSlotToContainer(151, 15, Slot.CPU, Tier.Any);
        addSlotToContainer(151, 34, Slot.Memory, Tier.Any);
        addSlotToContainer(151, 53, Slot.HDD, Tier.Any);
        addSlot(new StaticComponentSlot(this, otherInventory(), slots.size(), 178, 15, getHostClass(), Slot.Card, Tier.Any) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                ItemInfo item = Items.get(stack);
                if (item != WirelessNetworkCardTier1() && item != WirelessNetworkCardTier2() && item != LinkedCard()) {
                    return false;
                }
                return super.mayPlace(stack);
            }
        });
        addPlayerInventorySlots(8, 84);
    }

    public ItemInfo WirelessNetworkCardTier1() {
        if (wirelessNetworkCardTier1 == null) {
            wirelessNetworkCardTier1 = Items.get(Constants.ItemName$.MODULE$.WirelessNetworkCardTier1());
        }
        return wirelessNetworkCardTier1;
    }

    public ItemInfo WirelessNetworkCardTier2() {
        if (wirelessNetworkCardTier2 == null) {
            wirelessNetworkCardTier2 = Items.get(Constants.ItemName$.MODULE$.WirelessNetworkCardTier2());
        }
        return wirelessNetworkCardTier2;
    }

    public ItemInfo LinkedCard() {
        if (linkedCard == null) {
            linkedCard = Items.get(Constants.ItemName$.MODULE$.LinkedCard());
        }
        return linkedCard;
    }

    @Override
    public Class<? extends li.cil.oc.api.network.EnvironmentHost> getHostClass() {
        return li.cil.oc.common.blockentity.Relay.class;
    }

    public int relayDelay() {
        return synchronizedData().getInt("relayDelay");
    }

    public int relayAmount() {
        return synchronizedData().getInt("relayAmount");
    }

    public int maxQueueSize() {
        return synchronizedData().getInt("maxQueueSize");
    }

    public int packetsPerCycleAvg() {
        return synchronizedData().getInt("packetsPerCycleAvg");
    }

    public int queueSize() {
        return synchronizedData().getInt("queueSize");
    }

    @Override
    public void detectCustomDataChanges(CompoundTag nbt) {
        if (relay instanceof li.cil.oc.common.blockentity.Relay entity) {
            synchronizedData().putInt("relayDelay", entity.relayDelay());
            synchronizedData().putInt("relayAmount", entity.relayAmount());
            synchronizedData().putInt("maxQueueSize", entity.maxQueueSize());
            synchronizedData().putInt("packetsPerCycleAvg", entity.packetsPerCycleAvg().get());
            synchronizedData().putInt("queueSize", entity.queue().size());
        }
        super.detectCustomDataChanges(nbt);
    }
}
