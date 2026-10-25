package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;

import java.util.Map;

/** Java factories for metadata exposed by server-side components. */
final class ServerComponentDeviceInfo {
    private ServerComponentDeviceInfo() { }

    static Map<String, String> diskDriveMountable() {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Disk,
            DeviceAttribute.Description, "Floppy disk drive",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "RackDrive 100 Rev. 2"
        );
    }

    static Map<String, String> server(String capacity) {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.System,
            DeviceAttribute.Description, "Server",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "Blader",
            DeviceAttribute.Capacity, capacity
        );
    }
}
