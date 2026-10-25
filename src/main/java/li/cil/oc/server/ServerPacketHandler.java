package li.cil.oc.server;

import li.cil.oc.common.PacketHandlerBase;
import li.cil.oc.common.PacketHandlerBase.PacketParser;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/** Server-side packet parser and dispatcher. */
public class ServerPacketHandler extends PacketHandlerBase {
    @Override
    protected Optional<Level> world(Player player, ResourceLocation dimension) {
        return Optional.ofNullable(ServerLifecycleHooks.getCurrentServer()
                .getLevel(ResourceKey.create(Registries.DIMENSION, dimension)));
    }

    @Override
    public void dispatch(PacketParser packet) {
        ServerPacketDispatch.dispatch(packet);
    }

    @Override
    protected PacketParser createParser(InputStream stream, Player player) {
        try {
            return new PacketParser(stream, player, this);
        } catch (IOException error) {
            return ServerPacketHandler.<RuntimeException, PacketParser>sneakyThrow(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable, R> R sneakyThrow(Throwable error) throws T {
        throw (T) error;
    }
}
