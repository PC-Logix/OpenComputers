package li.cil.oc.client;

import li.cil.oc.common.PacketHandlerBase.PacketParser;
import li.cil.oc.common.blockentity.Hologram;
import li.cil.oc.common.blockentity.Projector;
import li.cil.oc.common.blockentity.Screen;
import net.minecraft.world.phys.Vec3;
import java.io.IOException;
import java.util.Optional;

/** Applies clientbound hologram and projector state updates. */
public final class ClientBlockStatePacketHandlers {
    private ClientBlockStatePacketHandlers() {}

    public static void hologramClear(PacketParser packet) throws IOException {
        Optional<Hologram> hologram = packet.readBlockEntityOptional(Hologram.class);
        if (hologram.isPresent()) {
            Hologram target = hologram.get();
            int[] volume = target.volume();
            for (int index = 0; index < volume.length; index++) volume[index] = 0;
            target.needsRendering_$eq(true);
        }
    }

    public static void hologramColor(PacketParser packet) throws IOException {
        Optional<Hologram> hologram = packet.readBlockEntityOptional(Hologram.class);
        if (hologram.isPresent()) {
            Hologram target = hologram.get();
            int index = packet.readInt();
            int value = packet.readInt();
            target.colors()[index] = value & 0xFFFFFF;
            target.needsRendering_$eq(true);
        }
    }

    public static void hologramPower(PacketParser packet) throws IOException {
        Optional<Hologram> hologram = packet.readBlockEntityOptional(Hologram.class);
        if (hologram.isPresent()) hologram.get().hasPower_$eq(packet.readBoolean());
    }

    public static void hologramScale(PacketParser packet) throws IOException {
        Optional<Hologram> hologram = packet.readBlockEntityOptional(Hologram.class);
        if (hologram.isPresent()) hologram.get().scale_$eq(packet.readDouble());
    }

    public static void hologramArea(PacketParser packet) throws IOException {
        Optional<Hologram> hologram = packet.readBlockEntityOptional(Hologram.class);
        if (hologram.isPresent()) {
            Hologram target = hologram.get();
            int fromX = packet.readByte();
            int untilX = packet.readByte();
            int fromZ = packet.readByte();
            int untilZ = packet.readByte();
            int[] volume = target.volume();
            int width = target.width();
            for (int x = fromX; x < untilX; x++) {
                for (int z = fromZ; z < untilZ; z++) {
                    volume[x + z * width] = packet.readInt();
                    volume[x + z * width + width * width] = packet.readInt();
                }
            }
            target.needsRendering_$eq(true);
        }
    }

    public static void hologramValues(PacketParser packet) throws IOException {
        Optional<Hologram> hologram = packet.readBlockEntityOptional(Hologram.class);
        if (hologram.isPresent()) {
            Hologram target = hologram.get();
            int count = packet.readInt();
            int width = target.width();
            int[] volume = target.volume();
            for (int index = 0; index < count; index++) {
                short xz = packet.readShort();
                int x = (byte) (xz >> 8);
                int z = (byte) xz;
                volume[x + z * width] = packet.readInt();
                volume[x + z * width + width * width] = packet.readInt();
            }
            target.needsRendering_$eq(true);
        }
    }

    public static void projectorPower(PacketParser packet) throws IOException {
        Optional<Projector> projector = packet.readBlockEntityOptional(Projector.class);
        if (projector.isPresent()) {
            Projector target = projector.get();
            target.isOn_$eq(packet.readBoolean());
            target.hasPower_$eq(packet.readBoolean());
        }
    }

    public static void hologramTranslation(PacketParser packet) throws IOException {
        Optional<Hologram> hologram = packet.readBlockEntityOptional(Hologram.class);
        if (hologram.isPresent()) {
            double x = packet.readDouble();
            double y = packet.readDouble();
            double z = packet.readDouble();
            hologram.get().translation_$eq(new Vec3(x, y, z));
        }
    }

    public static void hologramRotation(PacketParser packet) throws IOException {
        Optional<Hologram> hologram = packet.readBlockEntityOptional(Hologram.class);
        if (hologram.isPresent()) {
            Hologram target = hologram.get();
            target.rotationAngle_$eq(packet.readFloat());
            target.rotationX_$eq(packet.readFloat());
            target.rotationY_$eq(packet.readFloat());
            target.rotationZ_$eq(packet.readFloat());
        }
    }

    public static void hologramRotationSpeed(PacketParser packet) throws IOException {
        Optional<Hologram> hologram = packet.readBlockEntityOptional(Hologram.class);
        if (hologram.isPresent()) {
            Hologram target = hologram.get();
            target.rotationSpeed_$eq(packet.readFloat());
            target.rotationSpeedX_$eq(packet.readFloat());
            target.rotationSpeedY_$eq(packet.readFloat());
            target.rotationSpeedZ_$eq(packet.readFloat());
        }
    }

    public static void screenTouchMode(PacketParser packet) throws IOException {
        Optional<Screen> screen = packet.readBlockEntityOptional(Screen.class);
        if (screen.isPresent()) screen.get().invertTouchMode_$eq(packet.readBoolean());
    }
}
