package li.cil.oc.common.item;

import li.cil.oc.OpenComputers$;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Debugger extends SimpleItemBase {
    private static final DebuggerEnvironment ENVIRONMENT = new DebuggerEnvironment();

    public static Node node = ENVIRONMENT.node;
    private String unlocalizedName;

    public Debugger(final Properties properties) {
        super(properties);
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(final String value) { unlocalizedName = value; }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final var world = context.getLevel();
        if (context.getPlayer() == null || context.getPlayer() instanceof FakePlayer) {
            return InteractionResult.FAIL;
        }

        final var blockEntity = world.getBlockEntity(context.getClickedPos());
        if (!world.isClientSide()) {
            if (blockEntity instanceof SidedEnvironment host) {
                reconnect(new Node[]{host.sidedNode(context.getClickedFace())});
            } else if (blockEntity instanceof Environment host) {
                reconnect(new Node[]{host.node()});
            } else {
                node.remove();
            }
        }
        return InteractionResult.sidedSuccess(world.isClientSide());
    }

    public static void reconnect(final Node[] nodes) {
        node.remove();
        Network.joinNewNetwork(node);
        for (final var target : nodes) {
            if (target != null) {
                node.connect(target);
            }
        }
    }

    private static final class DebuggerEnvironment implements Environment {
        private final Node node = Network.newNode(this, Visibility.Network).create();

        @Override
        public Node node() {
            return node;
        }

        @Override
        public void onConnect(final Node connectedNode) {
            OpenComputers$.MODULE$.log().info("[NETWORK DEBUGGER] New node in network: {}", nodeInfo(connectedNode));
        }

        @Override
        public void onDisconnect(final Node disconnectedNode) {
            OpenComputers$.MODULE$.log().info("[NETWORK DEBUGGER] Node removed from network: {}", nodeInfo(disconnectedNode));
        }

        @Override
        public void onMessage(final Message message) {
            OpenComputers$.MODULE$.log().info("[NETWORK DEBUGGER] Received message: {}.", messageInfo(message));
        }

        private String nodeInfo(final Node target) {
            final var info = new StringBuilder("{address = ").append(target.address())
                .append(", reachability = ").append(target.reachability().name());
            if (target instanceof ComponentConnector componentConnector) {
                info.append(componentInfo(componentConnector)).append(connectorInfo(componentConnector));
            } else if (target instanceof Component component) {
                info.append(componentInfo(component));
            } else if (target instanceof Connector connector) {
                info.append(connectorInfo(connector));
            }
            return info.append('}').toString();
        }

        private String componentInfo(final Component component) {
            return ", type = component, name = " + component.name() + ", visibility = " + component.visibility().name();
        }

        private String connectorInfo(final Connector connector) {
            return ", type = connector, buffer = " + connector.localBuffer() + ", bufferSize = " + connector.localBufferSize();
        }

        private String messageInfo(final Message message) {
            return "{name = " + message.name() + ", source = " + nodeInfo(message.source()) + ", data = [" +
                Arrays.stream(message.data()).map(String::valueOf).collect(Collectors.joining(", ")) + "]}";
        }
    }
}
