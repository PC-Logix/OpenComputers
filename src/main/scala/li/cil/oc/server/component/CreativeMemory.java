package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;

import java.util.Map;

public class CreativeMemory extends AbstractManagedEnvironment implements DeviceInfo {
    private Map<String, String> deviceInfo;

    public CreativeMemory() {
        setNode(Network.newNode(this, Visibility.Neighbors).create());
    }

    @Override
    public synchronized Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Memory,
                DeviceAttribute.Description, "Memory bank",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.ViridiaComputronics(),
                DeviceAttribute.Product, "MagicalMemory 3000",
                DeviceAttribute.Clock, Integer.toString(Integer.MAX_VALUE));
        }
        return deviceInfo;
    }
}
