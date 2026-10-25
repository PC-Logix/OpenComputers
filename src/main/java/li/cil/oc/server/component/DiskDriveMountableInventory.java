package li.cil.oc.server.component;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.container.AbstractComponentInventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Java host adapter for the disk drive's component inventory. */
public final class DiskDriveMountableInventory extends AbstractComponentInventory {
    private final DiskDriveMountable drive;

    public DiskDriveMountableInventory(DiskDriveMountable drive) {
        this.drive = drive;
    }

    @Override public EnvironmentHost host() { return drive.rack(); }
    @Override public ItemStack[] items() { return drive.items(); }
    @Override public int getContainerSize() { return drive.getContainerSize(); }
    @Override public void setChanged() { }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return drive.canPlaceItem(slot, stack); }
    @Override public boolean stillValid(Player player) { return drive.stillValid(player); }
    @Override public Node node() { return drive.node(); }
    @Override public void onConnect(Node node) { }
    @Override public void onDisconnect(Node node) { }
    @Override public void onMessage(Message message) { }
}
