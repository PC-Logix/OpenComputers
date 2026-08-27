package li.cil.oc.integration.computercraft;

import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dan200.computercraft.api.peripheral.WorkMonitor;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.BlacklistedPeripheral;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.util.Reflection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;

public final class DriverPeripheral implements li.cil.oc.api.driver.DriverBlock {
    private static Set<Class<?>> blacklist;

    private boolean isBlacklisted(final Object o) {
        // Create peripherals have native OC drivers. Wrapping Create's CC:T
        // capability as well would expose duplicate components and duplicate
        // every event when both computer mods are installed.
        if (o.getClass().getName().startsWith(
                "com.simibubi.create.compat.computercraft.implementation.peripherals.")) {
            return true;
        }

        if (o instanceof BlacklistedPeripheral) {
            return ((BlacklistedPeripheral) o).isPeripheralBlacklisted();
        }

        if (blacklist == null) {
            blacklist = new HashSet<>();
            for (String name : Settings.get().peripheralBlacklist()) {
                final Class<?> clazz = Reflection.getClass(name);
                if (clazz != null) {
                    blacklist.add(clazz);
                }
            }
        }

        for (Class<?> clazz : blacklist) {
            if (clazz.isInstance(o)) {
                return true;
            }
        }

        return false;
    }

    private IPeripheral findPeripheral(final Level world, final BlockPos pos, final Direction side) {
        final IPeripheral p = world.getCapability(PeripheralCapability.get(), pos, side);

        if (p != null && !isBlacklisted(p)) {
            return p;
        }
        return null;
    }

    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        final BlockEntity tileEntity = world.getBlockEntity(pos);

