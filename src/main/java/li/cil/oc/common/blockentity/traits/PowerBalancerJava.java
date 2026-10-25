package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.network.Connector;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;

/** Java implementation of the synchronized power distribution algorithm. */
public final class PowerBalancerJava {
    private PowerBalancerJava() { }

    public static void update(PowerBalancer self, double tickFrequency) {
        if (!self.isServer() || !self.powerNetworkConnected()) return;
        if (((BlockEntity) self).getLevel().getGameTime() % tickFrequency != 0) return;

        Connector[] lockOrder = connectors(self);
        withNetworkLocks(self, lockOrder, 0, () -> {
            double[] totals = distribute(self);
            double sumBuffer = totals[0];
            double sumSize = totals[1];

            if (sumSize > 0) {
                double ratio = sumBuffer / sumSize;
                for (Connector connector : connectors(self)) {
                    if (isPrimary(connectors(self), connector)) {
                        connector.changeBuffer(connector.globalBufferSize() * ratio - connector.globalBuffer());
                    }
                }
            }
            self.globalBuffer_$eq(sumBuffer);
            self.globalBufferSize_$eq(sumSize);
            self.updatePowerInformation();
        });
    }

    public static double[] distribute(PowerBalancer self) {
        Connector[] nodes = connectors(self);
        double sumBuffer = 0;
        double sumSize = 0;
        for (Connector connector : nodes) {
            if (isPrimary(nodes, connector)) {
                sumBuffer += connector.globalBuffer();
                sumSize += connector.globalBufferSize();
            }
        }
        return new double[]{sumBuffer, sumSize};
    }

    private static Connector[] connectors(PowerBalancer self) {
        Direction[] directions = Direction.values();
        Connector[] result = new Connector[directions.length];
        for (int index = 0; index < directions.length; index++) {
            Object node = self.sidedNode(directions[index]);
            if (node instanceof Connector connector) result[index] = connector;
        }
        return result;
    }

    private static boolean isPrimary(Connector[] nodes, Connector connector) {
        if (connector == null) return false;
        Object network = connector.network();
        for (Connector node : nodes) {
            if (node != null && Objects.equals(node.network(), network)) return node == connector;
        }
        return false;
    }

    private static Object networkOrSelf(PowerBalancer self, Connector connector) {
        return connector != null && connector.network() != null ? connector.network() : self;
    }

    private static void withNetworkLocks(PowerBalancer self, Connector[] nodes, int index, Runnable action) {
        if (index == nodes.length) {
            action.run();
            return;
        }
        synchronized (networkOrSelf(self, nodes[index])) {
            withNetworkLocks(self, nodes, index + 1, action);
        }
    }
    public static void updateAfterEnvironment(Object host) {
        if (host instanceof PowerBalancer balancer && !balancer.powerBalanceAfterHub())
            update(balancer, li.cil.oc.SettingsAccessJava.get().tickFrequency());
    }

    public static void updateAfterHub(Object host) {
        if (host instanceof PowerBalancer balancer && balancer.powerBalanceAfterHub())
            update(balancer, li.cil.oc.SettingsAccessJava.get().tickFrequency());
    }
}
