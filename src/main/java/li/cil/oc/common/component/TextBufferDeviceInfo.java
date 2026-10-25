package li.cil.oc.common.component;

import li.cil.oc.Constants;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;

import java.util.Map;

/** Device metadata for text buffers. */
final class TextBufferDeviceInfo {
    private TextBufferDeviceInfo() { }

    static Map<String, String> create(int capacity, int depthOrdinal) {
        String[] widths = {"1", "4", "8", "16"};
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Display,
            DeviceAttribute.Description, "Text buffer",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "Text Screen V0",
            DeviceAttribute.Capacity, Integer.toString(capacity),
            DeviceAttribute.Width, widths[depthOrdinal]
        );
    }
}
