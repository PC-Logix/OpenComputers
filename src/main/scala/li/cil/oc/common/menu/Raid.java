package li.cil.oc.common.menu;

import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;

public class Raid extends AbstractMenu {
    public Raid(int id, Inventory playerInventory, Container raid) {
        super(MenuTypes.RAID.get(), id, playerInventory, raid);
        addSlotToContainer(60, 23, Slot.HDD, Tier.Seven);
        addSlotToContainer(80, 23, Slot.HDD, Tier.Seven);
        addSlotToContainer(100, 23, Slot.HDD, Tier.Seven);
        addPlayerInventorySlots(8, 84);
    }

    @Override
    public Class<? extends li.cil.oc.api.network.EnvironmentHost> getHostClass() {
        return li.cil.oc.common.blockentity.Raid.class;
    }
}
