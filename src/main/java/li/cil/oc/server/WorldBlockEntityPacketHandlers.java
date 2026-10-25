package li.cil.oc.server;

import li.cil.oc.common.PacketHandlerBase.PacketParser;
import li.cil.oc.common.blockentity.HoloScreen;
import li.cil.oc.common.blockentity.RobotProxy;
import li.cil.oc.common.blockentity.Waypoint;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import java.io.IOException;
import java.util.Optional;

/** Server handlers for spatial block-entity update and interaction packets. */
public final class WorldBlockEntityPacketHandlers {
    private WorldBlockEntityPacketHandlers() {}

    public static void onHoloScreenResize(PacketParser packet) throws IOException {
        Optional<HoloScreen> screen = packet.readBlockEntityOptional(HoloScreen.class);
        Optional<Direction> side = packet.readDirectionOptional();
        if (screen.isEmpty() || side.isEmpty() || !(packet.player instanceof ServerPlayer player)) return;

        HoloScreen holo = screen.get();
        if (player.distanceToSqr(holo.x() + 0.5, holo.y() + 0.5, holo.z() + 0.5) <= 64 && holo.resize(side.get())) {
            holo.getLevel().sendBlockUpdated(holo.getBlockPos(), holo.getBlockState(), holo.getBlockState(), 3);
        }
    }

    public static void onRobotStateRequest(PacketParser packet) throws IOException {
        Optional<RobotProxy> proxy = packet.readBlockEntityOptional(RobotProxy.class);
        if (proxy.isEmpty()) return;

        RobotProxy robot = proxy.get();
        robot.getEnvironmentLevel().sendBlockUpdated(robot.getBlockPos(),
            robot.getEnvironmentLevel().getBlockState(robot.getBlockPos()),
            robot.getEnvironmentLevel().getBlockState(robot.getBlockPos()), 3);
    }

    public static void onWaypointLabel(PacketParser packet) throws IOException {
        Optional<Waypoint> entity = packet.readBlockEntityOptional(Waypoint.class);
        String label = packet.readUTF();
        label = label.substring(0, Math.min(label.length(), 32));
        if (entity.isEmpty() || !(packet.player instanceof ServerPlayer player)) return;

        Waypoint waypoint = entity.get();
        if (player.distanceToSqr(waypoint.x() + 0.5, waypoint.y() + 0.5, waypoint.z() + 0.5) <= 64 && !label.equals(waypoint.label())) {
            waypoint.label_$eq(label);
            waypoint.setChanged();
            ActivityPacketSender.sendWaypointLabel(waypoint);
        }
    }
}
