package li.cil.oc.server.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import com.google.common.hash.Hashing;
import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

import java.nio.ByteBuffer;
import java.util.Map;

public class EEPROM extends AbstractManagedEnvironment implements DeviceInfo {
    private final Connector node;
    private byte[] codeData = new byte[0];
    private byte[] volatileData = new byte[0];
    private boolean readonly;
    private String label = "EEPROM";
    private Map<String, String> deviceInfo;

    public EEPROM() {
        node = (Connector) Network.newNode(this, Visibility.Neighbors)
                .withComponent("eeprom", Visibility.Neighbors).withConnector().create();
    }

    @Override public Connector node() { return node; }
    public byte[] codeData() { return codeData; }
    public void codeData_$eq(byte[] value) { codeData = value; }
    public byte[] volatileData() { return volatileData; }
    public void volatileData_$eq(byte[] value) { volatileData = value; }
    public boolean readonly() { return readonly; }
    public void readonly_$eq(boolean value) { readonly = value; }
    public String label() { return label; }
    public void label_$eq(String value) { label = value; }
    public String checksum() { return Hashing.crc32().hashBytes(codeData).toString(); }

    @Override
    public synchronized Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            String capacity = Integer.toString(Settings.get().eepromSize());
            deviceInfo = Map.of(
                    DeviceAttribute.Class, DeviceClass.Memory.toString(),
                    DeviceAttribute.Description, "EEPROM",
                    DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                    DeviceAttribute.Product, "FlashStick2k",
                    DeviceAttribute.Capacity, capacity,
                    DeviceAttribute.Size, capacity);
        }
        return deviceInfo;
    }

    @Callback(direct = true, doc = "function():string -- Get the currently stored byte array.")
    public Object[] get(Context context, Arguments args) { return ResultWrapper.result(codeData); }

    @Callback(doc = "function(data:string) -- Overwrite the currently stored byte array.")
    public Object[] set(Context context, Arguments args) {
        if (readonly) return ResultWrapper.result(null, "storage is readonly");
        if (!node.tryChangeBuffer(-Settings.get().eepromWriteCost())) return ResultWrapper.result(null, "not enough energy");
        byte[] newData = args.optByteArray(0, new byte[0]);
        if (newData.length > Settings.get().eepromSize()) throw new IllegalArgumentException("not enough space");
        codeData = newData;
        context.pause(2);
        return null;
    }

    @Callback(direct = true, doc = "function():string -- Get the label of the EEPROM.")
    public Object[] getLabel(Context context, Arguments args) { return ResultWrapper.result(label); }

    @Callback(doc = "function(data:string):string -- Set the label of the EEPROM.")
    public Object[] setLabel(Context context, Arguments args) {
        if (readonly) return ResultWrapper.result(null, "storage is readonly");
        label = args.optString(0, "EEPROM").trim();
        if (label.length() > 24) label = label.substring(0, 24);
        if (label.isEmpty()) label = "EEPROM";
        return ResultWrapper.result(label);
    }

    @Callback(direct = true, doc = "function():number -- Get the storage capacity of this EEPROM.")
    public Object[] getSize(Context context, Arguments args) { return ResultWrapper.result(Settings.get().eepromSize()); }

    @Callback(direct = true, doc = "function():string -- Get the checksum of the data on this EEPROM.")
    public Object[] getChecksum(Context context, Arguments args) { return ResultWrapper.result(checksum()); }

    @Callback(direct = true, doc = "function(checksum:string):boolean -- Make this EEPROM readonly if it isn't already. This process cannot be reversed!")
    public Object[] makeReadonly(Context context, Arguments args) {
        if (args.checkString(0).equals(checksum())) {
            readonly = true;
            return ResultWrapper.result(true);
        }
        return ResultWrapper.result(null, "incorrect checksum");
    }

    @Callback(direct = true, doc = "function():number -- Get the storage capacity of this EEPROM.")
    public Object[] getDataSize(Context context, Arguments args) { return ResultWrapper.result(Settings.get().eepromDataSize()); }

    @Callback(direct = true, doc = "function():string -- Get the currently stored byte array.")
    public Object[] getData(Context context, Arguments args) { return ResultWrapper.result(volatileData); }

    @Callback(doc = "function(data:string) -- Overwrite the currently stored byte array.")
    public Object[] setData(Context context, Arguments args) {
        if (!node.tryChangeBuffer(-Settings.get().eepromWriteCost())) return ResultWrapper.result(null, "not enough energy");
        byte[] newData = args.optByteArray(0, new byte[0]);
        if (newData.length > Settings.get().eepromDataSize()) throw new IllegalArgumentException("not enough space");
        volatileData = newData;
        context.pause(1);
        return null;
    }

    private byte[] asArray(ByteBuffer buffer) {
        if (buffer.hasArray()) return buffer.array();
        byte[] array = new byte[buffer.remaining()];
        buffer.get(array);
        return array;
    }

    @Override
    public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        super.loadData(holder);
        ByteBuffer code = holder.get(OCComponentsJava.EEPROM_CODE.get());
        ByteBuffer data = holder.get(OCComponentsJava.EEPROM_DATA.get());
        String savedLabel = holder.get(OCComponentsJava.LABEL.get());
        Object savedReadonly = holder.get((DataComponentType) OCComponentsJava.READONLY.get());
        if (code != null) codeData = asArray(code);
        if (data != null) volatileData = asArray(data);
        if (savedLabel != null) label = savedLabel;
        readonly = Boolean.TRUE.equals(savedReadonly);
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        holder.set(OCComponentsJava.EEPROM_CODE.get(), ByteBuffer.wrap(codeData));
        holder.set(OCComponentsJava.EEPROM_DATA.get(), ByteBuffer.wrap(volatileData));
        holder.set(OCComponentsJava.LABEL.get(), label);
        if (readonly) holder.set(OCComponentsJava.READONLY.get(), true);
        else holder.remove(OCComponentsJava.READONLY.get());
    }
}
