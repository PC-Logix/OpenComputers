package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;

import java.util.Map;

public class UpgradeBattery extends AbstractManagedEnvironment implements DeviceInfo {
    private final int tier;
    private Map<String, String> deviceInfo;

    public UpgradeBattery(int tier) {
        this.tier = tier;
        setNode(Network.newNode(this, Visibility.Network)
            .withConnector(Settings.get().bufferCapacitorUpgrades()[tier]).create());
    }

    public int tier() { return tier; }

    @Override
    public synchronized Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Power,
                DeviceAttribute.Description, "Battery",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, "Unlimited Power (Almost Ed.)",
                DeviceAttribute.Capacity, Double.toString(Settings.get().bufferCapacitorUpgrades()[tier]));
        }
        return deviceInfo;
    }
}
