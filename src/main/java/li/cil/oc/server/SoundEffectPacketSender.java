package li.cil.oc.server;

import li.cil.oc.Settings;
import li.cil.oc.common.PacketType;
import li.cil.oc.common.SimplePacketBuilder;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import scala.Option$;

import java.io.IOException;

public final class SoundEffectPacketSender {
    private SoundEffectPacketSender() {}

    public static void sendEffect(Level level, double x, double y, double z,
                                  ResourceLocation sound, SoundSource category, double range) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.SoundEffect);
        packet.writeUTF(level.dimension().location().toString());
        packet.writeDouble(x);
        packet.writeDouble(y);
        packet.writeDouble(z);
        packet.writeUTF(sound.toString());
        packet.writeByte(category.ordinal());
        packet.writeFloat((float) range);
        packet.sendToNearbyPlayers(level, x, y, z, Option$.MODULE$.apply(range));
    }

    public static void sendTone(Level level, double x, double y, double z, int frequency, int duration) throws IOException {
        BlockPosition position = BlockPositionsJava.apply(x, y, z);
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.Sound);
        packet.writeUTF(level.dimension().location().toString());
        packet.writeInt(position.x());
        packet.writeInt(position.y());
        packet.writeInt(position.z());
        packet.writeShort((short) frequency);
        packet.writeShort((short) duration);
        packet.sendToNearbyPlayers(level, x, y, z,
                Option$.MODULE$.apply(Settings.get().maxNetworkClientSoundPacketDistance()));
    }

    public static void sendPattern(Level level, double x, double y, double z, String pattern) throws IOException {
        BlockPosition position = BlockPositionsJava.apply(x, y, z);
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.SoundPattern);
        packet.writeUTF(level.dimension().location().toString());
        packet.writeInt(position.x());
        packet.writeInt(position.y());
        packet.writeInt(position.z());
        packet.writeUTF(pattern);
        packet.sendToNearbyPlayers(level, x, y, z,
                Option$.MODULE$.apply(Settings.get().maxNetworkClientSoundPacketDistance()));
    }

    public static void sendEffectUnchecked(Level level, double x, double y, double z,
                                           ResourceLocation sound, SoundSource category, double range) {
        try {
            sendEffect(level, x, y, z, sound, category, range);
        } catch (IOException error) {
            SoundEffectPacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    public static void sendToneUnchecked(Level level, double x, double y, double z, int frequency, int duration) {
        try {
            sendTone(level, x, y, z, frequency, duration);
        } catch (IOException error) {
            SoundEffectPacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    public static void sendPatternUnchecked(Level level, double x, double y, double z, String pattern) {
        try {
            sendPattern(level, x, y, z, pattern);
        } catch (IOException error) {
            SoundEffectPacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwUnchecked(Throwable error) throws E {
        throw (E) error;
    }
}
