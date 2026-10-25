package li.cil.oc.server.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.server.ActivityEventPacketSender;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import scala.Option;
import scala.Option$;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Drive extends AbstractManagedEnvironment implements DeviceInfo {
    private final int capacity;
    private final int platterCount;
    private final Label label;
    private final Optional<EnvironmentHost> host;
    private final Optional<String> sound;
    private final int speed;
    private final boolean isLocked;
    private final boolean isSolidState;
    private static final int SECTOR_SIZE = 512;
    private final byte[] data;
    private final int sectorCount;
    private final int sectorsPerPlatter;
    private final int speedIndex;
    private int headPos;
    private Map<String, String> deviceInfo;

    public final double[] readSectorCosts = {1.0 / 10, 1.0 / 20, 1.0 / 30, 1.0 / 40, 1.0 / 50, 1.0 / 60};
    public final double[] writeSectorCosts = {1.0 / 5, 1.0 / 10, 1.0 / 15, 1.0 / 20, 1.0 / 25, 1.0 / 30};
    public final double[] readByteCosts = {1.0 / 48, 1.0 / 64, 1.0 / 80, 1.0 / 96, 1.0 / 112, 1.0 / 128};
    public final double[] writeByteCosts = {1.0 / 24, 1.0 / 32, 1.0 / 40, 1.0 / 48, 1.0 / 56, 1.0 / 64};

    public Drive(int capacity, int platterCount, Label label, Option<EnvironmentHost> host, Option<String> sound,
                 int speed, boolean isLocked) {
        this(capacity, platterCount, label, toOptional(host), toOptional(sound), speed, isLocked, false);
    }

    public Drive(int capacity, int platterCount, Label label, Option<EnvironmentHost> host, Option<String> sound,
                 int speed, boolean isLocked, boolean isSolidState) {
        this(capacity, platterCount, label, toOptional(host), toOptional(sound), speed, isLocked, isSolidState);
    }

    public Drive(int capacity, int platterCount, Label label, Optional<EnvironmentHost> host, Optional<String> sound,
                 int speed, boolean isLocked) {
        this(capacity, platterCount, label, host, sound, speed, isLocked, false);
    }

    public Drive(int capacity, int platterCount, Label label, Optional<EnvironmentHost> host, Optional<String> sound,
                 int speed, boolean isLocked, boolean isSolidState) {
        this.capacity = capacity;
        this.platterCount = platterCount;
        this.label = label;
        this.host = host;
        this.sound = sound;
        this.speed = speed;
        this.isLocked = isLocked;
        this.isSolidState = isSolidState;
        setNode(Network.newNode(this, Visibility.Network).withComponent("drive", Visibility.Neighbors)
            .withConnector().create());
        data = new byte[capacity];
        sectorCount = capacity / SECTOR_SIZE;
        sectorsPerPlatter = sectorCount / platterCount;
        speedIndex = Math.max(0, Math.min(5, speed));
    }

    public int capacity() { return capacity; }
    public int platterCount() { return platterCount; }
    public Label label() { return label; }
    public Option<String> sound() { return toScalaOption(sound); }
    public Optional<String> soundOptional() { return sound; }
    public int speed() { return speed; }
    public boolean isLocked() { return isLocked; }
    public boolean isSolidState() { return isSolidState; }
    public double[] readSectorCosts() { return readSectorCosts; }
    public double[] writeSectorCosts() { return writeSectorCosts; }
    public double[] readByteCosts() { return readByteCosts; }
    public double[] writeByteCosts() { return writeByteCosts; }

    private File savePath() {
        return ServerLifecycleHooks.getCurrentServer()
            .getWorldPath(new LevelResource(Settings.savePath() + node().address() + ".bin")).toFile();
    }

    @Override
    public synchronized Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Disk,
                DeviceAttribute.Description, isSolidState ? "Solid state drive" : "Hard disk drive",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, (isSolidState ? "MPS" : "MPD") + capacity / 1024 + "L" + platterCount,
                DeviceAttribute.Capacity, Integer.toString((int) (capacity * 1.024)),
                DeviceAttribute.Size, Integer.toString(capacity),
                DeviceAttribute.Clock, ((int) (2000 / readSectorCosts[speedIndex]) / 100) + "/" +
                    ((int) (2000 / writeSectorCosts[speedIndex]) / 100) + "/" +
                    ((int) (2000 / readByteCosts[speedIndex]) / 100) + "/" +
                    ((int) (2000 / writeByteCosts[speedIndex]) / 100));
        }
        return deviceInfo;
    }

    @Callback(direct = true, doc = "function():string -- Get the current label of the drive.")
    public synchronized Object[] getLabel(Context context, Arguments args) {
        return label == null ? null : ResultWrapper.result(label.getLabel(ServerLifecycleHooks.getCurrentServer().registryAccess()));
    }

    @Callback(doc = "function(value:string):string -- Sets the label of the drive. Returns the new value, which may be truncated.")
    public synchronized Object[] setLabel(Context context, Arguments args) throws Exception {
        if (isLocked) throw new Exception("drive is read only");
        if (label == null) throw new Exception("drive does not support labeling");
        label.setLabel(args.checkAny(0) == null ? null : args.checkString(0));
        return ResultWrapper.result(label.getLabel(ServerLifecycleHooks.getCurrentServer().registryAccess()));
    }

    @Callback(direct = true, doc = "function():number -- Returns the total capacity of the drive, in bytes.")
    public Object[] getCapacity(Context context, Arguments args) { return ResultWrapper.result(capacity); }

    @Callback(direct = true, doc = "function():number -- Returns the size of a single sector on the drive, in bytes.")
    public Object[] getSectorSize(Context context, Arguments args) { return ResultWrapper.result(SECTOR_SIZE); }

    @Callback(direct = true, doc = "function():number -- Returns the number of platters in the drive.")
    public Object[] getPlatterCount(Context context, Arguments args) { return ResultWrapper.result(platterCount); }

    @Callback(direct = true, doc = "function(sector:number):string -- Read the current contents of the specified sector.")
    public synchronized Object[] readSector(Context context, Arguments args) throws Exception {
        context.consumeCallBudget(readSectorCosts[speedIndex]);
        int sector = moveToSector(context, checkSector(args, 0));
        diskActivity();
        byte[] sectorData = new byte[SECTOR_SIZE];
        System.arraycopy(data, sector * SECTOR_SIZE, sectorData, 0, SECTOR_SIZE);
        return ResultWrapper.result(sectorData);
    }

    @Callback(direct = true, doc = "function(sector:number, value:string) -- Write the specified contents to the specified sector.")
    public synchronized Object[] writeSector(Context context, Arguments args) throws Exception {
        if (isLocked) throw new Exception("drive is read only");
        context.consumeCallBudget(writeSectorCosts[speedIndex]);
        byte[] sectorData = args.checkByteArray(1);
        int sector = moveToSector(context, checkSector(args, 0));
        diskActivity();
        System.arraycopy(sectorData, 0, data, sector * SECTOR_SIZE, Math.min(SECTOR_SIZE, sectorData.length));
        return null;
    }

    @Callback(direct = true, doc = "function(offset:number):number -- Read a single byte at the specified offset.")
    public synchronized Object[] readByte(Context context, Arguments args) throws Exception {
        context.consumeCallBudget(readByteCosts[speedIndex]);
        int offset = args.checkInteger(0) - 1;
        moveToSector(context, checkSector(offset));
        diskActivity();
        return ResultWrapper.result(data[offset]);
    }

    @Callback(direct = true, doc = "function(offset:number, value:number) -- Write a single byte to the specified offset.")
    public synchronized Object[] writeByte(Context context, Arguments args) throws Exception {
        if (isLocked) throw new Exception("drive is read only");
        context.consumeCallBudget(writeByteCosts[speedIndex]);
        int offset = args.checkInteger(0) - 1;
        byte value = (byte) args.checkInteger(1);
        moveToSector(context, checkSector(offset));
        diskActivity();
        data[offset] = value;
        return null;
    }

    @Override
    public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        super.loadData(holder);
        loadDriveContents();
        Object head = holder.get(OCComponentsJava.HEAD_POS.get());
        if (head != null) headPos = ((Number) head).intValue();
        if (label != null) label.loadData(holder);
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        saveDriveContents();
        holder.set(OCComponentsJava.HEAD_POS.get(), headPos);
        if (label != null) label.saveData(holder);
    }

    public void loadDriveContents() {
        if (node().address() != null) {
            try {
                File path = savePath();
                if (path.exists()) {
                    try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(Files.readAllBytes(path.toPath())))) {
                        int offset = 0;
                        int read = 0;
                        while (read >= 0 && offset < data.length) {
                            read = input.read(data, offset, data.length - offset);
                            offset += read;
                        }
                    }
                }
            }
            catch (Throwable error) {
                OpenComputers.log().warn("Failed loading drive contents for '{}'.", node().address(), error);
            }
        }
    }

    public void saveDriveContents() {
        if (node().address() != null) {
            try {
                File path = savePath();
                path.getParentFile().mkdirs();
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                try (GZIPOutputStream compressed = new GZIPOutputStream(output)) { compressed.write(data); }
                Files.write(path.toPath(), output.toByteArray());
            }
            catch (Throwable error) {
                OpenComputers.log().warn("Failed saving drive contents for '{}'.", node().address(), error);
            }
        }
    }

    private int validateSector(int sector) {
        if (sector < 0 || sector >= sectorCount) throw new IllegalArgumentException("invalid offset, not in a usable sector");
        return sector;
    }

    private int checkSector(int offset) { return validateSector(offset / SECTOR_SIZE); }
    private int checkSector(Arguments args, int index) { return validateSector(args.checkInteger(index) - 1); }

    private int moveToSector(Context context, int sector) {
        if (!isSolidState) {
            int next = sector % sectorsPerPlatter;
            if (headPos != next) {
                int delta = Math.abs(headPos - next);
                if (delta > Settings.get().sectorSeekThreshold()) context.pause(Settings.get().sectorSeekTime());
                headPos = next;
            }
        }
        return sector;
    }

    private void diskActivity() {
        if (sound.isPresent() && host.isPresent()) ActivityEventPacketSender.sendFileSystemActivityUnchecked(node(), host.get(), sound.get(),
                Settings.get().diskActivitySoundDelay(), Settings.get().threads(), Settings.get().maxNetworkClientSoundPacketDistance());
    }

    private static <T> Optional<T> toOptional(Option<T> value) {
        return value.isDefined() ? Optional.ofNullable(value.get()) : Optional.empty();
    }

    private static <T> Option<T> toScalaOption(Optional<T> value) {
        return value.isPresent() ? Option$.MODULE$.apply(value.get()) : Option$.MODULE$.empty();
    }
}
