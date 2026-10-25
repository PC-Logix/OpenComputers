package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.server.ActivityPacketSender;
import li.cil.oc.server.component.traits.InventoryTransfer;
import li.cil.oc.server.component.traits.LevelInventoryAnalytics;
import li.cil.oc.server.component.traits.LevelTankAnalytics;
import li.cil.oc.server.network.Component;
import li.cil.oc.server.network.Connector;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.ExtendedArgumentsJava;
import net.minecraft.core.Direction;

import java.util.Map;
import java.util.Optional;

public final class Transposer {
    private Transposer() {}

    public abstract static class Common extends AbstractManagedEnvironment
            implements LevelInventoryAnalytics, LevelTankAnalytics, InventoryTransfer, DeviceInfo {
        private final Node node;

        protected Common() {
            this.node = Network.newNode(this, Visibility.Network)
                    .withComponent("transposer")
                    .withConnector()
                    .create();
        }

        @Override
        public Node node() {
            return node;
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return Map.of(
                    DeviceAttribute.Class, DeviceClass.Generic,
                    DeviceAttribute.Description, "Transposer",
                    DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                    DeviceAttribute.Product, "TP4k-iX");
        }

        @Override
        public Direction checkSideForAction(Arguments args, int n) {
            return new ExtendedArgumentsJava(args).checkSideAny(n);
        }

        @Override
        public Optional<String> onTransferContents() {
            if (((Connector) node).tryChangeBuffer(-Settings.get().transposerCost())) {
                return Optional.empty();
            }
            return Optional.of("not enough energy");
        }
    }

    public static class Block extends Common {
        private final li.cil.oc.common.blockentity.Transposer host;

        public Block(li.cil.oc.common.blockentity.Transposer host) {
            this.host = host;
        }

        public li.cil.oc.common.blockentity.Transposer host() {
            return host;
        }

        @Override
        public BlockPosition position() {
            return BlockPositionsJava.apply(host);
        }

        @Override
        public Optional<String> onTransferContents() {
            Optional<String> result = super.onTransferContents();
            if (result.isEmpty()) {
                ActivityPacketSender.sendTransposerActivityUnchecked(host,
                        Settings.get().maxNetworkClientEffectPacketDistance() / 2.0D);
            }
            return result;
        }
    }

    public static class Upgrade extends Common {
        private final EnvironmentHost host;

        public Upgrade(EnvironmentHost host) {
            this.host = host;
            ((Component) node()).setVisibility(Visibility.Neighbors);
        }

        public EnvironmentHost host() {
            return host;
        }

        @Override
        public BlockPosition position() {
            return BlockPositionsJava.apply(host);
        }
    }
}
