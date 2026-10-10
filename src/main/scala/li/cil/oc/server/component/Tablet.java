package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.item.TabletWrapper;
import li.cil.oc.util.ResultWrapper;

import java.util.Map;

public class Tablet extends AbstractManagedEnvironment implements DeviceInfo {
    private final TabletWrapper tablet;
    private Map<String, String> deviceInfo;

    public Tablet(TabletWrapper tablet) {
        this.tablet = tablet;
        setNode(Network.newNode(this, Visibility.Network)
            .withComponent("tablet")
            .withConnector(Settings.get().bufferTablet())
            .create());
    }

    public TabletWrapper tablet() { return tablet; }

    @Override
    public ComponentConnector node() { return (ComponentConnector) super.node(); }

    @Override
    public synchronized Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.System,
                DeviceAttribute.Description, "Tablet",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, "Jogger",
                DeviceAttribute.Capacity, Integer.toString(tablet.getContainerSize()));
        }
        return deviceInfo;
    }

    @Callback(doc = "function():number -- Gets the pitch of the player holding the tablet.")
    public Object[] getPitch(Context context, Arguments args) {
        return ResultWrapper.result(tablet.player().getXRot());
    }

    @Callback(doc = "function():number -- Gets the yaw of the player holding the tablet.")
    public Object[] getYaw(Context context, Arguments args) {
        return ResultWrapper.result(tablet.player().getYRot());
    }
}
