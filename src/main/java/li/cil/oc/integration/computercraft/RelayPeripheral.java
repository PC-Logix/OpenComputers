package li.cil.oc.integration.computercraft;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.common.blockentity.Relay;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;
import scala.collection.Iterator;
import scala.collection.mutable.Set$;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Map;
import java.util.Objects;

/** ComputerCraft modem interface for an OpenComputers relay. */
public final class RelayPeripheral implements IDynamicPeripheral {
    @FunctionalInterface
    private interface MethodCall {
        Object[] apply(IComputerAccess computer, ILuaContext context, Object[] arguments) throws Exception;
    }

    private final Relay relay;
    private final Map<String, MethodCall> methods = new LinkedHashMap<>();
    private final String[] methodNames;

    public RelayPeripheral(Relay relay) {
        this.relay = relay;
        methods.put("open", (computer, context, arguments) -> {
            int port = checkPort(arguments, 0);
            if (relay.openPorts().apply(computer).size() >= 128) throw new IllegalArgumentException("too many open channels");
            return ResultWrapper.result(relay.openPorts().apply(computer).add(port));
        });
        methods.put("isOpen", (computer, context, arguments) -> {
            int port = checkPort(arguments, 0);
            return ResultWrapper.result(relay.openPorts().apply(computer).contains(port));
        });
        methods.put("close", (computer, context, arguments) -> {
            int port = checkPort(arguments, 0);
            return ResultWrapper.result(relay.openPorts().apply(computer).remove(port));
        });
        methods.put("closeAll", (computer, context, arguments) -> {
            relay.openPorts().apply(computer).clear();
            return null;
        });
        methods.put("transmit", (computer, context, arguments) -> {
            int sendPort = checkPort(arguments, 0);
            int answerPort = checkPort(arguments, 1);
            Object[] data = Arrays.copyOfRange(arguments, 2, arguments.length + 1);
            data[data.length - 1] = answerPort;
            var packet = Network.newPacket("cc" + computer.getID() + "_" + computer.getAttachmentName(), null, sendPort, data);
            return ResultWrapper.result(relay.tryEnqueuePacketJava(Optional.empty(), packet));
        });
        methods.put("isWireless", (computer, context, arguments) -> ResultWrapper.result(false));
        methods.put("callRemote", (computer, context, arguments) -> {
            String address = checkString(arguments, 0);
            for (Component component : visibleComponents()) {
                if (Objects.equals(component.address(), address)) {
                    return component.invoke(checkString(arguments, 1), new CCContext(computer, context), Arrays.copyOfRange(arguments, 2, arguments.length));
                }
            }
            return null;
        });
        methods.put("getMethodsRemote", (computer, context, arguments) -> {
            Component component = findVisible(checkString(arguments, 0));
            if (component == null) return null;
            Map<Integer, String> result = new LinkedHashMap<>();
            java.util.Iterator<String> iterator = component.methods().iterator();
            int index = 1;
            while (iterator.hasNext()) result.put(index++, iterator.next());
            return ResultWrapper.result(result);
        });
        methods.put("getNamesRemote", (computer, context, arguments) -> {
            Map<Integer, String> result = new LinkedHashMap<>();
            int index = 1;
            for (Component component : visibleComponents()) result.put(index++, component.address());
            return ResultWrapper.result(result);
        });
        methods.put("getTypeRemote", (computer, context, arguments) -> {
            Component component = findVisible(checkString(arguments, 0));
            return component == null ? null : ResultWrapper.result(component.name());
        });
        methods.put("isPresentRemote", (computer, context, arguments) -> ResultWrapper.result(findVisible(checkString(arguments, 0)) != null));
        methods.put("isAccessPoint", (computer, context, arguments) -> ResultWrapper.result(relay.isWirelessEnabled()));
        methods.put("isTunnel", (computer, context, arguments) -> ResultWrapper.result(relay.isLinkedEnabled()));
        methods.put("maxPacketSize", (computer, context, arguments) -> ResultWrapper.result(Settings.get().maxNetworkPacketSize()));
        methodNames = methods.keySet().stream().sorted().toArray(String[]::new);
    }

    public Relay relay() { return relay; }

    @Override public String getType() { return "opencomputers:relay"; }
    @Override public java.util.Set<String> getAdditionalTypes() { return java.util.Set.of("modem"); }
    @Override public Object getTarget() { return relay; }

    @Override
    public void attach(IComputerAccess computer) {
        relay.computers().append(computer);
        relay.openPorts().put(computer, Set$.MODULE$.empty());
    }

    @Override
    public void detach(IComputerAccess computer) {
        int index = relay.computers().indexOf(computer);
        if (index >= 0) relay.computers().remove(index);
        relay.openPorts().remove(computer);
    }

    @Override public String[] getMethodNames() { return methodNames; }

    @Override
    public MethodResult callMethod(IComputerAccess computer, ILuaContext context, int method, dan200.computercraft.api.lua.IArguments arguments) throws LuaException {
        try {
            return MethodResult.of(methods.get(methodNames[method]).apply(computer, context, arguments.getAll()));
        }
        catch (LuaException e) {
            throw e;
        }
        catch (Throwable t) {
            t.printStackTrace();
            throw new LuaException(t.getMessage());
        }
    }

    @Override
    public boolean equals(IPeripheral other) {
        return other instanceof RelayPeripheral peripheral && Objects.equals(peripheral.relay, relay);
    }

    private int checkPort(Object[] args, int index) {
        if (args.length <= index || !(args[index] instanceof Number number))
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (number expected)");
        int port = (int) number.doubleValue();
        if (port < 0 || port > 0xFFFF)
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (number in [1, 65535] expected)");
        return port;
    }

    private String checkString(Object[] args, int index) {
        if (args.length <= index || !(args[index] instanceof String value))
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (string expected)");
        return value;
    }

    private Component findVisible(String address) {
        for (Component component : visibleComponents()) if (Objects.equals(component.address(), address)) return component;
        return null;
    }

    private Iterable<Component> visibleComponents() {
        java.util.ArrayList<Component> visible = new java.util.ArrayList<>();
        for (Direction side : Direction.values()) {
            var node = relay.sidedNode(side);
            java.util.Iterator<?> iterator = node.reachableNodes().iterator();
            while (iterator.hasNext()) {
                Object candidate = iterator.next();
                if (candidate instanceof Component component && component.canBeSeenFrom(node)) visible.add(component);
            }
        }
        return visible;
    }

    private final class CCContext implements Context {
        private final IComputerAccess computer;
        private final ILuaContext context;

        private CCContext(IComputerAccess computer, ILuaContext context) {
            this.computer = computer;
            this.context = context;
        }

        @Override public li.cil.oc.api.network.Node node() { return relay.node(); }
        @Override public boolean isPaused() { return false; }
        @Override public boolean stop() { return false; }
        @Override public boolean canInteract(String player) { return true; }
        @Override public boolean signal(String name, Object... args) { computer.queueEvent(name, args); return true; }
        @Override public boolean pause(double seconds) { return false; }
        @Override public boolean isRunning() { return true; }
        @Override public boolean start() { return false; }
        @Override public void consumeCallBudget(double callCost) { }
    }
}
