package li.cil.oc.common.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import li.cil.oc.common.item.Tape;

public class TapeDrive extends AbstractMenu {
    private int syncedState;

    public TapeDrive(int id, Inventory playerInventory, Container drive) {
        super(MenuTypes.TAPE_DRIVE.get(), id, playerInventory, drive);
        addDataSlot(new DataSlot() {
            @Override public int get() {
                return otherInventory() instanceof li.cil.oc.common.blockentity.TapeDrive
                    ? ((li.cil.oc.common.blockentity.TapeDrive) otherInventory()).guiState() : syncedState;
            }
            @Override public void set(int value) { syncedState = value; }
        });
        addSlot(new Slot(drive, 0, 80, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof Tape; }
        });
        addPlayerInventorySlots(8, 84);
    }

    public int guiState() { return syncedState; }

    @Override
    public Class<? extends li.cil.oc.api.network.EnvironmentHost> getHostClass() {
        return li.cil.oc.common.blockentity.TapeDrive.class;
    }
}
