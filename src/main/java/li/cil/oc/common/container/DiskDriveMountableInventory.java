package li.cil.oc.common.container;

import li.cil.oc.api.Driver;
import li.cil.oc.common.Slot;
import li.cil.oc.common.blockentity.DiskDrive;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class DiskDriveMountableInventory implements ItemStackInventory {
    private final Player player;
    private final InteractionHand hand;
    private final ItemStack stack;
    private final ItemStack[] items = {ItemStack.EMPTY};

    public DiskDriveMountableInventory(Player player, InteractionHand hand) {
        this.player = player;
        this.hand = hand;
        this.stack = player.getItemInHand(hand);
        // The Scala trait initialized its slots from the stack during construction.
        reinitialize();
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public String inventoryName() {
        return "diskdrive";
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != 0) return false;
        var driver = Driver.driverFor(stack, DiskDrive.class);
        return driver != null && driver.slot(stack) == Slot.Floppy;
    }

    @Override
    public ItemStack container() {
        return stack;
    }

    @Override
    public ItemStack[] items() {
        return items;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player && player.getItemInHand(hand) == stack;
    }
}
