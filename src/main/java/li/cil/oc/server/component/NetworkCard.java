package li.cil.oc.server.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.Tier;
import li.cil.oc.server.ActivityEventPacketSender;
import li.cil.oc.server.component.traits.WakeMessageAware;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.collection.immutable.List$;
import scala.jdk.javaapi.CollectionConverters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class NetworkCard extends AbstractManagedEnvironment implements RackBusConnectable, DeviceInfo, WakeMessageAware {
    protected final EnvironmentHost host;
    protected final Visibility visibility;
    protected final Set<Integer> openPorts = new HashSet<>();
    private Optional<String> wakeMessage = Optional.empty();
    private boolean wakeMessageFuzzy;
    private Map<String, String> deviceInfo;

    public NetworkCard(EnvironmentHost host) {
        this(host, false);
    }

    protected NetworkCard(EnvironmentHost host, boolean wireless) {
        this.host = host;
        visibility = host instanceof Rack ? Visibility.Neighbors : Visibility.Network;
        if (wireless) {
            setNode(Network.newNode(this, Visibility.Network).withComponent("modem", Visibility.Neighbors)
                .withConnector().create());
        }
        else setNode(Network.newNode(this, visibility).withComponent("modem", Visibility.Neighbors).create());
    }

    public EnvironmentHost host() { return host; }

    @Override
    public Component node() { return (Component) super.node(); }

    protected int maxOpenPorts() { return Settings.get().maxOpenPorts()[Tier.One]; }

    @Override
    public synchronized Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) deviceInfo = Map.of(
            DeviceAttribute.Class, DeviceClass.Network,
            DeviceAttribute.Description, "Ethernet controller",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "42i520 (MPN-01)",
            DeviceAttribute.Version, "1.0",
            DeviceAttribute.Capacity, Integer.toString(Settings.get().maxNetworkPacketSize()),
            DeviceAttribute.Size, Integer.toString(maxOpenPorts()),
            DeviceAttribute.Width, Integer.toString(Settings.get().maxNetworkPacketParts()));
        return deviceInfo;
    }

    @Callback(doc = "function(port:number):boolean -- Opens the specified port. Returns true if the port was opened.")
    public Object[] open(Context context, Arguments args) throws IOException {
        int port = checkPort(args.checkInteger(0));
        if (openPorts.contains(port)) return ResultWrapper.result(false);
        if (openPorts.size() >= maxOpenPorts()) throw new IOException("too many open ports");
        return ResultWrapper.result(openPorts.add(port));
    }

    @Callback(doc = "function([port:number]):boolean -- Closes the specified port (default: all ports). Returns true if ports were closed.")
    public Object[] close(Context context, Arguments args) {
        if (args.count() == 0) {
            boolean closed = !openPorts.isEmpty();
            openPorts.clear();
            return ResultWrapper.result(closed);
        }
        return ResultWrapper.result(openPorts.remove(checkPort(args.checkInteger(0))));
    }

    @Callback(direct = true, doc = "function(port:number):boolean -- Whether the specified port is open.")
    public Object[] isOpen(Context context, Arguments args) {
        return ResultWrapper.result(openPorts.contains(checkPort(args.checkInteger(0))));
    }

    @Callback(direct = true, doc = "function():boolean -- Whether this card has wireless networking capability.")
    public Object[] isWireless(Context context, Arguments args) { return ResultWrapper.result(false); }

    @Callback(direct = true, doc = "function():boolean -- Whether this card has wired networking capability.")
    public Object[] isWired(Context context, Arguments args) { return ResultWrapper.result(true); }

    @Callback(doc = "function(address:string, port:number, data...) -- Sends the specified data to the specified target.")
    public Object[] send(Context context, Arguments args) throws IOException {
        String address = args.checkString(0);
        int port = checkPort(args.checkInteger(1));
        Packet packet = Network.newPacket(node().address(), address, port, packetData(args, 2));
        doSend(packet);
        networkActivity();
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function(port:number, data...) -- Broadcasts the specified data on the specified port.")
    public Object[] broadcast(Context context, Arguments args) throws IOException {
        int port = checkPort(args.checkInteger(0));
        Packet packet = Network.newPacket(node().address(), null, port, packetData(args, 1));
        doBroadcast(packet);
        networkActivity();
        return ResultWrapper.result(true);
    }

    private Object[] packetData(Arguments args, int skip) {
        List<Object> values = new ArrayList<>();
        int index = 0;
        for (Object value : args) {
            if (index++ >= skip) values.add(value);
        }
        return values.toArray();
    }

    protected void doSend(Packet packet) throws IOException {
        if (visibility == Visibility.Neighbors) node().sendToNeighbors("network.message", packet);
        else if (visibility == Visibility.Network) node().sendToReachable("network.message", packet);
    }

    protected void doBroadcast(Packet packet) throws IOException {
        if (visibility == Visibility.Neighbors) node().sendToNeighbors("network.message", packet);
        else if (visibility == Visibility.Network) node().sendToReachable("network.message", packet);
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == node()) openPorts.clear();
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if (("computer.stopped".equals(message.name()) || "computer.started".equals(message.name())) &&
            node().isNeighborOf(message.source())) openPorts.clear();
        if ("network.message".equals(message.name())) {
            Object[] data = message.data();
            if (data != null && data.length == 1 && data[0] instanceof Packet packet) receivePacket(packet);
        }
    }

    @Override
    public boolean isPacketAccepted(Packet packet, double distance) {
        if (WakeMessageAware.super.isPacketAccepted(packet, distance) && openPorts.contains(packet.port())) {
            networkActivity();
            return true;
        }
        return false;
    }

    @Override
    public void receivePacket(Packet packet) {
        receivePacket(packet, 0, host);
    }

    @Override
    public Optional<String> wakeMessage() { return wakeMessage; }
    @Override
    public void wakeMessage_$eq(Optional<String> value) { wakeMessage = value; }
    @Override
    public boolean wakeMessageFuzzy() { return wakeMessageFuzzy; }
    @Override
    public void wakeMessageFuzzy_$eq(boolean value) { wakeMessageFuzzy = value; }

    @Override
    @Callback(direct = true, doc = "function():string, boolean -- Get the current wake-up message.")
    public Object[] getWakeMessage(Context context, Arguments args) {
        return WakeMessageAware.super.getWakeMessage(context, args);
    }

    @Override
    @Callback(doc = "function(message:string[, fuzzy:boolean]):string -- Set the wake-up message and whether to ignore additional data/parameters.")
    public Object[] setWakeMessage(Context context, Arguments args) {
        return WakeMessageAware.super.setWakeMessage(context, args);
    }

    @Override
    public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        super.loadData(holder);
        if (!openPorts.isEmpty()) throw new AssertionError("assertion failed");
        scala.collection.immutable.List<Integer> saved = holder.get(OCComponentsJava.OPEN_PORTS.get());
        if (saved != null) {
            for (Object port : CollectionConverters.asJava(saved)) openPorts.add(((Number) port).intValue());
        }
        loadWakeMessage(holder);
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        List<Integer> ports = new ArrayList<>(openPorts);
        holder.set(OCComponentsJava.OPEN_PORTS.get(), List$.MODULE$.from(CollectionConverters.asScala(ports)));
        saveWakeMessage(holder);
    }

    protected int checkPort(int port) {
        if (port < 1 || port > 0xFFFF) throw new IllegalArgumentException("invalid port number");
        return port;
    }

    private void networkActivity() {
        if (host != null) ActivityEventPacketSender.sendNetworkActivityUnchecked(node(), host,
                Settings.get().maxNetworkClientEffectPacketDistance());
    }
}
