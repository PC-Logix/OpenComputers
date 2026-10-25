package li.cil.oc.client;

import li.cil.oc.common.PacketHandlerBase.PacketParser;
import li.cil.oc.common.RobotFlags;
import li.cil.oc.common.blockentity.Assembler;
import li.cil.oc.common.blockentity.Adapter;
import li.cil.oc.common.blockentity.Charger;
import li.cil.oc.common.blockentity.Disassembler;
import li.cil.oc.common.blockentity.DiskDrive;
import li.cil.oc.common.blockentity.NetSplitter;
import li.cil.oc.common.blockentity.Printer;
import li.cil.oc.common.blockentity.Raid;
import li.cil.oc.common.blockentity.Relay;
import li.cil.oc.common.blockentity.Robot;
import li.cil.oc.common.blockentity.RobotProxy;
import li.cil.oc.common.blockentity.Transposer;
import li.cil.oc.common.blockentity.traits.Colored;
import li.cil.oc.common.blockentity.traits.Computer;
import li.cil.oc.common.blockentity.traits.PowerInformation;
import li.cil.oc.common.blockentity.traits.RedstoneAware;
import li.cil.oc.common.blockentity.traits.Rotatable;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import li.cil.oc.common.blockentity.Waypoint;
import li.cil.oc.util.BlockPositionsJava;
import net.minecraft.world.level.block.entity.BlockEntity;
import li.cil.oc.util.ExtendedLevelJava;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Applies clientbound machine activity and status updates. */
public final class ClientDeviceStatePacketHandlers {
    private ClientDeviceStatePacketHandlers() {}

    public static void robotAnimateSwing(PacketParser packet) throws IOException {
        Optional<RobotProxy> proxy = packet.readBlockEntityOptional(RobotProxy.class);
        if (proxy.isPresent()) proxy.get().robot().setAnimateSwing(packet.readInt());
    }

    public static void robotAnimateTurn(PacketParser packet) throws IOException {
        Optional<RobotProxy> proxy = packet.readBlockEntityOptional(RobotProxy.class);
        if (proxy.isPresent()) proxy.get().robot().setAnimateTurn(packet.readByte(), packet.readInt());
    }

    public static void robotAssemblingState(PacketParser packet) throws IOException {
        Optional<Assembler> assembler = packet.readBlockEntityOptional(Assembler.class);
        if (assembler.isPresent()) assembler.get().requiredEnergy_$eq(packet.readBoolean() ? 9001 : 0);
    }

    public static void robotInventoryChange(PacketParser packet) throws IOException {
        Optional<RobotProxy> proxy = packet.readBlockEntityOptional(RobotProxy.class);
        if (proxy.isPresent()) {
            Robot robot = proxy.get().robot();
            int slot = packet.readInt();
            var stack = packet.readItemStack();
            if (slot >= robot.getContainerSize() - robot.componentCount()) {
                robot.info().components()[slot - (robot.getContainerSize() - robot.componentCount())] = stack;
            } else {
                robot.setItem(slot, stack);
            }
        }
    }

    public static void robotLightChange(PacketParser packet) throws IOException {
        Optional<RobotProxy> proxy = packet.readBlockEntityOptional(RobotProxy.class);
        if (proxy.isPresent()) proxy.get().robot().info().lightColor_$eq(packet.readInt());
    }

    public static void robotFlagChange(PacketParser packet) throws IOException {
        Optional<RobotProxy> proxy = packet.readBlockEntityOptional(RobotProxy.class);
        if (proxy.isPresent()) {
            ResourceLocation id = ResourceLocation.tryParse(packet.readUTF());
            Optional<ResourceLocation> flag = RobotFlags.byIdOptional(id).map(RobotFlags.Flag::id);
            proxy.get().robot().info().setFlag(flag);
        }
    }

    public static void robotNameChange(PacketParser packet) throws IOException {
        Optional<RobotProxy> proxy = packet.readBlockEntityOptional(RobotProxy.class);
        if (proxy.isPresent()) {
            short length = packet.readShort();
            char[] name = new char[length];
            for (int index = 0; index < length; index++) name[index] = packet.readChar();
            proxy.get().robot().setName(new String(name));
        }
    }

    public static void robotSelectedSlotChange(PacketParser packet) throws IOException {
        Optional<RobotProxy> proxy = packet.readBlockEntityOptional(RobotProxy.class);
        if (proxy.isPresent()) proxy.get().robot().selectedSlot_$eq(packet.readInt());
    }

    public static Object robotMove(PacketParser packet) throws IOException {
        ResourceLocation dimension = ResourceLocation.tryParse(packet.readUTF());
        int x = packet.readInt();
        int y = packet.readInt();
        int z = packet.readInt();
        Optional<Direction> direction = packet.readDirectionOptional();
        Optional<RobotProxy> proxy = packet.getBlockEntityOptional(dimension, x, y, z, RobotProxy.class);
        if (direction.isPresent()) {
            if (proxy.isPresent()) return proxy.get().robot().move(direction.get());
            PacketSender.sendRobotStateRequest(dimension,
                x + direction.get().getStepX(), y + direction.get().getStepY(), z + direction.get().getStepZ());
        }
        return scala.runtime.BoxedUnit.UNIT;
    }

    public static void adapterState(PacketParser packet) throws IOException {
        Optional<Adapter> adapter = packet.readBlockEntityOptional(Adapter.class);
        if (adapter.isPresent()) {
            Adapter target = adapter.get();
            target.openSides_$eq(target.uncompressSides(packet.readByte()));
            new ExtendedLevelJava(target.getEnvironmentLevel()).notifyBlockUpdate(target.getBlockPos());
        }
    }

