package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ComponentConnectableRackMountableEnvironment;
import li.cil.oc.common.datacomponents.OCComponents$;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

import java.util.Map;

public class CapacitorMountable extends ComponentConnectableRackMountableEnvironment implements Analyzable {
    private final Rack rack;
    private Map<String, String> deviceInfo;

    public CapacitorMountable(Rack rack) {
        this.rack = rack;
        setNode(Network.newNode(this, Visibility.Network)
            .withComponent("rack_capacitor", Visibility.Network)
            .withConnector(maxCapacity()).create());
    }

    public Rack rack() { return rack; }

    @Override
    public ComponentConnector node() { return (ComponentConnector) super.node(); }

    @Override
    public synchronized Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Power,
                DeviceAttribute.Description, "Battery",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.ViridiaComputronics(),
                DeviceAttribute.Product, "PowerBank X",
                DeviceAttribute.Capacity, Double.toString(maxCapacity()));
        }
        return deviceInfo;
    }

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        return new Node[]{node()};
    }

    @Callback(doc = "function():number; Returns the amount of energy stored in this capacitor.", direct = true)
    public Object[] energy(Context context, Arguments args) {
        return ResultWrapper.result(node().localBuffer());
    }

    @Callback(doc = "function():number; Returns the total amount of energy this capacitor can store.", direct = true)
    public Object[] maxEnergy(Context context, Arguments args) {
        return ResultWrapper.result(node().localBufferSize());
    }

    protected double maxCapacity() {
        return Settings.get().bufferCapacitor() + Settings.get().bufferCapacitorAdjacencyBonus() * 9;
    }

    @Override
    public void describeForClient(MutableDataComponentHolder holder) {
        holder.set(OCComponents$.MODULE$.IS_POWERED().get(), node().localBuffer() > 0);
    }
}
