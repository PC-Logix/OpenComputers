package li.cil.oc.common.entity;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.container.AbstractComponentInventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Java implementation of the drone's component inventory host. */
public final class DroneComponentInventory extends AbstractComponentInventory {
    private final Drone drone;

    public DroneComponentInventory(Drone drone) {
        this.drone = drone;
    }

    @Override
    public EnvironmentHost host() {
        return drone;
    }

    @Override
    public ItemStack[] items() {
        return drone.info().components;
    }

    @Override
    public int getContainerSize() {
        return drone.info().components.length;
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public Node node() {
        return drone.machine() == null ? null : drone.machine().node();
    }

    @Override
    public void onConnect(Node node) {
    }

    @Override
    public void onDisconnect(Node node) {
    }

    @Override
    public void onMessage(Message message) {
    }
}