        return tileEntity != null
                && !li.cil.oc.api.network.Environment.class.isAssignableFrom(tileEntity.getClass())
                && !isBlacklisted(tileEntity)
                && findPeripheral(world, pos, side) != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        return new Environment(findPeripheral(world, pos, side));
    }

    public static class Environment extends li.cil.oc.api.prefab.AbstractManagedEnvironment implements li.cil.oc.api.network.ManagedPeripheral, NamedBlock {
        protected final IPeripheral peripheral;
        protected final String[] methodNames;
        protected final Map<String, FakeComputerAccess> accesses = new HashMap<>();
        protected final Map<String, Method> reflectedMethods = new HashMap<>();
        protected final AtomicLong nextTaskId = new AtomicLong();

        public Environment(final IPeripheral peripheral) {
            this.peripheral = peripheral;

            if (peripheral instanceof IDynamicPeripheral dynamic) {
                methodNames = dynamic.getMethodNames();
            } else {
                final List<String> names = new ArrayList<>();

                for (Method method : peripheral.getClass().getMethods()) {
                    if (method.isAnnotationPresent(LuaFunction.class)) {
                        reflectedMethods.put(method.getName(), method);
                        names.add(method.getName());
                    }
                }

                methodNames = names.toArray(new String[0]);
            }

            setNode(Network.newNode(this, Visibility.Network).create());
        }

        @Override
        public String[] methods() {
            return methodNames;
        }

        @Override
        public Object[] invoke(final String name, final Context context, final Arguments args) throws Exception {
            final FakeComputerAccess access;

            if (accesses.containsKey(context.node().address())) {
                access = accesses.get(context.node().address());
            } else {
                access = new FakeComputerAccess(this, context);
            }

            final Object[] argArray = CallableHelper.convertArguments(args);
            final ILuaContext luaContext = new SynchronousLuaContext(context, nextTaskId);

            if (peripheral instanceof IDynamicPeripheral dynamic) {
                final String[] names = dynamic.getMethodNames();

                int index = -1;

                for (int i = 0; i < names.length; i++) {
                    if (names[i].equals(name)) {
                        index = i;
                        break;
                    }
                }

                if (index == -1) {
                    throw new NoSuchMethodException();
                }

                return dynamic.callMethod(
                        access,
                        luaContext,
                        index,
                        new ObjectArguments(argArray)
                ).getResult();
            }

            final Method method = reflectedMethods.get(name);

            if (method == null) {
                throw new NoSuchMethodException();
            }

            final Object[] invokeArgs = buildInvokeArguments(method, argArray, luaContext);

            final Object result = method.invoke(peripheral, invokeArgs);

            return wrapResult(result);
        }

        private Object[] buildInvokeArguments(final Method method, final Object[] args, final ILuaContext luaContext) {
            final Class<?>[] parameterTypes = method.getParameterTypes();
            final Object[] invokeArgs = new Object[parameterTypes.length];

            int argIndex = 0;

            for (int i = 0; i < parameterTypes.length; i++) {
                final Class<?> type = parameterTypes[i];

                if (type == IComputerAccess.class) {
                    invokeArgs[i] = null;
                } else if (type == ILuaContext.class) {
                    invokeArgs[i] = luaContext;
                } else if (type == ObjectArguments.class) {
                    invokeArgs[i] = new ObjectArguments(args);
                } else {
                    invokeArgs[i] = argIndex < args.length ? coerce(args[argIndex], type) : defaultValue(type);
                    argIndex++;
                }
            }

            return invokeArgs;
        }

        private Object coerce(final Object value, final Class<?> type) {
            if (value == null) {
                return defaultValue(type);
            }

            if (type.isInstance(value)) {
                return value;
            }

            if (type == int.class || type == Integer.class) {
                return ((Number) value).intValue();
            }

            if (type == long.class || type == Long.class) {
                return ((Number) value).longValue();
            }

            if (type == double.class || type == Double.class) {
                return ((Number) value).doubleValue();
            }

            if (type == float.class || type == Float.class) {
                return ((Number) value).floatValue();
            }

            if (type == short.class || type == Short.class) {
                return ((Number) value).shortValue();
            }

            if (type == byte.class || type == Byte.class) {
                return ((Number) value).byteValue();
            }

            if (type == boolean.class || type == Boolean.class) {
                return value;
            }

            if (type == String.class) {
                return String.valueOf(value);
            }

            if (type.getName().contains("Coerced")) {
                if (String.valueOf(value) != null) {
                    return new Coerced<>(String.valueOf(value));
                }
            }

            return value;
        }

        private Object defaultValue(final Class<?> type) {
            if (!type.isPrimitive()) {
                return null;
            }

            if (type == boolean.class) {
                return false;
            }

            if (type == char.class) {
                return '\0';
            }

            return 0;
        }

        private Object[] wrapResult(final Object result) {
            if (result == null) {
                return new Object[0];
            }

            if (result instanceof Object[] objects) {
                return objects;
            }

            if (result.getClass().isArray()) {
                final int len = Array.getLength(result);
                final Object[] out = new Object[len];

                for (int i = 0; i < len; i++) {
                    out[i] = Array.get(result, i);
                }

                return out;
            }

            return new Object[]{result};
        }

        @Override
        public void onConnect(final Node node) {
            super.onConnect(node);

            if (node.host() instanceof Context && !accesses.containsKey(node.address())) {
                final FakeComputerAccess access = new FakeComputerAccess(this, (Context) node.host());

                accesses.put(node.address(), access);

                peripheral.attach(access);
            }
        }

        @Override
        public void onDisconnect(final Node node) {
            super.onDisconnect(node);

            if (node.host() instanceof Context) {
                final FakeComputerAccess access = accesses.remove(node.address());

                if (access != null) {
                    peripheral.detach(access);
                }
            } else if (node == this.node()) {
                for (FakeComputerAccess access : accesses.values()) {
                    peripheral.detach(access);
                    access.close();
                }

                accesses.clear();
            }
        }

        @Override
        public String preferredName() {
            return peripheral.getType();
        }

        @Override
        public int priority() {
            return -1;
        }

        public static class FakeComputerAccess implements IComputerAccess {
            protected final Environment owner;
            protected final Context context;
            protected final Map<String, ManagedEnvironment> fileSystems = new HashMap<>();

            public FakeComputerAccess(final Environment owner, final Context context) {
                this.owner = owner;
                this.context = context;
            }

            public void close() {
                for (ManagedEnvironment fileSystem : fileSystems.values()) {
                    fileSystem.node().remove();
                }

                fileSystems.clear();
            }

            @Override
            public String mount(final String desiredLocation, final Mount mount) {
                if (fileSystems.containsKey(desiredLocation)) {
                    return null;
                }

                return mount(desiredLocation, FileSystem.asManagedEnvironment(DriverComputerCraftMedia.fromComputerCraft(mount)));
            }

            @Override
            public String mount(final String desiredLocation, final Mount mount, final String driveName) {
                if (fileSystems.containsKey(desiredLocation)) {
                    return null;
                }

                return mount(desiredLocation, FileSystem.asManagedEnvironment(DriverComputerCraftMedia.fromComputerCraft(mount), driveName));
            }

            @Override
            public String mountWritable(final String desiredLocation, final WritableMount mount) {
                if (fileSystems.containsKey(desiredLocation)) {
                    return null;
                }

                return mount(desiredLocation, FileSystem.asManagedEnvironment(DriverComputerCraftMedia.fromComputerCraft(mount)));
            }

            @Override
            public String mountWritable(final String desiredLocation, final WritableMount mount, final String driveName) {
                if (fileSystems.containsKey(desiredLocation)) {
                    return null;
                }

                return mount(desiredLocation, FileSystem.asManagedEnvironment(DriverComputerCraftMedia.fromComputerCraft(mount), driveName));
            }

            private String mount(final String path, final ManagedEnvironment fileSystem) {
                fileSystems.put(path, fileSystem);

                context.node().connect(fileSystem.node());

                return path;
            }

            @Override
            public void unmount(final String location) {
                final ManagedEnvironment fileSystem = fileSystems.remove(location);

                if (fileSystem != null) {
                    fileSystem.node().remove();
                }
            }

            @Override
            public int getID() {
                return context.node().address().hashCode();
            }

            @Override
            public void queueEvent(final String event, final Object... arguments) {
                context.signal(event, arguments);
            }

            @Override
            public String getAttachmentName() {
                return owner.node().address();
            }

            @Override
            public @NotNull Map<String, IPeripheral> getAvailablePeripherals() {
                return Collections.emptyMap();
            }

            @Override
            public IPeripheral getAvailablePeripheral(final String name) {
                return null;
            }

            @Override
            public @NotNull WorkMonitor getMainThreadMonitor() {
                return new WorkMonitor() {
                    @Override
                    public boolean canWork() {
                        return false;
                    }

                    @Override
                    public boolean shouldWork() {
                        return false;
                    }

                    @Override
                    public void trackWork(long l, @NotNull TimeUnit timeUnit) {

                    }
                };
            }
        }

        /**
         * The enclosing ManagedPeripheral callback is non-direct, so OC has
         * already moved execution to the server thread before this context is
         * used. CC:T main-thread tasks can therefore run synchronously here.
         */
        public static final class SynchronousLuaContext implements ILuaContext {
            private final Context context;
            private final AtomicLong nextTaskId;

            public SynchronousLuaContext(final Context context, final AtomicLong nextTaskId) {
                this.context = context;
                this.nextTaskId = nextTaskId;
            }

            @Override
            public long issueMainThreadTask(@NotNull final LuaTask task) throws LuaException {
                final long taskId = nextTaskId.getAndIncrement();

                try {
                    signalTaskCompleted(taskId, true, task.execute());
                } catch (final LuaException | RuntimeException e) {
                    signalTaskCompleted(taskId, false, new Object[]{e.getMessage()});
                }

                return taskId;
            }

            @Override
            public MethodResult executeMainThreadTask(@NotNull final LuaTask task) throws LuaException {
                final Object[] result = task.execute();
                return MethodResult.of(result == null ? new Object[0] : result);
            }

            private void signalTaskCompleted(final long taskId, final boolean success, final Object[] result) {
                final Object[] signal = new Object[2 + (result == null ? 0 : result.length)];
                signal[0] = taskId;
                signal[1] = success;

                if (result != null) {
                    System.arraycopy(result, 0, signal, 2, result.length);
                }

                context.signal("task_completed", signal);
            }
        }
    }
}
