package li.cil.oc.server.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.Tier;
import li.cil.oc.server.component.traits.WakeMessageAware;
import li.cil.oc.server.network.QuantumNetwork;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.jdk.javaapi.CollectionConverters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LinkedCard extends AbstractManagedEnvironment implements QuantumNetwork.QuantumNode, DeviceInfo, WakeMessageAware {
    public String tunnel = "creative";
    private Optional<String> wakeMessage = Optional.empty();
    private boolean wakeMessageFuzzy;
    private Map<String, String> deviceInfo;

    public LinkedCard() {
        setNode(Network.newNode(this, Visibility.Network).withComponent("tunnel", Visibility.Neighbors).withConnector().create());
    }

    @Override
    public ComponentConnector node() { return (ComponentConnector) super.node(); }
    @Override
    public String tunnel() { return tunnel; }
    public void tunnel_$eq(String value) { tunnel = value; }

    @Override
    public synchronized Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) deviceInfo = Map.of(
            DeviceAttribute.Class, DeviceClass.Network,
            DeviceAttribute.Description, "Quantumnet controller",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "HyperLink IV: Ender Edition",
            DeviceAttribute.Capacity, Integer.toString(Settings.get().maxNetworkPacketSize()),
            DeviceAttribute.Width, Integer.toString(Settings.get().maxNetworkPacketParts()));
        return deviceInfo;
    }

    @Callback(doc = "function(data...) -- Sends the specified data to the card this one is linked to.")
    public Object[] send(Context context, Arguments args) {
        List<QuantumNetwork.QuantumNode> endpoints = new ArrayList<>();
        for (QuantumNetwork.QuantumNode endpoint : CollectionConverters.asJava(QuantumNetwork.getEndpoints(tunnel))) {
            if (endpoint != this) endpoints.add(endpoint);
        }
        // Iterating Arguments keeps binary payloads intact, unlike its legacy array conversion.
        List<Object> values = new ArrayList<>();
        for (Object value : args) values.add(value);
        Packet packet = Network.newPacket(node().address(), null, 0, values.toArray());
        double cost = packet.size() / 32.0 + Settings.get().wirelessCostPerRange()[Tier.Two] *
            Settings.get().maxWirelessRange()[Tier.Two] * 5;
        if (!node().tryChangeBuffer(-cost)) return ResultWrapper.result(null, "not enough energy");
        for (QuantumNetwork.QuantumNode endpoint : endpoints) endpoint.receivePacket(packet);
        return ResultWrapper.result(true);
    }

    @Callback(direct = true, doc = "function():number -- Gets the maximum packet size (config setting).")
    public Object[] maxPacketSize(Context context, Arguments args) { return ResultWrapper.result(Settings.get().maxNetworkPacketSize()); }

    @Override
    public void receivePacket(Packet packet) { receivePacket(packet, 0, null); }

    @Callback(direct = true, doc = "function():string -- Gets this link card's shared channel address")
    public Object[] getChannel(Context context, Arguments args) { return ResultWrapper.result(tunnel); }

    @Override public Optional<String> wakeMessage() { return wakeMessage; }
    @Override public void wakeMessage_$eq(Optional<String> value) { wakeMessage = value; }
    @Override public boolean wakeMessageFuzzy() { return wakeMessageFuzzy; }
    @Override public void wakeMessageFuzzy_$eq(boolean value) { wakeMessageFuzzy = value; }

    @Override
    @Callback(direct = true, doc = "function():string, boolean -- Get the current wake-up message.")
    public Object[] getWakeMessage(Context context, Arguments args) { return WakeMessageAware.super.getWakeMessage(context, args); }

    @Override
    @Callback(doc = "function(message:string[, fuzzy:boolean]):string -- Set the wake-up message and whether to ignore additional data/parameters.")
    public Object[] setWakeMessage(Context context, Arguments args) { return WakeMessageAware.super.setWakeMessage(context, args); }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == node()) QuantumNetwork.add(this);
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == node()) QuantumNetwork.remove(this);
    }

    @Override
    public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        super.loadData(holder);
        String saved = holder.get(OCComponentsJava.TUNNEL.get());
        if (saved != null) tunnel = saved;
        loadWakeMessage(holder);
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        holder.set(OCComponentsJava.TUNNEL.get(), tunnel);
        saveWakeMessage(holder);
    }
}
