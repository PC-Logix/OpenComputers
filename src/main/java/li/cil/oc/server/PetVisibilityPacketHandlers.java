package li.cil.oc.server;

import li.cil.oc.common.PacketHandlerBase.PacketParser;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.Optional;

/** Server handler for player pet visibility changes. */
public final class PetVisibilityPacketHandlers {
    private PetVisibilityPacketHandlers() {}

    public static void onPetVisibility(PacketParser packet) throws IOException {
        boolean visible = packet.readBoolean();
        if (packet.player instanceof ServerPlayer player) {
            String name = player.getName().getString();
            if (PetVisibility.setHidden(name, !visible)) {
                PetVisibilityPacketSender.send(Optional.of(name), Optional.empty());
            }
        }
    }
}
