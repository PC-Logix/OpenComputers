package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.server.component.traits.ContainerAnalytics;
import li.cil.oc.server.component.traits.ContainerLevelControlMk2;
import li.cil.oc.server.component.traits.ItemContainerControl;
import li.cil.oc.server.component.traits.LevelInventoryAnalytics;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;

import java.util.Map;

public final class UpgradeInventoryController {
    private UpgradeInventoryController() {}

    public interface Common extends DeviceInfo {
        @Override
        default Map<String, String> getDeviceInfo() {
            return Map.of(
                    DeviceAttribute.Class, DeviceClass.Generic.toString(),
                    DeviceAttribute.Description, "Inventory controller",
                    DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                    DeviceAttribute.Product, "Item Cataloguer R1");
        }
    }

    public static class Adapter extends AbstractManagedEnvironment implements LevelInventoryAnalytics, Common {
        private final EnvironmentHost host;
        private final Node node;

        public Adapter(EnvironmentHost host) {
            this.host = host;
            this.node = Network.newNode(this, Visibility.Network)
                    .withComponent("inventory_controller", Visibility.Network).create();
        }

        public EnvironmentHost host() { return host; }
        @Override public Node node() { return node; }
        @Override public BlockPosition position() { return BlockPositionsJava.apply(host); }
        @Override public Direction checkSideForAction(Arguments args, int n) {
            return new ExtendedArgumentsJava(args).checkSideAny(n);
        }
    }

    public static class Drone extends AbstractManagedEnvironment implements ContainerAnalytics, ContainerLevelControlMk2,
            LevelInventoryAnalytics, ItemContainerControl, Common {
        private final Agent host;
        private final Node node;

        public Drone(Agent host) {
            this.host = host;
            this.node = Network.newNode(this, Visibility.Network)
                    .withComponent("inventory_controller", Visibility.Neighbors).create();
        }

        public Agent host() { return host; }
        @Override public Node node() { return node; }
        @Override public BlockPosition position() { return BlockPositionsJava.apply(host); }
        @Override public Container inventory() { return host.mainInventory(); }
        @Override public net.minecraft.world.entity.player.Player fakePlayer() { return host.player(); }
        @Override public int selectedSlot() { return host.selectedSlot(); }
        @Override public void selectedSlot_$eq(int value) { host.setSelectedSlot(value); }
        @Override public Direction checkSideForAction(Arguments args, int n) {
            return new ExtendedArgumentsJava(args).checkSideAny(n);
        }
    }

    public static class Robot extends AbstractManagedEnvironment implements ContainerAnalytics, ContainerLevelControlMk2,
            LevelInventoryAnalytics, ItemContainerControl, Common {
        private final li.cil.oc.common.blockentity.Robot host;
        private final Node node;

        public Robot(li.cil.oc.common.blockentity.Robot host) {
            this.host = host;
            this.node = Network.newNode(this, Visibility.Network)
                    .withComponent("inventory_controller", Visibility.Neighbors).create();
        }

        public li.cil.oc.common.blockentity.Robot host() { return host; }
        @Override public Node node() { return node; }
        @Override public BlockPosition position() { return BlockPositionsJava.apply(host); }
        @Override public Container inventory() { return host.mainInventory(); }
        @Override public net.minecraft.world.entity.player.Player fakePlayer() { return host.player(); }
        @Override public int selectedSlot() { return host.selectedSlot(); }
        @Override public void selectedSlot_$eq(int value) { host.setSelectedSlot(value); }
        @Override public Direction checkSideForAction(Arguments args, int n) {
            return host.toGlobal(new ExtendedArgumentsJava(args).checkSideForAction(n));
        }

        @Callback(doc = "function():boolean -- Swaps the equipped tool with the content of the currently selected inventory slot.")
        public Object[] equip(Context context, Arguments args) {
            Container inventory = inventory();
            if (inventory.getContainerSize() > 0) {
                var equipped = host.getItem(0);
                var selected = inventory.getItem(selectedSlot());
                host.setItem(0, selected);
                inventory.setItem(selectedSlot(), equipped);
                return ResultWrapper.result(true);
            }
            return ResultWrapper.result(false);
        }
    }
}

