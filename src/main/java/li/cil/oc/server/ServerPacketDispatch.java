package li.cil.oc.server;

import li.cil.oc.common.PacketHandlerBase.PacketParser;
import java.io.IOException;

/** Routes parsed serverbound packets to their implementation handlers. */
public final class ServerPacketDispatch {
    private ServerPacketDispatch() {}

    public static void dispatch(PacketParser packet) {
        try {
            switch (packet.packetType) {
                case ComputerPower -> MachinePowerPacketHandlers.onComputerPower(packet);
                case CopyToAnalyzer -> TextBufferInputPacketHandlers.onCopyToAnalyzer(packet);
                case DriveLock -> ItemActionPacketHandlers.onDriveLock(packet);
                case DriveMode -> ItemActionPacketHandlers.onDriveMode(packet);
                case DronePower -> MachinePowerPacketHandlers.onDronePower(packet);
                case KeyDown -> TextBufferInputPacketHandlers.onKeyDown(packet);
                case KeyUp -> TextBufferInputPacketHandlers.onKeyUp(packet);
                case TextInput -> TextBufferInputPacketHandlers.onTextInput(packet);
                case Clipboard -> TextBufferInputPacketHandlers.onClipboard(packet);
                case MachineItemStateRequest -> ItemActionPacketHandlers.onMachineItemStateRequest(packet);
                case MouseClickOrDrag -> TextBufferInputPacketHandlers.onMouseClick(packet);
                case MouseScroll -> TextBufferInputPacketHandlers.onMouseScroll(packet);
                case MouseUp -> TextBufferInputPacketHandlers.onMouseUp(packet);
                case PetVisibility -> PetVisibilityPacketHandlers.onPetVisibility(packet);
                case RackMountableMapping -> RackPacketHandlers.onRackMountableMapping(packet);
                case RackRelayState -> RackPacketHandlers.onRackRelayState(packet);
                case RackKVMSelection -> RackPacketHandlers.onRackKVMSelection(packet);
                case RobotAssemblerStart -> ContainerActionPacketHandlers.onRobotAssemblerStart(packet);
                case RobotStateRequest -> WorldBlockEntityPacketHandlers.onRobotStateRequest(packet);
                case ServerPower -> MachinePowerPacketHandlers.onServerPower(packet);
                case TabletCursorTick -> ItemActionPacketHandlers.onTabletCursorTick(packet);
                case TapeDriveControl -> ContainerActionPacketHandlers.onTapeDriveControl(packet);
                case TextBufferInit -> TextBufferInputPacketHandlers.onTextBufferInit(packet);
                case WaypointLabel -> WorldBlockEntityPacketHandlers.onWaypointLabel(packet);
                case HoloScreenResize -> WorldBlockEntityPacketHandlers.onHoloScreenResize(packet);
                default -> { /* Ignore packet types that are not serverbound. */ }
            }
        } catch (IOException error) {
            ServerPacketDispatch.<RuntimeException>sneakyThrow(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable error) throws T {
        throw (T) error;
    }
}
