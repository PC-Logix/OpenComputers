package li.cil.oc.common;

import li.cil.oc.OpenComputers$;
import li.cil.oc.api.Network;
import li.cil.oc.integration.SableTickBridge;
import li.cil.oc.integration.util.WirelessRedstone;
import li.cil.oc.server.component.RedstoneWireless;
import li.cil.oc.server.machine.Machine;
import li.cil.oc.common.blockentity.Robot;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.ExtendedLevelJava;
import li.cil.oc.util.SideTracker;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import li.cil.oc.server.component.Keyboard;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.WeakHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.PriorityQueue;

/** Queues work for the next server or client tick. */
final class ScheduledEvents {
    private record DelayedAction(long tick, Runnable action) {}

    private static final Set<Keyboard> keyboards = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<Robot> runningRobots = new HashSet<>();
    private static final Set<Machine> machines = new HashSet<>();
    private static final List<Runnable> serverActions = new ArrayList<>();
    private static final List<Runnable> clientActions = new ArrayList<>();
    private static final PriorityQueue<DelayedAction> delayedServerActions =
            new PriorityQueue<>(Comparator.comparingLong(DelayedAction::tick));
    private static volatile long serverTicks;

    private ScheduledEvents() {}

    static void addKeyboard(Keyboard keyboard) { keyboards.add(keyboard); }
    static void releaseKeyboardKeys(Player player) { keyboards.forEach(keyboard -> keyboard.releasePressedKeys(player)); }
    static void onRobotStart(Robot robot) { runningRobots.add(robot); }
    static void onRobotStopped(Robot robot) { runningRobots.remove(robot); }
    static void scheduleClose(Machine machine) { machines.add(machine); }
    static void unscheduleClose(Machine machine) { machines.remove(machine); }

    static void scheduleServer(BlockEntity tileEntity) {
        if (SideTracker.isServer()) scheduleServer(() -> Network.joinOrCreateNetwork(tileEntity));
    }

    static void scheduleServer(Runnable action) {
        synchronized (serverActions) {
            serverActions.add(action);
        }
    }

    static void scheduleServer(Runnable action, int delay) {
        synchronized (delayedServerActions) {
            delayedServerActions.add(new DelayedAction(serverTicks + Math.max(delay, 0), action));
        }
    }

    static void scheduleClient(Runnable action) {
        synchronized (clientActions) {
            clientActions.add(action);
        }
    }

    static void scheduleWirelessRedstone(RedstoneWireless receiver) {
        if (SideTracker.isServer()) {
            scheduleServer(() -> {
                if (receiver.node().network() != null) {
                    WirelessRedstone.addReceiver(receiver);
                    WirelessRedstone.updateOutput(receiver);
                }
            });
        }
    }

    static void onServerTickPre(ServerTickEvent.Pre event) {
        runPending(serverActions);
        serverTicks++;
        SableTickBridge.tick(event.getServer());
        while (true) {
            DelayedAction action;
            synchronized (delayedServerActions) {
                action = delayedServerActions.peek();
                if (action == null || action.tick() >= serverTicks) break;
                delayedServerActions.remove();
            }
            run(action.action());
        }
        var invalidRobots = new ArrayList<Robot>();
        for (Robot robot : runningRobots) {
            if (robot.isRemoved()) invalidRobots.add(robot);
            else if (robot.getEnvironmentLevel() != null) robot.machine().update();
        }
        runningRobots.removeAll(invalidRobots);
    }

    static void onServerTickPost() {
        var closedMachines = new ArrayList<Machine>();
        for (Machine machine : machines) {
            if (machine.tryClose()) {
                closedMachines.add(machine);
                var host = machine.host();
                var level = host.getEnvironmentLevel();
                if (level == null || !new ExtendedLevelJava(level).blockExists(BlockPositionsJava.apply(host))) {
                    if (machine.node() != null) machine.node().remove();
                }
            }
        }
        machines.removeAll(closedMachines);
    }

    static void onClientTickPre() {
        runPending(clientActions);
    }

    private static void runPending(List<Runnable> pending) {
        List<Runnable> actions;
        synchronized (pending) {
            actions = new ArrayList<>(pending);
            pending.clear();
        }
        actions.forEach(ScheduledEvents::run);
    }

    private static void run(Runnable action) {
        try {
            action.run();
        } catch (Throwable error) {
            OpenComputers$.MODULE$.log().warn("Error in scheduled tick action.", error);
        }
    }
}
