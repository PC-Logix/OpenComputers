package li.cil.oc.server.machine;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.item.CallBudget;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.machine.*;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.client.ClientUtil;
import li.cil.oc.common.EventHandler;
import li.cil.oc.common.SaveHandler;
import li.cil.oc.common.Slot;
import li.cil.oc.common.datacomponents.MachineData;
import li.cil.oc.server.SoundEffectPacketSender;
import li.cil.oc.server.GeneralPacketSender;
import li.cil.oc.server.driver.Registry;
import li.cil.oc.server.fs.FileSystem;
import li.cil.oc.util.ExtendedDataComponentHolder;
import li.cil.oc.util.ExtendedMutableDataComponentHolder;
import li.cil.oc.util.ThreadPoolFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import scala.jdk.javaapi.CollectionConverters;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class Machine extends AbstractManagedEnvironment
    implements li.cil.oc.api.machine.Machine, Runnable, DeviceInfo {
    private static final ScheduledExecutorService THREAD_POOL =
        ThreadPoolFactory.create("Computer", Settings.get().threads());

    private final MachineHost host;
    private final ComponentConnector node;
    private final li.cil.oc.server.component.FileSystem tmp;
    public Architecture architecture;
    public final Deque<MachineState> state = new ArrayDeque<>();
    private final Map<String, String> components = new HashMap<>();
    private final Set<Component> addedComponents = new HashSet<>();
    private int componentRestoreGrace;
    private final Map<String, String> deferredRemovedComponents = new HashMap<>();
    private final Set<String> users = new HashSet<>();
    private final Queue<Signal> signals = new ArrayDeque<>();
    public int maxComponents;
    private double maxCallBudget = 1.0;
    private boolean hasMemory;
    private volatile double callBudget;
    private boolean inSynchronizedCall;
    public long worldTime;
    private long uptime;
    private long cpuTotal;
    private long cpuStart;
    private int remainIdle;
    private int remainingPause;
    private boolean usersChanged;
    private String message;
    private double cost = Settings.get().computerCost() * Settings.get().tickFrequency();
    private final int maxSignalQueueSize = Settings.get().maxSignalQueueSize();

    public Machine(MachineHost host) {
        this.host = host;
        node = Network.newNode(this, Visibility.Network)
            .withComponent("computer", Visibility.Neighbors)
            .withConnector(Settings.get().bufferComputer()).create();
        tmp = Settings.get().tmpSize() > 0
            ? FileSystem.asManagedEnvironment(
                FileSystem.fromMemory(Settings.get().tmpSize() * 1024), "tmpfs", null, null, 5)
            : null;
        state.push(MachineState.Stopped);
    }

    @Override public MachineHost host() { return host; }
    @Override public ComponentConnector node() { return node; }
    @Override public Architecture architecture() { return architecture; }
    @Override public Map<String, String> components() { return components; }
    @Override public int maxComponents() { return maxComponents; }
    @Override public long worldTime() { return worldTime; }
    public int componentCount() {
        double count = -1;
        for (String name : components.values()) count += "filesystem".equals(name) ? .25 : 1;
        for (Component component : addedComponents) count += "filesystem".equals(component.name()) ? .25 : 1;
        return (int) count;
    }
    @Override public String tmpAddress() { return tmp == null ? null : tmp.node().address(); }
    @Override public String lastError() { return message; }
    @Override public void setCostPerTick(double value) { cost = value * Settings.get().tickFrequency(); }
    @Override public double getCostPerTick() { return cost / Settings.get().tickFrequency(); }
    @Override public String[] users() {
        synchronized (users) { return users.toArray(String[]::new); }
    }
    @Override public double upTime() {
        if (uptime < 0) uptime = worldTime + uptime;
        return uptime / 20.0;
    }
    @Override public double cpuTime() { return (cpuTotal + System.nanoTime() - cpuStart) * 10e-10; }
    @Override public Map<String, String> getDeviceInfo() {
        return host instanceof DeviceInfo info ? info.getDeviceInfo() : null;
    }

    @Override public void onHostChanged() {
        Iterable<ItemStack> internal = host.internalComponents();
        int capacity = 0;
        double budget = 0;
        int budgetCount = 0;
        Architecture selected = null;
        boolean architectureFound = false;
        for (ItemStack stack : internal) {
            if (stack == null) continue;
            var driver = Driver.driverFor(stack, host.getClass());
            if (driver instanceof Processor processor) {
                capacity += processor.supportedComponents(stack);
                if (!architectureFound && Slot.CPU.equals(processor.slot(stack))) {
                    Class<? extends Architecture> type = processor.architecture(stack);
                    if (type != null) {
                        architectureFound = true;
                        if (architecture != null && architecture.getClass() == type) selected = architecture;
                        else try {
                            selected = type.getConstructor(li.cil.oc.api.machine.Machine.class).newInstance(this);
                        } catch (Throwable error) {
                            OpenComputers.log().warn("Failed instantiating a CPU architecture.", error);
                        }
                    }
                }
            }
            if (driver instanceof CallBudget callBudgetDriver) {
                budget += callBudgetDriver.getCallBudget(stack);
                budgetCount++;
            }
        }
        maxComponents = capacity;
        maxCallBudget = budgetCount == 0 ? 1 : budget / budgetCount;
        if (selected != architecture) synchronized (this) {
            architecture = selected;
            if (architecture != null && node.network() != null) architecture.onConnect();
        }
        hasMemory = architecture != null && architecture.recomputeMemory(internal);
    }

    @Override public boolean canInteract(String player) {
        if (!Settings.get().canComputersBeOwned()) return true;
        synchronized (users) { if (users.isEmpty() || users.contains(player)) return true; }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.isSingleplayer()) return true;
        var list = server.getPlayerList();
        var entity = list.getPlayerByName(player);
        return entity != null && list.isOp(entity.getGameProfile());
    }
    @Override public boolean isRunning() {
        synchronized (state) {
            return state.peek() != MachineState.Stopped && state.peek() != MachineState.Stopping;
        }
    }
    @Override public boolean isPaused() {
        synchronized (state) { return state.peek() == MachineState.Paused && remainingPause > 0; }
    }
    @Override public boolean start() {
        synchronized (state) {
            if (state.peek() == MachineState.Paused && remainingPause > 0) {
                remainingPause = 0;
                host.markChanged();
                return true;
            }
            if (state.peek() == MachineState.Stopping) {
                switchTo(MachineState.Restarting);
                EventHandler.unscheduleClose(this);
                return true;
            }
            if (state.peek() != MachineState.Stopped || node.network() == null) return false;
            onHostChanged();
            processAddedComponents();
            verifyComponents();
            if (!Settings.get().ignorePower() && node.globalBuffer() < cost) {
                crash("gui.Error.NoEnergy");
            } else if (architecture == null || maxComponents == 0) {
                beep("-"); crash("gui.Error.NoCPU");
            } else if (componentCount() > maxComponents) {
                beep("-.."); crash("gui.Error.ComponentOverflow");
            } else if (!hasMemory) {
                beep("-."); crash("gui.Error.NoRAM");
            } else if (!init()) {
                beep("--");
            } else {
                switchTo(MachineState.Starting);
                uptime = 0;
                node.sendToReachable("computer.started");
                return true;
            }
            return false;
        }
    }
    private boolean shouldPause(MachineState current, int ticks) {
        return current != MachineState.Stopped && current != MachineState.Stopping
            && (current != MachineState.Paused || ticks > remainingPause);
    }
    @Override public boolean pause(double seconds) {
        int ticks = Math.max((int) (seconds * 20), 0);
        synchronized (state) { if (!shouldPause(state.peek(), ticks)) return false; }
        synchronized (this) {
            synchronized (state) {
                if (!shouldPause(state.peek(), ticks)) return false;
                if (state.peek() != MachineState.Paused) {
                    assert !state.contains(MachineState.Paused);
                    state.push(MachineState.Paused);
                }
                remainingPause = ticks;
                host.markChanged();
                return true;
            }
        }
    }
    @Override public boolean stop() {
        synchronized (state) {
            if (state.peek() == MachineState.Stopped || state.peek() == MachineState.Stopping) return false;
            state.push(MachineState.Stopping);
            EventHandler.scheduleClose(this);
            return true;
        }
    }
    @Override public void consumeCallBudget(double callCost) {
        if (architecture.isInitialized() && !inSynchronizedCall) {
            double clamped = Math.max(0, callCost);
            if (clamped > callBudget) throw Machine.<RuntimeException>unchecked(new LimitReachedException());
            callBudget -= clamped;
        }
    }
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException unchecked(Throwable error) throws E {
        throw (E) error;
    }
    @Override public void beep(short frequency, short duration) {
        SoundEffectPacketSender.sendToneUnchecked(host.getEnvironmentLevel(), host.xPosition(), host.yPosition(),
            host.zPosition(), frequency, duration);
    }
    @Override public void beep(String pattern) {
        SoundEffectPacketSender.sendPatternUnchecked(host.getEnvironmentLevel(), host.xPosition(), host.yPosition(),
            host.zPosition(), pattern);
    }
    @Override public boolean crash(String error) {
        message = error;
        synchronized (state) {
            boolean changed = stop();
            if (state.peek() == MachineState.Stopping) {
                state.clear();
                state.push(MachineState.Stopping);
            }
            return changed;
        }
    }

    private static Object convertArg(Object value) {
        if (value instanceof Boolean || value instanceof Byte || value instanceof Short ||
            value instanceof Integer || value instanceof Long || value instanceof String ||
            value instanceof byte[] || value instanceof CompoundTag) return value;
        if (value instanceof Character character) return (int) character;
        if (value instanceof Number number) return number.doubleValue();
        if (value instanceof java.util.HashMap<?, ?> map) return CollectionConverters.asScala(map);
        if (value != null) OpenComputers.log().warn(
            "Trying to push signal with an unsupported argument of type " + value.getClass().getName());
        return null;
    }

    @Override public boolean signal(String name, Object... arguments) {
        synchronized (state) {
            if (state.peek() == MachineState.Stopped || state.peek() == MachineState.Stopping) return false;
            synchronized (signals) {
                if (signals.size() >= maxSignalQueueSize) return false;
                if (arguments == null) signals.add(new Signal(name, new Object[0]));
                else {
                    Object[] converted = new Object[arguments.length];
                    for (int i = 0; i < arguments.length; i++) {
                        Object value = arguments[i];
                        if (value == null || value == scala.runtime.BoxedUnit.UNIT ||
                            value == scala.None$.MODULE$) converted[i] = null;
                        else if (value instanceof scala.collection.Map<?, ?> map) converted[i] = map;
                        else if (value instanceof java.util.Map<?, ?> map) {
                            Map<Object, Object> valid = new HashMap<>();
                            for (var entry : map.entrySet()) {
                                Object key = convertArg(entry.getKey());
                                Object item = convertArg(entry.getValue());
                                if (key != null && item != null) valid.put(key, item);
                            }
                            converted[i] = CollectionConverters.asScala(valid);
                        } else converted[i] = convertArg(value);
                    }
                    signals.add(new Signal(name, converted));
                }
            }
        }
        if (architecture != null) architecture.onSignal();
        return true;
    }

    @Override public Signal popSignal() {
        synchronized (signals) {
            Signal signal = signals.poll();
            return signal == null ? null : signal.convert();
        }
    }

    @Override public Map<String, Callback> methods(Object value) {
        Map<String, Callback> methods = new HashMap<>();
        Callbacks.apply(value).forEach((name, callback) -> methods.put(name, callback.annotation));
        return methods;
    }
    @Override public Object[] invoke(String address, String method, Object[] args) throws Exception {
        if (("setForeground".equals(method) || "setBackground".equals(method)) &&
            args.length > 0 && args[0] instanceof byte[]) {
            OpenComputers.log().warn("[Debug] Bad argument passed to {}! Array[Byte] detected.", method);
        }
        var network = node.network();
        if (network == null) throw new LimitReachedException();
        var found = network.node(address);
        if (found instanceof li.cil.oc.server.network.Component component &&
            (component.canBeSeenFrom(node) || component == node)) {
            Callback annotation = component.annotation(method);
            if (annotation.direct()) consumeCallBudget(1.0 / annotation.limit());
            return component.invoke(method, this, args);
        }
        throw new IllegalArgumentException("no such component");
    }
    @Override public Object[] invoke(Value value, String method, Object[] args) throws Exception {
        var callback = Callbacks.apply(value).get(method);
        if (callback == null) throw new NoSuchMethodException();
        if (callback.annotation.direct()) consumeCallBudget(1.0 / callback.annotation.limit());
        return Registry.convert(callback.apply(value, this, new ArgumentsImpl(args)));
    }
    @Override public void addUser(String name) throws Exception {
        if (users.size() >= Settings.get().maxUsers()) throw new Exception("too many users");
        if (users.contains(name)) throw new Exception("user exists");
        if (name.length() > Settings.get().maxUsernameLength()) throw new Exception("username too long");
        if (!Arrays.asList(ServerLifecycleHooks.getCurrentServer().getPlayerNames()).contains(name))
            throw new Exception("player must be online");
        synchronized (users) {
            users.add(name);
            usersChanged = true;
        }
    }
    @Override public boolean removeUser(String name) {
        synchronized (users) {
            boolean removed = users.remove(name);
            if (removed) usersChanged = true;
            return removed;
        }
    }

    @Callback(doc = "function():boolean -- Starts the computer. Returns true if the state changed.")
    public Object[] start(Context context, Arguments args) { return new Object[]{!isPaused() && start()}; }
    @Callback(doc = "function():boolean -- Stops the computer. Returns true if the state changed.")
    public Object[] stop(Context context, Arguments args) { return new Object[]{stop()}; }
    @Callback(direct = true, doc = "function():boolean -- Returns whether the computer is running.")
    public Object[] isRunning(Context context, Arguments args) { return new Object[]{isRunning()}; }
    @Callback(doc = "function([frequency:string or number[, duration:number]]) -- Plays a tone, useful to alert users via audible feedback.")
    public Object[] beep(Context context, Arguments args) {
        if (args.count() == 1 && args.isString(0)) beep(args.checkString(0));
        else {
            int frequency = args.optInteger(0, 440);
            if (frequency < 20 || frequency > 2000)
                throw new IllegalArgumentException("invalid frequency, must be in [20, 2000]");
            double duration = args.optDouble(1, 0.1);
            int millis = Math.max(50, Math.min(5000, (int) (duration * 1000)));
            context.pause(millis / 1000.0);
            beep((short) frequency, (short) millis);
        }
        return null;
    }
    @Callback(doc = "function():table -- Collect information on all connected devices.")
    public Object[] getDeviceInfo(Context context, Arguments args) {
        context.pause(1);
        Map<String, Map<String, String>> result = new HashMap<>();
        for (Node candidate : node.network().nodes()) {
            if (!(candidate.host() instanceof DeviceInfo info)) continue;
            boolean visible = candidate instanceof Component component
                ? component.canBeSeenFrom(node) || component == node
                : candidate.canBeReachedFrom(node);
            if (visible && info.getDeviceInfo() != null)
                result.put(candidate.address(), info.getDeviceInfo());
        }
        return new Object[]{result};
    }
    @Callback(doc = "function():table -- Returns a map of program name to disk label for known programs.")
    public Object[] getProgramLocations(Context context, Arguments args) {
        return new Object[]{ProgramLocations.getMappings(getArchitectureName(architecture.getClass()))};
    }

    public boolean isExecuting() {
        synchronized (state) { return state.contains(MachineState.Running); }
    }
    @Override public boolean canUpdate() { return true; }
    @Override public void update() {
        synchronized (state) { if (state.peek() == MachineState.Stopped) return; }
        processAddedComponents();
        tickComponentRestoreGrace();
        if (componentCount() > maxComponents) {
            beep("-..");
            crash("gui.Error.ComponentOverflow");
        }
        worldTime = host.getEnvironmentLevel().getDayTime();
        uptime++;
        if (remainIdle > 0) remainIdle--;
        callBudget = maxCallBudget;
        if (host.getEnvironmentLevel().getGameTime() % Settings.get().tickFrequency() == 0) {
            synchronized (state) {
                switch (state.peek()) {
                    case Paused, Restarting, Stopping, Stopped -> { }
                    case Sleeping -> {
                        double amount = remainIdle > 0 && signals.isEmpty()
                            ? cost * Settings.get().sleepCostFactor() : cost;
                        if (!node.tryChangeBuffer(-amount)) crash("gui.Error.NoEnergy");
                    }
                    default -> {
                        if (!node.tryChangeBuffer(-cost)) crash("gui.Error.NoEnergy");
                    }
                }
            }
        }
        if (host.getEnvironmentLevel().getGameTime() % 20 == 0 && usersChanged) {
            String[] currentUsers;
            synchronized (users) {
                usersChanged = false;
                currentUsers = users.toArray(String[]::new);
            }
            if (host instanceof li.cil.oc.common.blockentity.traits.Computer computer)
                GeneralPacketSender.sendComputerUserListUnchecked(computer, currentUsers);
        }
        MachineState current;
        synchronized (state) { current = state.peek(); }
        switch (current) {
            case Starting -> { verifyComponents(); switchTo(MachineState.Yielded); }
            case Restarting -> {
                close();
                if (Settings.get().eraseTmpOnReboot() && tmp != null) {
                    tmp.node().remove();
                    node.connect(tmp.node());
                }
                node.sendToReachable("computer.stopped");
                start();
            }
            case Sleeping -> {
                if (remainIdle <= 0 || !signals.isEmpty()) switchTo(MachineState.Yielded);
            }
            case Paused -> {
                if (remainingPause > 0) remainingPause--;
                else {
                    verifyComponents();
                    state.pop();
                    switchTo(state.peek());
                }
            }
            case SynchronizedCall -> {
                switchTo(MachineState.Running);
                try {
                    inSynchronizedCall = true;
                    architecture.runSynchronized();
                    inSynchronizedCall = false;
                    switch (state.peek()) {
                        case Running -> switchTo(MachineState.SynchronizedReturn);
                        case Paused -> {
                            state.pop();
                            state.pop();
                            state.push(MachineState.SynchronizedReturn);
                            state.push(MachineState.Paused);
                        }
                        case Stopping -> { state.clear(); state.push(MachineState.Stopping); }
                        default -> throw new AssertionError();
                    }
                } catch (java.lang.Error error) {
                    if ("not enough memory".equals(error.getMessage())) crash("gui.Error.OutOfMemory");
                    else {
                        OpenComputers.log().warn("Faulty architecture implementation for synchronized calls.", error);
                        crash("gui.Error.InternalError");
                    }
                } catch (Throwable error) {
                    OpenComputers.log().warn("Faulty architecture implementation for synchronized calls.", error);
                    crash("gui.Error.InternalError");
                } finally { inSynchronizedCall = false; }
            }
            default -> { }
        }
        boolean stopping;
        synchronized (state) { stopping = state.peek() == MachineState.Stopping; }
        if (stopping) synchronized (this) {
            synchronized (state) { tryClose(); }
        }
    }

    @Override public void onMessage(Message message) {
        Object[] data = message.data();
        if ("computer.signal".equals(message.name()) && data.length > 0 && data[0] instanceof String name) {
            Object[] args = new Object[data.length];
            args[0] = message.source().address();
            System.arraycopy(data, 1, args, 1, data.length - 1);
            signal(name, args);
        } else if ("computer.checked_signal".equals(message.name()) &&
                   data.length > 1 && data[0] instanceof Player player && data[1] instanceof String name) {
            if (canInteract(player.getName().getString())) {
                Object[] args = new Object[data.length - 1];
                args[0] = message.source().address();
                System.arraycopy(data, 2, args, 1, data.length - 2);
                signal(name, args);
            }
        } else if ("computer.start".equals(message.name()) && !isPaused()) start();
        else if ("computer.stop".equals(message.name())) stop();
    }

    @Override public void onConnect(Node connected) {
        if (connected == node) {
            components.put(node.address(), node.name());
            if (tmp != null) node.connect(tmp.node());
            if (architecture != null) architecture.onConnect();
        } else if (connected instanceof Component component) addComponent(component);
        host.onMachineConnect(connected);
    }
    @Override public void onDisconnect(Node disconnected) {
        if (disconnected == node) {
            close();
            if (tmp != null) tmp.node().remove();
        } else if (disconnected instanceof Component component) removeComponent(component);
        host.onMachineDisconnect(disconnected);
    }
    public void addComponent(Component component) {
        deferredRemovedComponents.remove(component.address());
        if (!components.containsKey(component.address())) addedComponents.add(component);
    }
    public void removeComponent(Component component) {
        if (components.containsKey(component.address())) {
            if (componentRestoreGrace > 0)
                deferredRemovedComponents.put(component.address(), component.name());
            else {
                synchronized (components) { components.remove(component.address()); }
                signal("component_removed", component.address(), component.name());
            }
        }
        addedComponents.remove(component);
    }
    public void beginComponentRestoreGrace(int ticks) {
        if (isRunning()) {
            componentRestoreGrace = Math.max(componentRestoreGrace, ticks);
            pause(componentRestoreGrace / 20.0);
        }
    }
    private void tickComponentRestoreGrace() {
        if (componentRestoreGrace <= 0 || --componentRestoreGrace != 0) return;
        var network = node.network();
        for (var entry : deferredRemovedComponents.entrySet()) {
            String address = entry.getKey();
            String name = entry.getValue();
            Node restored = network == null ? null : network.node(address);
            if (!(restored instanceof Component component && name.equals(component.name())) &&
                components.containsKey(address)) {
                synchronized (components) { components.remove(address); }
                signal("component_removed", address, name);
            }
        }
        deferredRemovedComponents.clear();
        verifyComponents();
    }
    private void processAddedComponents() {
        if (addedComponents.isEmpty()) return;
        for (Component component : addedComponents) {
            if (component.canBeSeenFrom(node)) {
                synchronized (components) { components.put(component.address(), component.name()); }
                if (architecture != null && architecture.isInitialized())
                    signal("component_added", component.address(), component.name());
            }
        }
        addedComponents.clear();
    }
    private void verifyComponents() {
        if (componentRestoreGrace > 0) return;
        // Splitting or reconstructing a moving structure can leave the machine
        // briefly detached. Keep addresses until the network is available again.
        var network = node.network();
        if (network == null) return;
        Set<String> invalid = new HashSet<>();
        for (var entry : components.entrySet()) {
            String address = entry.getKey();
            String name = entry.getValue();
            Node found = network.node(address);
            if (found instanceof Component component && name.equals(component.name())) continue;
            if ("filesystem".equals(name)) {
                OpenComputers.log().trace("A component of type '{}' disappeared ({})! This usually means that it didn't save its node.", name, address);
                OpenComputers.log().trace("If this was a file system provided by a ComputerCraft peripheral, this is normal.");
            } else OpenComputers.log().warn(
                "A component of type '{}' disappeared ({})! This usually means that it didn't save its node.", name, address);
            signal("component_removed", address, name);
            invalid.add(address);
        }
        invalid.forEach(components::remove);
    }

    private String tmpPath() { return node.address() + "_tmp"; }

    @Override public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        synchronized (this) {
            synchronized (state) {
                assert state.peek() == MachineState.Stopped || state.peek() == MachineState.Paused;
                close();
                state.clear();
                ExtendedDataComponentHolder extended = holder instanceof MutableDataComponentHolder mutable
                    ? new ExtendedMutableDataComponentHolder(mutable)
                    : new ExtendedDataComponentHolder(holder);
                MachineData data = extended.getComponentOptional(OCComponentsJava.MACHINE).orElse(null);
                if (data != null && data.nodeAddress().isPresent())
                    ((li.cil.oc.server.network.Node) node).loadAddress(data.nodeAddress().get());
                else super.loadData(holder);

                if (data != null) {
                    List<MachineState> saved = new ArrayList<>(data.state());
                    for (int i = saved.size() - 1; i >= 0; i--) state.push(saved.get(i));
                    users.addAll(data.users());
                    if (data.message().isPresent()) message = data.message().get();
                    for (MachineData.Component component : data.components())
                        components.put(component.address(), component.name());
                    if (tmp != null) {
                        var level = host.getEnvironmentLevel();
                        var chunk = new ChunkPos(new BlockPos(
                            (int) host.xPosition(), (int) host.yPosition(), (int) host.zPosition()));
                        tmp.loadData(SaveHandler.loadNBT(level.dimension().location(), chunk, tmpPath()),
                            level.registryAccess());
                    }
                    if (!state.isEmpty() && isRunning() && init()) {
                        try {
                            architecture.loadData(holder, data.architectureData());
                        } catch (Throwable error) {
                            OpenComputers.log().error(
                                "Unexpected error loading a state of computer at {}. State: {}. Unless you're upgrading/downgrading across a major version, please report this! Thank you.",
                                host.machinePosition(), state.peek(), error);
                            close();
                            start();
                        }
                        for (MachineData.Signal signal : data.signals()) {
                            signals.add(new Signal(signal.name(), MachineData.signalArgs(signal)));
                        }
                        uptime = data.uptime();
                        cpuTotal = data.cpuTotal();
                        remainingPause = data.remainingPause();
                        if (state.peek() != MachineState.Restarting) pause(Settings.get().startupDelay());
                    } else {
                        onHostChanged();
                        close();
                    }
                }
                if (state.isEmpty()) state.push(MachineState.Stopped);
            }
        }
    }

    @Override public void saveData(MutableDataComponentHolder holder) {
        synchronized (this) {
            synchronized (state) {
                if (isExecuting() || SaveHandler.savingForClients) return;
                pause(.05);
                processAddedComponents();
                var network = node.network();
                if (network != null) {
                    synchronized (components) {
                        components.entrySet().removeIf(entry -> {
                            Node found = network.node(entry.getKey());
                            return !(found instanceof Component component &&
                                entry.getValue().equals(component.name()));
                        });
                    }
                }
                if (tmp != null) {
                    var level = host.getEnvironmentLevel();
                    var chunk = new ChunkPos(new BlockPos(
                        (int) host.xPosition(), (int) host.yPosition(), (int) host.zPosition()));
                    SaveHandler.scheduleSave(level.dimension().location(), chunk, tmpPath(), tag -> {
                        tmp.saveData(tag, level.registryAccess());
                        return scala.runtime.BoxedUnit.UNIT;
                    });
                }
                CompoundTag architectureData = new CompoundTag();
                if (architecture != null) try {
                    architecture.saveData(holder, architectureData);
                } catch (Throwable error) {
                    OpenComputers.log().error(
                        "Unexpected error saving a state of computer at {}. State: {}. Unless you're upgrading/downgrading across a major version, please report this! Thank you.",
                        host.machinePosition(), state.peek(), error);
                }
                List<MachineData.Component> componentData = new ArrayList<>();
                components.forEach((address, name) ->
                    componentData.add(new MachineData.Component(address, name)));
                List<MachineData.Signal> signalData = new ArrayList<>();
                if (state.peek() != MachineState.Stopped) for (Signal signal : signals) {
                    signalData.add(MachineData.signal(signal.name(), signal.args()));
                }
                MachineData data = new MachineData(
                    new ArrayList<>(state),
                    new HashSet<>(users),
                    java.util.Optional.ofNullable(message),
                    componentData,
                    java.util.Optional.ofNullable(node.address()),
                    architectureData,
                    signalData,
                    uptime, cpuTotal, remainingPause);
                holder.set(OCComponentsJava.MACHINE, data);
            }
        }
    }

    private boolean init() {
        onHostChanged();
        if (architecture == null) return false;
        message = null;
        signals.clear();
        if (node.network() != null && tmp != null) node.connect(tmp.node());
        try {
            return architecture.initialize();
        } catch (Throwable error) {
            OpenComputers.log().warn("Failed initializing computer.", error);
            close();
            return false;
        }
    }
    public boolean tryClose() {
        if (isExecuting()) return false;
        close();
        if (tmp != null) {
            tmp.node().remove();
            if (node.network() != null) node.connect(tmp.node());
        }
        node.sendToReachable("computer.stopped");
        return true;
    }
    private void close() {
        synchronized (state) {
            if (!state.isEmpty() && state.peek() == MachineState.Stopped) return;
        }
        synchronized (this) {
            synchronized (state) {
                state.clear();
                state.push(MachineState.Stopped);
                componentRestoreGrace = 0;
                deferredRemovedComponents.clear();
                if (architecture != null) architecture.close();
                signals.clear();
                uptime = 0;
                cpuTotal = 0;
                cpuStart = 0;
                remainIdle = 0;
            }
            host.markChanged();
        }
    }
    private MachineState switchTo(MachineState next) {
        synchronized (state) {
            MachineState previous = state.pop();
            if (next == MachineState.Stopping || next == MachineState.Restarting) state.clear();
            state.push(next);
            if (next == MachineState.Yielded || next == MachineState.SynchronizedReturn) {
                remainIdle = 0;
                THREAD_POOL.schedule(this, Settings.get().executionDelay(), TimeUnit.MILLISECONDS);
            }
            host.markChanged();
            return previous;
        }
    }
    private boolean isGamePaused() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server != null && !server.isDedicatedServer() &&
            FMLEnvironment.dist.isClient() && ClientUtil.isPaused();
    }
    @Override public void run() {
        synchronized (this) {
            boolean synchronizedReturn;
            synchronized (state) {
                if (state.peek() != MachineState.Yielded &&
                    state.peek() != MachineState.SynchronizedReturn) return;
                if (isGamePaused()) {
                    state.push(MachineState.Paused);
                    return;
                }
                synchronizedReturn = switchTo(MachineState.Running) == MachineState.SynchronizedReturn;
            }
            cpuStart = System.nanoTime();
            try {
                ExecutionResult result = architecture.runThreaded(synchronizedReturn);
                synchronized (state) {
                    switch (state.peek()) {
                        case Running -> {
                            if (result instanceof ExecutionResult.Sleep sleep) {
                                synchronized (signals) {
                                    if (signals.isEmpty() && sleep.ticks > 0) {
                                        switchTo(MachineState.Sleeping);
                                        remainIdle = sleep.ticks;
                                    } else switchTo(MachineState.Yielded);
                                }
                            } else if (result instanceof ExecutionResult.SynchronizedCall)
                                switchTo(MachineState.SynchronizedCall);
                            else if (result instanceof ExecutionResult.Shutdown shutdown)
                                switchTo(shutdown.reboot ? MachineState.Restarting : MachineState.Stopping);
                            else if (result instanceof ExecutionResult.Error error) {
                                beep("--");
                                crash(error.message == null ? "unknown error" : error.message);
                            }
                        }
                        case Paused -> {
                            state.pop();
                            state.pop();
                            if (result instanceof ExecutionResult.Sleep sleep) {
                                remainIdle = sleep.ticks;
                                state.push(MachineState.Sleeping);
                            } else if (result instanceof ExecutionResult.SynchronizedCall)
                                state.push(MachineState.SynchronizedCall);
                            else if (result instanceof ExecutionResult.Shutdown shutdown)
                                state.push(shutdown.reboot ? MachineState.Restarting : MachineState.Stopping);
                            else if (result instanceof ExecutionResult.Error error)
                                crash(error.message == null ? "unknown error" : error.message);
                            state.push(MachineState.Paused);
                        }
                        case Stopping -> { state.clear(); state.push(MachineState.Stopping); }
                        case Restarting -> { }
                        default -> throw new AssertionError("Invalid state in executor post-processing.");
                    }
                    assert !isExecuting();
                }
            } catch (Throwable error) {
                OpenComputers.log().warn("Architecture's runThreaded threw an error. This should never happen!", error);
                crash("gui.Error.InternalError");
            }
            cpuTotal += System.nanoTime() - cpuStart;
        }
    }

    public static final class Signal implements li.cil.oc.api.machine.Signal {
        private final String name;
        private final Object[] args;
        public Signal(String name, Object[] args) { this.name = name; this.args = args; }
        @Override public String name() { return name; }
        @Override public Object[] args() { return args; }
        Signal convert() { return new Signal(name, Registry.convert(args)); }
    }

    public static String getArchitectureName(Class<? extends Architecture> architecture) {
        Architecture.Name name = architecture.getAnnotation(Architecture.Name.class);
        return name == null ? architecture.getSimpleName() : name.value();
    }
    private static final LinkedHashSet<Class<? extends Architecture>> ARCHITECTURES = new LinkedHashSet<>();
    public static final li.cil.oc.api.detail.MachineAPI FACTORY = new li.cil.oc.api.detail.MachineAPI() {
        @Override public void add(Class<? extends Architecture> architecture) {
            if (ARCHITECTURES.contains(architecture)) return;
            try {
                architecture.getConstructor(li.cil.oc.api.machine.Machine.class);
            } catch (Throwable error) {
                throw new IllegalArgumentException("Architecture does not have required constructor.", error);
            }
            ARCHITECTURES.add(architecture);
        }
        @Override public Collection<Class<? extends Architecture>> architectures() {
            return List.copyOf(ARCHITECTURES);
        }
        @Override public String getArchitectureName(Class<? extends Architecture> architecture) {
            return Machine.getArchitectureName(architecture);
        }
        @Override public li.cil.oc.api.machine.Machine create(MachineHost host) {
            return new Machine(host);
        }
    };
}
