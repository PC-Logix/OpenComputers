package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;

import java.util.Map;

public class APU extends GraphicsCard {
    private Map<String, String> deviceInfo;

    public APU(int tier) {
        super(tier, scala.Option.empty(), li.cil.oc.api.network.Visibility.Neighbors);
    }

    @Override
    public synchronized Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Processor,
                DeviceAttribute.Description, "APU",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, "FlexiArch " + (tier() + 1) + " Processor (Builtin Graphics)",
                DeviceAttribute.Capacity, capacityInfo(),
                DeviceAttribute.Width, widthInfo(),
                DeviceAttribute.Clock, Integer.toString((int) (Settings.get().callBudgets()[tier()] * 1000)) + "+" + clockInfo());
        }
        return deviceInfo;
    }
}
