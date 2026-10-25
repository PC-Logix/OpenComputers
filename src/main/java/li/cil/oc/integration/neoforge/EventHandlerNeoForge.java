package li.cil.oc.integration.neoforge;

import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.common.blockentity.traits.PowerAcceptor;
import li.cil.oc.integration.util.Power;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class EventHandlerNeoForge {
    private EventHandlerNeoForge() {}

    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        BlockEntityType<?>[] types = {
                BlockEntityTypes.ADAPTER.get(), BlockEntityTypes.ASSEMBLER.get(), BlockEntityTypes.CABLE.get(),
                BlockEntityTypes.CAPACITOR.get(), BlockEntityTypes.CARPETED_CAPACITOR.get(), BlockEntityTypes.CASE.get(),
                BlockEntityTypes.CHARGER.get(), BlockEntityTypes.DISASSEMBLER.get(), BlockEntityTypes.DISK_DRIVE.get(),
                BlockEntityTypes.GEOLYZER.get(), BlockEntityTypes.HOLOGRAM.get(), BlockEntityTypes.KEYBOARD.get(),
                BlockEntityTypes.MICROCONTROLLER.get(), BlockEntityTypes.MOTION_SENSOR.get(), BlockEntityTypes.NET_SPLITTER.get(),
                BlockEntityTypes.POWER_CONVERTER.get(), BlockEntityTypes.POWER_DISTRIBUTOR.get(), BlockEntityTypes.PRINT.get(),
                BlockEntityTypes.PRINTER.get(), BlockEntityTypes.RACK.get(), BlockEntityTypes.RAID.get(),
                BlockEntityTypes.REDSTONE_IO.get(), BlockEntityTypes.RELAY.get(), BlockEntityTypes.ROBOT.get(),
                BlockEntityTypes.SCREEN.get(), BlockEntityTypes.TRANSPOSER.get(), BlockEntityTypes.WAYPOINT.get()
        };
        for (var type : types) {
            // Expose every side and let canReceive decide dynamically, so consumers can cache the capability.
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type,
                    (blockEntity, side) -> blockEntity instanceof PowerAcceptor acceptor
                            ? new EnergyStorageImpl(acceptor, side) : null);
        }
    }

    public static boolean canCharge(ItemStack stack) {
        var storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return storage != null && storage.canReceive();
    }

    public static double charge(ItemStack stack, double amount, boolean simulate) {
        var storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return storage == null ? amount : amount - Power.fromRF(storage.receiveEnergy(Power.toRF(amount), simulate));
    }

    public static final class EnergyStorageImpl implements IEnergyStorage {
        private final PowerAcceptor tile;
        private final Direction side;

        public EnergyStorageImpl(PowerAcceptor tile, Direction side) {
            this.tile = tile;
            this.side = side;
        }

        @Override
        public int getEnergyStored() { return Power.toRF(tile.globalBuffer(side)); }

        @Override
        public int getMaxEnergyStored() { return Power.toRF(tile.globalBufferSize(side)); }

        @Override
        public boolean canReceive() { return tile.canConnectPower(side); }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return Power.toRF(tile.tryChangeBuffer(side, Power.fromRF(maxReceive), !simulate));
        }

        @Override
        public boolean canExtract() { return false; }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
    }
}
