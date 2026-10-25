package li.cil.oc.server;

import li.cil.oc.common.PacketType;
import li.cil.oc.common.SimplePacketBuilder;
import net.minecraft.server.level.ServerPlayer;
import java.io.IOException;
import java.util.Optional;

public final class PetVisibilityPacketSender {
    private PetVisibilityPacketSender() {}

    public static void send(Optional<String> name, Optional<ServerPlayer> player) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.PetVisibility);
        if (name.isPresent()) {
            String visibleName = name.get();
            packet.writeInt(1);
            packet.writeUTF(visibleName);
            packet.writeBoolean(!PetVisibility.hidden.contains(visibleName));
        } else {
            packet.writeInt(PetVisibility.hidden.size());
            for (String hiddenName : PetVisibility.hidden) {
                packet.writeUTF(hiddenName);
                packet.writeBoolean(false);
            }
        }

        if (player.isPresent()) packet.sendToPlayer(player.get());
        else packet.sendToAllPlayers();
    }
}
