package li.cil.oc.server.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.common.Tier;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.ResultWrapper;
import li.cil.oc.util.SableCompat;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

import java.io.IOException;
import java.util.Map;

public abstract class WirelessNetworkCard extends NetworkCard implements WirelessEndpoint {
    public double strength;

    protected WirelessNetworkCard(EnvironmentHost host) {
        super(host, true);
        strength = maxWirelessRange();
    }

    protected abstract double wirelessCostPerRange();
    protected abstract double maxWirelessRange();
    protected abstract boolean shouldSendWiredTraffic();

    public BlockPosition position() { return BlockPositionsJava.apply(host); }
    public double strength() { return strength; }
    public void strength_$eq(double value) { strength = value; }

    @Override
    public int x() { return position().x(); }
    @Override
    public int y() { return position().y(); }
    @Override
    public int z() { return position().z(); }
    @Override
    public Level getWirelessLevel() { return host.getEnvironmentLevel(); }

    @Override
    public void receivePacket(Packet packet, WirelessEndpoint source) {
        Vec3 sourcePosition = SableCompat.physicalPosition(getWirelessLevel(), new Vec3(source.x() + 0.5, source.y() + 0.5, source.z() + 0.5));
        Vec3 receiverPosition = SableCompat.physicalPosition(getWirelessLevel(), new Vec3(host.xPosition(), host.yPosition(), host.zPosition()));
        double distance = Math.sqrt(SableCompat.distanceSquared(getWirelessLevel(), sourcePosition, receiverPosition));
        receivePacket(packet, distance, host);
    }

    @Callback(direct = true, doc = "function():number -- Get the signal strength (range) used when sending messages.")
    public Object[] getStrength(Context context, Arguments args) { return ResultWrapper.result(strength); }

    @Callback(doc = "function(strength:number):number -- Set the signal strength (range) used when sending messages.")
    public Object[] setStrength(Context context, Arguments args) {
        strength = Math.max(0, Math.min(args.checkDouble(0), maxWirelessRange()));
        return ResultWrapper.result(strength);
    }

    @Override
    public Object[] isWireless(Context context, Arguments args) { return ResultWrapper.result(true); }
    @Override
    public Object[] isWired(Context context, Arguments args) { return ResultWrapper.result(shouldSendWiredTraffic()); }

    @Override
    protected void doSend(Packet packet) throws IOException {
        if (strength > 0) {
            checkPower();
            Network.sendWirelessPacket(this, strength, packet);
        }
        if (shouldSendWiredTraffic()) super.doSend(packet);
    }

    @Override
    protected void doBroadcast(Packet packet) throws IOException {
        if (strength > 0) {
            checkPower();
            Network.sendWirelessPacket(this, strength, packet);
        }
        if (shouldSendWiredTraffic()) super.doBroadcast(packet);
    }

    private void checkPower() throws IOException {
        double cost = wirelessCostPerRange();
        if (cost > 0 && !Settings.get().ignorePower() && !((Connector) node()).tryChangeBuffer(-strength * cost)) {
            throw new IOException("not enough energy");
        }
    }

    @Override
    public boolean canUpdate() { return true; }

    @Override
    public void update() {
        super.update();
        if (getWirelessLevel().getGameTime() % 20 == 0) Network.updateWirelessNetwork(this);
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == node()) Network.joinWirelessNetwork(this);
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == node() || !getWirelessLevel().isLoaded(position().toBlockPos())) Network.leaveWirelessNetwork(this);
    }

    @Override
    public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        super.loadData(holder);
        Object saved = holder.get(OCComponentsJava.STRENGTH.get());
        if (saved != null) strength = ((Number) saved).doubleValue();
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        holder.set(OCComponentsJava.STRENGTH.get(), strength);
    }

    public static class Tier1 extends WirelessNetworkCard {
        private Map<String, String> deviceInfo;
        public Tier1(EnvironmentHost host) { super(host); }
        @Override
        protected double wirelessCostPerRange() { return Settings.get().wirelessCostPerRange()[Tier.One]; }
        @Override
        protected double maxWirelessRange() { return Settings.get().maxWirelessRange()[Tier.One]; }
        @Override
        protected int maxOpenPorts() { return Settings.get().maxOpenPorts()[Tier.One + 1]; }
        @Override
        protected boolean shouldSendWiredTraffic() { return false; }

        @Override
        public Map<String, String> getDeviceInfo() { return wirelessDeviceInfo("39i110 (LPPW-01)", "1.0"); }

        protected synchronized Map<String, String> wirelessDeviceInfo(String product, String version) {
            if (deviceInfo == null) deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Network,
                DeviceAttribute.Description, "Wireless ethernet controller",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, product,
                DeviceAttribute.Version, version,
                DeviceAttribute.Capacity, Integer.toString(Settings.get().maxNetworkPacketSize()),
                DeviceAttribute.Size, Integer.toString(maxOpenPorts()),
                DeviceAttribute.Width, Double.toString(maxWirelessRange()));
            return deviceInfo;
        }

        @Override
        public boolean isPacketAccepted(Packet packet, double distance) {
            return distance <= maxWirelessRange() && (distance > 0 || shouldSendWiredTraffic()) &&
                super.isPacketAccepted(packet, distance);
        }
    }

    public static class Tier2 extends Tier1 {
        public Tier2(EnvironmentHost host) { super(host); }
        @Override
        protected double wirelessCostPerRange() { return Settings.get().wirelessCostPerRange()[Tier.Two]; }
        @Override
        protected double maxWirelessRange() { return Settings.get().maxWirelessRange()[Tier.Two]; }
        @Override
        protected int maxOpenPorts() { return Settings.get().maxOpenPorts()[Tier.Two + 1]; }
        @Override
        protected boolean shouldSendWiredTraffic() { return true; }
        @Override
        public Map<String, String> getDeviceInfo() { return wirelessDeviceInfo("62i230 (MPW-01)", "2.0"); }
    }
}
