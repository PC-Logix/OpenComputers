package li.cil.oc.server;

import li.cil.oc.common.PacketType;
import li.cil.oc.common.SimplePacketBuilder;
import li.cil.oc.common.blockentity.Assembler;
import li.cil.oc.common.blockentity.Robot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import scala.Option$;

import java.io.IOException;

public final class RobotPacketSender {
    private RobotPacketSender() {}

    public static void sendAssembling(Assembler assembler, boolean assembling) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.RobotAssemblingState);
        packet.writeTileEntity(assembler);
        packet.writeBoolean(assembling);
        packet.sendToPlayersNearHost(assembler, packet.sendToPlayersNearHost$default$2());
    }

    public static void sendMove(Robot robot, BlockPos position, Direction direction) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.RobotMove);
        packet.writeUTF(robot.getEnvironmentLevel().dimension().location().toString());
        packet.writeInt(position.getX());
        packet.writeInt(position.getY());
        packet.writeInt(position.getZ());
        packet.writeByte(direction == null ? -1 : direction.ordinal());
        packet.sendToPlayersNearTileEntity(robot, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendAnimateSwing(Robot robot, double range) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.RobotAnimateSwing);
        packet.writeTileEntity(robot.proxy());
        packet.writeInt(robot.animationTicksTotal());
        packet.sendToPlayersNearTileEntity(robot, Option$.MODULE$.apply(range));
    }

    public static void sendAnimateTurn(Robot robot, double range) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.RobotAnimateTurn);
        packet.writeTileEntity(robot.proxy());
        packet.writeByte(robot.turnAxis());
        packet.writeInt(robot.animationTicksTotal());
        packet.sendToPlayersNearTileEntity(robot, Option$.MODULE$.apply(range));
    }

    public static void sendInventory(Robot robot, int slot, ItemStack stack) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.RobotInventoryChange);
        packet.writeTileEntity(robot.proxy());
        packet.writeInt(slot);
        packet.writeItemStack(stack, robot.getLevel().registryAccess());
        packet.sendToPlayersNearTileEntity(robot, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendLightChange(Robot robot) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.RobotLightChange);
        packet.writeTileEntity(robot.proxy());
        packet.writeInt(robot.info().lightColor());
        packet.sendToPlayersNearTileEntity(robot, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendFlagChange(Robot robot) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.RobotFlagChange);
        packet.writeTileEntity(robot.proxy());
        var flag = robot.info().flag();
        packet.writeUTF(flag.isDefined() ? flag.get().toString() : "");
        packet.sendToPlayersNearTileEntity(robot, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendNameChange(Robot robot) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.RobotNameChange);
        packet.writeTileEntity(robot.proxy());
        String name = robot.name();
        short length = (short) name.length();
        packet.writeShort(length);
        for (int i = 0; i < length; i++) packet.writeChar(name.charAt(i));
        packet.sendToPlayersNearTileEntity(robot, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendSelectedSlotChange(Robot robot, double range) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.RobotSelectedSlotChange);
        packet.writeTileEntity(robot.proxy());
        packet.writeInt(robot.selectedSlot());
        packet.sendToPlayersNearTileEntity(robot, Option$.MODULE$.apply(range));
    }

    public static void sendSelectedSlotChangeUnchecked(Robot robot, double range) {
        try {
            sendSelectedSlotChange(robot, range);
        } catch (IOException error) {
            RobotPacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwUnchecked(Throwable error) throws E {
        throw (E) error;
    }
}
