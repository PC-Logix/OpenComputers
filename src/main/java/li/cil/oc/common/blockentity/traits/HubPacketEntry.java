package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.network.Packet;
import net.minecraft.core.Direction;

import java.util.Optional;

/** A queued relay packet and the side from which it arrived, if any. */
public record HubPacketEntry(Direction sourceSide, Packet packet) {
    public Optional<Direction> sourceSideOptional() { return Optional.ofNullable(sourceSide); }
}
