package li.cil.oc.client;

import li.cil.oc.common.PacketHandlerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/** Java entry point for client packet parsing and routing. */
public final class PacketHandler extends PacketHandlerBase {
    public static final PacketHandler INSTANCE = new PacketHandler();

    private PacketHandler() {}

    public static void update() { ClientPacketHandlerJava.update(); }
    public static void stopAllAudio() { ClientPacketHandlerJava.stopAllAudio(); }
    public static void clearPendingProjectorFrames() { ClientPacketHandlerJava.clearPendingProjectorFrames(); }

    @Override protected Optional<Level> world(Player player, ResourceLocation dimension) {
        return ClientPacketHandlerJava.worldOptional(player, dimension);
    }
    @Override public void dispatch(PacketParser parser) { ClientPacketHandlerJava.dispatch(parser); }
    @Override protected PacketParser createParser(InputStream stream, Player player) {
        try {
            return new PacketParser(stream, Minecraft.getInstance().player, this);
        } catch (IOException error) {
            throw unchecked(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException unchecked(Throwable error) throws T { throw (T) error; }
}