    public static void colorChange(PacketParser packet) throws IOException {
        Optional<Colored> colored = packet.readBlockEntityOptional(Colored.class);
        if (colored.isPresent()) {
            Colored target = colored.get();
            target.setColor(packet.readInt());
            BlockEntity blockEntity = (BlockEntity) target;
            new ExtendedLevelJava(blockEntity.getLevel()).notifyBlockUpdate(
                BlockPositionsJava.apply(blockEntity.getBlockPos(), blockEntity.getLevel()));
        }
    }

    public static void computerState(PacketParser packet) throws IOException {
        Optional<Computer> computer = packet.readBlockEntityOptional(Computer.class);
        if (computer.isPresent()) {
            Computer target = computer.get();
            target.setRunning(packet.readBoolean());
            target.hasErrored_$eq(packet.readBoolean());
        }
    }

    public static void computerUserList(PacketParser packet) throws IOException {
        Optional<Computer> computer = packet.readBlockEntityOptional(Computer.class);
        if (computer.isPresent()) {
            int count = packet.readInt();
            List<String> users = new ArrayList<>(Math.max(0, count));
            for (int index = 0; index < count; index++) users.add(packet.readUTF());
            computer.get().setUsersJava(users);
        }
    }

    public static void netSplitterState(PacketParser packet) throws IOException {
        Optional<NetSplitter> splitter = packet.readBlockEntityOptional(NetSplitter.class);
        if (splitter.isPresent()) {
            NetSplitter target = splitter.get();
            target.isInverted_$eq(packet.readBoolean());
            target.openSides_$eq(target.uncompressSides(packet.readByte()));
            new ExtendedLevelJava(target.getEnvironmentLevel()).notifyBlockUpdate(target.getBlockPos());
        }
    }

    public static void powerState(PacketParser packet) throws IOException {
        Optional<PowerInformation> information = packet.readBlockEntityOptional(PowerInformation.class);
        if (information.isPresent()) {
            PowerInformation target = information.get();
            target.globalBuffer_$eq(packet.readDouble());
            target.globalBufferSize_$eq(packet.readDouble());
        }
    }

    public static void redstoneState(PacketParser packet) throws IOException {
        Optional<RedstoneAware> redstone = packet.readBlockEntityOptional(RedstoneAware.class);
        if (redstone.isPresent()) {
            RedstoneAware target = redstone.get();
            target.setOutputEnabled(packet.readBoolean());
            for (Direction direction : Direction.values()) {
                target.setOutput(direction, packet.readByte());
            }
        }
    }

    public static void rotatableState(PacketParser packet) throws IOException {
        Optional<Rotatable> rotatable = packet.readBlockEntityOptional(Rotatable.class);
        if (rotatable.isPresent()) {
            Rotatable target = rotatable.get();
            target.pitch_$eq(packet.readDirectionOptional().get());
            target.yaw_$eq(packet.readDirectionOptional().get());
        }
    }

    public static void chargerState(PacketParser packet) throws IOException {
        Optional<Charger> charger = packet.readBlockEntityOptional(Charger.class);
        if (charger.isPresent()) {
            Charger target = charger.get();
            target.chargeSpeed_$eq(packet.readDouble());
            target.hasPower_$eq(packet.readBoolean());
            new ExtendedLevelJava(target.getEnvironmentLevel()).notifyBlockUpdate(target.position());
        }
    }

    public static void disassemblerActive(PacketParser packet) throws IOException {
        Optional<Disassembler> disassembler = packet.readBlockEntityOptional(Disassembler.class);
        if (disassembler.isPresent()) disassembler.get().isActive_$eq(packet.readBoolean());
    }

    public static void floppyChange(PacketParser packet) throws IOException {
        Optional<DiskDrive> diskDrive = packet.readBlockEntityOptional(DiskDrive.class);
        if (diskDrive.isPresent()) diskDrive.get().setItem(0, packet.readItemStack());
    }

    public static void printerState(PacketParser packet) throws IOException {
        Optional<Printer> printer = packet.readBlockEntityOptional(Printer.class);
        if (printer.isPresent()) printer.get().requiredEnergy_$eq(packet.readBoolean() ? 9001 : 0);
    }

    public static void raidStateChange(PacketParser packet) throws IOException {
        Optional<Raid> raid = packet.readBlockEntityOptional(Raid.class);
        if (raid.isPresent()) {
            Raid target = raid.get();
            boolean[] presence = target.presence();
            for (int slot = 0; slot < target.getContainerSize(); slot++) {
                presence[slot] = packet.readBoolean();
            }
        }
    }

    public static void switchActivity(PacketParser packet) throws IOException {
        Optional<Relay> relay = packet.readBlockEntityOptional(Relay.class);
        if (relay.isPresent()) relay.get().lastMessage_$eq(System.currentTimeMillis());
    }

    public static void transposerActivity(PacketParser packet) throws IOException {
        Optional<Transposer> transposer = packet.readBlockEntityOptional(Transposer.class);
        if (transposer.isPresent()) transposer.get().lastOperation_$eq(System.currentTimeMillis());
    }

    public static void waypointLabel(PacketParser packet) throws IOException {
        Optional<Waypoint> waypoint = packet.readBlockEntityOptional(Waypoint.class);
        if (waypoint.isPresent()) waypoint.get().label_$eq(packet.readUTF());
    }
}
