package li.cil.oc.server.network;

import li.cil.oc.Settings;
import li.cil.oc.api.detail.Builder;
import li.cil.oc.api.detail.NetworkAPI;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.util.SideTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import scala.Option;
import java.util.Optional;

/** Server implementation of the network API and its internal factory methods. */
public final class Network {
    public static final NetworkAPI API = new NetworkApi();

    private Network() {}

    public static void joinOrCreateNetwork(BlockGetter world, BlockPos position) {
        NetworkConnectionHelper.joinOrCreateNetwork(world, position);
    }

    public static void joinOrCreateNetwork(BlockEntity tileEntity) {
        NetworkConnectionHelper.joinOrCreateNetwork(tileEntity);
    }

    public static void joinNewNetwork(li.cil.oc.api.network.Node node) {
        if (node instanceof Node mutableNode && mutableNode.network() == null) {
            new NetworkCore(mutableNode);
        }
    }

    public static Option<li.cil.oc.api.network.Node> getNetworkNode(BlockEntity tileEntity, Direction side) {
        return NetworkConnectionHelper.getNetworkNode(tileEntity, side);
    }

    public static Optional<li.cil.oc.api.network.Node> getNetworkNodeJava(BlockEntity tileEntity, Direction side) {
        return NetworkConnectionHelper.getNetworkNodeOptional(tileEntity, side);
    }

    public static void joinWirelessNetwork(WirelessEndpoint endpoint) {
        WirelessNetwork.add(endpoint);
    }

    public static void updateWirelessNetwork(WirelessEndpoint endpoint) {
        WirelessNetwork.update(endpoint);
    }

    public static void leaveWirelessNetwork(WirelessEndpoint endpoint) {
        WirelessNetwork.remove(endpoint);
    }

    public static void leaveWirelessNetwork(WirelessEndpoint endpoint, ResourceKey<Level> dimension) {
        WirelessNetwork.remove(endpoint, dimension);
    }

    public static void sendWirelessPacket(WirelessEndpoint source, double strength, Packet packet) {
        for (WirelessEndpoint endpoint : WirelessNetwork.computeReachableFrom(source, strength)) {
            endpoint.receivePacket(packet, source);
        }
    }

    public static NetworkBuilders.NodeBuilder newNode(Environment host, Visibility reachability) {
        return new NetworkBuilders.NodeBuilder(host, reachability, SideTracker::isServer);
    }

    public static Packet newPacket(String source, String destination, int port, Object[] data) {
        return NetworkPacketFactory.create(source, destination, port, data,
            Settings.get().initialNetworkPacketTTL(), Settings.get().maxNetworkPacketSize());
    }

    public static Packet newPacket(CompoundTag nbt) {
        return NetworkPacketFactory.load(nbt);
    }
}

final class NetworkApi implements NetworkAPI {
    @Override public void joinOrCreateNetwork(BlockEntity tileEntity) {
        Network.joinOrCreateNetwork(tileEntity);
    }

    @Override public void joinOrCreateNetwork(BlockGetter world, BlockPos position) {
        Network.joinOrCreateNetwork(world, position);
    }

    @Override public void joinNewNetwork(li.cil.oc.api.network.Node node) {
        Network.joinNewNetwork(node);
    }

    @Override public void joinWirelessNetwork(WirelessEndpoint endpoint) {
        Network.joinWirelessNetwork(endpoint);
    }

    @Override public void updateWirelessNetwork(WirelessEndpoint endpoint) {
        Network.updateWirelessNetwork(endpoint);
    }

    @Override public void leaveWirelessNetwork(WirelessEndpoint endpoint) {
        Network.leaveWirelessNetwork(endpoint);
    }

    @Override public void leaveWirelessNetwork(WirelessEndpoint endpoint, ResourceKey<Level> dimension) {
        Network.leaveWirelessNetwork(endpoint, dimension);
    }

    @Override public void sendWirelessPacket(WirelessEndpoint source, double strength, Packet packet) {
        Network.sendWirelessPacket(source, strength, packet);
    }

    @Override public Builder.NodeBuilder newNode(Environment host, Visibility reachability) {
        return Network.newNode(host, reachability);
    }

    @Override public Packet newPacket(String source, String destination, int port, Object[] data) {
        return Network.newPacket(source, destination, port, data);
    }

    @Override public Packet newPacket(CompoundTag nbt) {
        return Network.newPacket(nbt);
    }
}
