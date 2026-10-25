package li.cil.oc.common;

import li.cil.oc.common.blockentity.Robot;
import li.cil.oc.server.component.Keyboard;
import li.cil.oc.server.component.RedstoneWireless;
import li.cil.oc.server.machine.Machine;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import scala.Function0;
import scala.runtime.BoxedUnit;

import java.util.Calendar;

/** Scheduling and lifecycle entry points shared by Java and Scala callers. */
public final class EventHandler {
    private EventHandler() {}

    public static void onRobotStart(Robot robot) { ScheduledEvents.onRobotStart(robot); }
    public static void onRobotStopped(Robot robot) { ScheduledEvents.onRobotStopped(robot); }
    public static void addKeyboard(Keyboard keyboard) { ScheduledEvents.addKeyboard(keyboard); }
    public static void scheduleClose(Machine machine) { ScheduledEvents.scheduleClose(machine); }
    public static void unscheduleClose(Machine machine) { ScheduledEvents.unscheduleClose(machine); }
    public static void scheduleServer(BlockEntity tileEntity) { ScheduledEvents.scheduleServer(tileEntity); }

    public static void scheduleServer(Function0<?> action) {
        ScheduledEvents.scheduleServer(() -> action.apply());
    }

    public static void scheduleServer(Function0<?> action, int delay) {
        ScheduledEvents.scheduleServer(() -> action.apply(), delay);
    }

    public static void scheduleClientTask(Runnable action) { ScheduledEvents.scheduleClient(action); }

    public static void scheduleClient(Function0<?> action) {
        ScheduledEvents.scheduleClient(() -> action.apply());
    }

    public static void scheduleWirelessRedstone(RedstoneWireless receiver) {
        ScheduledEvents.scheduleWirelessRedstone(receiver);
    }

    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        EventHandlerHelper.registerCapabilities(event);
        li.cil.oc.integration.neoforge.EventHandlerNeoForge.onRegisterCapabilities(event);
    }

    public static boolean isItTime() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.MONTH) == Calendar.APRIL && now.get(Calendar.DAY_OF_MONTH) == 1;
    }
}
