package li.cil.oc.integration.mekanism;

import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.common.blockentity.traits.PowerAcceptor;
import li.cil.oc.integration.util.Power;
import mekanism.api.Action;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class EventHandlerMekanism {
    private EventHandlerMekanism() {
    }

    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        for (final var entry : BlockEntityTypes.BLOCK_ENTITY_TYPES.getEntries()) {
            event.registerBlockEntity(
                    Capabilities.STRICT_ENERGY.block(),
                    (BlockEntityType<?>) entry.get(),
                    (blockEntity, side) -> blockEntity instanceof PowerAcceptor
                            ? new StrictEnergyStorage((PowerAcceptor) blockEntity, side)
                            : null);
        }
    }

    public static final class StrictEnergyStorage implements IStrictEnergyHandler {
        private final PowerAcceptor tile;
        private final Direction side;

        public StrictEnergyStorage(final PowerAcceptor tile, final Direction side) {
            this.tile = tile;
            this.side = side;
        }

        private Direction effectiveSide() {
            if (side != null) {
                return side;
            }
            for (final Direction direction : Direction.values()) {
                if (tile.canConnectPower(direction)) {
                    return direction;
                }
            }
            return Direction.NORTH;
        }

        private boolean connected() {
            return tile.canConnectPower(effectiveSide());
        }

        private boolean valid(final int container) {
            return container == 0 && connected();
        }

        @Override
        public int getEnergyContainerCount() {
            return connected() ? 1 : 0;
        }

        @Override
        public long getEnergy(final int container) {
            return valid(container) ? (long) Power.toJoules(tile.globalBuffer(effectiveSide())) : 0L;
        }

        @Override
        public void setEnergy(final int container, final long energy) {
        }

        @Override
        public long getMaxEnergy(final int container) {
            return valid(container) ? (long) Power.toJoules(tile.globalBufferSize(effectiveSide())) : 0L;
        }

        @Override
        public long getNeededEnergy(final int container) {
            return Math.max(0L, getMaxEnergy(container) - getEnergy(container));
        }

        @Override
        public long insertEnergy(final int container, final long amount, final Action action) {
            if (!valid(container) || amount <= 0L) {
                return amount;
            }
            final double accepted = tile.tryChangeBuffer(
                    effectiveSide(), Power.fromJoules((double) amount), action.execute());
            return Math.max(0L, amount - (long) Power.toJoules(accepted));
        }

        @Override
        public long extractEnergy(final int container, final long amount, final Action action) {
            return 0L;
        }
    }
}
