package li.cil.oc.server;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.Advancement;
import li.cil.oc.common.PacketHandlerBase.PacketParser;
import li.cil.oc.util.StackOption;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.IOException;

/** Server handlers for actions sent by the assembler and tape-drive menus. */
public final class ContainerActionPacketHandlers {
    private static final Marker SECURITY_MARKER = MarkerManager.getMarker("SuspiciousPackets");

    private ContainerActionPacketHandlers() {}

    public static void onTapeDriveControl(PacketParser packet) throws IOException {
        int containerId = packet.readInt();
        int action = packet.readByte();
        if (!(packet.player instanceof ServerPlayer player)) return;

        if (player.containerMenu.containerId == containerId && player.containerMenu.stillValid(player)) {
            if (player.containerMenu instanceof li.cil.oc.common.menu.TapeDrive menu) {
                if (menu.otherInventory() instanceof li.cil.oc.common.blockentity.TapeDrive drive) drive.handleControl(action, player);
                else logForgedPacket(player);
            } else logForgedPacket(player);
        } else logForgedPacket(player);
    }

    public static void onRobotAssemblerStart(PacketParser packet) throws IOException {
        int containerId = packet.readInt();
        if (packet.player.containerMenu instanceof li.cil.oc.common.menu.Assembler menu && menu.containerId == containerId &&
                menu.assembler() instanceof li.cil.oc.common.blockentity.Assembler assembler) {
            boolean finishImmediately = packet.player instanceof ServerPlayer player && player.isCreative();
            if (assembler.start(finishImmediately)) {
                StackOption output = assembler.output();
                if (!output.isEmpty()) Advancement.onAssemble(output.get(), packet.player);
            }
        }
    }

    private static void logForgedPacket(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            OpenComputers.log.warn(SECURITY_MARKER, "Player {} tried to send GUI packets without opening them", serverPlayer.getGameProfile());
        }
    }
}
