package li.cil.oc.server.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import com.google.common.io.Files;
import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.fs.Handle;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.fs.Mode;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.SaveHandler;
import li.cil.oc.server.ActivityEventPacketSender;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FileSystem extends AbstractManagedEnvironment implements DeviceInfo {
    private static final double[] READ_COSTS = {1.0, 1.0 / 4, 1.0 / 7, 1.0 / 10, 1.0 / 13, 1.0 / 15};
    private static final double[] SEEK_COSTS = {1.0, 1.0 / 4, 1.0 / 7, 1.0 / 10, 1.0 / 13, 1.0 / 15};
    private static final double[] WRITE_COSTS = {1.0, 1.0 / 2, 1.0 / 3, 1.0 / 4, 1.0 / 5, 1.0 / 6};

    private final li.cil.oc.api.fs.FileSystem fileSystem;
    private Label label;
    private final Optional<EnvironmentHost> host;
    private final Optional<String> sound;
    private final int speed;
    private final double readEnergyCost;
    private final double writeEnergyCost;
    private final Connector node;
    private final Map<String, Set<Integer>> owners = new HashMap<>();

    public FileSystem(li.cil.oc.api.fs.FileSystem fileSystem, Label label, scala.Option<EnvironmentHost> host,
                      scala.Option<String> sound, int speed, double readEnergyCost, double writeEnergyCost) {
        this(fileSystem, label, toOptional(host), toOptional(sound), speed, readEnergyCost, writeEnergyCost);
    }

    public FileSystem(li.cil.oc.api.fs.FileSystem fileSystem, Label label, Optional<EnvironmentHost> host,
                      Optional<String> sound, int speed, double readEnergyCost, double writeEnergyCost) {
        this.fileSystem = fileSystem;
        this.label = label;
        this.host = host;
        this.sound = sound;
        this.speed = speed;
        this.readEnergyCost = readEnergyCost;
        this.writeEnergyCost = writeEnergyCost;
        this.node = (Connector) Network.newNode(this, Visibility.Network)
                .withComponent("filesystem", Visibility.Neighbors).withConnector().create();
    }

    public li.cil.oc.api.fs.FileSystem fileSystem() { return fileSystem; }
    public Label label() { return label; }
    public void label_$eq(Label value) { label = value; }
    public scala.Option<EnvironmentHost> host() { return toScalaOption(host); }
    public scala.Option<String> sound() { return toScalaOption(sound); }
    public Optional<EnvironmentHost> hostOptional() { return host; }
    public Optional<String> soundOptional() { return sound; }
    public int speed() { return speed; }
    public double readEnergyCost() { return readEnergyCost; }
    public double writeEnergyCost() { return writeEnergyCost; }
    @Override public Connector node() { return node; }
    public double[] readCosts() { return READ_COSTS.clone(); }
    public double[] seekCosts() { return SEEK_COSTS.clone(); }
    public double[] writeCosts() { return WRITE_COSTS.clone(); }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceAttribute.Class, DeviceClass.Volume.toString(),
                DeviceAttribute.Description, "Filesystem",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, "MPFS.21.6",
                DeviceAttribute.Capacity, Integer.toString((int) (fileSystem.spaceTotal() * 1.024)),
                DeviceAttribute.Size, Long.toString(fileSystem.spaceTotal()),
                DeviceAttribute.Clock, (int) (2000 / READ_COSTS[speed]) / 100 + "/" + (int) (2000 / SEEK_COSTS[speed]) / 100 + "/" + (int) (2000 / WRITE_COSTS[speed]) / 100);
    }

    @Callback(direct = true, doc = "function():string -- Get the current label of the drive.")
    public Object[] getLabel(Context context, Arguments args) {
        synchronized (fileSystem) {
            return label != null ? ResultWrapper.result(label.getLabel(ServerLifecycleHooks.getCurrentServer().registryAccess())) : null;
        }
    }

    @Callback(doc = "function(value:string):string -- Sets the label of the drive. Returns the new value, which may be truncated.")
    public Object[] setLabel(Context context, Arguments args) {
        synchronized (fileSystem) {
            if (label == null) throw new IllegalStateException("drive does not support labeling");
            label.setLabel(args.checkAny(0) == null ? null : args.checkString(0));
            return ResultWrapper.result(label.getLabel(ServerLifecycleHooks.getCurrentServer().registryAccess()));
        }
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the file system is read-only.")
    public Object[] isReadOnly(Context context, Arguments args) {
        synchronized (fileSystem) { return ResultWrapper.result(fileSystem.isReadOnly()); }
    }

    @Callback(direct = true, doc = "function():number -- The overall capacity of the file system, in bytes.")
    public Object[] spaceTotal(Context context, Arguments args) {
        synchronized (fileSystem) {
            long space = fileSystem.spaceTotal();
            return ResultWrapper.result(space < 0 ? Double.POSITIVE_INFINITY : space);
        }
    }

    @Callback(direct = true, doc = "function():number -- The currently used capacity of the file system, in bytes.")
    public Object[] spaceUsed(Context context, Arguments args) {
        synchronized (fileSystem) { return ResultWrapper.result(fileSystem.spaceUsed()); }
    }

    @Callback(direct = true, doc = "function(path:string):boolean -- Returns whether an object exists at the specified absolute path in the file system.")
    public Object[] exists(Context context, Arguments args) throws FileNotFoundException {
        synchronized (fileSystem) { diskActivity(); return ResultWrapper.result(fileSystem.exists(clean(args.checkString(0)))); }
    }

    @Callback(direct = true, doc = "function(path:string):number -- Returns the size of the object at the specified absolute path in the file system.")
    public Object[] size(Context context, Arguments args) throws FileNotFoundException {
        synchronized (fileSystem) { diskActivity(); return ResultWrapper.result(fileSystem.size(clean(args.checkString(0)))); }
    }

    @Callback(direct = true, doc = "function(path:string):boolean -- Returns whether the object at the specified absolute path in the file system is a directory.")
    public Object[] isDirectory(Context context, Arguments args) throws FileNotFoundException {
        synchronized (fileSystem) { diskActivity(); return ResultWrapper.result(fileSystem.isDirectory(clean(args.checkString(0)))); }
    }

    @Callback(direct = true, doc = "function(path:string):number -- Returns the (real world) timestamp of when the object at the specified absolute path in the file system was modified.")
    public Object[] lastModified(Context context, Arguments args) throws FileNotFoundException {
        synchronized (fileSystem) { diskActivity(); return ResultWrapper.result(fileSystem.lastModified(clean(args.checkString(0)))); }
    }

    @Callback(doc = "function(path:string):table -- Returns a list of names of objects in the directory at the specified absolute path in the file system.")
    public Object[] list(Context context, Arguments args) throws FileNotFoundException {
        synchronized (fileSystem) {
            String[] names = fileSystem.list(clean(args.checkString(0)));
            if (names == null) return null;
            diskActivity();
            return new Object[]{names};
        }
    }

    @Callback(doc = "function(path:string):boolean -- Creates a directory at the specified absolute path in the file system. Creates parent directories, if necessary.")
    public Object[] makeDirectory(Context context, Arguments args) throws FileNotFoundException {
        synchronized (fileSystem) {
            boolean success = makeDirectoryRecursive(clean(args.checkString(0)));
            diskActivity();
            return ResultWrapper.result(success);
        }
    }

    private boolean makeDirectoryRecursive(String path) {
        if (fileSystem.exists(path)) return false;
        if (fileSystem.makeDirectory(path)) return true;
        int separator = path.lastIndexOf('/');
        String parent = separator < 0 ? "" : path.substring(0, separator);
        return makeDirectoryRecursive(parent) && fileSystem.makeDirectory(path);
    }

    @Callback(doc = "function(path:string):boolean -- Removes the object at the specified absolute path in the file system.")
    public Object[] remove(Context context, Arguments args) throws IOException {
        synchronized (fileSystem) {
            boolean success = removeRecursive(clean(args.checkString(0)));
            diskActivity();
            return ResultWrapper.result(success);
        }
    }

    private boolean removeRecursive(String path) throws IOException {
        if (fileSystem.isDirectory(path)) {
            String[] children = fileSystem.list(path);
            if (children != null) for (String child : children) if (!removeRecursive(path + "/" + child)) return false;
        }
        return fileSystem.delete(path);
    }

    @Callback(doc = "function(from:string, to:string):boolean -- Renames/moves an object from the first specified absolute path in the file system to the second.")
    public Object[] rename(Context context, Arguments args) throws IOException {
        synchronized (fileSystem) {
            boolean success = fileSystem.rename(clean(args.checkString(0)), clean(args.checkString(1)));
            diskActivity();
            return ResultWrapper.result(success);
        }
    }

    @Callback(direct = true, doc = "function(handle:userdata) -- Closes an open file descriptor with the specified handle.")
    public Object[] close(Context context, Arguments args) throws IOException {
        synchronized (fileSystem) { close(context, checkHandle(args, 0)); return null; }
    }

    @Callback(direct = true, limit = 4, doc = "function(path:string[, mode:string='r']):userdata -- Opens a new file descriptor in r, w, a, r+, w+, or a+ mode and returns its handle. A mode may include b.")
    public Object[] open(Context context, Arguments args) throws IOException {
        synchronized (fileSystem) {
            Set<Integer> handles = owners.get(context.node().address());
            if (handles != null && handles.size() >= Settings.get().maxHandles()) throw new IOException("too many open handles");
            int handle = fileSystem.open(clean(args.checkString(0)), parseMode(args.optString(1, "r")));
            if (handle > 0) owners.computeIfAbsent(context.node().address(), ignored -> new HashSet<>()).add(handle);
            diskActivity();
            return ResultWrapper.result(new HandleValue(node.address(), handle));
        }
    }

    @Callback(direct = true, limit = 15, doc = "function(handle:userdata, count:number):string or nil -- Reads up to the specified amount of data from an open file descriptor with the specified handle. Returns nil when EOF is reached.")
    public Object[] read(Context context, Arguments args) throws IOException {
        synchronized (fileSystem) {
            context.consumeCallBudget(READ_COSTS[speed]);
            int handle = checkHandle(args, 0);
            int count = Math.min(Settings.get().maxReadBuffer(), Math.max(0, args.checkInteger(1)));
            checkOwner(context.node().address(), handle);
            Handle file = fileSystem.getHandle(handle);
            if (file == null) throw new IOException("bad file descriptor");
            byte[] buffer = new byte[count];
            int read = file.read(buffer);
            if (read < 0) return ResultWrapper.result();
            byte[] bytes = read == buffer.length ? buffer : Arrays.copyOf(buffer, read);
            if (!node.tryChangeBuffer(-readEnergyCost * bytes.length)) throw new IOException("not enough energy");
            diskActivity();
            return ResultWrapper.result(bytes);
        }
    }

    @Callback(direct = true, doc = "function(handle:userdata, whence:string, offset:number):number -- Seeks in an open file descriptor with the specified handle. Returns the new pointer position.")
    public Object[] seek(Context context, Arguments args) throws IOException {
        synchronized (fileSystem) {
            context.consumeCallBudget(SEEK_COSTS[speed]);
            int handle = checkHandle(args, 0);
            String whence = args.checkString(1);
            int offset = args.checkInteger(2);
            checkOwner(context.node().address(), handle);
            Handle file = fileSystem.getHandle(handle);
            if (file == null) throw new IOException("bad file descriptor");
            long position = switch (whence) {
                case "cur" -> file.seek(file.position() + offset);
                case "set" -> file.seek(offset);
                case "end" -> file.seek(file.length() + offset);
                default -> throw new IllegalArgumentException("invalid mode");
            };
            return ResultWrapper.result(position);
        }
    }

    @Callback(direct = true, doc = "function(handle:userdata, value:string):boolean -- Writes the specified data to an open file descriptor with the specified handle.")
    public Object[] write(Context context, Arguments args) throws IOException {
        synchronized (fileSystem) {
            context.consumeCallBudget(WRITE_COSTS[speed]);
            int handle = checkHandle(args, 0);
            byte[] value = args.checkByteArray(1);
            if (!node.tryChangeBuffer(-writeEnergyCost * value.length)) throw new IOException("not enough energy");
            checkOwner(context.node().address(), handle);
            Handle file = fileSystem.getHandle(handle);
            if (file == null) throw new IOException("bad file descriptor");
            file.write(value);
            diskActivity();
            return ResultWrapper.result(true);
        }
    }

    public int checkHandle(Arguments args, int index) throws IOException {
        if (args.isInteger(index)) return args.checkInteger(index);
        if (args.isTable(index)) {
            Object handle = args.checkTable(index).get("handle");
            if (handle instanceof Number number) return number.intValue();
            throw new IOException("bad file descriptor");
        }
        Object handle = args.checkAny(index);
        if (handle instanceof HandleValue value) return value.handle();
        throw new IOException("bad file descriptor");
    }

    public void close(Context context, int handle) throws IOException {
        Handle file = fileSystem.getHandle(handle);
        Set<Integer> handles = owners.get(context.node().address());
        if (file != null && handles != null && handles.remove(handle)) { file.close(); return; }
        throw new IOException("bad file descriptor");
    }

    private void checkOwner(String owner, int handle) throws IOException {
        Set<Integer> handles = owners.get(owner);
        if (handles == null || !handles.contains(handle)) throw new IOException("bad file descriptor");
    }

    @Override
    public void onMessage(Message message) {
        synchronized (fileSystem) {
            super.onMessage(message);
            if ("computer.stopped".equals(message.name()) || "computer.started".equals(message.name())) {
                Set<Integer> handles = owners.get(message.source().address());
                if (handles != null) {
                    for (int handle : handles) { Handle file = fileSystem.getHandle(handle); if (file != null) file.close(); }
                    handles.clear();
                }
            }
        }
    }

    @Override
    public void onDisconnect(Node node) {
        synchronized (fileSystem) {
            super.onDisconnect(node);
            if (node == this.node) fileSystem.close();
            else {
                Set<Integer> handles = owners.remove(node.address());
                if (handles != null) for (int handle : handles) { Handle file = fileSystem.getHandle(handle); if (file != null) file.close(); }
            }
        }
    }

    @Override
    public void loadData(DataComponentHolder holder) throws li.cil.oc.api.UnrecoverablePersistanceException {
        super.loadData(holder);
        var handles = holder.get(OCComponentsJava.HANDLES.get());
        if (handles != null) {
            for (Object item : scala.jdk.javaapi.CollectionConverters.asJava(handles).entrySet()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) item;
                owners.put((String) entry.getKey(), new HashSet<>(scala.jdk.javaapi.CollectionConverters.asJava((scala.collection.Set<Integer>) entry.getValue())));
            }
        }
        if (label != null) label.loadData(holder);
        CompoundTag saved = holder.get(OCComponentsJava.FILESYSTEM_DATA.get());
        if (saved != null) fileSystem.loadData(saved);
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        if (label != null) label.saveData(holder);
        if (!SaveHandler.savingForClients) {
            scala.collection.immutable.Map<String, scala.collection.immutable.Set<Integer>> savedHandles = scala.collection.immutable.Map$.MODULE$.empty();
            for (var entry : owners.entrySet()) {
                scala.collection.immutable.Set<Integer> savedValues = scala.jdk.javaapi.CollectionConverters.asScala(entry.getValue()).toSet();
                savedHandles = savedHandles.updated(entry.getKey(), savedValues);
            }
            holder.set(OCComponentsJava.HANDLES.get(), savedHandles);
            var dataType = OCComponentsJava.FILESYSTEM_DATA.get();
            holder.update(dataType, new CompoundTag(), tag -> { fileSystem.saveData(tag); return tag; });
        }
    }

    private String clean(String path) throws FileNotFoundException {
        String result = Files.simplifyPath(path);
        if (result.startsWith("../") || result.equals("..")) throw new FileNotFoundException(path);
        return result.equals("/") || result.equals(".") ? "" : result;
    }

    private Mode parseMode(String value) {
        return switch (value) {
            case "r", "rb" -> Mode.Read;
            case "w", "wb" -> Mode.Write;
            case "a", "ab" -> Mode.Append;
            case "r+", "r+b", "rb+" -> Mode.ReadWrite;
            case "w+", "w+b", "wb+" -> Mode.ReadWriteTruncate;
            case "a+", "a+b", "ab+" -> Mode.ReadAppend;
            default -> throw new IllegalArgumentException("unsupported mode");
        };
    }

    private void diskActivity() {
        if (sound.isPresent() && host.isPresent()) {
            ActivityEventPacketSender.sendFileSystemActivityUnchecked(node, host.get(), sound.get(),
                    Settings.get().diskActivitySoundDelay(), Settings.get().threads(), Settings.get().maxNetworkClientSoundPacketDistance());
        }
    }

    private static <T> Optional<T> toOptional(scala.Option<T> value) {
        return value.isDefined() ? Optional.ofNullable(value.get()) : Optional.empty();
    }

    private static <T> scala.Option<T> toScalaOption(Optional<T> value) {
        return value.isPresent() ? scala.Option$.MODULE$.apply(value.get()) : scala.Option$.MODULE$.empty();
    }
}
