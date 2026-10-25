package li.cil.oc.server;

import li.cil.oc.Localization;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.common.PacketHandlerBase.PacketParser;
import li.cil.oc.common.blockentity.traits.Computer;
import li.cil.oc.common.entity.DroneInventory;
import li.cil.oc.common.item.TabletWrapper;
import li.cil.oc.common.menu.AbstractMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.IOException;

/** Server handlers for computer, rack server, and drone power controls. */
public final class MachinePowerPacketHandlers {
    private static final Marker SECURITY_MARKER = MarkerManager.getMarker("SuspiciousPackets");

    private MachinePowerPacketHandlers() {}

    public static void onComputerPower(PacketParser packet) throws IOException {
        int containerId = packet.readInt();
        boolean setPower = packet.readBoolean();
        if (!(packet.player instanceof ServerPlayer player)) return;

        AbstractMenu menu = player.containerMenu instanceof AbstractMenu abstractMenu ? abstractMenu : null;
        if (menu instanceof li.cil.oc.common.menu.Case computer && computer.containerId == containerId) {
            if (computer.otherInventory() instanceof Computer host) trySetComputerPower(host.machine(), setPower, player);
            else logForgedPacket(player);
        } else if (menu instanceof li.cil.oc.common.menu.Robot robot && robot.containerId == containerId) {
            if (robot.otherInventory() instanceof Computer host) trySetComputerPower(host.machine(), setPower, player);
            else logForgedPacket(player);
        } else if (menu instanceof li.cil.oc.common.menu.Tablet tablet && tablet.containerId == containerId) {
            if (tablet.otherInventory() instanceof TabletWrapper wrapper) {
                trySetComputerPower(wrapper.machine(), setPower, player);
                wrapper.syncRunningState();
                player.containerMenu.broadcastChanges();
            } else logForgedPacket(player);
        } else logForgedPacket(player);
    }

    public static void onServerPower(PacketParser packet) throws IOException {
        int containerId = packet.readInt();
        int index = packet.readInt();
        boolean setPower = packet.readBoolean();
        if (!(packet.player instanceof ServerPlayer player)) return;

        if (player.containerMenu instanceof li.cil.oc.common.menu.Server menu && menu.containerId == containerId) {
            if (menu.otherInventory() instanceof li.cil.oc.server.component.Server component) {
                if (component.rack() != null && component.rack().getMountable(index) == component)
                    trySetComputerPower(component.machine(), setPower, player);
                // Otherwise the client may simply be lagging behind.
            } else logForgedPacket(player);
        } else logForgedPacket(player);
    }

    public static void onDronePower(PacketParser packet) throws IOException {
        int containerId = packet.readInt();
        boolean power = packet.readBoolean();
        if (!(packet.player instanceof ServerPlayer player)) return;

        if (player.containerMenu instanceof li.cil.oc.common.menu.Drone menu && menu.containerId == containerId) {
            if (menu.otherInventory() instanceof DroneInventory inventory) trySetDronePower(inventory.drone(), power, player);
            else logForgedPacket(player);
        } else logForgedPacket(player);
    }

    private static void trySetComputerPower(Machine computer, boolean value, ServerPlayer player) {
        if (!computer.canInteract(player.getName().getString())) return;
        if (!value) {
            computer.stop();
        } else if (!computer.isPaused()) {
            computer.start();
            String message = computer.lastError();
            if (message != null) player.sendSystemMessage(Localization.Analyzer.LastError(message));
        }
    }

    private static void trySetDronePower(li.cil.oc.common.entity.Drone drone, boolean value, ServerPlayer player) {
        Machine computer = drone.machine;
        if (!computer.canInteract(player.getName().getString())) return;
        if (!value) {
            drone.stop();
        } else if (!computer.isPaused()) {
            drone.start();
            String message = computer.lastError();
            if (message != null) player.sendSystemMessage(Localization.Analyzer.LastError(message));
        }
    }

    private static void logForgedPacket(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            OpenComputers.log.warn(SECURITY_MARKER, "Player {} tried to send GUI packets without opening them", serverPlayer.getGameProfile());
        }
    }
}
