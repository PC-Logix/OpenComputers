package li.cil.oc.server.agent;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

public final class FakeNetworkManager extends Connection {
    public static final FakeNetworkManager INSTANCE = new FakeNetworkManager();

    private FakeNetworkManager() {
        super(PacketFlow.CLIENTBOUND);
    }

    @Override
    public void send(Packet<?> packet) {}

    @Override
    public void send(Packet<?> packet, PacketSendListener listener) {}
}
