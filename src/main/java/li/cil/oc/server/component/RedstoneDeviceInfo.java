package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;

import java.util.Map;

/** Device metadata shared by the redstone component implementations. */
final class RedstoneDeviceInfo {
    private RedstoneDeviceInfo() { }

    static Map<String, String> vanilla() {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Communication,
            DeviceAttribute.Description, "Redstone controller",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "Rs100-V",
            DeviceAttribute.Capacity, "16",
            DeviceAttribute.Width, "1"
        );
    }

    static Map<String, String> bundledWireless() {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Communication,
            DeviceAttribute.Description, "Combined redstone controller",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "Rx900-M",
            DeviceAttribute.Capacity, "65536",
            DeviceAttribute.Width, "16"
        );
    }

    static Map<String, String> bundled() {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Communication,
            DeviceAttribute.Description, "Advanced redstone controller",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "Rb800-M",
            DeviceAttribute.Capacity, "65536",
            DeviceAttribute.Width, "16"
        );
    }

    static Map<String, String> wireless() {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Communication,
            DeviceAttribute.Description, "Wireless redstone controller",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "Rw400-M",
            DeviceAttribute.Capacity, "1",
            DeviceAttribute.Width, "1"
        );
    }
}
