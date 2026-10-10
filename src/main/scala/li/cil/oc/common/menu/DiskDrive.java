package li.cil.oc.common.menu;

import li.cil.oc.common.Slot;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;

public class DiskDrive extends AbstractMenu {
    public DiskDrive(int id, Inventory playerInventory, Container drive) {
        super(MenuTypes.DISK_DRIVE.get(), id, playerInventory, drive);
        addSlotToContainer(80, 35, Slot.Floppy, li.cil.oc.common.Tier.Any);
        addPlayerInventorySlots(8, 84);
    }

    @Override
    public Class<? extends li.cil.oc.api.network.EnvironmentHost> getHostClass() {
        return li.cil.oc.common.blockentity.DiskDrive.class;
    }
}
