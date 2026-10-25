package li.cil.oc.common.menu;

import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;

public class Adapter extends AbstractMenu {
    public Adapter(int id, Inventory playerInventory, Container adapter) {
        super(MenuTypes.ADAPTER.get(), id, playerInventory, adapter);
        addSlotToContainer(80, 35, Slot.Upgrade, Tier.Any);
        addPlayerInventorySlots(8, 84);
    }

    @Override
    public Class<? extends li.cil.oc.api.network.EnvironmentHost> getHostClass() {
        return li.cil.oc.common.blockentity.Adapter.class;
    }
}
