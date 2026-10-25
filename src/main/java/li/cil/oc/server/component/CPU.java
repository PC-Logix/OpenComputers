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

public class CPU extends AbstractManagedEnvironment implements DeviceInfo {
    private final int tier;
    private Map<String, String> deviceInfo;

    public CPU(int tier) {
        this.tier = tier;
        setNode(Network.newNode(this, Visibility.Neighbors).create());
    }

    public int tier() { return tier; }

    @Override
    public synchronized Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Processor,
                DeviceAttribute.Description, "CPU",
                DeviceAttribute.Vendor, tier == 3 ? Constants.DeviceInfo$.MODULE$.ViridiaComputronics() : Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, tier == 3 ? "SpeedStar Max Processor" : "FlexiArch " + (tier + 1) + " Processor",
                DeviceAttribute.Clock, Integer.toString((int) (Settings.get().callBudgets()[tier] * 1000)));
        }
        return deviceInfo;
    }
}
