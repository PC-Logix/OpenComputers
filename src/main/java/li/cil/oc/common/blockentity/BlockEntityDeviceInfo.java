package li.cil.oc.common.blockentity;

import li.cil.oc.Constants;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;

import java.util.Map;

/** Java factories for the static metadata exposed by common block entities. */
final class BlockEntityDeviceInfo {
    private BlockEntityDeviceInfo() { }

    static Map<String, String> adapter() {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Bus,
            DeviceAttribute.Description, "Adapter",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "Multiplug Ext.1"
        );
    }

    static Map<String, String> assembler() {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Generic,
            DeviceAttribute.Description, "Assembler",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "Factorizer R1D1"
        );
    }

    static Map<String, String> capacitor(String capacity) {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Power,
            DeviceAttribute.Description, "Battery",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "CapBank3x",
            DeviceAttribute.Capacity, capacity
        );
    }

    static Map<String, String> computerCase(String capacity) {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.System,
            DeviceAttribute.Description, "Computer",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "Blocker",
            DeviceAttribute.Capacity, capacity
        );
    }

    static Map<String, String> charger() {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Generic,
            DeviceAttribute.Description, "Charger",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "PowerUpper"
        );
    }

    static Map<String, String> disassembler() {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Generic,
            DeviceAttribute.Description, "Disassembler",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "Break.3R-100"
        );
    }

    static Map<String, String> diskDrive() {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Disk,
            DeviceAttribute.Description, "Floppy disk drive",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "Spinner 520p1"
        );
    }

    static Map<String, String> printer() {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Printer,
            DeviceAttribute.Description, "3D Printer",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "Omni-Materializer T6.1"
        );
    }

    static Map<String, String> powerConverter(String capacity) {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Power,
            DeviceAttribute.Description, "Power converter",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "Transgizer-PX5",
            DeviceAttribute.Capacity, capacity
        );
    }

    static Map<String, String> netSplitter() {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Network,
            DeviceAttribute.Description, "Ethernet controller",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "NetSplits",
            DeviceAttribute.Version, "1.0",
            DeviceAttribute.Width, "6"
        );
    }

    static Map<String, String> projector(int width, int height) {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Display,
            DeviceAttribute.Description, "Framebuffer projector",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "Projector P1",
            DeviceAttribute.Capacity, Integer.toString(width * height),
            DeviceAttribute.Width, Integer.toString(width)
        );
    }

    static Map<String, String> microcontroller(String capacity) {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.System,
            DeviceAttribute.Description, "Microcontroller",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "Cubicle",
            DeviceAttribute.Capacity, capacity
        );
    }

    static Map<String, String> hologram(int tier, int width, int height, int colorCount) {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Display,
            DeviceAttribute.Description, "Holographic projector",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "VirtualViewer H1-" + tier,
            DeviceAttribute.Capacity, Integer.toString(width * width * height),
            DeviceAttribute.Width, Integer.toString(colorCount)
        );
    }
}
